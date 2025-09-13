package org.example.bot.message;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

public class EditMessageBuilder {
    EditMessageText message = new EditMessageText();

    public EditMessageBuilder(long chatId, String data, int messageId) {
        message.setMessageId(messageId);
        message.setText(data);
        message.setChatId(chatId);
    }

    public EditMessageText getMessage() {
        return message;
    }
}
