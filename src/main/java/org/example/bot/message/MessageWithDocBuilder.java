package org.example.bot.message;

import org.example.bot.config.BotConfig;
import org.example.dto.FileDTO;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;

public class MessageWithDocBuilder {
    SendDocument sendDocument = new SendDocument();

    public MessageWithDocBuilder(long chatId, FileDTO fileDTO) {
        sendDocument.setChatId(chatId);
        String correctPath = BotConfig.getFileStoragePath() + fileDTO.getFolder() + BotConfig.getFileDelimiter() + fileDTO.getFileName();
        sendDocument.setDocument(new InputFile(new File(correctPath)));
    }

    public SendDocument getMessage() {
        return sendDocument;
    }
}
