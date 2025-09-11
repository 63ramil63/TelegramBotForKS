package org.example.bot.message.markup;

import org.example.bot.message.markup.button.ButtonSetter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MarkupSetter {

    public static HashMap<MarkupKey, InlineKeyboardMarkup> savedMarkup = new HashMap<>();

    public InlineKeyboardMarkup getMarkup(MarkupKey key) {
        if (!checkSavedMarkup(key)) {
            setMarkup(key);
        }
        return savedMarkup.get(key);
    }

    private boolean checkSavedMarkup(MarkupKey key) {
        return savedMarkup.containsKey(key);
    }

    private void setMarkup(MarkupKey key) {
        switch (key) {
            case MarkupKey.MainMenu -> savedMarkup.put(key, getMainMenuButtons());
            case MarkupKey.LessonMenu -> savedMarkup.put(key, getLessonMenuButtons());
        }
    }

    private InlineKeyboardMarkup getLessonMenuButtons() {

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        //создание кнопок и добавление к ним возвращаемого значения при нажатии
        InlineKeyboardButton today = ButtonSetter.setButton("На сегодня", "TodayLessonsButtonPressed");
        InlineKeyboardButton tomorrow = ButtonSetter.setButton("На завтра", "TomorrowLessonsButtonPressed");
        keyboard.add(ButtonSetter.setRow(today, tomorrow));

        InlineKeyboardButton selectYear = ButtonSetter.setButton("Выбрать курс", "SelectYearButtonPressed");
        //кнопка для выбора курса
        keyboard.add(ButtonSetter.setRow(selectYear));

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

        InlineKeyboardButton helpButton = ButtonSetter.setButton("Помощь", "/help");
        keyboard.add(ButtonSetter.setRow(helpButton));

        //создание самого объекта клавиатуры, к которому все добавляем
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }
}
