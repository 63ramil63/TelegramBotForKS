package org.example.files;

import org.example.bot.TBot;
import org.example.bot.message.markup.button.ButtonSetter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FilesController {

    public FilesController() {

    }


    private List<InlineKeyboardButton>setRow(Path path) {
        List<InlineKeyboardButton> row = new ArrayList<>();

        // Удаление пути из названия файла
        String fileName = path.toString().replace(TBot.path, "");
        InlineKeyboardButton button;

        if (!Files.isDirectory(path)) {
            // Удаление лишней \
            int index = fileName.indexOf(TBot.delimiter);
            String buttonName = fileName.substring(index).replace(TBot.delimiter, "");
            button = ButtonSetter.setButton(buttonName, buttonName + "File");
            row.add(button);
        } else {
            button = ButtonSetter.setButton(fileName, fileName + "Folder");
            row.add(button);
        }
        return row;
    }

    public InlineKeyboardMarkup getFilesFromFolder(String path) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (!path.equals(TBot.path)) {
            path = TBot.path + path;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))) {
            for (Path _path: stream) {
                keyboard.add(setRow(_path));
            }

            InlineKeyboardButton back = ButtonSetter.setButton("Назад", "BackButtonPressed");
            // Добавление различных кнопок
            if (path.equals(TBot.path)) {
                InlineKeyboardButton addFolder = ButtonSetter.setButton("Добавить папку", "AddFolderButtonPressed");
                // Установка кнопки назад и добавить папку
                keyboard.add(ButtonSetter.setRow(addFolder, back));
            } else {
                // Установка кнопки назад
                keyboard.add(ButtonSetter.setRow(back));
            }
            markup.setKeyboard(keyboard);
        } catch (IOException e) {
            System.err.println("Error: FilesControllerClass (method getFilesFromFolder()) " + e);
        }
        return markup;
    }

    public void addFolder(String text) {
        try {
            if (Files.isDirectory(Path.of(TBot.path + text))) {
                Files.createDirectory(Path.of(TBot.path + text));
            }
        } catch (IOException e) {
            System.err.println("Error: FilesControllerClass (method addFolder()) " + e);
        }
    }
}
