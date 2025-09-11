package org.example.bot;

import org.example.Main;
import org.example.bot.message.MessageBuilder;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.database.UserRepository;
import org.example.files.FilesController;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
    private final MarkupSetter markupSetter = new MarkupSetter();

    private String bot_token;
    private String bot_name;
    private int duration;
    private List<String> allowedExtensions;
    public static String path;
    public static int maxFileSize;
    public static String delimiter;

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

    private void checkAndAddUser(Update update) {
        long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getChat().getUserName();
        if (!checkUser(chatId)) {
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

        } else if (update.getMessage().hasDocument()) {

        } else {
            updateHasTextOnly(update);
        }
    }

    private void sendNewMessageResponse(long chatId, String data) {
        switch (data) {
            case "/start" -> {
                System.out.println("case start");
                MessageBuilder messageBuilder = new MessageBuilder("Добро пожаловать в бота", chatId);
                try {
                    SendMessage sendMessage = messageBuilder.getMessage();
                    sendMessage.setReplyMarkup(markupSetter.getMarkup(MarkupKey.MainMenu));
                    System.out.println("execute message");
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.err.println("Error (TBotClass (method sendNewMessageResponse(case /start))) " + e);
                }
                return;
            }
        }
        if (!FilesController.checkFileName(data)) {
            return;
        }
        if (userRepository.getCanAddFolder(chatId)) {
            FilesController filesController = new FilesController();
            filesController.addFolder(data.trim());
        }
    }

    // Если сообщение имеет только текст
    private void updateHasTextOnly(Update update) {

        long chatId = update.getMessage().getChatId();
        String data = update.getMessage().getText();
        checkAndAddUser(update);

        sendNewMessageResponse(chatId, data);
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
