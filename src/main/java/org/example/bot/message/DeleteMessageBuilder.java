package org.example.bot.message;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;

public class DeleteMessageBuilder {
    private DeleteMessage message = new DeleteMessage();

    public DeleteMessageBuilder(long chatId, int messageId) {
        message.setChatId(chatId);
        message.setMessageId(messageId);
    }

    public DeleteMessage getMessage() {
        return message;
    }
}
