package org.example.bot.response;

import org.example.bot.TBot;
import org.example.bot.ban.types.ban.info.BanInfo;
import org.example.bot.message.MessageBuilder;
import org.example.bot.message.MessageWithDocBuilder;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.controller.FilesAndFoldersController;
import org.example.controller.LinksAndGroupsController;
import org.example.controller.UserBansController;
import org.example.controller.UserController;
import org.example.dto.FileDTO;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

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

    public SendMessage createMessageWithChangeableMarkup(long chatId, String data, String markupKey) {
        MessageBuilder messageBuilder = new MessageBuilder(data, chatId);
        SendMessage sendMessage = messageBuilder.getMessage();
        sendMessage.setReplyMarkup(markupSetter.getChangeableMarkup(markupKey));
        return sendMessage;
    }

    // Основные обработчики ошибок и уведомлений
    public void sendResponse(long chatId, ResponseType responseType, Object... args) {
        switch (responseType) {
            case DOCUMENT_SAVED -> handleDocumentSaved(chatId);
            case INCORRECT_FILE_EXTENSION -> handleIncorrectFileExtension(chatId);
            case FILE_TOO_BIG -> handleFileTooBig(chatId);
            case FOLDER_ADDED -> handleFolderAdded(chatId);
            case SIMPLE_ERROR -> handleSimpleError(chatId);
            case ADMIN_ERROR -> handleAdminError(chatId);
            case PATH_CHECK_ERROR -> handlePathCheckError(chatId);
            case INVALID_FILE_NAME -> handleInvalidFileName(chatId);
            case LINK_SAVED -> handleLinkSaved(chatId);
            case GROUP_SAVED -> handleGroupSaved(chatId);
            case TOO_FEW_ARGS -> handleTooFewArgs(chatId);
            case INVALID_USER_CHAT_ID -> handleInvalidUserChatId(chatId);
            case FULL_BAN -> {
                if (args.length > 0 && args[0] instanceof BanInfo) {
                    handleFullBan(chatId, (BanInfo) args[0]);
                } else {
                    handleSimpleError(chatId);
                }
            }
            case SHARING_BAN -> {
                if (args.length > 0 && args[0] instanceof BanInfo) {
                    handleSharingBan(chatId, (BanInfo) args[0]);
                } else {
                    handleSimpleError(chatId);
                }
            }
            case ADMIN_ADDED -> handleAdminAdded(chatId);
            case START_MENU -> handleStartMenu(chatId);
            case FILE_RESPONSE -> {
                if (args.length >= 2) {
                    handleFileResponse(chatId, (String) args[0], (Boolean) args[1]);
                }
            }
            case LINK_RESPONSE -> {
                if (args.length >= 2) {
                    handleLinkResponse(chatId, (String) args[0], (Boolean) args[1]);
                }
            }
            default -> handleSimpleError(chatId);
        }
    }

    // Административные команды
    public void sendToAllUsers(List<Long> chatIds, String text) {
        for (long chatId : chatIds) {
            SendMessage message = createMessageWithMarkup(chatId, text, MarkupKey.MAIN_MENU);
            executeSafely(message, "/sendToAll", chatId);
        }
    }

    public void handleAdminCommand(long chatId, AdminCommand command, String... args) {
        if (!userController.checkAdminByChatId(chatId)) {
            handleAdminError(chatId);
            return;
        }

        switch (command) {
            case SEND_TO_ALL -> {
                if (args.length > 0) {
                    List<Long> chatIds = userController.getAllUsersChatId();
                    sendToAllUsers(chatIds, args[0]);
                }
            }
            case DELETE_FOLDER -> {
                SendMessage message = createMessageWithChangeableMarkup(chatId,
                        "Выберите папку, которую хотите удалить вместе с файлами внутри",
                        "/delete_Folder");
                executeSafely(message, "/delete_Folder", chatId);
            }
            case DELETE_GROUP -> {
                SendMessage message = createMessageWithChangeableMarkup(chatId,
                        "Выберите группу, которую хотите удалить",
                        "/delete_Group");
                executeSafely(message, "/delete_Group", chatId);
            }
            case ADD_ADMIN -> {
                if (args.length > 0 && userController.addBaseAdmin(args[0])) {
                    handleAdminAdded(chatId);
                }
            }
        }
    }

    // Приватные методы обработки
    private void handleDocumentSaved(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Файл сохранен", MarkupKey.MAIN_MENU);
        executeSafely(message, "DocumentSaved", chatId);
    }

    private void handleIncorrectFileExtension(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId,
                "Неверный формат файла, попробуйте поменять название или тип файла",
                MarkupKey.MAIN_MENU);
        executeSafely(message, "IncorrectFileExtension", chatId);
    }

    private void handleFileTooBig(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Файл слишком большой!", MarkupKey.MAIN_MENU);
        executeSafely(message, "FileTooBig", chatId);
    }

    private void handleFolderAdded(long chatId) {
        userController.updateCanAddFolder(chatId, (byte) 0);
        SendMessage message = createMessageWithMarkup(chatId, "Папка сохранена", MarkupKey.MAIN_MENU);
        executeSafely(message, "FolderAdded", chatId);
    }

    private void handleSimpleError(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Произошла неизвестная ошибка", MarkupKey.MAIN_MENU);
        executeSafely(message, "SimpleError", chatId);
    }

    private void handleAdminError(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "У вас нет прав на использование команды", MarkupKey.MAIN_MENU);
        executeSafely(message, "AdminError", chatId);
    }

    private void handleAdminAdded(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Админ добавлен", MarkupKey.MAIN_MENU);
        executeSafely(message, "AdminAdded", chatId);
    }

    private void handlePathCheckError(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId,
                "Для хранения файлов выберите папку своей группы",
                MarkupKey.MAIN_MENU);
        executeSafely(message, "PathCheckError", chatId);
    }

    private void handleInvalidFileName(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId,
                "Возникла ошибка, название файла слишком большое, сделайте его поменьше и попытайтесь снова",
                MarkupKey.MAIN_MENU);
        executeSafely(message, "InvalidFileName", chatId);
    }

    private void handleLinkSaved(long chatId) {
        userController.updateCanAddLink(chatId, (byte) 0);
        SendMessage message = createMessageWithMarkup(chatId, "Ссылка сохранена!", MarkupKey.MAIN_MENU);
        executeSafely(message, "LinkSaved", chatId);
    }

    private void handleGroupSaved(long chatId) {
        userController.updateCanAddGroup(chatId, (byte) 0);
        SendMessage message = createMessageWithMarkup(chatId, "Новая группа сохранена", MarkupKey.MAIN_MENU);
        executeSafely(message, "GroupSaved", chatId);
    }

    private void handleTooFewArgs(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Недостаточно аргументов команды", MarkupKey.MAIN_MENU);
        executeSafely(message, "TooFewArgs", chatId);
    }

    private void handleInvalidUserChatId(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Неправильный формат chatId пользователя", MarkupKey.MAIN_MENU);
        executeSafely(message, "InvalidUserChatId", chatId);
    }

    private void handleFullBan(long chatId, BanInfo banInfo) {
        String text = "Вы заблокированы, причина : \n" + banInfo.getReason();
        SendMessage message = createMessageWithMarkup(chatId, text, MarkupKey.NONE);
        executeSafely(message, "FullBan", chatId);
    }

    private void handleSharingBan(long chatId, BanInfo banInfo) {
        String text = "Вы не можете сохранять папки и ссылки, причина : \n" + banInfo.getReason();
        SendMessage message = createMessageWithMarkup(chatId, text, MarkupKey.MAIN_MENU);
        executeSafely(message, "SharingBan", chatId);
    }

    private void handleStartMenu(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Выберите функцию", MarkupKey.MAIN_MENU);
        executeSafely(message, "/start", chatId);
    }

    private void handleFileResponse(long chatId, String data, boolean isAdmin) {
        long fileId = Long.parseLong(data.replaceAll("File$", ""));
        FileDTO fileDTO = filesAndFoldersController.getFileInfoByFileId(fileId);
        MessageWithDocBuilder message = new MessageWithDocBuilder(chatId, fileDTO);
        SendDocument sendDocument = message.getMessage();

        if (isAdmin) {
            long userChatId = filesAndFoldersController.getFilesChatIdById(fileId);
            sendDocument.setCaption(userController.getUserInfo(userChatId));
        }

        SendMessage nextMessage = createMessageWithMarkup(chatId, "Выберите функцию", MarkupKey.MAIN_MENU);

        try {
            bot.execute(sendDocument);
            executeSafely(nextMessage, data, chatId);
        } catch (TelegramApiException e) {
            System.err.printf("Error sending file: %s%n", e.getMessage());
            handleSimpleError(chatId);
        }
    }

    private void handleLinkResponse(long chatId, String data, boolean isAdmin) {
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

        SendMessage nextMessage = createMessageWithMarkup(chatId, "Выберите функцию", MarkupKey.MAIN_MENU);
        executeSafely(nextMessage, data + "_menu", chatId);
    }

    private void executeSafely(SendMessage message, String data, long chatId) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            System.err.printf("Error (TextResponseHandler - data: %s, chatId: %d): %s%n",
                    data, chatId, e.getMessage());
        }
    }

    // Enum для типов ответов
    public enum ResponseType {
        DOCUMENT_SAVED,
        INCORRECT_FILE_EXTENSION,
        FILE_TOO_BIG,
        FOLDER_ADDED,
        SIMPLE_ERROR,
        ADMIN_ERROR,
        PATH_CHECK_ERROR,
        INVALID_FILE_NAME,
        LINK_SAVED,
        GROUP_SAVED,
        TOO_FEW_ARGS,
        INVALID_USER_CHAT_ID,
        FULL_BAN,
        SHARING_BAN,
        ADMIN_ADDED,
        START_MENU,
        FILE_RESPONSE,
        LINK_RESPONSE
    }

    // Enum для административных команд
    public enum AdminCommand {
        SEND_TO_ALL,
        DELETE_FOLDER,
        DELETE_GROUP,
        ADD_ADMIN
    }
}