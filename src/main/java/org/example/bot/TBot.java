package org.example.bot;

import org.example.bot.ban.types.BanType;
import org.example.bot.ban.types.ban.info.BanInfo;
import org.example.bot.config.BotConfig;
import org.example.bot.message.DeleteMessageBuilder;
import org.example.bot.message.EditMessageBuilder;
import org.example.bot.message.MessageBuilder;
import org.example.bot.message.MessageWithDocBuilder;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.controller.FilesAndFoldersController;
import org.example.controller.LinksAndGroupsController;
import org.example.controller.UserBansController;
import org.example.controller.UserController;
import org.example.database.repository.*;
import org.example.dto.FileDTO;
import org.example.files.FilesController;
import org.example.files.exception.FileSizeException;
import org.example.files.exception.IncorrectExtensionException;
import org.example.files.exception.InvalidCallbackDataException;
import org.example.role.AdminRole;
import org.example.schedule.ScheduleCache;
import org.example.utility.LinkUtil;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class TBot extends TelegramLongPollingBot {

    private ExecutorService executorService;
    private FilesAndFoldersController filesAndFoldersController;
    private MarkupSetter markupSetter;
    private FilesController filesController;
    private ScheduleCache scheduleCache;
    private UserController userController;
    private LinksAndGroupsController linksAndGroupsController;
    private DeletionLogRepository deletionLogRepository;
    private UserBansController userBansController;

    private final String helpResponse = "Напишите /start, если что-то сломалось \n" +
            "Чтобы сохранить файл, выберите путь и скиньте файл боту\n" +
            "Старайтесь не делать названия файлов и директории слишком большими, бот может дать сбой \n" +
            "Если столкнулись с проблемой, напишите в личку @wrotoftanks63";
    private final String adminHelpResponse = """
            Команды :
            /sendToAll text - Отправляет всем пользователям бота сообщение с указанным текстом
            /sendNotification text - Устанавливает текст при нажатии кнопки
            /addAdmin username - Добавляет нового админа с базовыми правами
            /delete_Folder - позволяет удалить папку с файлами
            /delete_Group - позволяет удалить группу с ссылками
            /ban_user chatId banType(FULL_BAN | SHARING_BAN) причина - блокирует пользователя, SHARING_BAN Лишает возможности сохранять ссылки и файлы
            /unban_user chatId - снять бан с пользователя
            """;

    private final StringBuilder notification = new StringBuilder("Нет каких либо оповещений");

    public TBot() {
        loadConfig();
    }

    private void loadConfig() {
        executorService = new ThreadPoolExecutor(BotConfig.getThreadPoolSize(), BotConfig.getMaxThreadPoolSize(), 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100));
        filesAndFoldersController = new FilesAndFoldersController();
        filesController = new FilesController(this, filesAndFoldersController, BotConfig.getBotToken(),
                BotConfig.getFileDelimiter(), BotConfig.getFileStoragePath(), BotConfig.getMaxFileSize());
        scheduleCache = new ScheduleCache(BotConfig.getCacheDuration());
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::close));
        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            scheduler.scheduleAtFixedRate(scheduleCache::clearExpiredCache, BotConfig.getCacheDuration(), BotConfig.getCacheDuration(), TimeUnit.MINUTES);
        }
        userController = new UserController();
        linksAndGroupsController = new LinksAndGroupsController();
        markupSetter = new MarkupSetter(filesController, filesAndFoldersController, userController, linksAndGroupsController);
        userController.addAdminsFromProperty(BotConfig.getInitialAdmins());
        filesController.synchronizeFoldersWithDatabase();
        filesController.synchronizeFilesWithDatabase();
        deletionLogRepository = new DeletionLogRepository();
        userBansController = new UserBansController();
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> handleUpdate(update));
    }

    private void handleUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            updateHasCallbackQuery(update);
        } else if (update.getMessage().hasDocument()) {
            updateHasDocument(update);
        } else {
            updateHasTextOnly(update);
        }
    }

    private BanInfo getBanInfo(long chatId) {
        return userBansController.getUserBanInfo(chatId);
    }

    private boolean isBanned(long chatId) {
        return userBansController.isUserBanned(chatId);
    }

    private SendMessage setSendMessageWithDefaultMarkup(long chatId, String data, MarkupKey key) {
        MessageBuilder messageBuilder = new MessageBuilder(data, chatId);
        SendMessage sendMessage = messageBuilder.getMessage();
        if (key != MarkupKey.NONE) {
            sendMessage.setReplyMarkup(markupSetter.getBasicMarkup(key));
        }
        return sendMessage;
    }

    private SendMessage setSendMessageWithChangeableMarkup(long chatId, String data, String markupKey) throws IllegalArgumentException {
        MessageBuilder messageBuilder = new MessageBuilder(data, chatId);
        SendMessage sendMessage = messageBuilder.getMessage();
        sendMessage.setReplyMarkup(markupSetter.getChangeableMarkup(markupKey));
        return sendMessage;
    }

    private void sendToAll(List<Long> chatIdArray, String text) {
        for (long id : chatIdArray) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(id);
            sendMessage.setText(text);
            try {
                execute(sendMessage);
                checkMessageBeforeResponse(id, "/start");
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendNewMessageResponse(/sendToAll))) " + e);
            }
        }
    }

    private boolean isAdmin(long chatId) {
        return userController.checkAdminByChatId(chatId);
    }

    private void sendNewMessageResponse(long chatId, String data) {
        SendMessage sendMessage;
        switch (data) {
            case "DocumentSaved" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Файл сохранен", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "IncorrectFileExtension" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Неверный формат файла, попробуйте поменять название или тип файла", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "FileTooBig" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Файл слишком большой!", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "FolderAdded" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Папка сохранена", MarkupKey.MAIN_MENU);
                // Убираем статус создания папки
                userController.updateCanAddFolder(chatId, (byte) 0);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "SimpleError" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Произошла неизвестная ошибка", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "AdminError" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "У вас нет прав на использование команды", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "AdminAdded" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Админ добавлен", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "PathCheckError" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Для хранения файлов выберите папку своей группы", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "InvalidFileName" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Возникла ошибка, название файла слишком большое, сделайте его поменьше и попытайтесь снова", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "LinkSaved" -> {
                userController.updateCanAddLink(chatId, (byte) 0);
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Ссылка сохранена!", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "GroupSaved" -> {
                userController.updateCanAddGroup(chatId, (byte) 0);
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Новая группа сохранена", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "TooFewArgs" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Недостаточно аргументов команды", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "InvalidUserChatId" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Неправильный формат chatId пользователя", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "FullBan" -> {
                BanInfo banInfo = userBansController.getUserBanInfo(chatId);
                String text = "Вы заблокированы, причина : \n" + banInfo.getReason();
                sendMessage = setSendMessageWithDefaultMarkup(chatId, text, MarkupKey.NONE);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "SharingBan" -> {
                BanInfo banInfo = userBansController.getUserBanInfo(chatId);
                String text = "Вы не можете сохранять папки и ссылки, причина : \n" + banInfo.getReason();
                sendMessage = setSendMessageWithDefaultMarkup(chatId, text, MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
        }
        if (data.endsWith("File")) {
            long fileId = Long.parseLong(data.replaceAll("File$", ""));
            FileDTO fileDTO = filesAndFoldersController.getFileInfoByFileId(fileId);
            MessageWithDocBuilder message = new MessageWithDocBuilder(chatId, fileDTO);
            SendDocument sendDocument = message.getMessage();
            if (isAdmin(chatId)) {
                long userChatId = filesAndFoldersController.getFilesChatIdById(fileId);
                sendDocument.setCaption(userController.getUserInfo(userChatId));
            }
            try {
                execute(sendDocument);
                checkMessageBeforeResponse(chatId, "/start");
            } catch (TelegramApiException e) {
                System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                checkMessageBeforeResponse(chatId, "SimpleError");
            }
        } else if (data.endsWith("_lnk")) {
            long linkId = Long.parseLong(data.replaceAll("_lnk$", ""));
            String link = linksAndGroupsController.getLinkById(linkId);
            if (isAdmin(chatId)) {
                long userChatId = linksAndGroupsController.getUsersChatIdByLinkId(linkId);
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Вот ваша ссылка \n" + link + "\n" + userController.getUserInfo(userChatId), MarkupKey.NONE);
            } else {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Вот ваша ссылка \n" + link, MarkupKey.NONE);
            }
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
            }
        } else if (!isBanned(chatId)) {
            // Если пользователь не забанен по любой причине, то может сохранять ссылки, папки и т.д.
            if (LinkUtil.isValidLinkFormat(data.trim())) {
                if (userController.getCanAddLink(chatId)) {
                    String[] parts = data.split(":");
                    String linkName = parts[0].trim();
                    String link = parts[1].trim();
                    String group = userController.getGroupForLinks(chatId);
                    linksAndGroupsController.addLink(linkName, link, group, chatId);
                    sendNewMessageResponse(chatId, "LinkSaved");
                } else {
                    sendNewMessageResponse(chatId, "SimpleError");
                }
            } else if (userController.getCanAddGroup(chatId)) {
                linksAndGroupsController.addNewGroup(chatId, data.trim());
                sendNewMessageResponse(chatId, "GroupSaved");
            } else if (FilesController.checkFileName(data) && userController.getCanAddFolder(chatId)) {
                if (!FilesController.checkFileName(data)) {
                    sendNewMessageResponse(chatId, "InvalidFileName");
                } else {
                    String folderName = data.trim();
                    filesController.addFolder(chatId, folderName);
                    sendNewMessageResponse(chatId, "FolderAdded");
                }

            }
        }
    }

    // Специальная обработка команд, начинающихся с /
    private void sendNewMessageResponseOnCommand(long chatId, String data) {
        SendMessage sendMessage;
        switch (data) {
            case "/start" -> {
                sendMessage = setSendMessageWithDefaultMarkup(chatId, "Выберите функцию", MarkupKey.MAIN_MENU);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e);
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "/delete_Folder" -> {
                if (isAdmin(chatId)) {
                    try {
                        sendMessage = setSendMessageWithChangeableMarkup(chatId, "Выберите папку, которую хотите удалить вместе с файлами внутри", data);
                        execute(sendMessage);
                    } catch (TelegramApiException | IllegalArgumentException e) {
                        System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e);
                        checkMessageBeforeResponse(chatId, "SimpleError");
                    }
                } else {
                    sendNewMessageResponse(chatId, "AdminError");
                }
                return;
            }
            case "/delete_Group" -> {
                if (isAdmin(chatId)) {
                    try {
                        sendMessage = setSendMessageWithChangeableMarkup(chatId, "Выберите папку, которую хотите удалить", data);
                        execute(sendMessage);
                    } catch (TelegramApiException | IllegalArgumentException e) {
                        System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e);
                        checkMessageBeforeResponse(chatId, "SimpleError");
                    }
                } else {
                    sendNewMessageResponse(chatId, "AdminError");
                }
                return;
            }
        }
        if (data.contains("/sendToAll")) {
            if (!isAdmin(chatId)) {
                checkMessageBeforeResponse(chatId, "AdminError");
                System.out.printf("Trying to access admin command without admin rights, chatId : %d%n", chatId);
                return;
            }
            String text = data.replaceAll("/sendToAll", "");
            if (!text.isEmpty()) {
                List<Long> chatIdArray = userController.getAllUsersChatId();
                if (!chatIdArray.isEmpty()) {
                    sendToAll(chatIdArray, text);
                }
            }
        } else if (data.contains("/sendNotification")) {
            if (!isAdmin(chatId)) {
                checkMessageBeforeResponse(chatId, "AdminError");
                System.out.printf("Trying to access admin command without admin rights, chatId : %d%n", chatId);
                return;
            }
            String text = data.replaceAll("/sendNotification", "");
            if (!text.isEmpty()) {
                notification.setLength(0);
                notification.append(text);
                checkMessageBeforeResponse(chatId, "/start");
            }
        } else if (data.contains("/addAdmin")) {
            boolean isAdmin = isAdmin(chatId);
            String adminRole = userController.getAdminRole(chatId);
            if (!isAdmin || !adminRole.equals(AdminRole.Main.toString())) {
                checkMessageBeforeResponse(chatId, "AdminError");
                System.out.printf("Trying to access admin command without admin rights, chatId : %d%n", chatId);
                return;
            }
            String adminUsername = data.replaceAll("/addAdmin", "").trim();
            if (userController.addBaseAdmin(adminUsername)) {
                checkMessageBeforeResponse(chatId, "AdminAdded");
            }
        } else if (data.contains("/ban_user")) {
            String[] args = data.split(" ");
            if (args.length < 3) {
                sendNewMessageResponse(chatId, "TooFewArgs");
                return;
            }
            try {
                long userChatId = Long.parseLong(args[1]);
                if (userChatId == chatId) {
                    sendNewMessageResponse(chatId, "InvalidUserChatId");
                    return;
                }
                BanInfo banInfo = setBanInfo(args, chatId);
                userBansController.banUser(userChatId, banInfo);
                checkMessageBeforeResponse(chatId, "/start");
            } catch (NumberFormatException e) {
                sendNewMessageResponse(chatId, "InvalidUserChatId");
            }
        } else if (data.contains("/unban_user")) {
            String[] args = data.split(" ");
            if (args.length < 2) {
                sendNewMessageResponse(chatId, "TooFewArgs");
                return;
            }
            try {
                long userChatId = Long.parseLong(args[1]);
                userBansController.unbanUser(userChatId, chatId);
                checkMessageBeforeResponse(chatId, "/start");
            } catch (NumberFormatException e) {
                sendNewMessageResponse(chatId, "InvalidUserChatId");
            }
        }
    }

    private BanInfo setBanInfo(String[] args, long adminChatId) {
        String banType = args[2];
        if (!banType.equals(BanType.FULL_BAN.toString()) && !banType.equals(BanType.SHARING_BAN.toString())) {
            banType = "SHARING_BAN";
        }
        StringBuilder reason = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            reason.append(args[i]);
        }
        String banReason = reason.toString();
        return BanInfo.builder()
                .banType(banType)
                .reason(banReason)
                .adminChatId(adminChatId)
                .build();
    }

    private void checkMessageBeforeResponse(long chatId, String data) {
        System.out.printf("checkMessageBeforeResponse(chatId : %d | data : %s) %tF %<tT%n", chatId, data, LocalDateTime.now());
        //Фикс для Unix систем
        if (data.startsWith("/")) {
            sendNewMessageResponseOnCommand(chatId, data);
        } else {
            sendNewMessageResponse(chatId, data);
        }
    }

    // Если сообщение имеет только текст
    private void updateHasTextOnly(Update update) {

        long chatId = userController.getChatId(update);

        if (isBanned(chatId)) {
            BanInfo banInfo = getBanInfo(chatId);
            if (banInfo.getBanType().equals(BanType.FULL_BAN.toString())) {
                sendNewMessageResponse(chatId, "FullBan");
                return;
            }
        }

        String data = update.getMessage().getText();
        userController.checkAndAddUser(update);

        checkMessageBeforeResponse(chatId, data);
    }

    // Получение расширения, документа и описания к нему
    private void saveDocument(Update update, String fileName, String userPath, long chatId)
            throws IncorrectExtensionException, IOException, FileSizeException, TelegramApiException, InvalidCallbackDataException {
        // Получение расширения, документа и описания к нему
        String extension = FilesController.checkFileExtension(fileName, BotConfig.getAllowedExtensions());
        Document document = update.getMessage().getDocument();
        String caption = update.getMessage().getCaption();
        String pathToFile = filesController.saveDocument(document, caption, extension, userPath);
        if (!pathToFile.isEmpty()) {
            String target = pathToFile.replace(BotConfig.getFileStoragePath(), "");
            int delimiterIndex = target.indexOf(BotConfig.getFileDelimiter());
            String folder = target.substring(0, delimiterIndex);
            String file = target.substring(delimiterIndex + 1);
            filesAndFoldersController.putFileInfo(chatId, folder, file);
            System.out.printf("Сохранен документ от пользователя %d%nДокумент: %s/%s%n", chatId, folder, document.getFileName());
            checkMessageBeforeResponse(chatId, "DocumentSaved");
        } else {
            checkMessageBeforeResponse(chatId, "SimpleError");
        }
    }

    private void updateHasDocument(Update update) {
        long chatId = userController.getChatId(update);

        if (isBanned(chatId)) {
            BanInfo banInfo = getBanInfo(chatId);
            if (banInfo.getBanType().equals(BanType.FULL_BAN.toString())) {
                sendNewMessageResponse(chatId, "FullBan");
                return;
            } else if (banInfo.getBanType().equals(BanType.SHARING_BAN.toString())) {
                sendNewMessageResponse(chatId, "SharingBan");
                return;
            }
        }

        // Убираем статус добавления новой папки
        userController.updateCanAddFolder(chatId, (byte) 0);

        // Проверка выбранного пользователем пути
        String userPath = userController.getFilePath(chatId);
        if (userPath.isEmpty()) {
            checkMessageBeforeResponse(chatId, "PathCheckError");
            return;
        }
        userPath = BotConfig.getFileStoragePath() + userPath;
        String fileName = update.getMessage().getDocument().getFileName();
        try {
            saveDocument(update, fileName, userPath, chatId);
        } catch (IncorrectExtensionException e) {
            System.err.printf("Error (TBotClass (method updateHasDocument(IncorrectExtensionException))) chatId : %d %n%s%n", chatId, e.getMessage());
            checkMessageBeforeResponse(chatId, "IncorrectFileExtension");
        } catch (FileSizeException e) {
            System.err.printf("Error (TBotClass (method updateHasDocument(FileSizeException))) chatId : %d %n%s%n", chatId, e.getMessage());
            checkMessageBeforeResponse(chatId, "FileTooBig");
        } catch (TelegramApiException | IOException e) {
            System.err.printf("Error (TBotClass (method updateHasDocument(TelegramApi/IOException))) chatId : %d %n%s%n", chatId, e.getMessage());
            checkMessageBeforeResponse(chatId, "SimpleError");
        } catch (InvalidCallbackDataException e) {
            System.err.printf("Error (TBotClass (method updateHasDocument(InvalidCallbackDataException))) chatId : %d %n%s%n", chatId, e.getMessage());
            checkMessageBeforeResponse(chatId, "InvalidFileName");
        }
    }

    private EditMessageText setEditMessageWithoutMarkup(long chatId, String data, int messageId) {
        EditMessageBuilder editMessageBuilder = new EditMessageBuilder(chatId, data, messageId);
        return editMessageBuilder.getMessage();
    }

    private void sendEditMessageResponse(long chatId, String data, int messageId) {
        System.out.printf("sendEditMessageResponse(chatId : %d | data : %s | messageId : %d) %tF %<tT%n", chatId, data, messageId, LocalDateTime.now());
        EditMessageText message;
        switch (data) {
            case "Help" -> {
                message = setEditMessageWithoutMarkup(chatId, isAdmin(chatId) ?  adminHelpResponse : helpResponse, messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.printf("The new text equals with old: Error (TBotClass (method sendEditMessageResponse(Help))) %s%n", e);
                } catch (IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                }
                return;
            }
            case "LessonButton" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите дату", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LESSON_MENU));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "BackButton" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите функцию", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "FileButton" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите вашу группу", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException  e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
            }
            case "AddFolderButton" -> {
                if (isAdmin(chatId)) {
                    userController.updateCanAddFolder(chatId, (byte) 1);
                    message = setEditMessageWithoutMarkup(chatId, "Отправьте название папки", messageId);
                    try {
                        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_FILES));
                        execute(message);
                    } catch (TelegramApiException | IllegalArgumentException e) {
                        System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                        sendEditMessageResponse(chatId, "SimpleError", messageId);
                    }
                } else {
                    sendEditMessageResponse(chatId, "AdminError", messageId);
                }
                return;
            }
            case "AddGroupButton" -> {
                if (isAdmin(chatId)) {
                    userController.updateCanAddGroup(chatId, (byte) 1);
                    message = setEditMessageWithoutMarkup(chatId, "Отправьте название группы", messageId);
                    try {
                        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_LINKS));
                        execute(message);
                    } catch (TelegramApiException | IllegalArgumentException e) {
                        System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                        sendEditMessageResponse(chatId, "SimpleError", messageId);
                    }
                } else {
                    sendEditMessageResponse(chatId, "AdminError", messageId);
                }
                return;
            }
            case "TodayScheduleButton" -> {
                String groupId = userController.getGroupId(chatId);
                if (groupId.isEmpty()) {
                    sendEditMessageResponse(chatId, "GroupNotSelected", messageId);
                    return;
                }
                String scheduleToday = scheduleCache.getScheduleToday(groupId);
                message = setEditMessageWithoutMarkup(chatId, scheduleToday, messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LESSON_MENU));
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("The new text equals with old: Error (TBotClass (method sendEditMessage(TodaySchedule))) " + e);
                } catch (IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                }
                return;
            }
            case "TomorrowScheduleButton" -> {
                String groupId = userController.getGroupId(chatId);
                if (groupId.isEmpty()) {
                    sendEditMessageResponse(chatId, "GroupNotSelected", messageId);
                    return;
                }
                String scheduleTomorrow = scheduleCache.getScheduleTomorrow(groupId);
                message = setEditMessageWithoutMarkup(chatId, scheduleTomorrow, messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LESSON_MENU));
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("The new text equals with old: Error (TBotClass (method sendEditMessage(TomorrowSchedule))) " + e);
                } catch (IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "SelectYearButton" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите курс", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getChangeableMarkup("Year"));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "GetNotification" -> {
                message = setEditMessageWithoutMarkup(chatId, notification.toString(), messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.println("The new text equals with old: Error (TBotClass (method sendEditMessageResponse(GetNotification))) " + e);
                }
                return;
            }
            case "DeleteFileButton" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите файл, который хотите удалить", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getChangeableMarkup("DeleteFileButton" + chatId));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "GroupNotSelected" -> {
                message = setEditMessageWithoutMarkup(chatId, "У вас не выбрана группа \n Выберите курс", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getChangeableMarkup("Year"));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "LinksButton" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите группу", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getChangeableMarkup("LinksButton"));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "DeleteLinksButton" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите группу", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getChangeableMarkup(data + chatId));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "ErrorDeleteLinks" -> {
                message = setEditMessageWithoutMarkup(chatId, "Возникла ошибка при удалении ссылки", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(SelectYearButtons))) " + e);
                }
                return;
            }
            case "SimpleError" -> {
                message = setEditMessageWithoutMarkup(chatId, "Произошла неожиданная ошибка", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(SelectYearButtons))) " + e);
                }
                return;
            }
            case "AdminError" -> {
                message = setEditMessageWithoutMarkup(chatId, "У вас нет прав для этого действия", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(SelectYearButtons))) " + e);
                }
                return;
            }
            case "FullBan" -> {
                BanInfo banInfo = getBanInfo(chatId);
                String text = "Вы заблокированы, причина : \n" + banInfo.getReason();
                message = setEditMessageWithoutMarkup(chatId, text, messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {}
                return;
            }
            case "SharingBan" -> {
                BanInfo banInfo = getBanInfo(chatId);
                String text = "Вы не можете сохранять файлы и ссылки, причина : \n" + banInfo.getReason();
                message = setEditMessageWithoutMarkup(chatId, text, messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {}
                return;
            }
        }

        if (data.endsWith("FilesDelAdm")) {
            message = setEditMessageWithoutMarkup(chatId, "Выберите файл, который хотите удалить", messageId);
            try {
                message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("_FDel")) {
            long fileId = Long.parseLong(data.replaceAll("_FDel$", ""));
            FileDTO fileDTO = filesAndFoldersController.getFileInfoByFileId(fileId);
            String correctPath = fileDTO.getFolder() + BotConfig.getFileDelimiter() + fileDTO.getFileName();
            try {
                deleteFile(chatId, messageId, fileId, correctPath);
            } catch (IOException | TelegramApiException e) {
                System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("_LDel")) {
            long id = Long.parseLong(data.replaceAll("_LDel", ""));
            boolean isDeleted = linksAndGroupsController.deleteLinkById(id);
            if (isDeleted) {
                message = setEditMessageWithoutMarkup(chatId, "Ссылка удалена!", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
            } else {
                sendEditMessageResponse(chatId, "ErrorDeleteLinks", messageId);
            }
        } else if (data.endsWith("_DFolder")) {
            long id = Long.parseLong(data.replaceAll("_DFolder$", ""));
            String folderName = filesAndFoldersController.getFolderNameById(id);
            if (filesAndFoldersController.deleteFolderById(id)) {
                try {
                    deleteFolder(chatId, messageId, folderName);
                } catch (IOException | TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
            } else {
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if(data.endsWith("_DGroup")) {
            long id = Long.parseLong(data.replaceAll("_DGroup$", ""));
            linksAndGroupsController.deleteGroupWithLinksByGroupId(id);
            String groupName = linksAndGroupsController.getGroupNameById(id);
            deletionLogRepository.addDeletionLog(chatId, "Delete group -> " + groupName);
            try {
                message = setEditMessageWithoutMarkup(chatId, "Группа с ссылками удалена", messageId);
                message.setReplyMarkup(markupSetter.getChangeableMarkup("LinksButton"));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("_LinkFlrDel")) {
            message = setEditMessageWithoutMarkup(chatId, "Выберите ссылку, которую хотите удалить", messageId);
            try {
                message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("_Folder")) {
            message = setEditMessageWithoutMarkup(chatId, "Выберите вашу группу", messageId);
            try {
                message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("GroupForLinks")) {
            message = setEditMessageWithoutMarkup(chatId, "Выберите нужную вам ссылку", messageId);
            try {
                message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("AddFileButton")) {
            if (isBanned(chatId)) {
                sendEditMessageResponse(chatId, "SharingBan", messageId);
                return;
            }
            String folder = data.replaceAll("AddFileButton$", "");
            userController.updateFilePath(chatId, folder);
            message = setEditMessageWithoutMarkup(chatId, "Теперь отправленные вами файлы будут сохраняться в эту папку: " + folder, messageId);
            try {
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_FILES));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                System.err.printf("Error (TBotClass (method sendEditMessage(data : %s)))%n%s%n", data, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("AddLinkButton")) {
            if (isBanned(chatId)) {
                sendEditMessageResponse(chatId, "SharingBan", messageId);
                return;
            }
            String group = data.replace("AddLinkButton", "");
            userController.updateGroupForLinks(chatId, group);
            userController.updateCanAddLink(chatId, (byte) 1);
            message = setEditMessageWithoutMarkup(chatId, "Отправьте название ссылки и саму ссылку в виде\n" +
                    "название_ссылки:ссылка", messageId);
            try {
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_LINKS));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                userController.updateCanAddLink(chatId, (byte) 0);
                System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("_lnk")) {
            deleteMessage(chatId, messageId);
            sendNewMessageResponse(chatId, data);
            checkMessageBeforeResponse(chatId, "/start");
        } else if (data.contains("Year")) {
            message = setEditMessageWithoutMarkup(chatId, "Выберите вашу группу", messageId);

            // Получаем index начала year
            int index = data.indexOf("Year");
            // Создаем строку к которой привяжем значение groupId
            String num = data.substring(index);
            num = num.replace("Year", "");
            message.setReplyMarkup(markupSetter.getChangeableMarkup("Group" + num));
            try {
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                System.err.println("Error (TBotClass (method sendEditMessageResponse(Year))) " + e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.contains("Group")) {
            int index = data.indexOf("Group");
            String group = data.substring(index);
            group = group.replace("Group=", "");

            userController.updateGroupId(chatId, group);

            message = setEditMessageWithoutMarkup(chatId, "Группа сохранена", messageId);
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
            try {
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendEditMessageResponse()))");
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        }
    }

    private void deleteFile(long chatId, int messageId, long fileId, String correctPath) throws IOException, TelegramApiException {
        filesController.deleteFile(correctPath);
        if (filesAndFoldersController.deleteUserFileFromRepository(fileId)) {
            EditMessageText message = setEditMessageWithoutMarkup(chatId, "Файл удален!", messageId);
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
            execute(message);
        } else {
            sendEditMessageResponse(chatId, "SimpleError", messageId);
        }
    }

    private void deleteFolder(long chatId, int messageId, String folderName) throws IOException, TelegramApiException {
        EditMessageText message;
        filesController.deleteFolderWithFiles(folderName);
        deletionLogRepository.addDeletionLog(chatId, "Deleted a folder -> " + folderName);
        message = setEditMessageWithoutMarkup(chatId, "Папка успешно удалена", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        execute(message);
    }

    private void deleteMessage(long chatId, int messageId) {
        DeleteMessageBuilder message = new DeleteMessageBuilder(chatId, messageId);
        try {
            execute(message.getMessage());
        } catch (TelegramApiException e) {
            System.err.println("Error (TBotClass (method deleteMessage())) " + e);
            sendEditMessageResponse(chatId, "SimpleError", messageId);
        }
    }

    private void checkCallbackData(long chatId, String data, int messageId) {
        if (data.contains("_Folder") || data.equals("FileButton")) {
            sendEditMessageResponse(chatId, data, messageId);
        } else if (data.contains("DeleteFileButton") || data.contains("FilesDelAdm")) {
            sendEditMessageResponse(chatId, data, messageId);
        } else if (data.endsWith("File")) {
            deleteMessage(chatId, messageId);
            checkMessageBeforeResponse(chatId, data);
        } else {
            sendEditMessageResponse(chatId, data, messageId);
        }
    }

    private void updateHasCallbackQuery(Update update) {
        //getCallBackQuery дает те же возможности, что и message, но получить message можно только из CallBackQuery.getMessage
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        userController.checkAndAddUser(update);

        //удаление статуса создания папки/группы/ссылки
        userController.updateCanAddFolder(chatId, (byte) 0);
        userController.updateCanAddGroup(chatId, (byte) 0);
        userController.updateCanAddLink(chatId, (byte) 0);

        String callbackQuery = update.getCallbackQuery().getData().trim();

        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (isBanned(chatId)) {
            System.out.println("User is banned " + chatId);
            BanInfo banInfo = getBanInfo(chatId);
            System.out.println("banInfo type : " + banInfo.getBanType());
            if (banInfo.getBanType().equals(BanType.FULL_BAN.toString())) {
                sendEditMessageResponse(chatId, "FullBan", messageId);
                return;
            }
        }

        checkCallbackData(chatId, callbackQuery, messageId);
    }

    // Метод для класса FilesController
    public String getFilePath(String fileId) throws TelegramApiException {
        return execute(new GetFile(fileId)).getFilePath();
    }

    @Override
    public String getBotUsername() {
        return BotConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return BotConfig.getBotToken();
    }
}
