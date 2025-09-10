package org.example.bot.message;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class MessageBuilder {
    SendMessage sendMessage = new SendMessage();

    String data;
    long chatId;

    public MessageBuilder(String data, long chatId) {
        this.data = data;
        this.chatId = chatId;
        sendMessage.setText(data);
        sendMessage.setChatId(chatId);
    }

    public SendMessage getMessage() {
        return sendMessage;
    }
}
