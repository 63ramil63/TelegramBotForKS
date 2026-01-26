package org.example.bot.response;

import org.example.bot.TBot;
import org.example.bot.ban.types.BanType;
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
import org.example.role.AdminRole;
import org.example.utility.LinkUtil;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.List;

public class TextResponseHandler {

    private final TBot bot;
    private final MarkupSetter markupSetter;
    private final FilesAndFoldersController filesAndFoldersController;
    private final LinksAndGroupsController linksAndGroupsController;
    private final UserController userController;
    private final UserBansController userBansController;

    public TextResponseHandler(TBot bot, MarkupSetter markupSetter,
                               FilesAndFoldersController filesAndFoldersController,
                               LinksAndGroupsController linksAndGroupsController,
                               UserController userController, UserBansController userBansController) {
        this.bot = bot;
        this.markupSetter = markupSetter;
        this.filesAndFoldersController = filesAndFoldersController;
        this.linksAndGroupsController = linksAndGroupsController;
        this.userController = userController;
        this.userBansController = userBansController;
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

    public void handleTextResponse(long chatId, String data) {
        if (handleSpecCommand(chatId, data)) return;
        if (handleBasicCommands(chatId, data)) return;

        // Проверка бана на отправку контента (SHARING_BAN)
        if (checkBanAndRespond(chatId, BanType.SHARING_BAN)) {
            return;
        }

        // Обработка ссылок
        if (LinkUtil.isValidLinkFormat(data.trim())) {
            if (userController.getCanAddLink(chatId)) {
                saveLink(chatId, data);
            } else {
                handleSimpleError(chatId);
            }
        }
        // Обработка добавления группы
        else if (userController.getCanAddGroup(chatId)) {
            saveGroup(chatId, data);
        }
        // Обработка добавления папки
        else if (userController.getCanAddFolder(chatId)) {
            saveFolder(chatId, data);
        }
        // Обработка файлов (если text заканчивается на File или _lnk)
        else if (data.endsWith("File")) {
            handleFileResponse(chatId, data);
        } else if (data.endsWith("_lnk")) {
            handleLinkResponse(chatId, data);
        }

    }

    private boolean checkBanAndRespond(long chatId, BanType banType) {
        if (userBansController.isUserBanned(chatId)) {
            BanInfo banInfo = userBansController.getUserBanInfo(chatId);
            if (banInfo != null) {
                if (banType == BanType.FULL_BAN && banInfo.getBanType().equals(BanType.FULL_BAN.toString())) {
                    handleFullBan(chatId);
                    return true;
                } else if (banType == BanType.SHARING_BAN && banInfo.getBanType().equals(BanType.SHARING_BAN.toString())) {
                    handleSharingBan(chatId);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleBasicCommands(long chatId, String data) {
        switch (data) {
            case "DocumentSaved" -> {
                handleDocumentSaved(chatId);
                return true;
            }
            case "IncorrectFileException" -> {
                handleIncorrectFileExtension(chatId);
                return true;
            }
            case "FileSizeException" -> {
                handleFileTooBig(chatId);
                return true;
            }
            case "FolderAdded" -> {
                handleFolderAdded(chatId);
                return true;
            }
            case "SimpleError" -> {
                handleSimpleError(chatId);
                return true;
            }
            case "AdminError" -> {
                handleAdminError(chatId);
                return true;
            }
            case "PathCheckError" -> {
                handlePathCheckError(chatId);
                return true;
            }
            case "InvalidFileName" -> {
                handleInvalidFileName(chatId);
                return true;
            }
            case "LinkSaved" -> {
                handleLinkSaved(chatId);
                return true;
            }
            case "GroupSaved" -> {
                handleGroupSaved(chatId);
                return true;
            }
            case "TooFewArgs" -> {
                handleTooFewArgs(chatId);
                return true;
            }
            case "InvalidUserChatId" -> {
                handleInvalidUserChatId(chatId);
                return true;
            }
            case "FullBan" -> {
                handleFullBan(chatId);
                return true;
            }
            case "SharingBan" -> {
                handleSharingBan(chatId);
                return true;
            }
            case "AdminAdded" -> {
                handleAdminAdded(chatId);
                return true;
            }
            case "StartMenu" -> {
                handleStartMenu(chatId);
                return true;
            }
            case "FileResponse" -> {
                handleFileResponse(chatId, data);
                return true;
            }
            case "LinkResponse" -> {
                handleLinkResponse(chatId, data);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleSpecCommand(long chatId, String data) {
        String[] parts = data.split(" ");
        String cmd = parts[0];

        switch (cmd) {
            case "/start" -> {
                handleStartMenu(chatId);
                return true;
            }
            case "/sendToAll" -> {
                handleSendToAllUsers(chatId, data);
                return true;
            }
            case "/sendNotification" -> {
                handleSendNotification(chatId, data);
                return true;
            }
            case "/addAdmin" -> {
                handleAddAdmin(chatId, data);
                return true;
            }
            case "/deleteFolder" -> {
                handleDeleteFolder(chatId);
                return true;
            }
            case "/deleteGroup" -> {
                handleDeleteGroup(chatId);
                return true;
            }
            case "/ban_user" -> {
                handleBanUser(chatId, data);
                return true;
            }
            case "/unban_user" -> {
                handleUnbanUser(chatId, data);
                return true;
            }
        }
        return false;
    }

    private void saveLink(long chatId, String text) {
        String[] parts = text.split(":");
        if (parts.length >= 2) {
            String linkName = parts[0].trim();
            String link = parts[1].trim();
            String group = userController.getGroupForLinks(chatId);
            linksAndGroupsController.addLink(linkName, link, group, chatId);
            handleLinkSaved(chatId);
        }
    }

    private void saveGroup(long chatId, String text) {
        linksAndGroupsController.addNewGroup(chatId, text.trim());
        handleGroupSaved(chatId);
    }

    private void saveFolder(long chatId, String text) {
        if (!org.example.files.FilesController.checkFileName(text)) {
            handleInvalidFileName(chatId);
            return;
        }

        String folderName = text.trim();
        filesAndFoldersController.addFolder(chatId, folderName);
        handleFolderAdded(chatId);
    }

    // Административные команды
    private void handleSendToAllUsers(long chatId, String text) {
        if (!userController.checkAdminByChatId(chatId)) {
            handleAdminError(chatId);
            return;
        }
        List<Long> chatIds = userController.getAllUsersChatId();
        sendToAllUsers(chatIds, text);

        SendMessage message = createMessageWithMarkup(chatId, "Сообщения отправлены", MarkupKey.MAIN_MENU);
        executeSafely(message, "SendToAllUsers", chatId);
    }

    private void handleSendNotification(long chatId, String text) {
        if (!userController.checkAdminByChatId(chatId)) {
            handleAdminError(chatId);
            return;
        }

        SendMessage message;
        if (!text.isEmpty()) {
            text = text.replaceAll("/sendNotification", "").trim();
            bot.notification.setLength(0);
            bot.notification.append(text);
            message = createMessageWithMarkup(chatId, "Оповещение изменено", MarkupKey.MAIN_MENU);
            executeSafely(message, "SendNotification", chatId);
        } else {
            message = createMessageWithMarkup(chatId, "Текст оповещения не должен быть пустым", MarkupKey.MAIN_MENU);
            executeSafely(message, "AddAdmin", chatId);
        }
    }

    private void handleAddAdmin(long chatId, String username) {
        boolean isMainAdmin = userController.checkAdminByChatId(chatId) &&
                userController.getAdminRole(chatId).equals(AdminRole.Main.toString());

        if (!isMainAdmin) {
            handleAdminError(chatId);
            return;
        }

        SendMessage message;
        String adminUsername = username.trim();
        if (!adminUsername.isEmpty()) {
            userController.addBaseAdmin(adminUsername);
            message = createMessageWithMarkup(chatId, "Админ добавлен", MarkupKey.MAIN_MENU);
            executeSafely(message, "AddAdmin", chatId);
        } else {
            message = createMessageWithMarkup(chatId, "Username не должен быть пустым", MarkupKey.MAIN_MENU);
            executeSafely(message, "AddAdmin", chatId);
        }
    }

    private void sendToAllUsers(List<Long> chatIds, String text) {
        for (long chatId : chatIds) {
            SendMessage message = createMessageWithMarkup(chatId, text, MarkupKey.NONE);
            executeSafely(message, "/sendToAll", chatId);
        }
    }

    private void handleDeleteFolder(long chatId) {
        if (!userController.checkAdminByChatId(chatId)) {
            handleAdminError(chatId);
            return;
        }

        SendMessage message = createMessageWithChangeableMarkup(chatId,
                "Выберите папку, которую хотите удалить вместе с файлами внутри",
                "/delete_Folder");
        executeSafely(message, "/delete_Folder", chatId);
    }

    private void handleDeleteGroup(long chatId) {
        if (! userController.checkAdminByChatId(chatId)) {
            handleAdminError(chatId);
            return;
        }

        SendMessage message = createMessageWithChangeableMarkup(chatId, "Выберите группу, которую хотите удалить вместе с содержимым", "/delete_Group");
        executeSafely(message, "/delete_Group", chatId);
    }

    private void handleBanUser(long chatId, String data) {
        if (!userController.checkAdminByChatId(chatId)) {
            handleAdminError(chatId);
            return;
        }

        String[] parts = data.split(" ");
        if (parts.length < 3) {
            handleTooFewArgs(chatId);
            return;
        }

        try {
            long userChatId = Long.parseLong(parts[0]);
            if (userChatId == chatId) {
                handleInvalidUserChatId(chatId);
                return;
            }

            BanInfo banInfo = createBanInfo(parts, chatId);
            userBansController.banUser(userChatId, banInfo);
            handleStartMenu(chatId);

        } catch (NumberFormatException e) {
            handleInvalidUserChatId(chatId);
        }
    }

    private BanInfo createBanInfo(String[] args, long adminChatId) {
        System.out.println(Arrays.toString(args));
        String banType = args[1];
        if (!banType.equals(BanType.FULL_BAN.toString()) &&
                !banType.equals(BanType.SHARING_BAN.toString())) {
            banType = BanType.SHARING_BAN.toString();
        }

        StringBuilder reason = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        return BanInfo.builder()
                .banType(banType)
                .reason(reason.toString().trim())
                .adminChatId(adminChatId)
                .build();
    }

    private void handleUnbanUser(long chatId, String args) {
        if (!userController.checkAdminByChatId(chatId)) {
            handleAdminError(chatId);
            return;
        }

        String[] parts = args.split(" ");
        if (parts.length < 1) {
            handleTooFewArgs(chatId);
            return;
        }

        try {
            long userChatId = Long.parseLong(parts[0]);
            userBansController.unbanUser(userChatId, chatId);
            handleStartMenu(chatId);
        } catch (NumberFormatException e) {
            handleInvalidUserChatId(chatId);
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

    private void handleFullBan(long chatId) {
        BanInfo banInfo = userBansController.getUserBanInfo(chatId);
        String text = "Вы заблокированы, причина : \n" + banInfo.getReason();
        SendMessage message = createMessageWithMarkup(chatId, text, MarkupKey.NONE);
        executeSafely(message, "FullBan", chatId);
    }

    private void handleSharingBan(long chatId) {
        BanInfo banInfo = userBansController.getUserBanInfo(chatId);
        String text = "Вы не можете сохранять папки и ссылки, причина : \n" + banInfo.getReason();
        SendMessage message = createMessageWithMarkup(chatId, text, MarkupKey.MAIN_MENU);
        executeSafely(message, "SharingBan", chatId);
    }

    private void handleStartMenu(long chatId) {
        SendMessage message = createMessageWithMarkup(chatId, "Выберите функцию", MarkupKey.MAIN_MENU);
        executeSafely(message, "/start", chatId);
    }

    private void handleFileResponse(long chatId, String data) {
        long fileId = Long.parseLong(data.replaceAll("File$", ""));
        FileDTO fileDTO = filesAndFoldersController.getFileInfoByFileId(fileId);
        MessageWithDocBuilder message = new MessageWithDocBuilder(chatId, fileDTO);
        SendDocument sendDocument = message.getMessage();

        if (userController.checkAdminByChatId(chatId)) {
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

    private void handleLinkResponse(long chatId, String data) {
        // Удаление всех не цифровых символов в строке
        long linkId = Long.parseLong(data.replaceAll("[^0-9]+$", ""));
        String link = linksAndGroupsController.getLinkById(linkId);
        String messageText;

        if (userController.checkAdminByChatId(chatId)) {
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
}