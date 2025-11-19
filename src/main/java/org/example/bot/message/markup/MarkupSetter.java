package org.example.bot.message.markup;

import org.example.bot.TBot;
import org.example.bot.message.markup.button.ButtonSetter;
import org.example.controller.UserController;
import org.example.database.repository.FileTrackerRepository;
import org.example.database.repository.GroupRepository;
import org.example.database.repository.LinksRepository;
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
    private final UserController userController;

    private final LinksRepository linksRepository;
    private final GroupRepository groupRepository;

    private static InlineKeyboardButton backButton;
    private static InlineKeyboardButton addLinkButton;

    public MarkupSetter(FilesController filesController, FileTrackerRepository fileTrackerRepository,
                        UserController userController, LinksRepository linksRepository, GroupRepository groupRepository) {
        this.filesController = filesController;
        this.fileTrackerRepository = fileTrackerRepository;
        this.userController = userController;
        this.linksRepository = linksRepository;
        this.groupRepository = groupRepository;
        backButton = ButtonSetter.setButton("Назад", "BackButtonPressed");
        addLinkButton = ButtonSetter.setButton("Добавить ссылку", "AddNewLink");
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
        }
    }

    private InlineKeyboardMarkup getLessonMenuButtons() {

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        //создание кнопок и добавление к ним возвращаемого значения при нажатии
        InlineKeyboardButton today = ButtonSetter.setButton("На сегодня", "TodayScheduleButtonPressed");
        InlineKeyboardButton tomorrow = ButtonSetter.setButton("На завтра", "TomorrowScheduleButtonPressed");
        keyboard.add(ButtonSetter.setRow(today, tomorrow));

        InlineKeyboardButton selectYear = ButtonSetter.setButton("Выбрать курс", "SelectYearButtonPressed");
        //кнопка для выбора курса
        keyboard.add(ButtonSetter.setRow(selectYear));

        InlineKeyboardButton siteButton = new InlineKeyboardButton();
        siteButton.setText("На сайт колледжа");
        siteButton.setUrl("https://lk.ks.psuti.ru/");
        keyboard.add(ButtonSetter.setRow(siteButton));

        //кнопка для возвращения в глав меню
        keyboard.add(ButtonSetter.setRow(backButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getMainMenuButtons() {
        //создание кнопки и установка текста и возвращаемого значения при нажатии
        InlineKeyboardButton lessonButton = ButtonSetter.setButton("Расписание", "LessonButtonPressed");
        InlineKeyboardButton fileButton = ButtonSetter.setButton("Файлы", "FileButtonPressed");
        InlineKeyboardButton linksButton = ButtonSetter.setButton("Ссылки", "LinksButtonPressed");

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
        keyboard.add(ButtonSetter.setRow(backButton));
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Получение файлов, которые сохранил пользователь и устанавливаем их как кнопки к сообщению
    private InlineKeyboardMarkup setDeleteUserFilesMarkup(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        // Получение всех файлов, отправленных пользователем
        List<String> filesPath = fileTrackerRepository.getAllUserFiles(chatId);
        if (filesPath != null && !filesPath.isEmpty()) {
            for (String filePath : filesPath) {
                InlineKeyboardButton button = ButtonSetter.setButton(filePath, filePath + "Del");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        keyboard.add(ButtonSetter.setRow(backButton));
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Задание клавиатуры для выбора папки, из которой будут удаляться файлы
    private InlineKeyboardMarkup setSelectFolderToDeleteFilesByAdmin() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> folders = filesController.getFoldersFromTable();
        if (folders != null && !folders.isEmpty()) {
            for (String folder : folders) {
                InlineKeyboardButton button = ButtonSetter.setButton(folder, folder + "FilesDelAdm");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        keyboard.add(ButtonSetter.setRow(backButton));
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Задание клавиатуры для удаления файлов админом
    private InlineKeyboardMarkup setDeleteFilesFromFolderByAdm(String folder) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> files = filesController.getFilesFromDatabaseByFolder(folder);
        if (files != null && !files.isEmpty()) {
            for (String file : files) {
                InlineKeyboardButton button = ButtonSetter.setButton(file, folder + TBot.delimiter + file + "Del");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        keyboard.add(ButtonSetter.setRow(backButton));
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Проверка является ли пользователь админом и последующие шаги
    private InlineKeyboardMarkup setDeleteFilesMarkup(String key) {
        if (key.contains("DeleteFileButtonPressed")) {
            long chatId = Long.parseLong(key.replaceAll("DeleteFileButtonPressed", ""));
            if (userController.checkAdmin(userController.getUsername(chatId))) {
                return setSelectFolderToDeleteFilesByAdmin();
            } else {
                return setDeleteUserFilesMarkup(chatId);
            }
        } else if (key.contains("FilesDelAdm")) {
            String folder = key.replaceAll("FilesDelAdm", "");
            return setDeleteFilesFromFolderByAdm(folder);
        }
        return getBasicMarkup(MarkupKey.MainMenu);
    }

    private InlineKeyboardMarkup setLinksMainMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> groups = groupRepository.getAllGroups();
        for (String group : groups) {
            InlineKeyboardButton button = ButtonSetter.setButton(group, group + "GroupForLinks");
            keyboard.add(ButtonSetter.setRow(button));
        }
        keyboard.add(ButtonSetter.setRow(backButton, addLinkButton));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup setLinksFromGroup(String key) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        String group = key.replace("GroupForLinks", "");
        List<String> links = linksRepository.getAllLinksByGroupName(group);
        for (String linkName : links) {
            InlineKeyboardButton button = ButtonSetter.setButton(linkName, linkName + "LinkN");
            keyboard.add(ButtonSetter.setRow(button));
        }
        keyboard.add(ButtonSetter.setRow(backButton, addLinkButton));
        markup.setKeyboard(keyboard);
        return markup;
    }

    public InlineKeyboardMarkup getChangeableMarkup(String key) {
        if (key.contains("Folder")) {
            key = key.replaceAll("Folder$", "");
            return filesController.getFilesFromFolderMarkup(key);
        } else if (key.equals("FileButtonPressed")) {
            return filesController.getFoldersFromDatabaseMarkup();
        } else if (key.equals("LinksButtonPressed")) {
            return setLinksMainMarkup();
        } else if (key.contains("DeleteFileButtonPressed") || key.contains("FilesDelAdm")) {
            return setDeleteFilesMarkup(key);
        } else if (key.contains("Year")) {
            if (!savedChangeableMarkup.containsKey("Year")) {
                WebSite webSite = new WebSite();
                List<String> yearsList = webSite.getYears();
                InlineKeyboardMarkup markup = setMarkupFromList(yearsList, "Year");
                savedChangeableMarkup.put("Year", markup);
            }
            return savedChangeableMarkup.get("Year");
        } else if (key.contains("GroupForLinks")) {
            return setLinksFromGroup(key);
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
        return null;
    }
}
