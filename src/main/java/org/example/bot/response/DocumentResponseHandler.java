package org.example.bot.response;

import org.example.bot.TBot;
import org.example.bot.ban.types.BanType;
import org.example.bot.ban.types.ban.info.BanInfo;
import org.example.bot.config.BotConfig;
import org.example.controller.FilesAndFoldersController;
import org.example.controller.UserBansController;
import org.example.controller.UserController;
import org.example.files.FilesController;
import org.example.files.exception.FileSizeException;
import org.example.files.exception.IncorrectExtensionException;
import org.example.files.exception.InvalidCallbackDataException;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public class DocumentResponseHandler {
    private final TBot bot;
    private final TextResponseHandler textResponseHandler;
    private final FilesController filesController;
    private final FilesAndFoldersController filesAndFoldersController;

    public DocumentResponseHandler(TBot bot, TextResponseHandler textResponseHandler, FilesController filesController,
                                   FilesAndFoldersController filesAndFoldersController) {
        this.bot = bot;
        this.textResponseHandler = textResponseHandler;
        this.filesController = filesController;
        this.filesAndFoldersController = filesAndFoldersController;
    }

    public void handleDocument(Update update, String fileName, String userPath, long chatId) {
        try {
            saveDocument(update, fileName, userPath, chatId);
        } catch (IncorrectExtensionException e) {
            bot.logError("IncorrectFileException", chatId, e);
            textResponseHandler.handleTextResponse(chatId, "IncorrectFileException");
        } catch (FileSizeException e) {
            bot.logError("FileSizeException", chatId, e);
            textResponseHandler.handleTextResponse(chatId, "FileSizeException");
        } catch (TelegramApiException | IOException e) {
            bot.logError("TelegramApi/IOException", chatId, e);
            textResponseHandler.handleTextResponse(chatId, "SimpleError");
        } catch (InvalidCallbackDataException e) {
            bot.logError("InvalidCallbackDataException", chatId, e);
            textResponseHandler.handleTextResponse(chatId, "InvalidFileName");
        }
    }

    private void saveDocument(Update update, String fileName, String userPath, long chatId)
            throws IncorrectExtensionException, IOException, FileSizeException,
            TelegramApiException, InvalidCallbackDataException {

        String extension = org.example.files.FilesController.checkFileExtension(
                fileName, BotConfig.getAllowedExtensions());

        Document document = update.getMessage().getDocument();
        String caption = update.getMessage().getCaption();

        String pathToFile = filesController.saveDocument(document, caption, extension, userPath);

        if (!pathToFile.isEmpty()) {
            String target = pathToFile.replace(BotConfig.getFileStoragePath(), "");
            int delimiterIndex = target.indexOf(BotConfig.getFileDelimiter());
            String folder = target.substring(0, delimiterIndex);
            String file = target.substring(delimiterIndex + 1);

            filesAndFoldersController.putFileInfo(chatId, folder, file);
            System.out.printf("Сохранен документ от пользователя %d%nДокумент: %s/%s%n",
                    chatId, folder, document.getFileName());

            textResponseHandler.handleTextResponse(chatId, "DocumentSaved");
        } else {
            textResponseHandler.handleTextResponse(chatId, "SimpleError");
        }
    }
}
