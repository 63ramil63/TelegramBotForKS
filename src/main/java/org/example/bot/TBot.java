package org.example.bot;

import org.example.bot.ban.types.BanType;
import org.example.bot.ban.types.ban.info.BanInfo;
import org.example.bot.config.BotConfig;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.bot.response.CallbackResponseHandler;
import org.example.bot.response.TextResponseHandler;
import org.example.controller.FilesAndFoldersController;
import org.example.controller.LinksAndGroupsController;
import org.example.controller.UserBansController;
import org.example.controller.UserController;
import org.example.database.repository.DeletionLogRepository;
import org.example.files.FilesController;
import org.example.files.exception.FileSizeException;
import org.example.files.exception.IncorrectExtensionException;
import org.example.files.exception.InvalidCallbackDataException;
import org.example.role.AdminRole;
import org.example.schedule.ScheduleCache;
import org.example.utility.LinkUtil;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class TBot extends TelegramLongPollingBot {

    private ExecutorService executorService;
    private TextResponseHandler textResponseHandler;
    private CallbackResponseHandler callbackResponseHandler;
    private FilesController filesController;
    private ScheduleCache scheduleCache;
    private UserController userController;
    private UserBansController userBansController;
    private MarkupSetter markupSetter;
    private FilesAndFoldersController filesAndFoldersController;
    private LinksAndGroupsController linksAndGroupsController;

    private final StringBuilder notification = new StringBuilder("Нет каких либо оповещений");

    public TBot() {
        loadConfig();
    }

    private void loadConfig() {
        // Инициализация ExecutorService
        executorService = new ThreadPoolExecutor(
                BotConfig.getThreadPoolSize(),
                BotConfig.getMaxThreadPoolSize(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100)
        );

        // Инициализация контроллеров
        filesAndFoldersController = new FilesAndFoldersController();
        userController = new UserController();
        linksAndGroupsController = new LinksAndGroupsController();
        userBansController = new UserBansController();

        // Инициализация FilesController
        filesController = new FilesController(
                this, filesAndFoldersController, BotConfig.getBotToken(),
                BotConfig.getFileDelimiter(), BotConfig.getFileStoragePath(),
                BotConfig.getMaxFileSize()
        );

        // Инициализация кэша расписания
        scheduleCache = new ScheduleCache(BotConfig.getCacheDuration());

        // Инициализация MarkupSetter
        markupSetter = new MarkupSetter(
                filesController, filesAndFoldersController,
                userController, linksAndGroupsController
        );

        // Инициализация репозитория логов удаления
        DeletionLogRepository deletionLogRepository = new DeletionLogRepository();

        // Инициализация обработчиков ответов
        textResponseHandler = new TextResponseHandler(
                this, markupSetter, filesAndFoldersController,
                linksAndGroupsController, userController
        );

        callbackResponseHandler = new CallbackResponseHandler(
                this, markupSetter, filesAndFoldersController,
                linksAndGroupsController, userController, userBansController,
                filesController, deletionLogRepository
        );

        // Настройка админов
        userController.addAdminsFromProperty(BotConfig.getInitialAdmins());

        // Синхронизация с БД
        filesController.synchronizeFoldersWithDatabase();
        filesController.synchronizeFilesWithDatabase();

        // Настройка shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));

        // Настройка очистки кэша
        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            scheduler.scheduleAtFixedRate(
                    scheduleCache::clearExpiredCache,
                    BotConfig.getCacheDuration(),
                    BotConfig.getCacheDuration(),
                    TimeUnit.MINUTES
            );
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> handleUpdate(update));
    }

    private void handleUpdate(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            } else if (update.getMessage() != null && update.getMessage().hasDocument()) {
                handleDocument(update);
            } else if (update.getMessage() != null && update.getMessage().hasText()) {
                handleTextMessage(update);
            }
        } catch (Exception e) {
            System.err.printf("Error handling update: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        userController.checkAndAddUser(update);

        String callbackData = update.getCallbackQuery().getData().trim();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        // Проверка бана
        if (userBansController.isUserBanned(chatId)) {
            BanInfo banInfo = userBansController.getUserBanInfo(chatId);
            if (banInfo != null && banInfo.getBanType().equals(BanType.FULL_BAN.toString())) {
                callbackResponseHandler.handleFullBan(chatId, messageId);
                return;
            }
        }

        boolean isAdmin = userController.checkAdminByChatId(chatId);

        // Обработка callback
        callbackResponseHandler.handleCallback(
                chatId, callbackData, messageId, isAdmin,
                notification.toString(), textResponseHandler
        );
    }

    private void handleDocument(Update update) {
        long chatId = userController.getChatId(update);

        // Проверка бана
        if (checkBanAndRespond(chatId, BanType.FULL_BAN)) {
            return;
        }

        // Сброс статуса добавления папки
        userController.updateCanAddFolder(chatId, (byte) 0);

        // Проверка выбранного пути
        String userPath = userController.getFilePath(chatId);
        if (userPath.isEmpty()) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.PATH_CHECK_ERROR);
            return;
        }

        userPath = BotConfig.getFileStoragePath() + userPath;
        String fileName = update.getMessage().getDocument().getFileName();

        try {
            saveDocument(update, fileName, userPath, chatId);
        } catch (IncorrectExtensionException e) {
            logError("IncorrectExtensionException", chatId, e);
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.INCORRECT_FILE_EXTENSION);
        } catch (FileSizeException e) {
            logError("FileSizeException", chatId, e);
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.FILE_TOO_BIG);
        } catch (TelegramApiException | IOException e) {
            logError("TelegramApi/IOException", chatId, e);
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.SIMPLE_ERROR);
        } catch (InvalidCallbackDataException e) {
            logError("InvalidCallbackDataException", chatId, e);
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.INVALID_FILE_NAME);
        }
    }

    private void handleTextMessage(Update update) {
        long chatId = userController.getChatId(update);

        // Проверка бана (полный бан)
        if (checkBanAndRespond(chatId, BanType.FULL_BAN)) {
            return;
        }

        String text = update.getMessage().getText();
        userController.checkAndAddUser(update);

        logMessage(chatId, text);

        // Обработка команд
        if (text.startsWith("/")) {
            handleCommand(chatId, text);
        } else {
            handleRegularText(chatId, text);
        }
    }

    private boolean checkBanAndRespond(long chatId, BanType banType) {
        if (userBansController.isUserBanned(chatId)) {
            BanInfo banInfo = userBansController.getUserBanInfo(chatId);
            if (banInfo != null) {
                if (banType == BanType.FULL_BAN && banInfo.getBanType().equals(BanType.FULL_BAN.toString())) {
                    textResponseHandler.sendResponse(chatId,
                            TextResponseHandler.ResponseType.FULL_BAN, banInfo);
                    return true;
                } else if (banType == BanType.SHARING_BAN && banInfo.getBanType().equals(BanType.SHARING_BAN.toString())) {
                    textResponseHandler.sendResponse(chatId,
                            TextResponseHandler.ResponseType.SHARING_BAN, banInfo);
                    return true;
                }
            }
        }
        return false;
    }

    private void handleCommand(long chatId, String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "/start" -> textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.START_MENU);
            case "/help" -> sendHelpResponse(chatId);
            case "/sendToAll" -> handleSendToAll(chatId, args);
            case "/sendNotification" -> handleSendNotification(chatId, args);
            case "/addAdmin" -> handleAddAdmin(chatId, args);
            case "/delete_Folder" -> handleDeleteFolder(chatId);
            case "/delete_Group" -> handleDeleteGroup(chatId);
            case "/ban_user" -> handleBanUser(chatId, args);
            case "/unban_user" -> handleUnbanUser(chatId, args);
            default -> textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.SIMPLE_ERROR);
        }
    }

    private void handleRegularText(long chatId, String text) {
        // Проверка бана на отправку контента (SHARING_BAN)
        if (checkBanAndRespond(chatId, BanType.SHARING_BAN)) {
            return;
        }

        // Обработка ссылок
        if (LinkUtil.isValidLinkFormat(text.trim())) {
            if (userController.getCanAddLink(chatId)) {
                saveLink(chatId, text);
            } else {
                textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.SIMPLE_ERROR);
            }
        }
        // Обработка добавления группы
        else if (userController.getCanAddGroup(chatId)) {
            saveGroup(chatId, text);
        }
        // Обработка добавления папки
        else if (userController.getCanAddFolder(chatId)) {
            saveFolder(chatId, text);
        }
        // Обработка файлов (если text заканчивается на File или _lnk)
        else if (text.endsWith("File")) {
            boolean isAdmin = userController.checkAdminByChatId(chatId);
            textResponseHandler.sendResponse(chatId,
                    TextResponseHandler.ResponseType.FILE_RESPONSE, text, isAdmin);
        } else if (text.endsWith("_lnk")) {
            boolean isAdmin = userController.checkAdminByChatId(chatId);
            textResponseHandler.sendResponse(chatId,
                    TextResponseHandler.ResponseType.LINK_RESPONSE, text, isAdmin);
        }
    }

    private void sendHelpResponse(long chatId) {
        boolean isAdmin = userController.checkAdminByChatId(chatId);
        String adminHelp = """
                Команды :
                /sendToAll text - Отправляет всем пользователям бота сообщение с указанным текстом
                /sendNotification text - Устанавливает текст при нажатии кнопки
                /addAdmin username - Добавляет нового админа с базовыми правами
                /delete_Folder - позволяет удалить папку с файлами
                /delete_Group - позволяет удалить группу с ссылками
                /ban_user chatId banType(FULL_BAN | SHARING_BAN) причина - блокирует пользователя
                /unban_user chatId - снять бан с пользователя
                """;
        String userHelp = "Напишите /start, если что-то сломалось \n" +
                "Чтобы сохранить файл, выберите путь и скиньте файл боту\n" +
                "Старайтесь не делать названия файлов и директории слишком большими, бот может дать сбой \n" +
                "Если столкнулись с проблемой, напишите в личку @wrotoftanks63";

        String helpText = isAdmin ? adminHelp : userHelp;
        SendMessage message = textResponseHandler.createMessageWithMarkup(chatId, helpText, MarkupKey.MAIN_MENU);
        executeSafely(message, "/help", chatId);
    }

    private void handleSendToAll(long chatId, String text) {
        if (!userController.checkAdminByChatId(chatId)) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.ADMIN_ERROR);
            return;
        }

        if (!text.isEmpty()) {
            List<Long> chatIds = userController.getAllUsersChatId();
            textResponseHandler.sendToAllUsers(chatIds, text);
        }
    }

    private void handleSendNotification(long chatId, String text) {
        if (!userController.checkAdminByChatId(chatId)) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.ADMIN_ERROR);
            return;
        }

        if (!text.isEmpty()) {
            notification.setLength(0);
            notification.append(text);
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.SIMPLE_ERROR);
        }
    }

    private void handleAddAdmin(long chatId, String username) {
        boolean isMainAdmin = userController.checkAdminByChatId(chatId) &&
                userController.getAdminRole(chatId).equals(AdminRole.Main.toString());

        if (!isMainAdmin) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.ADMIN_ERROR);
            return;
        }

        String adminUsername = username.trim();
        if (!adminUsername.isEmpty()) {
            textResponseHandler.handleAdminCommand(chatId,
                    TextResponseHandler.AdminCommand.ADD_ADMIN, adminUsername);
        }
    }

    private void handleDeleteFolder(long chatId) {
        if (!userController.checkAdminByChatId(chatId)) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.ADMIN_ERROR);
            return;
        }

        textResponseHandler.handleAdminCommand(chatId,
                TextResponseHandler.AdminCommand.DELETE_FOLDER);
    }

    private void handleDeleteGroup(long chatId) {
        if (!userController.checkAdminByChatId(chatId)) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.ADMIN_ERROR);
            return;
        }

        textResponseHandler.handleAdminCommand(chatId,
                TextResponseHandler.AdminCommand.DELETE_GROUP);
    }

    private void handleBanUser(long chatId, String args) {
        if (!userController.checkAdminByChatId(chatId)) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.ADMIN_ERROR);
            return;
        }

        String[] parts = args.split(" ");
        if (parts.length < 3) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.TOO_FEW_ARGS);
            return;
        }

        try {
            long userChatId = Long.parseLong(parts[0]);
            if (userChatId == chatId) {
                textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.INVALID_USER_CHAT_ID);
                return;
            }

            BanInfo banInfo = createBanInfo(parts, chatId);
            userBansController.banUser(userChatId, banInfo);
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.SIMPLE_ERROR);

        } catch (NumberFormatException e) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.INVALID_USER_CHAT_ID);
        }
    }

    private void handleUnbanUser(long chatId, String args) {
        if (!userController.checkAdminByChatId(chatId)) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.ADMIN_ERROR);
            return;
        }

        String[] parts = args.split(" ");
        if (parts.length < 1) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.TOO_FEW_ARGS);
            return;
        }

        try {
            long userChatId = Long.parseLong(parts[0]);
            userBansController.unbanUser(userChatId, chatId);
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.SIMPLE_ERROR);

        } catch (NumberFormatException e) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.INVALID_USER_CHAT_ID);
        }
    }

    private BanInfo createBanInfo(String[] args, long adminChatId) {
        String banType = args[1];
        if (!banType.equals(BanType.FULL_BAN.toString()) &&
                !banType.equals(BanType.SHARING_BAN.toString())) {
            banType = BanType.SHARING_BAN.toString();
        }

        StringBuilder reason = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        return BanInfo.builder()
                .banType(banType)
                .reason(reason.toString().trim())
                .adminChatId(adminChatId)
                .build();
    }

    private void saveLink(long chatId, String text) {
        String[] parts = text.split(":");
        if (parts.length >= 2) {
            String linkName = parts[0].trim();
            String link = parts[1].trim();
            String group = userController.getGroupForLinks(chatId);
            linksAndGroupsController.addLink(linkName, link, group, chatId);
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.LINK_SAVED);
        }
    }

    private void saveGroup(long chatId, String text) {
        linksAndGroupsController.addNewGroup(chatId, text.trim());
        textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.GROUP_SAVED);
    }

    private void saveFolder(long chatId, String text) {
        if (!org.example.files.FilesController.checkFileName(text)) {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.INVALID_FILE_NAME);
            return;
        }

        String folderName = text.trim();
        filesController.addFolder(chatId, folderName);
        textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.FOLDER_ADDED);
    }

    private void saveDocument(Update update, String fileName, String userPath, long chatId)
            throws IncorrectExtensionException, IOException, FileSizeException,
            TelegramApiException, InvalidCallbackDataException {

        String extension = org.example.files.FilesController.checkFileExtension(
                fileName, BotConfig.getAllowedExtensions());

        Document document = update.getMessage().getDocument();
        String caption = update.getMessage().getCaption();

        String pathToFile = filesController.saveDocument(document, caption, extension, userPath);

        if (!pathToFile.isEmpty()) {
            String target = pathToFile.replace(BotConfig.getFileStoragePath(), "");
            int delimiterIndex = target.indexOf(BotConfig.getFileDelimiter());
            String folder = target.substring(0, delimiterIndex);
            String file = target.substring(delimiterIndex + 1);

            filesAndFoldersController.putFileInfo(chatId, folder, file);
            System.out.printf("Сохранен документ от пользователя %d%nДокумент: %s/%s%n",
                    chatId, folder, document.getFileName());

            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.DOCUMENT_SAVED);
        } else {
            textResponseHandler.sendResponse(chatId, TextResponseHandler.ResponseType.SIMPLE_ERROR);
        }
    }

    private void executeSafely(SendMessage message, String context, long chatId) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.printf("Error (TBot - context: %s, chatId: %d): %s%n",
                    context, chatId, e.getMessage());
        }
    }

    private void logMessage(long chatId, String text) {
        System.out.printf("Message (chatId: %d | text: %s) %tF %<tT%n",
                chatId, text, LocalDateTime.now());
    }

    private void logError(String errorType, long chatId, Exception e) {
        System.err.printf("Error (TBot - %s, chatId: %d): %s%n",
                errorType, chatId, e.getMessage());
    }

    @Override
    public String getBotUsername() {
        return BotConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return BotConfig.getBotToken();
    }

    // Метод для FilesController
    public String getFilePath(String fileId) throws TelegramApiException {
        return execute(new GetFile(fileId)).getFilePath();
    }
}