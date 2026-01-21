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

    public final StringBuilder notification = new StringBuilder("Нет каких либо оповещений");

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
                linksAndGroupsController, userController, userBansController
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
            textResponseHandler.handleTextResponse(chatId, "PathCheckError");
            return;
        }

        userPath = BotConfig.getFileStoragePath() + userPath;
        String fileName = update.getMessage().getDocument().getFileName();

        try {
            saveDocument(update, fileName, userPath, chatId);
        } catch (IncorrectExtensionException e) {
            logError("IncorrectFileException", chatId, e);
            textResponseHandler.handleTextResponse(chatId, "IncorrectFileException");
        } catch (FileSizeException e) {
            logError("FileSizeException", chatId, e);
            textResponseHandler.handleTextResponse(chatId, "FileSizeException");
        } catch (TelegramApiException | IOException e) {
            logError("TelegramApi/IOException", chatId, e);
            textResponseHandler.handleTextResponse(chatId, "SimpleError");
        } catch (InvalidCallbackDataException e) {
            logError("InvalidCallbackDataException", chatId, e);
            textResponseHandler.handleTextResponse(chatId, "InvalidFileName");
        }
    }

    private void handleTextMessage(Update update) {
        long chatId = userController.getChatId(update);

        // Проверка бана (полный бан)
        if (checkBanAndRespond(chatId, BanType.FULL_BAN)) {
            BanInfo banInfo = userBansController.getUserBanInfo(chatId);
            textResponseHandler.handleTextResponse(chatId, "FullBan");
            return;
        }

        String text = update.getMessage().getText();
        userController.checkAndAddUser(update);

        logMessage(chatId, text);

        // Обработка команд
        textResponseHandler.handleTextResponse(chatId, text);
    }

    private boolean checkBanAndRespond(long chatId, BanType banType) {
        if (userBansController.isUserBanned(chatId)) {
            BanInfo banInfo = userBansController.getUserBanInfo(chatId);
            if (banInfo != null) {
                if (banType == BanType.FULL_BAN && banInfo.getBanType().equals(BanType.FULL_BAN.toString())) {
                    textResponseHandler.handleTextResponse(chatId, "FullBan");
                    return true;
                } else if (banType == BanType.SHARING_BAN && banInfo.getBanType().equals(BanType.SHARING_BAN.toString())) {
                    textResponseHandler.handleTextResponse(chatId, "SharingBan");
                    return true;
                }
            }
        }
        return false;
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

            textResponseHandler.handleTextResponse(chatId, "DocumentSaved");
        } else {
            textResponseHandler.handleTextResponse(chatId, "SimpleError");
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