package org.example.bot.response;

import org.example.bot.TBot;
import org.example.bot.ban.types.ban.info.BanInfo;
import org.example.bot.message.MessageBuilder;
import org.example.bot.message.MessageWithDocBuilder;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.controller.FilesAndFoldersController;
import org.example.controller.LinksAndGroupsController;
import org.example.controller.UserController;
import org.example.dto.FileDTO;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TextResponseHandler {

    private final TBot bot;
    private final MarkupSetter markupSetter;
    private final FilesAndFoldersController filesAndFoldersController;
    private final LinksAndGroupsController linksAndGroupsController;
    private final UserController userController;

    public TextResponseHandler(TBot bot, MarkupSetter markupSetter,
                               FilesAndFoldersController filesAndFoldersController,
                               LinksAndGroupsController linksAndGroupsController,
                               UserController userController) {
        this.bot = bot;
        this.markupSetter = markupSetter;
        this.filesAndFoldersController = filesAndFoldersController;
        this.linksAndGroupsController = linksAndGroupsController;
        this.userController = userController;
    }

    public SendMessage createMessageWithMarkup(long chatId, String data, MarkupKey key) {
        MessageBuilder messageBuilder = new MessageBuilder(data, chatId);
        SendMessage sendMessage = messageBuilder.getMessage();
        if (key != MarkupKey.NONE) {
            sendMessage.setReplyMarkup(markupSetter.getBasicMarkup(key));
        }
        return sendMessage;
    }

    public void handleIncorrectFileExtension(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId,
                "Неверный формат файла, попробуйте поменять название или тип файла",
                MarkupKey.MAIN_MENU);
        executeSafely(message, "IncorrectFileExtension", chatId);
    }

    public void handleFileTooBig(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Файл слишком большой!", MarkupKey.MAIN_MENU);
        executeSafely(message, "FileTooBig", chatId);
    }

    public void handleFolderAdded(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Папка сохранена", MarkupKey.MAIN_MENU);
        userController.updateCanAddFolder(chatId, (byte) 0);
        executeSafely(message, "FolderAdded", chatId);
    }

    public void handleSimpleError(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Произошла неизвестная ошибка", MarkupKey.MAIN_MENU);
        executeSafely(message, "SimpleError", chatId);
    }

    public void handleAdminError(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "У вас нет прав на использование команды", MarkupKey.MAIN_MENU);
        executeSafely(message, "AdminError", chatId);
    }

    public void handlePathCheckError(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId,
                "Для хранения файлов выберите папку своей группы",
                MarkupKey.MAIN_MENU);
        executeSafely(message, "PathCheckError", chatId);
    }

    public void handleInvalidFileName(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId,
                "Возникла ошибка, название файла слишком большое, сделайте его поменьше и попытайтесь снова",
                MarkupKey.MAIN_MENU);
        executeSafely(message, "InvalidFileName", chatId);
    }

    public void handleLinkSaved(long chatId) {
        userController.updateCanAddLink(chatId, (byte) 0);
        SendMessage message = createMessageWithMarkup(chatId, "Ссылка сохранена!", MarkupKey.MAIN_MENU);
        executeSafely(message, "LinkSaved", chatId);
    }

    public void handleGroupSaved(long chatId) {
        userController.updateCanAddGroup(chatId, (byte) 0);
        SendMessage message = createMessageWithMarkup(chatId, "Новая группа сохранена", MarkupKey.MAIN_MENU);
        executeSafely(message, "GroupSaved", chatId);
    }

    public void handleTooFewArgs(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Недостаточно аргументов команды", MarkupKey.MAIN_MENU);
        executeSafely(message, "TooFewArgs", chatId);
    }

    public void handleInvalidUserChatId(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Неправильный формат chatId пользователя", MarkupKey.MAIN_MENU);
        executeSafely(message, "InvalidUserChatId", chatId);
    }

    public void handleFullBan(long chatId, BanInfo banInfo) {
        String text = "Вы заблокированы, причина : \n" + banInfo.getReason();
        SendMessage message = createMessageWithMarkup(chatId, text, MarkupKey.NONE);
        executeSafely(message, "FullBan", chatId);
    }

    public void handleSharingBan(long chatId, BanInfo banInfo) {
        String text = "Вы не можете сохранять папки и ссылки, причина : \n" + banInfo.getReason();
        SendMessage message = createMessageWithMarkup(chatId, text, MarkupKey.MAIN_MENU);
        executeSafely(message, "SharingBan", chatId);
    }

    public void handleFileResponse(long chatId, String data, boolean isAdmin) {
        long fileId = Long.parseLong(data.replaceAll("File$", ""));
        FileDTO fileDTO = filesAndFoldersController.getFileInfoByFileId(fileId);
        MessageWithDocBuilder message = new MessageWithDocBuilder(chatId, fileDTO);
        SendDocument sendDocument = message.getMessage();

        if (isAdmin) {
            long userChatId = filesAndFoldersController.getFilesChatIdById(fileId);
            sendDocument.setCaption(userController.getUserInfo(userChatId));
        }

        try {
            bot.execute(sendDocument);
        } catch (TelegramApiException e) {
            System.err.printf("Error sending file: %s%n", e.getMessage());
            handleSimpleError(chatId);
        }
    }

    public void handleLinkResponse(long chatId, String data, boolean isAdmin) {
        long linkId = Long.parseLong(data.replaceAll("_lnk$", ""));
        String link = linksAndGroupsController.getLinkById(linkId);
        String messageText;

        if (isAdmin) {
            long userChatId = linksAndGroupsController.getUsersChatIdByLinkId(linkId);
            messageText = "Вот ваша ссылка \n" + link + "\n" + userController.getUserInfo(userChatId);
        } else {
            messageText = "Вот ваша ссылка \n" + link;
        }

        SendMessage message = createMessageWithMarkup(chatId, messageText, MarkupKey.NONE);
        executeSafely(message, data, chatId);
    }

    public void handleDocumentSaved(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Файл сохранен", MarkupKey.MAIN_MENU);
        executeSafely(message, "DocumentSaved", chatId);
    }

    private void executeSafely(SendMessage message, String data, long chatId) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            System.err.printf("Error (TextResponseHandlerClass (method executeSafely(data : %s, chatId : %d))): %s%n", data, chatId, e.getMessage());
        }
    }
}
