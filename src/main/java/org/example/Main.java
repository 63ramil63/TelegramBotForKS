package org.example;

import org.example.bot.TBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static String propertyPath = "";

    public static void main(String[] args) {
        // получаем путь в котором находится файл с конфигурацией
        propertyPath = args[0];
        if (propertyPath.isEmpty() || propertyPath == null) {
            System.out.println("Property path cant be null");
            System.exit(101);
        }
        try {
            TelegramBotsApi tBot = new TelegramBotsApi(DefaultBotSession.class);
            tBot.registerBot(new TBot());
        } catch (TelegramApiException e) {
            System.out.println("Register bot error: " + e);
            System.exit(100);
        }
    }
}