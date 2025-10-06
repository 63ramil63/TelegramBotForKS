package org.example.bot.message.markup;

import org.example.bot.message.markup.button.ButtonSetter;
import org.example.database.repository.FileTrackerRepository;
import org.example.files.FilesController;
import org.example.site.WebSite;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MarkupSetter {

    private final FilesController filesController;
    private final String path;
    private final FileTrackerRepository fileTrackerRepository;

    public MarkupSetter(FilesController filesController, FileTrackerRepository fileTrackerRepository, String path) {
        this.filesController = filesController;
        this.path = path;
        this.fileTrackerRepository = fileTrackerRepository;
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
        InlineKeyboardButton back = ButtonSetter.setButton("Назад", "BackButtonPressed");
        keyboard.add(ButtonSetter.setRow(back));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getMainMenuButtons() {
        //создание кнопки и установка текста и возвращаемого значения при нажатии
        InlineKeyboardButton lessonButton = ButtonSetter.setButton("Расписание", "LessonButtonPressed");
        InlineKeyboardButton fileButton = ButtonSetter.setButton("Файлы", "FileButtonPressed");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        //добавляем ряд кнопок в клавиатуру
        keyboard.add(ButtonSetter.setRow(fileButton, lessonButton));

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
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(ButtonSetter.setButton("Назад", "LessonButtonPressed"));
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Получение файлов, которые сохранил пользователь и устанавливаем их как кнопки к сообщению
    private InlineKeyboardMarkup setDeleteFilesMarkup(String key) {
        // Удаляем лишние символы
        long chatId = Integer.parseInt(key.replaceAll("DeleteFileButtonPressed", ""));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        // Получение всех файлов, отправленных пользователем
        List<String> filesPath = fileTrackerRepository.getAllUserFiles(chatId);
        for (String filePath : filesPath) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = ButtonSetter.setButton(filePath, filePath + "Del");
            row.add(button);
            keyboard.add(row);
        }
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(ButtonSetter.setButton("Назад", "BackButtonPressed"));
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        return markup;
    }

    public InlineKeyboardMarkup getChangeableMarkup(String key) {
        if (key.contains("Folder")) {
            key = key.replaceAll("Folder$", "");
            return filesController.getFilesFromFolder(key);
        } else if (key.equals("FileButtonPressed")) {
            return filesController.getFilesFromFolder(path);
        } else if (key.contains("DeleteFileButtonPressed")) {
            return setDeleteFilesMarkup(key);
        }
        else if (key.contains("Year")) {
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
        return null;
    }
}
