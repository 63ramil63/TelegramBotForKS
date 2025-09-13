package org.example.bot.message;

import org.example.bot.TBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;

public class MessageWithDocBuilder {
    SendDocument sendDocument = new SendDocument();

    public MessageWithDocBuilder(long chatId, String fileName) {
        sendDocument.setChatId(chatId);
        String correctFileName = fileName.replaceAll("File$", "");
        sendDocument.setDocument(new InputFile(new File(TBot.path + correctFileName)));
    }

    public SendDocument getMessage() {
        return sendDocument;
    }
}
