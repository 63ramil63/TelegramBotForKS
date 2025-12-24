package org.example.bot.message.markup;

import org.example.bot.TBot;
import org.example.bot.message.markup.button.ButtonSetter;
import org.example.controller.UserController;
import org.example.database.repository.FileTrackerRepository;
import org.example.database.repository.FolderRepository;
import org.example.database.repository.GroupRepository;
import org.example.database.repository.LinksRepository;
import org.example.dto.FileDTO;
import org.example.dto.FolderDTO;
import org.example.dto.GroupDTO;
import org.example.dto.LinkDTO;
import org.example.files.FilesController;
import org.example.site.WebSite;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MarkupSetter {

    private final FilesController filesController;
    private final FileTrackerRepository fileTrackerRepository;
    private final FolderRepository folderRepository;
    private final UserController userController;

    private final LinksRepository linksRepository;
    private final GroupRepository groupRepository;

    private static InlineKeyboardButton backButtonToMainMenu;
    private static InlineKeyboardButton addNewGroupButton;
    private static InlineKeyboardButton backButtonToFiles;
    private static InlineKeyboardButton backButtonToLinks;
    private static InlineKeyboardButton backButtonToLessons;

    public MarkupSetter(FilesController filesController, FileTrackerRepository fileTrackerRepository,
                        FolderRepository folderRepository, UserController userController, LinksRepository linksRepository, GroupRepository groupRepository) {
        this.filesController = filesController;
        this.fileTrackerRepository = fileTrackerRepository;
        this.folderRepository = folderRepository;
        this.userController = userController;
        this.linksRepository = linksRepository;
        this.groupRepository = groupRepository;
        backButtonToMainMenu = ButtonSetter.setButton("Назад", "BackButton");
        backButtonToFiles = ButtonSetter.setButton("Назад", "FileButton");
        backButtonToLinks = ButtonSetter.setButton("Назад", "LinksButton");
        backButtonToLessons = ButtonSetter.setButton("Назад", "LessonButton");
        addNewGroupButton = ButtonSetter.setButton("Добавить группу", "AddGroupButton");
    }

    private final ConcurrentHashMap<MarkupKey, InlineKeyboardMarkup> savedBasicMarkup = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InlineKeyboardMarkup> savedChangeableMarkup = new ConcurrentHashMap<>();

    public InlineKeyboardMarkup getBasicMarkup(MarkupKey key) {
        if (!checkSavedMarkup(key)) {
            setMarkup(key);
        }
        return savedBasicMarkup.get(key);
    }

    private boolean checkSavedMarkup(MarkupKey key) {
        return savedBasicMarkup.containsKey(key);
    }

    private void setMarkup(MarkupKey key) {
        switch (key) {
            case MarkupKey.MainMenu -> savedBasicMarkup.put(key, getMainMenuButtons());
            case MarkupKey.LessonMenu -> savedBasicMarkup.put(key, getLessonMenuButtons());
            case MarkupKey.ONLY_BACK -> savedBasicMarkup.put(key, getOnlyCancelButton());
            case MarkupKey.NONE -> savedBasicMarkup.put(key, getNoneMarkup());
            case MarkupKey.ONLY_BACK_TO_FILES -> savedBasicMarkup.put(key, getOnlyBackToFileButton());
            case MarkupKey.ONLY_BACK_TO_LINKS -> savedBasicMarkup.put(key, getOnlyBackToLinksButton());
        }
    }

    private InlineKeyboardMarkup getNoneMarkup() {
        return new InlineKeyboardMarkup();
    }

    private InlineKeyboardMarkup getOnlyCancelButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(ButtonSetter.setRow(backButtonToMainMenu));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getOnlyBackToFileButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(ButtonSetter.setRow(backButtonToFiles));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getOnlyBackToLinksButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(ButtonSetter.setRow(backButtonToLinks));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private boolean userIsAdmin(long chatId) {
        return userController.checkAdmin(userController.getUsername(chatId));
    }

    private InlineKeyboardMarkup getLessonMenuButtons() {

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        //создание кнопок и добавление к ним возвращаемого значения при нажатии
        InlineKeyboardButton today = ButtonSetter.setButton("На сегодня", "TodayScheduleButton");
        InlineKeyboardButton tomorrow = ButtonSetter.setButton("На завтра", "TomorrowScheduleButton");
        keyboard.add(ButtonSetter.setRow(today, tomorrow));

        InlineKeyboardButton selectYear = ButtonSetter.setButton("Выбрать курс", "SelectYearButton");
        //кнопка для выбора курса
        keyboard.add(ButtonSetter.setRow(selectYear));

        InlineKeyboardButton siteButton = new InlineKeyboardButton();
        siteButton.setText("На сайт колледжа");
        siteButton.setUrl("https://lk.ks.psuti.ru/");
        keyboard.add(ButtonSetter.setRow(siteButton));

        //кнопка для возвращения в глав меню
        keyboard.add(ButtonSetter.setRow(backButtonToMainMenu));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getMainMenuButtons() {
        //создание кнопки и установка текста и возвращаемого значения при нажатии
        InlineKeyboardButton lessonButton = ButtonSetter.setButton("Расписание", "LessonButton");
        InlineKeyboardButton fileButton = ButtonSetter.setButton("Файлы", "FileButton");
        InlineKeyboardButton linksButton = ButtonSetter.setButton("Ссылки", "LinksButton");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        //добавляем ряд кнопок в клавиатуру
        keyboard.add(ButtonSetter.setRow(lessonButton));
        keyboard.add(ButtonSetter.setRow(fileButton, linksButton));

        InlineKeyboardButton notificationButton = ButtonSetter.setButton("Объявления", "GetNotification");
        keyboard.add(ButtonSetter.setRow(notificationButton));

        InlineKeyboardButton helpButton = ButtonSetter.setButton("Помощь", "Help");
        keyboard.add(ButtonSetter.setRow(helpButton));

        //создание самого объекта клавиатуры, к которому все добавляем
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup setMarkupFromList(List<String> obj, String indexValue) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (String o : obj) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            int index = o.indexOf(indexValue);
            String num = o.substring(index);
            o = o.replace(num, "");
            InlineKeyboardButton button = ButtonSetter.setButton(o, o + num);
            row.add(button);
            keyboard.add(row);
        }
        keyboard.add(ButtonSetter.setRow(backButtonToLessons));
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Получение файлов, которые сохранил пользователь и устанавливаем их как кнопки к сообщению
    private InlineKeyboardMarkup getDeleteUserFilesMarkup(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        // Получение всех файлов, отправленных пользователем
        List<FileDTO> filesDTOs = fileTrackerRepository.getAllUserFiles(chatId);
        if (filesDTOs != null && !filesDTOs.isEmpty()) {
            for (FileDTO fileDTO : filesDTOs) {
                String filePath = fileDTO.getFolder() + TBot.delimiter + fileDTO.getFileName();
                long fileId = fileDTO.getId();
                InlineKeyboardButton button = ButtonSetter.setButton(filePath, fileId + "_FDel");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        keyboard.add(ButtonSetter.setRow(backButtonToFiles));
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Задание клавиатуры для выбора папки, из которой будут удаляться файлы
    private InlineKeyboardMarkup getSelectFolderToDeleteFilesByAdmin() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<FolderDTO> folders = folderRepository.getFolders();
        if (folders != null && !folders.isEmpty()) {
            for (FolderDTO folder : folders) {
                long id = folder.getId();
                String folderName = folder.getFolder();
                InlineKeyboardButton button = ButtonSetter.setButton(folderName, id + "FilesDelAdm");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        keyboard.add(ButtonSetter.setRow(backButtonToFiles));
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Задание клавиатуры для удаления файлов админом
    private InlineKeyboardMarkup getDeleteFilesFromFolderByAdm(long id) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        String folderName = folderRepository.getFolderNameById(id);
        List<FileDTO> files = fileTrackerRepository.getFilesByFolderName(folderName);
        if (files != null && !files.isEmpty()) {
            for (FileDTO file : files) {
                long fileId = file.getId();
                String fileName = file.getFileName();
                InlineKeyboardButton button = ButtonSetter.setButton(fileName, fileId + "_FDel");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        keyboard.add(ButtonSetter.setRow(backButtonToFiles));
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Проверка является ли пользователь админом и последующие шаги
    private InlineKeyboardMarkup getDeleteFilesMarkup(String key) {
        if (key.contains("DeleteFileButton")) {
            long chatId = Long.parseLong(key.replaceAll("DeleteFileButton", ""));
            if (userIsAdmin(chatId)) {
                return getSelectFolderToDeleteFilesByAdmin();
            } else {
                return getDeleteUserFilesMarkup(chatId);
            }
        } else if (key.contains("FilesDelAdm")) {
            long folderId = Long.parseLong(key.replaceAll("FilesDelAdm", ""));
            return getDeleteFilesFromFolderByAdm(folderId);
        }
        return getBasicMarkup(MarkupKey.MainMenu);
    }

    private InlineKeyboardMarkup getLinksMainMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<GroupDTO> groups = groupRepository.getAllGroups();
        for (GroupDTO group : groups) {
            long id = group.getId();
            String groupName = group.getGroupName();
            InlineKeyboardButton button = ButtonSetter.setButton(groupName, id + "GroupForLinks");
            keyboard.add(ButtonSetter.setRow(button));
        }
        InlineKeyboardButton deleteLinksButton = ButtonSetter.setButton("Удалить ссылку", "DeleteLinksButton");
        keyboard.add(ButtonSetter.setRow(addNewGroupButton, deleteLinksButton));
        keyboard.add(ButtonSetter.setRow(backButtonToMainMenu));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getLinksFromGroup(String key) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        System.out.println(key);
        long groupId = Long.parseLong(key.replace("GroupForLinks", ""));

        String group = groupRepository.getGroupNameById(groupId);
        List<LinkDTO> links = linksRepository.getAllLinksByGroupName(group);
        if (!links.isEmpty()) {
            for (LinkDTO link : links) {
                long id = link.getId();
                String linkName = link.getLinkName();
                InlineKeyboardButton button = ButtonSetter.setButton(linkName, id + "_lnk");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        InlineKeyboardButton addLinkButton = ButtonSetter.setButton("Добавить ссылку", group + "AddLinkButton");
        keyboard.add(ButtonSetter.setRow(backButtonToLinks, addLinkButton));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getFilesFromFolderMarkup(String folder) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<FileDTO> files = filesController.getFilesFromDatabaseByFolder(folder);
        if (!files.isEmpty()) {
            for (FileDTO file : files) {
                long fileId = file.getId();
                String fileName = file.getFileName();
                InlineKeyboardButton button = ButtonSetter.setButton(fileName, fileId + "File");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        InlineKeyboardButton addFileButtons = ButtonSetter.setButton("Сделать основной: " + folder, folder + "AddFileButton");
        keyboard.add(ButtonSetter.setRow(backButtonToFiles, addFileButtons));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private List<InlineKeyboardButton> setRowForFolder(String data) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(ButtonSetter.setButton(data, data + "_Folder"));
        return row;
    }

    private InlineKeyboardMarkup getFoldersFromDatabaseMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<FolderDTO> folders = folderRepository.getFolders();
        if (folders != null && !folders.isEmpty()) {
            for (FolderDTO folder : folders) {
                String folderName = folder.getFolder();
                keyboard.add(setRowForFolder(folderName));
            }
        }
        InlineKeyboardButton deleteButton = ButtonSetter.setButton("Удалить файл", "DeleteFileButton");
        InlineKeyboardButton addFolder = ButtonSetter.setButton("Добавить папку", "AddFolderButton");
        keyboard.add(ButtonSetter.setRow(addFolder, deleteButton));

        keyboard.add(ButtonSetter.setRow(backButtonToMainMenu));

        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getDeleteLinksMarkup(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<LinkDTO> links = linksRepository.getAllLinksByUsersChatId(chatId);
        for (LinkDTO link : links) {
            long id = link.getId();
            String linkName = link.getLinkName();
            String groupName = link.getGroupName();
            InlineKeyboardButton button = ButtonSetter.setButton(groupName + " " + linkName, id + "_LDel");
            keyboard.add(ButtonSetter.setRow(button));
        }
        keyboard.add(ButtonSetter.setRow(backButtonToLinks));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getSelectGroupToDeleteLinksByAdminMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<GroupDTO> groups = groupRepository.getAllGroups();
        for (GroupDTO group : groups) {
            long id = group.getId();
            String groupName = group.getGroupName();
            InlineKeyboardButton button = ButtonSetter.setButton(groupName, id + "_LinkFlrDel");
            keyboard.add(ButtonSetter.setRow(button));
        }
        keyboard.add(ButtonSetter.setRow(backButtonToLinks));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup checkAdminBeforeSetDeleteLinksMarkup(String key) {
        long chatId = Long.parseLong(key.replaceAll("DeleteLinksButton", ""));
        if (userIsAdmin(chatId)) {
            return getSelectGroupToDeleteLinksByAdminMarkup();
        } else {
            return getDeleteLinksMarkup(chatId);
        }
    }

    private InlineKeyboardMarkup setLinksForDeleteFromGroup(long groupId) {
        String group = groupRepository.getGroupNameById(groupId);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<LinkDTO> links = linksRepository.getAllLinksByGroupName(group);
        for (LinkDTO link : links) {
            long id = link.getId();
            String linkName = link.getLinkName();
            InlineKeyboardButton button = ButtonSetter.setButton(linkName, id + "_LDel");
            keyboard.add(ButtonSetter.setRow(button));
        }
        keyboard.add(ButtonSetter.setRow(backButtonToLinks));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getDeleteFolders() throws IllegalArgumentException {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<FolderDTO> folders = folderRepository.getFolders();

        if (folders != null && !folders.isEmpty()) {
            for (FolderDTO folder : folders) {
                long folderId = folder.getId();
                String folderName = folder.getFolder();
                InlineKeyboardButton button = ButtonSetter.setButton(folderName, folderId + "_DFolder");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        keyboard.add(ButtonSetter.setRow(backButtonToFiles));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getDeleteGroups() throws IllegalArgumentException {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<GroupDTO> folders = groupRepository.getAllGroups();

        if (folders != null && !folders.isEmpty()) {
            for (GroupDTO folder : folders) {
                long groupId = folder.getId();
                String groupName = folder.getGroupName();
                InlineKeyboardButton button = ButtonSetter.setButton(groupName, groupId + "_DGroup");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        keyboard.add(ButtonSetter.setRow(backButtonToFiles));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup folderCase(String key) throws IllegalArgumentException {
        if (key.contains("_Folder")) {
            key = key.replace("_Folder", "");
            return getFilesFromFolderMarkup(key);
        }
        throw new IllegalArgumentException("Illegal argument(key) in method folderCase() MarkupSetterClass() \n " +
                "Argument is : " + key);
    }

    private InlineKeyboardMarkup fileCase(String key) throws IllegalArgumentException {
        if (key.equals("FileButton")) {
            return getFoldersFromDatabaseMarkup();
        } else if (key.contains("DeleteFileButton") || key.contains("FilesDelAdm")) {
            return getDeleteFilesMarkup(key);
        } else {
            throw new IllegalArgumentException("Illegal argument(key) in method fileCase() MarkupSetterClass() \n " +
                    "Argument is : " + key);
        }
    }

    private InlineKeyboardMarkup linksCase(String key) throws IllegalArgumentException {
        if (key.equals("LinksButton")) {
            return getLinksMainMarkup();
        } else if (key.contains("DeleteLinksButton")) {
            return checkAdminBeforeSetDeleteLinksMarkup(key);
        } else if (key.contains("GroupForLinks")) {
            return getLinksFromGroup(key);
        } else if (key.endsWith("_LinkFlrDel")) {
            long groupId = Long.parseLong(key.replaceAll("_LinkFlrDel$", ""));
            return setLinksForDeleteFromGroup(groupId);
        } else {
            throw new IllegalArgumentException("Illegal argument(key) in method linkCase() MarkupSetterClass() \n " +
                    "Argument is : " + key);
        }
    }

    private InlineKeyboardMarkup commandCase(String key) throws IllegalArgumentException{
        if (key.equals("/delete_Folder")) {
            return getDeleteFolders();
        } else if (key.equals("/delete_Group")) {
            return getDeleteGroups();
        }
        throw new IllegalArgumentException("Illegal argument(key) in method commandCase() MarkupSetterClass() \n " +
                "Argument is : " + key);
    }

    public InlineKeyboardMarkup getChangeableMarkup(String key) throws IllegalArgumentException {
        if (key.startsWith("/")) {
            return commandCase(key);
        } else if (key.contains("_Folder")) {
            return folderCase(key);
        } else if (key.contains("File") && !key.contains("Links")) {
            return fileCase(key);
        } else if (key.contains("Link")) {
            return linksCase(key);
        } else if (key.contains("Year")) {
            if (!savedChangeableMarkup.containsKey("Year")) {
                WebSite webSite = new WebSite();
                List<String> yearsList = webSite.getYears();
                InlineKeyboardMarkup markup = setMarkupFromList(yearsList, "Year");
                savedChangeableMarkup.put("Year", markup);
            }
            return savedChangeableMarkup.get("Year");
        } else if (key.contains("Group")) {
            if (!savedChangeableMarkup.containsKey(key)) {
                String num = key.replace("Group", "");
                int number = Integer.parseInt(num);
                WebSite webSite = new WebSite();
                List<String> groupList = webSite.getGroups(number);
                InlineKeyboardMarkup markup = setMarkupFromList(groupList, "Group");
                savedChangeableMarkup.put("Group" + number, markup);
            }
            return savedChangeableMarkup.get(key);
        }
        throw new IllegalArgumentException("Illegal argument(key) in method getChangeableMarkup() MarkupSetterClass() \n" +
                "Argument is : " + key);
    }
}
