package org.example.bot.message;

import org.example.bot.TBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;

public class SendMessageWithDocBuilder {
    SendDocument sendDocument = new SendDocument();

    public SendMessageWithDocBuilder(long chatId, String fileName) {
        sendDocument.setChatId(chatId);
        String correctFileName = fileName.replaceAll("File$", "");
        sendDocument.setDocument(new InputFile(new File(TBot.path + correctFileName)));
    }

    public SendDocument getSendDocument() {
        return sendDocument;
    }
}
