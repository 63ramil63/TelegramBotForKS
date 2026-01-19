package org.example.bot.response;

import org.example.bot.TBot;
import org.example.bot.message.DeleteMessageBuilder;
import org.example.bot.message.EditMessageBuilder;
import org.example.bot.message.markup.MarkupKey;
import org.example.bot.message.markup.MarkupSetter;
import org.example.controller.FilesAndFoldersController;
import org.example.controller.LinksAndGroupsController;
import org.example.controller.UserController;
import org.example.dto.FileDTO;
import org.example.files.FilesController;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public class CallbackResponseHandler {
    private final TBot bot;
    private final MarkupSetter markupSetter;
    private final FilesAndFoldersController filesAndFoldersController;
    private final LinksAndGroupsController linksAndGroupsController;
    private final UserController userController;
    private final FilesController filesController;
    private final TextResponseHandler textResponseHandler;

    public CallbackResponseHandler(TBot bot, MarkupSetter markupSetter,
                                   FilesAndFoldersController filesAndFoldersController,
                                   LinksAndGroupsController linksAndGroupsController,
                                   UserController userController, FilesController filesController,
                                   TextResponseHandler textResponseHandler) {
        this.bot = bot;
        this.markupSetter = markupSetter;
        this.filesAndFoldersController = filesAndFoldersController;
        this.linksAndGroupsController = linksAndGroupsController;
        this.userController = userController;
        this.filesController = filesController;
        this.textResponseHandler = textResponseHandler;
    }

    private EditMessageText createEditMessage(long chatId, String data, int messageId) {
        EditMessageBuilder editMessageBuilder = new EditMessageBuilder(chatId, data, messageId);
        return editMessageBuilder.getMessage();
    }

    public void handleHelpResponse(long chatId, int messageId, boolean isAdmin, String adminHelp, String userHelp) {
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
        EditMessageText message = createEditMessage(chatId, "Выберите дату", messageId);
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

    public void handleScheduleToday(long chatId, int messageId, String schedule) {
        EditMessageText message = createEditMessage(chatId, schedule, messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.LESSON_MENU));
        executeEditSafely(message, "TodayScheduleButton", chatId);
    }

    public void handleScheduleTomorrow(long chatId, int messageId, String schedule) {
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

    public void handleFileDeletion(long chatId, int messageId, long fileId) throws IOException, TelegramApiException {
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
    }

    public void handleLinkDeletion(long chatId, int messageId, long linkId) {
        boolean isDeleted = linksAndGroupsController.deleteLinkById(linkId);
        if (isDeleted) {
            EditMessageText message = createEditMessage(chatId, "Ссылка удалена!", messageId);
            message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
            executeEditSafely(message, "LinkDeletion", chatId);
        } else {
            handleErrorDeleteLinks(chatId, messageId);
        }
    }

    public void handleErrorDeleteLinks(long chatId, int messageId) {
        EditMessageText message = createEditMessage(chatId, "Возникла ошибка при удалении ссылки", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "ErrorDeleteLinks", chatId);
    }

    public void handleYearSelection(long chatId, int messageId, String data) {
        EditMessageText message = createEditMessage(chatId, "Выберите вашу группу", messageId);

        int index = data.indexOf("Year");
        String num = data.substring(index);
        num = num.replace("Year", "");

        message.setReplyMarkup(markupSetter.getChangeableMarkup("Group" + num));
        executeEditSafely(message, "YearSelection", chatId);
    }

    public void handleGroupSelection(long chatId, int messageId, String data) {
        int index = data.indexOf("Group");
        String group = data.substring(index);
        group = group.replace("Group=", "");

        userController.updateGroupId(chatId, group);

        EditMessageText message = createEditMessage(chatId, "Группа сохранена", messageId);
        message.setReplyMarkup(markupSetter.getBasicMarkup(MarkupKey.MAIN_MENU));
        executeEditSafely(message, "GroupSelection", chatId);
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
