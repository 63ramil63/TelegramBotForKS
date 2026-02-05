package org.example.bot.response;

import org.example.bot.TBot;
import org.example.bot.ban.types.ban.info.BanInfo;
import org.example.bot.message.DeleteMessageBuilder;
import org.example.bot.message.EditMessageBuilder;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.controller.FilesAndFoldersController;
import org.example.controller.LinksAndGroupsController;
import org.example.controller.UserBansController;
import org.example.controller.UserController;
import org.example.database.repository.DeletionLogRepository;
import org.example.dto.FileDTO;
import org.example.files.FilesController;
import org.example.site.manager.ScheduleManager;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class CallbackResponseHandler {

    private final TBot bot;
    private final MarkupSetter markupSetter;
    private final FilesAndFoldersController filesAndFoldersController;
    private final LinksAndGroupsController linksAndGroupsController;
    private final UserController userController;
    private final UserBansController userBansController;
    private final FilesController filesController;
    private final DeletionLogRepository deletionLogRepository;
    private final ScheduleManager scheduleManager;

    public CallbackResponseHandler(TBot bot, MarkupSetter markupSetter,
                                   FilesAndFoldersController filesAndFoldersController,
                                   LinksAndGroupsController linksAndGroupsController,
                                   UserController userController, UserBansController userBansController,
                                   FilesController filesController, DeletionLogRepository deletionLogRepository,
                                   ScheduleManager scheduleManager) {
        this.bot = bot;
        this.markupSetter = markupSetter;
        this.filesAndFoldersController = filesAndFoldersController;
        this.linksAndGroupsController = linksAndGroupsController;
        this.userController = userController;
        this.userBansController = userBansController;
        this.filesController = filesController;
        this.deletionLogRepository = deletionLogRepository;
        this.scheduleManager = scheduleManager;
    }

    public void handleCallback(long chatId, String callbackData, int messageId,
                               boolean isAdmin, String notification,
                               TextResponseHandler textHandler) {
        System.out.printf("handleCallback (chatId : %d, callbackData : %s, messageId : %d)%n", chatId, callbackData, messageId);

        // Очистка статусов пользователя
        resetUserStatuses(chatId);

        if (handleBasicCommand(chatId, callbackData, messageId, isAdmin, notification)) {
            return;
        }

        // Обработка callback по типам
        if (callbackData.endsWith("_Folder") || callbackData.equals("FileButton")) {
            handleFolderNavigation(chatId, callbackData, messageId);
        } else if (callbackData.contains("DeleteFileButton") || callbackData.contains("FilesDelAdm")) {
            handleFileDeletionMenu(chatId, callbackData, messageId);
        } else if (callbackData.contains("ScheduleDay")) {
            handleScheduleDay(chatId, messageId);
        } else if (callbackData.endsWith("_ScheduleDate")) {
            handleScheduleDate(chatId, messageId, callbackData);
        } else if (callbackData.endsWith("File")) {
            deleteMessageAndSendFile(chatId, callbackData, messageId, textHandler);
        } else if (callbackData.endsWith("_FDel")) {
            handleFileDeletion(chatId, callbackData, messageId);
        } else if (callbackData.endsWith("_LDel")) {
            handleLinkDeletion(chatId, callbackData, messageId);
        } else if (callbackData.endsWith("_DFolder")) {
            handleFolderDeletion(chatId, callbackData, messageId);
        } else if (callbackData.endsWith("_DGroup")) {
            handleGroupDeletion(chatId, callbackData, messageId);
        } else if (callbackData.endsWith("_LinkFlrDel")) {
            handleLinkDeletionMenu(chatId, callbackData, messageId);
        } else if (callbackData.endsWith("GroupForLinks")) {
            handleLinksGroupSelection(chatId, callbackData, messageId);
        } else if (callbackData.endsWith("AddFileButton")) {
            handleAddFileButton(chatId, callbackData, messageId);
        } else if (callbackData.endsWith("AddLinkButton")) {
            handleAddLinkButton(chatId, callbackData, messageId);
        } else if (callbackData.endsWith("_lnk")) {
            deleteMessageAndSendLink(chatId, callbackData, messageId, textHandler);
        } else if (callbackData.contains("Year")) {
            handleGroupSelectionFromYearList(chatId, callbackData, messageId);
        } else if (callbackData.contains("Group")) {
            handleGroupSelect(chatId, callbackData, messageId);
        } else {
            handleSimpleError(chatId, messageId);
        }
    }

    private void resetUserStatuses(long chatId) {
        userController.updateCanAddFolder(chatId, (byte) 0);
        userController.updateCanAddGroup(chatId, (byte) 0);
        userController.updateCanAddLink(chatId, (byte) 0);
    }

    private boolean handleBasicCommand(long chatId, String data, int messageId,
                                    boolean isAdmin, String notification) {
        switch (data) {
            case "Help" -> {
                handleHelpResponse(chatId, messageId, isAdmin);
                return true;
            }
            case "LessonButton" -> {
                handleLessonButton(chatId, messageId);
                return true;
            }
            case "BackButton" -> {
                handleBackButton(chatId, messageId);
                return true;
            }
            case "FileButton" -> {
                handleFileButton(chatId, messageId);
                return true;
            }
            case "AddFolderButton" -> {
                handleAddFolderButton(chatId, messageId, isAdmin);
                return true;
            }
            case "AddGroupButton" -> {
                handleAddGroupButton(chatId, messageId, isAdmin);
                return true;
            }
            case "TodayScheduleButton" -> {
                handleTodaySchedule(chatId, messageId);
                return true;
            }
            case "TomorrowScheduleButton" -> {
                handleTomorrowSchedule(chatId, messageId);
                return true;
            }
            case "SelectYearButton" -> {
                handleSelectYearButton(chatId, messageId);
                return true;
            }
            case "GetNotification" -> {
                handleGetNotification(chatId, messageId, notification);
                return true;
            }
            case "DeleteFileButton" -> {
                handleDeleteFileButton(chatId, messageId);
                return true;
            }
            case "GroupNotSelected" -> {
                handleGroupNotSelected(chatId, messageId);
                return true;
            }
            case "LinksButton" -> {
                handleLinksButton(chatId, messageId);
                return true;
            }
            case "DeleteLinksButton" -> {
                handleDeleteLinksButton(chatId, messageId);
                return true;
            }
            case "FullBan" -> {
                handleFullBan(chatId, messageId);
                return true;
            }
            case "SharingBan" -> {
                handleSharingBan(chatId, messageId);
                return true;
            }
            case "SimpleError" -> {
                handleSimpleError(chatId, messageId);
                return true;
            }
            case "AdminError" -> {
                handleAdminError(chatId, messageId);
                return true;
            }
        }
        return false;
    }

    public void handleHelpResponse(long chatId, int messageId, boolean isAdmin) {
        String adminHelp = """
                Команды :
                /sendToAll text - Отправляет всем пользователям бота сообщение с указанным текстом
                /sendNotification text - Устанавливает текст при нажатии кнопки
                /addAdmin username - Добавляет нового админа с базовыми правами
                /delete_Folder - позволяет удалить папку с файлами
                /delete_Group - позволяет удалить группу с ссылками
                /ban_user chatId banType(FULL_BAN | SHARING_BAN) причина - блокирует пользователя
                /unban_user chatId - снять бан с пользователя
                """;
        String userHelp = "Напишите /start, если что-то сломалось \n" +
                "Чтобы сохранить файл, выберите путь и скиньте файл боту\n" +
                "Старайтесь не делать названия файлов и директории слишком большими, бот может дать сбой \n" +
                "Если столкнулись с проблемой, напишите в личку @wrotoftanks63";

        EditMessageText message = createEditMessage(chatId, isAdmin ? adminHelp : userHelp, messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "Help", chatId);
    }

    public void handleBackButton(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите функцию", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "BackButton", chatId);
    }

    public void handleLessonButton(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите функцию", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LESSON_MENU));
        executeEditSafely(message, "LessonButton", chatId);
    }

    public void handleFileButton(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите вашу группу", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup("FileButton"));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    public void handleAddFolderButton(long chatId, int messageId, boolean isAdmin) {
        if (!isAdmin) {
            handleAdminError(chatId, messageId);
            return;
        }
        userController.updateCanAddFolder(chatId, (byte) 1);
        EditMessageText message = createEditMessage(chatId, "Отправьте название папки", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_FILES));
        executeEditSafely(message, "AddFolderButton", chatId);
    }

    public void handleAddGroupButton(long chatId, int messageId, boolean isAdmin) {
        if (!isAdmin) {
            handleAdminError(chatId, messageId);
            return;
        }
        userController.updateCanAddGroup(chatId, (byte) 1);
        EditMessageText message = createEditMessage(chatId, "Отправьте название группы", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_LINKS));
        executeEditSafely(message, "AddGroupButton", chatId);
    }

    public void handleScheduleDay(long chatId, int messageId) {
        String groupId = userController.getGroupId(chatId);
        if (groupId.isEmpty()) {
            handleGroupNotSelected(chatId, messageId);
            return;
        }
        // Здесь должен быть вызов scheduleCache

        EditMessageText message = createEditMessage(chatId, "Выберите день", messageId);
        message.setReplyMarkup(markupSetter.getChangeableMarkup("ScheduleDay" + groupId));
        executeEditSafely(message, "ScheduleDay", chatId);
    }

    public void handleScheduleDate(long chatId, int messageId, String key) {
        LocalDate date = null;
        try {
            date = LocalDate.parse(key.replaceAll("_ScheduleDate", ""));
        } catch (DateTimeParseException e) {
            System.err.println("Date parse error " + key.replaceAll("_ScheduleDate", ""));
        }
        String group = userController.getGroupId(chatId);
        String schedule = scheduleManager.getScheduleForDate(group, date);
        EditMessageText message = createEditMessage(chatId, schedule, messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_SCHEDULE));
        executeEditSafely(message, "ScheduleDate", chatId);
    }

    public void handleTodaySchedule(long chatId, int messageId) {
        String groupId = userController.getGroupId(chatId);
        if (groupId.isEmpty()) {
            handleGroupNotSelected(chatId, messageId);
            return;
        }
        // Здесь должен быть вызов scheduleCache
        String schedule = scheduleManager.getScheduleForDate(groupId, LocalDate.now());
        EditMessageText message = createEditMessage(chatId, schedule, messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LESSON_MENU));
        executeEditSafely(message, "TodayScheduleButton", chatId);
    }

    public void handleTomorrowSchedule(long chatId, int messageId) {
        String groupId = userController.getGroupId(chatId);
        if (groupId.isEmpty()) {
            handleGroupNotSelected(chatId, messageId);
            return;
        }
        // Здесь должен быть вызов scheduleCache
        String schedule = scheduleManager.getScheduleForDate(groupId, LocalDate.now());
        EditMessageText message = createEditMessage(chatId, schedule, messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LESSON_MENU));
        executeEditSafely(message, "TomorrowScheduleButton", chatId);
    }

    public void handleSelectYearButton(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите курс", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup("Year"));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    public void handleGetNotification(long chatId, int messageId, String notification) {
        EditMessageText message = createEditMessage(chatId, notification, messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "GetNotification", chatId);
    }

    public void handleDeleteFileButton(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите файл, который хотите удалить", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup("DeleteFileButton" + chatId));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    public void handleGroupNotSelected(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "У вас не выбрана группа \n Выберите курс", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup("Year"));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    public void handleLinksButton(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите группу", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup("LinksButton"));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    public void handleDeleteLinksButton(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите группу", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup("DeleteLinksButton" + chatId));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    public void handleSimpleError(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Произошла неожиданная ошибка", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "SimpleError", chatId);
    }

    public void handleAdminError(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "У вас нет прав для этого действия", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "AdminError", chatId);
    }

    public void handleFullBan(long chatId, int messageId) {
        // Получаем BanInfo из userBansController
        BanInfo banInfo = userBansController.getUserBanInfo(chatId);
        if (banInfo != null) {
            String text = "Вы заблокированы, причина: \n" + banInfo.getReason();
            EditMessageText message = createEditMessage(chatId, text, messageId);
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
            executeEditSafely(message, "FullBan", chatId);
        } else {
            handleSimpleError(chatId, messageId);
        }
    }

    public void handleSharingBan(long chatId, int messageId) {
        // Получаем BanInfo из userBansController
        BanInfo banInfo = userBansController.getUserBanInfo(chatId);
        if (banInfo != null) {
            String text = "Вы не можете сохранять файлы и ссылки, причина: \n" + banInfo.getReason();
            EditMessageText message = createEditMessage(chatId, text, messageId);
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
            executeEditSafely(message, "SharingBan", chatId);
        } else {
            handleSimpleError(chatId, messageId);
        }
    }

    // Обработчики навигации и специальных действий
    private void handleFolderNavigation(long chatId, String data, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите вашу группу", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    private void handleFileDeletionMenu(long chatId, String data, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите файл, который хотите удалить", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    private void deleteMessageAndSendFile(long chatId, String data, int messageId,
                                          TextResponseHandler textHandler) {
        deleteMessage(chatId, messageId);
        textHandler.handleTextResponse(chatId, data);
    }

    private void handleFileDeletion(long chatId, String data, int messageId) {
        long fileId = Long.parseLong(data.replaceAll("_FDel$", ""));
        try {
            FileDTO fileDTO = filesAndFoldersController.getFileInfoByFileId(fileId);
            String correctPath = fileDTO.getFolder() + "/" + fileDTO.getFileName();
            filesController.deleteFile(correctPath);

            if (filesAndFoldersController.deleteUserFileFromRepository(fileId)) {
                EditMessageText message = createEditMessage(chatId, "Файл удален!", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                bot.execute(message);
            } else {
                handleSimpleError(chatId, messageId);
            }
        } catch (IOException | TelegramApiException e) {
            System.err.printf("Error deleting file: %s%n", e.getMessage());
            handleSimpleError(chatId, messageId);
        }
    }

    private void handleLinkDeletion(long chatId, String data, int messageId) {
        long linkId = Long.parseLong(data.replaceAll("_LDel", ""));
        boolean isDeleted = linksAndGroupsController.deleteLinkById(linkId);

        if (isDeleted) {
            EditMessageText message = createEditMessage(chatId, "Ссылка удалена!", messageId);
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
            executeEditSafely(message, "LinkDeletion", chatId);
        } else {
            handleErrorDeleteLinks(chatId, messageId);
        }
    }

    private void handleFolderDeletion(long chatId, String data, int messageId) {
        long folderId = Long.parseLong(data.replaceAll("_DFolder$", ""));
        String folderName = filesAndFoldersController.getFolderNameById(folderId);

        if (filesAndFoldersController.deleteFolderById(folderId)) {
            try {
                filesController.deleteFolderWithFiles(folderName);
                deletionLogRepository.addDeletionLog(chatId, "Deleted a folder -> " + folderName);

                EditMessageText message = createEditMessage(chatId, "Папка успешно удалена", messageId);
                message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
                bot.execute(message);
            } catch (IOException | TelegramApiException e) {
                handleSimpleError(chatId, messageId);
            }
        } else {
            handleSimpleError(chatId, messageId);
        }
    }

    private void handleGroupDeletion(long chatId, String data, int messageId) {
        long groupId = Long.parseLong(data.replaceAll("_DGroup$", ""));
        String groupName = linksAndGroupsController.getGroupNameById(groupId);

        linksAndGroupsController.deleteGroupWithLinksByGroupId(groupId);
        deletionLogRepository.addDeletionLog(chatId, "Delete group -> " + groupName);

        try {
            EditMessageText message = createEditMessage(chatId, "Группа с ссылками удалена", messageId);
            message.setReplyMarkup(markupSetter.getChangeableMarkup("LinksButton"));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    private void handleLinkDeletionMenu(long chatId, String data, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите ссылку, которую хотите удалить", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    private void handleLinksGroupSelection(long chatId, String data, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите нужную вам ссылку", messageId);
        try {
            message.setReplyMarkup(markupSetter.getChangeableMarkup(data));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    private void handleAddFileButton(long chatId, String data, int messageId) {
        String folder = data.replaceAll("AddFileButton$", "");
        userController.updateFilePath(chatId, folder);

        EditMessageText message = createEditMessage(chatId,
                "Теперь отправленные вами файлы будут сохраняться в эту папку: " + folder,
                messageId);
        try {
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_FILES));
            bot.execute(message);
        } catch (Exception e) {
            handleSimpleError(chatId, messageId);
        }
    }

    private void handleAddLinkButton(long chatId, String data, int messageId) {
        String group = data.replace("AddLinkButton", "");
        userController.updateGroupForLinks(chatId, group);
        userController.updateCanAddLink(chatId, (byte) 1);

        EditMessageText message = createEditMessage(chatId,
                "Отправьте название ссылки и саму ссылку в виде\nназвание_ссылки:ссылка",
                messageId);
        try {
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.ONLY_BACK_TO_LINKS));
            bot.execute(message);
        } catch (Exception e) {
            userController.updateCanAddLink(chatId, (byte) 0);
            handleSimpleError(chatId, messageId);
        }
    }

    private void deleteMessageAndSendLink(long chatId, String data, int messageId,
                                          TextResponseHandler textHandler) {
        deleteMessage(chatId, messageId);
        textHandler.handleTextResponse(chatId, data + "_lnk");
    }

    private void handleGroupSelectionFromYearList(long chatId, String data, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Выберите вашу группу", messageId);
        int index = data.indexOf("Year");
        String groupNum = data.substring(index).replace("Year", "");
        message.setReplyMarkup(markupSetter.getChangeableMarkup("Group" + groupNum));
        executeEditSafely(message, "YearSelectionFromList", chatId);
    }

    private void handleGroupSelect(long chatId, String data, int messageId) {
        int index = data.indexOf("Group");
        if (index == -1) {
            handleSimpleError(chatId, messageId);
            return;
        }

        String group;
        if (data.length() > index + 5) {
            group = data.substring(index + 5); // Берем все после "Group"
        } else {
            handleSimpleError(chatId, messageId);
            return;
        }

        // Если есть "=", удаляем его
        if (group.startsWith("=")) {
            group = group.substring(1);
        }

        userController.updateGroupId(chatId, group);

        EditMessageText message = createEditMessage(chatId, "Группа сохранена", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "GroupSelection", chatId);
    }

    private void handleErrorDeleteLinks(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Возникла ошибка при удалении ссылки", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "ErrorDeleteLinks", chatId);
    }

    // Вспомогательные методы
    private EditMessageText createEditMessage(long chatId, String data, int messageId) {
        EditMessageBuilder editMessageBuilder = new EditMessageBuilder(chatId, data, messageId);
        return editMessageBuilder.getMessage();
    }

    public void deleteMessage(long chatId, int messageId) {
        DeleteMessageBuilder message = new DeleteMessageBuilder(chatId, messageId);
        try {
            bot.execute(message.getMessage());
        } catch (TelegramApiException e) {
            System.err.println("Error deleting message: " + e);
            handleSimpleError(chatId, messageId);
        }
    }

    private void executeEditSafely(EditMessageText message, String context, long chatId) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            System.err.printf("Error (CallbackResponseHandler - context: %s, chatId: %d): %s%n",
                    context, chatId, e.getMessage());
        }
    }
}