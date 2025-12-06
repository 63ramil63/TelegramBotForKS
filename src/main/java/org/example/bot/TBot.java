package org.example.bot;

import org.example.Main;
import org.example.bot.message.DeleteMessageBuilder;
import org.example.bot.message.EditMessageBuilder;
import org.example.bot.message.MessageBuilder;
import org.example.bot.message.MessageWithDocBuilder;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.controller.UserController;
import org.example.database.repository.*;
import org.example.dto.FileDTO;
import org.example.files.FilesController;
import org.example.files.exception.FileSizeException;
import org.example.files.exception.IncorrectExtensionException;
import org.example.files.exception.InvalidCallbackDataException;
import org.example.role.AdminRole;
import org.example.schedule.ScheduleCache;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class TBot extends TelegramLongPollingBot {

    private final ExecutorService executorService = new ThreadPoolExecutor(4, 10, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100));
    private UserRepository userRepository;
    private FileTrackerRepository fileTrackerRepository;
    private MarkupSetter markupSetter;
    private FilesController filesController;
    private ScheduleCache scheduleCache;
    private AdminRepository adminRepository;
    private UserController userController;
    private FolderRepository folderRepository;
    private GroupRepository groupRepository;
    private LinksRepository linksRepository;

    private String bot_token;
    private String bot_name;
    private int duration;
    private List<String> allowedExtensions;
    public static String path;
    public static int maxFileSize;
    public static String delimiter;
    private static List<String> adminsFromProperty;

    private final String helpResponse = "Напишите /start, если что-то сломалось \n" +
            "Чтобы сохранить файл, выберите путь и скиньте файл боту\n" +
            "Старайтесь не делать названия файлов и директории слишком большими, бот может дать сбой \n" +
            "Если столкнулись с проблемой, напишите в личку @wrotoftanks63";

    private final StringBuilder notification = new StringBuilder("Нет каких либо оповещений");

    public TBot() {
        loadConfig();
    }

    private void loadDataFromProperty() {
        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(Main.propertyPath)) {
            properties.load(is);
            bot_token = properties.getProperty("bot_token");
            bot_name = properties.getProperty("bot_name");
            //время обновления расписания в минутах
            duration = Integer.parseInt(properties.getProperty("duration"));
            //путь, где хранятся файлы
            path = properties.getProperty("path");
            //указываем разделитель '\\ для win' | '/ для Linux'
            delimiter = properties.getProperty("delimiter");
            //допустимые расширения файлов
            allowedExtensions = List.of(properties.getProperty("extensions").split(","));
            //макс размер файла в мб
            maxFileSize = Integer.parseInt(properties.getProperty("fileMaxSize"));
            adminsFromProperty = List.of(properties.getProperty("admins").split(","));
        } catch (IOException e) {
            System.err.printf("Error (TBotClass (method loadConfig())) %s", e);
            System.exit(505);
        }
    }

    private void loadConfig() {
        loadDataFromProperty();
        folderRepository = new FolderRepository();
        fileTrackerRepository = new FileTrackerRepository();
        filesController = new FilesController(this, folderRepository, fileTrackerRepository, bot_token, delimiter, path, maxFileSize);
        scheduleCache = new ScheduleCache(duration);
        userRepository = new UserRepository();
        adminRepository = new AdminRepository();
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::close));
        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            scheduler.scheduleAtFixedRate(scheduleCache::clearExpiredCache, duration, duration, TimeUnit.MINUTES);
        }
        userController = new UserController(userRepository, adminRepository);
        groupRepository = new GroupRepository();
        linksRepository = new LinksRepository();
        markupSetter = new MarkupSetter(filesController, fileTrackerRepository, userController, linksRepository, groupRepository);
        userController.addAdminsFromProperty(adminsFromProperty);
        filesController.synchronizeFoldersWithDatabase();
        filesController.synchronizeFilesWithDatabase();
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

    private SendMessage setSendMessage(long chatId, String data, MarkupKey key) {
        MessageBuilder messageBuilder = new MessageBuilder(data, chatId);
        SendMessage sendMessage = messageBuilder.getMessage();
        if (key != MarkupKey.NONE) {
            sendMessage.setReplyMarkup(markupSetter.getBasicMarkup(key));
        }
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

    private boolean isValidLinkFormat(String input) {
        return input != null && input.contains(":") && input.indexOf(":") > 1;
    }

    private void sendNewMessageResponse(long chatId, String data) {
        SendMessage sendMessage;
        switch (data) {
            case "DocumentSaved" -> {
                sendMessage = setSendMessage(chatId, "Файл сохранен", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "IncorrectFileExtension" -> {
                sendMessage = setSendMessage(chatId, "Неверный формат файла, попробуйте поменять название или тип файла", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "FileTooBig" -> {
                sendMessage = setSendMessage(chatId, "Файл слишком большой!", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "FolderAdded" -> {
                sendMessage = setSendMessage(chatId, "Папка сохранена", MarkupKey.MainMenu);
                // Убираем статус создания папки
                userRepository.updateCanAddFolder(chatId, (byte) 0);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "SimpleError" -> {
                sendMessage = setSendMessage(chatId, "Произошла неизвестная ошибка", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "AdminError" -> {
                sendMessage = setSendMessage(chatId, "У вас нет прав на использование команды", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "AdminAdded" -> {
                sendMessage = setSendMessage(chatId, "Админ добавлен", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "PathCheckError" -> {
                sendMessage = setSendMessage(chatId, "Для хранения файлов выберите папку своей группы", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "InvalidFileName" -> {
                sendMessage = setSendMessage(chatId, "Возникла ошибка, название файла слишком большое, сделайте его поменьше и попытайтесь снова", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "LinkSaved" -> {
                userRepository.updateCanAddLink(chatId, (byte) 0);
                sendMessage = setSendMessage(chatId, "Ссылка сохранена!", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                }
                return;
            }
            case "GroupSaved" -> {
                userRepository.updateCanAddGroup(chatId, (byte) 0);
                sendMessage = setSendMessage(chatId, "Новая группа сохранена", MarkupKey.MainMenu);
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
            FileDTO fileDTO = fileTrackerRepository.getFileInfoByFileId(fileId);
            MessageWithDocBuilder message = new MessageWithDocBuilder(chatId, fileDTO);
            SendDocument sendDocument = message.getMessage();
            if (adminRepository.getAdmin(userRepository.getUserName(chatId))) {
                long userId = fileTrackerRepository.getFilesChatIdById(fileId);
                String username = userRepository.getUserName(userId);
                sendDocument.setCaption("Данный файл отправлен пользователем: " +
                        (!username.isEmpty() ? username : " у данного пользователя нет username")
                        + "\nChatId: " + userId);
            }
            try {
                execute(sendDocument);
                checkMessageBeforeResponse(chatId, "/start");
            } catch (TelegramApiException e) {
                System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
                checkMessageBeforeResponse(chatId, "SimpleError");
            }
        } else if (data.endsWith("_lnk")) {
            String link = data.replaceAll("_lnk$", "");
            sendMessage = setSendMessage(chatId, "Вот ваша ссылка \n" + link, MarkupKey.NONE);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e.getMessage());
            }
        } else if (isValidLinkFormat(data.trim())) {
            if (userRepository.getCanAddLink(chatId)) {
                String[] parts = data.split(":");
                String linkName = parts[0].trim();
                String link = parts[1].trim();
                String group = userRepository.getGroupForLinks(chatId);
                linksRepository.addLink(linkName, link, group, chatId);
                sendNewMessageResponse(chatId, "LinkSaved");
            } else {
                sendNewMessageResponse(chatId, "SimpleError");
            }
        } else if (userRepository.getCanAddGroup(chatId)) {
            groupRepository.addNewGroup(data.trim());
            sendNewMessageResponse(chatId, "GroupSaved");
        } else if (FilesController.checkFileName(data) && userRepository.getCanAddFolder(chatId)) {
            if (!FilesController.checkFileName(data)) {
                sendNewMessageResponse(chatId, "InvalidFileName");
            } else {
                String folderName = data.trim();
                filesController.addFolder(folderName);
                sendNewMessageResponse(chatId, "FolderAdded");
            }
        }
    }

    // Специальная обработка команд, начинающихся с /
    private void sendNewMessageResponseOnCommand(long chatId, String data) {
        SendMessage sendMessage;
        switch (data) {
            case "/start" -> {
                sendMessage = setSendMessage(chatId, "Выберите функцию", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.printf("Error (TBotClass (method sendNewMessageResponse(data : %s))) %n%s%n", data, e);
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
        }
        if (data.contains("/sendToAll")) {
            if (!adminRepository.getAdmin(userRepository.getUserName(chatId))) {
                checkMessageBeforeResponse(chatId, "AdminError");
                System.out.printf("Trying to access admin command without admin rights, chatId : %d%n", chatId);
                return;
            }
            String text = data.replaceAll("/sendToAll", "");
            if (!text.isEmpty()) {
                List<Long> chatIdArray = userRepository.getAllUsersChatId();
                if (!chatIdArray.isEmpty()) {
                    sendToAll(chatIdArray, text);
                }
            }
        } else if (data.contains("/sendNotification")) {
            if (!adminRepository.getAdmin(userRepository.getUserName(chatId))) {
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
            boolean isAdmin = adminRepository.getAdmin(userRepository.getUserName(chatId));
            String adminRole = adminRepository.getAdminRole(userRepository.getUserName(chatId));
            if (!isAdmin || !adminRole.equals(AdminRole.Main.toString())) {
                checkMessageBeforeResponse(chatId, "AdminError");
                System.out.printf("Trying to access admin command without admin rights, chatId : %d%n", chatId);
                return;
            }
            String adminUsername = data.replaceAll("/addAdmin", "").trim();
            if (userController.addBaseAdmin(adminUsername)) {
                checkMessageBeforeResponse(chatId, "AdminAdded");
            }
        }
    }

    private void checkMessageBeforeResponse(long chatId, String data) {
        System.out.printf("checkMessageBeforeResponse(chatId : %d | data : %s)%n", chatId, data);
        //Фикс для Unix систем
        if (data.charAt(0) == '/') {
            sendNewMessageResponseOnCommand(chatId, data);
        } else {
            sendNewMessageResponse(chatId, data);
        }
    }

    // Если сообщение имеет только текст
    private void updateHasTextOnly(Update update) {

        long chatId = userController.getChatId(update);
        String data = update.getMessage().getText();
        userController.checkAndAddUser(update);

        checkMessageBeforeResponse(chatId, data);
    }

    // Получение расширения, документа и описания к нему
    private void saveDocument(Update update, String fileName, String userPath, long chatId)
            throws IncorrectExtensionException, IOException, FileSizeException, TelegramApiException, InvalidCallbackDataException {
        // Получение расширения, документа и описания к нему
        String extension = FilesController.checkFileExtension(fileName, allowedExtensions);
        Document document = update.getMessage().getDocument();
        String caption = update.getMessage().getCaption();
        String pathToFile = filesController.saveDocument(document, caption, extension, userPath);
        if (!pathToFile.isEmpty()) {
            String target = pathToFile.replace(path, "");
            int delimiterIndex = target.indexOf(delimiter);
            String folder = target.substring(0, delimiterIndex);
            String file = target.substring(delimiterIndex + 1);
            fileTrackerRepository.putFileInfo(chatId, folder, file);
            System.out.printf("Сохранен документ от пользователя %d%nДокумент: %s%n", chatId, document.getFileName());
            checkMessageBeforeResponse(chatId, "DocumentSaved");
        } else {
            checkMessageBeforeResponse(chatId, "SimpleError");
        }
    }

    private void updateHasDocument(Update update) {
        long chatId = userController.getChatId(update);

        // Убираем статус добавления новой папки
        userRepository.updateCanAddFolder(chatId, (byte) 0);

        // Проверка выбранного пользователем пути
        String userPath = userRepository.getFilePath(chatId);
        if (userPath.isEmpty()) {
            checkMessageBeforeResponse(chatId, "PathCheckError");
            return;
        }
        userPath = path + userPath;
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
        System.out.printf("sendEditMessageResponse(chatId : %d | data : %s | messageId : %d)%n", chatId, data, messageId);
        EditMessageText message;
        switch (data) {
            case "Help" -> {
                message = setEditMessageWithoutMarkup(chatId, helpResponse, messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
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
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LessonMenu));
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
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
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
                userRepository.updateCanAddFolder(chatId, (byte) 1);
                message = setEditMessageWithoutMarkup(chatId, "Отправьте название папки", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "AddGroupButton" -> {
                userRepository.updateCanAddGroup(chatId, (byte) 1);
                message = setEditMessageWithoutMarkup(chatId, "Отправьте название группы", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "TodayScheduleButton" -> {
                String groupId = userRepository.getGroupId(chatId);
                if (groupId.isEmpty()) {
                    sendEditMessageResponse(chatId, "GroupNotSelected", messageId);
                    return;
                }
                String scheduleToday = scheduleCache.getScheduleToday(groupId);
                message = setEditMessageWithoutMarkup(chatId, scheduleToday, messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LessonMenu));
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("The new text equals with old: Error (TBotClass (method sendEditMessage(TodaySchedule))) " + e);
                } catch (IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                }
                return;
            }
            case "TomorrowScheduleButton" -> {
                String groupId = userRepository.getGroupId(chatId);
                if (groupId.isEmpty()) {
                    sendEditMessageResponse(chatId, "GroupNotSelected", messageId);
                    return;
                }
                String scheduleTomorrow = scheduleCache.getScheduleTomorrow(groupId);
                message = setEditMessageWithoutMarkup(chatId, scheduleTomorrow, messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LessonMenu));
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
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
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
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(SelectYearButtons))) " + e);
                }
                return;
            }
            case "SimpleError" -> {
                message = setEditMessageWithoutMarkup(chatId, "Произошла неожиданная ошибка", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(SelectYearButtons))) " + e);
                }
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
            FileDTO fileDTO = fileTrackerRepository.getFileInfoByFileId(fileId);
            String correctPath = fileDTO.getFolder() + delimiter + fileDTO.getFileName();
            try {
                filesController.deleteFile(correctPath);
                if (fileTrackerRepository.deleteUserFileFromRepository(fileId)) {
                    message = setEditMessageWithoutMarkup(chatId, "Файл удален!", messageId);
                    try {
                        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                        execute(message);
                    } catch (TelegramApiException | IllegalArgumentException e) {
                        System.err.printf("Error (TBotClass (method sendEditMessageResponse(chatId : %d, data : %s)))%n%s%n", chatId, data, e);
                    }
                } else {
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
            } catch (IOException e) {
                System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("_LDel")) {
            long id = Long.parseLong(data.replaceAll("_LDel", ""));
            boolean isDeleted = linksRepository.deleteLinkById(id);
            if (isDeleted) {
                message = setEditMessageWithoutMarkup(chatId, "Ссылка удалена!", messageId);
                try {
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                    execute(message);
                } catch (TelegramApiException | IllegalArgumentException e) {
                    System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
            } else {
                sendEditMessageResponse(chatId, "ErrorDeleteLinks", messageId);
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
            String folder = data.replaceAll("AddFileButton$", "");
            userRepository.updateFilePath(chatId, folder);
            message = setEditMessageWithoutMarkup(chatId, "Теперь отправленные вами файлы будут сохраняться в эту папку: " + folder, messageId);
            try {
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                System.err.printf("Error (TBotClass (method sendEditMessage(data : %s)))%n%s%n", data, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("AddLinkButton")) {
            String group = data.replace("AddLinkButton", "");
            userRepository.updateGroupForLinks(chatId, group);
            userRepository.updateCanAddLink(chatId, (byte) 1);
            message = setEditMessageWithoutMarkup(chatId, "Отправьте название ссылки и саму ссылку в виде\n" +
                    "название_ссылки:ссылка", messageId);
            try {
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK));
                execute(message);
            } catch (TelegramApiException | IllegalArgumentException e) {
                userRepository.updateCanAddLink(chatId, (byte) 0);
                System.err.printf("Error (TBotClass (method sendEditMessageResponse(data : %s))) chatId : %d%n%s%n", data, chatId, e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("_lnk")) {

            DeleteMessageBuilder deleteMessage = new DeleteMessageBuilder(chatId, messageId);
            try {
                execute(deleteMessage.getMessage());
            } catch (TelegramApiException e) {
                System.err.printf("Error (TBotClass (sendEditMessageResponse(chatId : %d, data : %s)))%n%s%n", chatId, data, e);
            }

            // Получение индекса для удаления метки и получения данных
            long id = Long.parseLong(data.replaceAll("_lnk$", ""));
            String link = linksRepository.getLinkById(id) + "_lnk";

            sendNewMessageResponse(chatId, link);
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

            userRepository.updateGroupId(chatId, group);

            message = setEditMessageWithoutMarkup(chatId, "Группа сохранена", messageId);
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
            try {
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendEditMessageResponse()))");
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        }
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
        userRepository.updateCanAddFolder(chatId, (byte) 0);
        userRepository.updateCanAddGroup(chatId, (byte) 0);
        userRepository.updateCanAddLink(chatId, (byte) 0);

        String callbackQuery = update.getCallbackQuery().getData().trim();

        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        checkCallbackData(chatId, callbackQuery, messageId);
    }

    // Метод для класса FilesController
    public String getFilePath(String fileId) throws TelegramApiException {
        return execute(new GetFile(fileId)).getFilePath();
    }

    @Override
    public String getBotUsername() {
        return bot_name;
    }

    @Override
    public String getBotToken() {
        return bot_token;
    }
}
