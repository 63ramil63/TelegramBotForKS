package org.example.bot;

import org.example.Main;
import org.example.bot.message.DeleteMessageBuilder;
import org.example.bot.message.EditMessageBuilder;
import org.example.bot.message.MessageBuilder;
import org.example.bot.message.MessageWithDocBuilder;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.database.UserRepository;
import org.example.files.FilesController;
import org.example.files.exception.FileSizeException;
import org.example.files.exception.IncorrectExtensionException;
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
    private final UserRepository userRepository = new UserRepository();
    private MarkupSetter markupSetter;
    private FilesController filesController;
    private ScheduleCache scheduleCache;

    private String bot_token;
    private String bot_name;
    private int duration;
    private List<String> allowedExtensions;
    public static String path;
    public static int maxFileSize;
    public static String delimiter;

    private final String helpResponse = "Напишите /start, если что-то сломалось \n" +
            "Чтобы сохранить файл, выберите путь и скиньте файл боту" +
            "Старайтесь не делать названия файлов и директории слишком большими, бот может дать сбой \n" +
            "Если столкнулись с проблемой, напишите в личку @wrotoftanks63";

    public TBot() {
        loadConfig();
    }

    public void loadConfig() {
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
        } catch (IOException e) {
            System.err.println("Error (TBotClass (method loadConfig())) " + e);
        }
        filesController = new FilesController(this, bot_token, delimiter, path);
        markupSetter = new MarkupSetter(filesController, path);
        scheduleCache = new ScheduleCache(duration);
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::close));
        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            scheduler.scheduleAtFixedRate(scheduleCache::clearExpiredCache, duration, duration, TimeUnit.MINUTES);
        }
    }

    private boolean checkUser(long chatId) {
        return userRepository.getUser(chatId);
    }

    private void addUser(long chatId) {
        userRepository.addUser(chatId);
    }

    private void updateUserName(long chatId, String userName) {
        userRepository.updateUserName(chatId, userName);
    }

    private long getChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        } else if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        return 0;
    }

    private void checkAndAddUser(Update update) {
        long chatId = getChatId(update);
        if (!checkUser(chatId)) {
            String userName = "undefined";
            if (update.hasCallbackQuery()) {
                userName = update.getCallbackQuery().getFrom().getUserName();
            } else if (update.hasMessage()){
                userName = update.getMessage().getChat().getUserName();
            }
            addUser(chatId);
            updateUserName(chatId, userName);
        }
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
        sendMessage.setReplyMarkup(markupSetter.getBasicMarkup(key));
        return sendMessage;
    }

    private void sendNewMessageResponse(long chatId, String data) {
        SendMessage sendMessage;
        switch (data) {
            case "/start" -> {
                sendMessage = setSendMessage(chatId, "Выберите функцию", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case /start))) " + e);
                }
                return;
            }
            case "Document saved" -> {
                sendMessage = setSendMessage(chatId, "Файл сохранен", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'incorrect file extension'))) " + e);
                }
                return;
            }
            case "Incorrect file extension" -> {
                sendMessage = setSendMessage(chatId, "Неверный формат файла, попробуйте поменять название или тип файла", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'incorrect file extension'))) " + e);
                }
                return;
            }
            case "File too big" -> {
                sendMessage = setSendMessage(chatId, "Файл слишком большой!", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'file too big'))) " + e);
                }
                return;
            }
            case "Folder added" -> {
                sendMessage = setSendMessage(chatId, "Папка сохранена", MarkupKey.MainMenu);
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case 'file too big'))) " + e);
                }
                return;
            }
        }
        if (data.contains("File")) {
            MessageWithDocBuilder message = new MessageWithDocBuilder(chatId, data);
            try {
                execute(message.getMessage());
            } catch (TelegramApiException e) {
                System.err.println("Error (TBotClass (method sendNewMessageResponse())) " + e);
            }
            sendNewMessageResponse(chatId, "/start");
        } else if (FilesController.checkFileName(data)) {
            if (userRepository.getCanAddFolder(chatId)) {
                filesController.addFolder(data.trim());
                sendNewMessageResponse(chatId, "Folder added");
            }
        }
    }

    // Если сообщение имеет только текст
    private void updateHasTextOnly(Update update) {

        long chatId = update.getMessage().getChatId();
        String data = update.getMessage().getText();
        checkAndAddUser(update);

        sendNewMessageResponse(chatId, data);
    }

    private void updateHasDocument(Update update) {
        long chatId = update.getMessage().getChatId();

        // Убираем статус добавления новой папки
        userRepository.updateCanAddFolder(chatId, (byte) 0);

        // Проверка выбранного пользователем пути
        String userPath = userRepository.getFilePath(chatId);
        if (userPath.equals("Not found")) {
            sendNewMessageResponse(chatId, "PathCheckError");
            return;
        }
        userPath = path + userPath;
        String fileName = update.getMessage().getDocument().getFileName();
        try {
            // Получение расширения, документа и описания к нему
            String extension = FilesController.checkFileExtension(fileName, allowedExtensions);
            Document document = update.getMessage().getDocument();
            String caption = update.getMessage().getCaption();
            filesController.saveDocument(document, caption, extension, userPath);
            System.out.println("Сохранен документ от пользователя " + chatId + " / Документ: " + document.getFileName());
            sendNewMessageResponse(chatId, "Document saved");
        } catch (IncorrectExtensionException e) {
            System.err.println("Error (TBotClass (method updateHasDocument())) " + e);
            sendNewMessageResponse(chatId, "Incorrect file extension");
        } catch (FileSizeException e) {
            System.err.println("Error (TBotClass (method updateHasDocument())) " + e);
            sendNewMessageResponse(chatId, "File too big");
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
                    System.err.println("Error (TBotClass (method sendEditMessageResponse(Help))) " + e);
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
                }
                return;
            }
            case "TodayScheduleButtonPressed" -> {
                String groupId = userRepository.getGroupId(chatId);
                if (groupId.equals("Not found")) {
                    //todo
                    return;
                }
                String scheduleToday = scheduleCache.getScheduleToday(groupId);
                message = setEditMessageWithoutMarkup(chatId, scheduleToday, messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LessonMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessage(TodaySchedule))) " + e);
                }
                return;
            }
            case "TomorrowScheduleButtonPressed" -> {
                String groupId = userRepository.getGroupId(chatId);
                if (groupId.equals("Not found")) {
                    //todo
                    return;
                }
                String scheduleTomorrow = scheduleCache.getScheduleTomorrow(groupId);
                message = setEditMessageWithoutMarkup(chatId, scheduleTomorrow, messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LessonMenu));
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendEditMessage(TodaySchedule))) " + e);
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
                }
                return;
            }
        }
        if (data.contains("Folder")) {
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
            }
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
            }
        }
    }

    private void deleteMessage(long chatId, int messageId) {
        DeleteMessageBuilder message = new DeleteMessageBuilder(chatId, messageId);
        try {
            execute(message.getMessage());
        } catch (TelegramApiException e) {
            System.err.println("Error (TBotClass (method deleteMessage())) " + e);
        }
    }

    private void checkCallbackData(long chatId, String data, int messageId) {
        if (data.contains("Folder") || data.equals("FileButtonPressed")) {
            sendEditMessageResponse(chatId, data, messageId);
        } else if (data.contains("File")) {
            deleteMessage(chatId, messageId);
            sendNewMessageResponse(chatId, data);
        } else {
            sendEditMessageResponse(chatId, data, messageId);
        }
    }

    private void updateHasCallbackQuery(Update update) {

        //getCallBackQuery дает те же возможности, что и message, но получить message можно только из CallBackQuery.getMessage
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        checkAndAddUser(update);

        //удаление статуса создания папки
        userRepository.updateCanAddFolder(chatId, (byte) 0);

        String callbackQuery = update.getCallbackQuery().getData();

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
