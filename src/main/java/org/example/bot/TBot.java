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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
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
            System.err.println("Error (TBotClass (method loadConfig())) " + e);
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
        if (key != null) {
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
        System.out.println("Data is : " + data);
        SendMessage sendMessage;
        switch (data) {
            case "DocumentSaved" -> {
                sendMessage = setSendMessage(chatId, "Файл сохранен", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'DocumentSaved'))) " + e);
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "IncorrectFileExtension" -> {
                sendMessage = setSendMessage(chatId, "Неверный формат файла, попробуйте поменять название или тип файла", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'IncorrectFileExtension'))) " + e);
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
            case "FileTooBig" -> {
                sendMessage = setSendMessage(chatId, "Файл слишком большой!", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'FileTooBig'))) " + e);
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
                    checkMessageBeforeResponse(chatId, "SimpleError");
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'FolderAdded'))) " + e);
                }
                return;
            }
            case "SimpleError" -> {
                sendMessage = setSendMessage(chatId, "Произошла неизвестная ошибка", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'SimpleError'))) " + e);
                }
                return;
            }
            case "AdminError" -> {
                sendMessage = setSendMessage(chatId, "У вас нет прав на использование команды", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'AdminError'))) " + e);
                }
                return;
            }
            case "AdminAdded" -> {
                sendMessage = setSendMessage(chatId, "Админ добавлен", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'AdminAdded'))) " + e);
                }
                return;
            }
            case "PathCheckError" -> {
                sendMessage = setSendMessage(chatId, "Для хранения файлов выберите папку своей группы", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'PathCheckError'))) " + e);
                }
                return;
            }
            case "InvalidFileName" -> {
                sendMessage = setSendMessage(chatId, "Возникла ошибка, название файла слишком большое, сделайте его поменьше и попытайтесь снова", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'InvalidFileName'))) " + e);
                }
            }
        }
        if (data.endsWith("File")) {
            MessageWithDocBuilder message = new MessageWithDocBuilder(chatId, data);
            SendDocument sendDocument = message.getMessage();
            if (adminRepository.getAdmin(userRepository.getUserName(chatId))) {
                long userId = fileTrackerRepository.getFileInfo(data.replaceAll("File$", ""));
                String username = userRepository.getUserName(userId);
                sendDocument.setCaption("Данный файл отправлен пользователем: " +
                        (!username.isEmpty() ? username : " у данного пользователя нет username")
                        + "\nChatId: " + userId);
            }
            try {
                execute(sendDocument);
                checkMessageBeforeResponse(chatId, "/start");
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendNewMessageResponse())) " + e);
                checkMessageBeforeResponse(chatId, "SimpleError");
            }
        } else if (data.endsWith("LinkN")) {
            int index = data.lastIndexOf("LinkN");
            String link = data.substring(0, index);
            sendMessage = setSendMessage(chatId, "Вот ваша ссылка \n" + link, null);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendNewMessageResponse())) " + e);
            }
        } else if (isValidLinkFormat(data.trim())) {
            if (userRepository.getCanAddLink(chatId)) {
                String[] parts = data.split(":");
                String linkName = parts[0].trim();
                String link = parts[1].trim();
                String group = userRepository.getGroupForLinks(chatId);
                linksRepository.addLink(linkName, link, group, chatId);
                sendNewMessageResponseOnCommand(chatId, "/start");
            } else {
                sendNewMessageResponse(chatId, "SimpleError");
            }
        } else if (userRepository.getCanAddGroup(chatId)) {
            groupRepository.addNewGroup(data.trim());
            sendNewMessageResponseOnCommand(chatId, "/start");
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
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case /start))) " + e);
                    checkMessageBeforeResponse(chatId, "SimpleError");
                }
                return;
            }
        }
        if (data.contains("/sendToAll")) {
            if (!adminRepository.getAdmin(userRepository.getUserName(chatId))) {
                checkMessageBeforeResponse(chatId, "AdminError");
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
                return;
            }
            String adminUsername = data.replaceAll("/addAdmin", "").trim();
            if (userController.addBaseAdmin(adminUsername)) {
                checkMessageBeforeResponse(chatId, "AdminAdded");
            }
        }
    }

    private void checkMessageBeforeResponse(long chatId, String data) {
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
            System.out.println("Сохранен документ от пользователя " + chatId + "\n / Документ: " + document.getFileName());
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
            System.err.println("Error (TBotClass (method updateHasDocument(IncorrectExtensionException))) " + e);
            checkMessageBeforeResponse(chatId, "IncorrectFileExtension");
        } catch (FileSizeException e) {
            System.err.println("Error (TBotClass (method updateHasDocument(FileSizeException))) " + e);
            checkMessageBeforeResponse(chatId, "FileTooBig");
        } catch (TelegramApiException | IOException e) {
            System.err.println("Error (TBotClass (method updateHasDocument(TelegramApi/IOException))) " + e);
            checkMessageBeforeResponse(chatId, "SimpleError");
        } catch (InvalidCallbackDataException e) {
            System.err.println("Error (TBotClass (method updateHasDocument(InvalidCallbackDataException))) " + e);
            checkMessageBeforeResponse(chatId, "InvalidFileName");
        }
    }

    private EditMessageText setEditMessageWithoutMarkup(long chatId, String data, int messageId) {
        EditMessageBuilder editMessageBuilder = new EditMessageBuilder(chatId, data, messageId);
        return editMessageBuilder.getMessage();
    }

    private void sendEditMessageResponse(long chatId, String data, int messageId) {
        EditMessageText message;
        switch (data) {
            case "Help" -> {
                message = setEditMessageWithoutMarkup(chatId, helpResponse, messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("The new text equals with old: Error (TBotClass (method sendEditMessageResponse(Help))) " + e);
                }
                return;
            }
            case "LessonButtonPressed" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите дату", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LessonMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(LessonButtonPressed))) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "BackButtonPressed" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите функцию", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(BackButtonPressed))) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "FileButtonPressed" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите вашу группу", messageId);
                message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessage (FileButtonPressed))) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
            }
            case "AddFolderButtonPressed" -> {
                userRepository.updateCanAddFolder(chatId, (byte) 1);
                message = setEditMessageWithoutMarkup(chatId, "Отправьте название папки", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessage (AddFolderButtonPressed))) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "AddGroupButtonPressed" -> {
                userRepository.updateCanAddGroup(chatId, (byte) 1);
                message = setEditMessageWithoutMarkup(chatId, "Отправьте название группы", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessage (AddGroupButtonPressed())) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "AddLinkButtonPressed" -> {
                userRepository.updateCanAddLink(chatId, (byte) 1);
                message = setEditMessageWithoutMarkup(chatId, "Отправьте название ссылки и саму ссылку в виде\n" +
                        "'название ссылки':'ссылка'", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method AddLinkButtonPressed())) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "TodayScheduleButtonPressed" -> {
                String groupId = userRepository.getGroupId(chatId);
                if (groupId.isEmpty()) {
                    sendEditMessageResponse(chatId, "GroupNotSelected", messageId);
                    return;
                }
                String scheduleToday = scheduleCache.getScheduleToday(groupId);
                message = setEditMessageWithoutMarkup(chatId, scheduleToday, messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LessonMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("The new text equals with old: Error (TBotClass (method sendEditMessage(TodaySchedule))) " + e);
                }
                return;
            }
            case "TomorrowScheduleButtonPressed" -> {
                String groupId = userRepository.getGroupId(chatId);
                if (groupId.isEmpty()) {
                    sendEditMessageResponse(chatId, "GroupNotSelected", messageId);
                    return;
                }
                String scheduleTomorrow = scheduleCache.getScheduleTomorrow(groupId);
                message = setEditMessageWithoutMarkup(chatId, scheduleTomorrow, messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LessonMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("The new text equals with old: Error (TBotClass (method sendEditMessage(TomorrowSchedule))) " + e);
                }
                return;
            }
            case "SelectYearButtonPressed" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите курс", messageId);
                message.setReplyMarkup(markupSetter.getChangeableMarkup("Year"));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(SelectYearButtonsPressed))) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "GetNotification" -> {
                message = setEditMessageWithoutMarkup(chatId, notification.toString(), messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("The new text equals with old: Error (TBotClass (method sendEditMessageResponse(GetNotification))) " + e);
                }
                return;
            }
            case "DeleteFileButtonPressed" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите файл, который хотите удалить", messageId);
                message.setReplyMarkup(markupSetter.getChangeableMarkup("DeleteFileButtonPressed" + chatId));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(DeleteFileButtonPressed)))");
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "GroupNotSelected" -> {
                message = setEditMessageWithoutMarkup(chatId, "У вас не выбрана группа \n Выберите курс", messageId);
                message.setReplyMarkup(markupSetter.getChangeableMarkup("Year"));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(SelectYearButtonsPressed))) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "LinksButtonPressed" -> {
                message = setEditMessageWithoutMarkup(chatId, "Выберите группу", messageId);
                message.setReplyMarkup(markupSetter.getChangeableMarkup("LinksButtonPressed"));
                try {
                   execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(LinksButtonPressed))) " + e);
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
                return;
            }
            case "SimpleError" -> {
                message = setEditMessageWithoutMarkup(chatId, "Произошла неожиданная ошибка", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(SelectYearButtonsPressed))) " + e);
                }
                return;
            }
        }

        if (data.endsWith("FilesDelAdm")) {
            message = setEditMessageWithoutMarkup(chatId, "Выберите файл, который хотите удалить", messageId);
            message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
            try {
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendEditMessageResponse(FilesDelAdm))) " + e);
            }
        } else if (data.endsWith("Del")) {
            String correctPath = data.replaceAll("Del$", "");
            try {
                filesController.deleteFile(correctPath);
                int delimiterIndex = correctPath.indexOf(delimiter);
                String folder = correctPath.substring(0, delimiterIndex);
                String file = correctPath.substring(delimiterIndex + 1);
                if (fileTrackerRepository.deleteUserFileFromRepository(folder, file)) {
                    message = setEditMessageWithoutMarkup(chatId, "Файл удален!", messageId);
                    message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MainMenu));
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        System.err.println();
                    }
                } else {
                    sendEditMessageResponse(chatId, "SimpleError", messageId);
                }
            } catch (IOException e) {
                System.err.println("Error (TBotClass (method sendEditMessageResponse(Del))) " + e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("Folder")) {
            // Установка пути у пользователя
            String filePath = data.replaceAll("Folder$", "");
            // Установка нового пути пользователя
            userRepository.updateFilePath(chatId, filePath);

            message = setEditMessageWithoutMarkup(chatId, "Выберите вашу группу", messageId);
            message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
            try {
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendEditMessage (Folder))) " + e);
                sendEditMessageResponse(chatId, "SimpleError", messageId);
            }
        } else if (data.endsWith("GroupForLinks")) {
            String group = data.replace("GroupForLinks", "").trim();
            userRepository.updateGroupForLinks(chatId, group);
            message = setEditMessageWithoutMarkup(chatId, "Выберите нужную вам ссылку", messageId);
            message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
            try {
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendEditMessageResponse(GroupForLinks))) \n data: " + data + " \n" + e);
            }
        } else if (data.contains("LinkN")) {

            DeleteMessageBuilder deleteMessage = new DeleteMessageBuilder(chatId, messageId);
            try {
                execute(deleteMessage.getMessage());
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (sendEditMessageResponse())) " + e);
            }

            // Получение индекса для удаления метки и получения данных
            int index = data.lastIndexOf("LinkN");
            String group = data.substring(index + "LinkN".length());
            String linkName = data.substring(0, index);
            String link = linksRepository.getLinkByNameAndGroup(linkName, group) + "LinkN";

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
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendEditMessageResponse(Year)))");
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
        if (data.contains("Folder") || data.equals("FileButtonPressed")) {
            sendEditMessageResponse(chatId, data, messageId);
        } else if (data.contains("DeleteFileButtonPressed") || data.contains("FilesDelAdm")) {
            sendEditMessageResponse(chatId, data, messageId);
        } else if (data.contains("File")) {
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
