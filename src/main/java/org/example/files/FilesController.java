package org.example.files;

import org.example.bot.TBot;
import org.example.bot.message.markup.button.ButtonSetter;
import org.example.files.exception.FileSizeException;
import org.example.files.exception.IncorrectExtensionException;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FilesController {
    private final TBot tBot;
    private final String bot_token;
    private final String delimiter;
    private final String path;

    public FilesController(TBot tBot, String bot_token, String delimiter, String path) {
        this.tBot = tBot;
        this.bot_token = bot_token;
        this.delimiter = delimiter;
        this.path = path;
    }


    private List<InlineKeyboardButton> setRow(Path path) {

        System.out.println(path );
        List<InlineKeyboardButton> row = new ArrayList<>();

        // Удаление пути из названия файла
        String fileName = path.toString().replace(TBot.path, "");
        InlineKeyboardButton button;

        if (!Files.isDirectory(path)) {
            // Удаление лишней \
            int index = fileName.indexOf(TBot.delimiter);
            String buttonName = fileName.substring(index).replace(TBot.delimiter, "");
            button = ButtonSetter.setButton(buttonName, fileName + "File");
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
        System.out.println(path);
        if (!path.equals(TBot.path)) {
            path = TBot.path + path;
        }
        System.out.println("before try");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))) {
            System.out.println("try success");
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
        System.out.println("return markup");
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

    public static boolean checkFileName(String str) {
        // Допускаются: буквы (a-zA-Z), цифры (0-9), пробелы (\\s), нижние подчеркивания (_)
        return str.matches(".*[^a-zA-Z0-9_\\s].*");
    }

    public static String checkFileExtension(String fileName, List<String> extensions) throws IncorrectExtensionException {
        //получаем расширение файла(используем +1, чтобы получить расширение файла без точки)
        int index = fileName.lastIndexOf(".") + 1;
        String extension = fileName.substring(index);
        if (extensions.contains(extension)) {
            return extension;
        }
        throw new IncorrectExtensionException("Incorrect file extension");
    }

    private boolean checkMaxSize(Document document) {
        return document.getFileSize() > (long) TBot.maxFileSize * 1024 * 1024;
    }

    public void saveDocument(Document document, String caption, String extension, String userPath) throws FileSizeException {
        if (checkMaxSize(document)) {
            throw new FileSizeException("File too large");
        }

        String fileId = document.getFileId();
        try {
            System.out.println("try success");
            String filePath = tBot.getFilePath(fileId);
            System.out.println("filepath");
            InputStream is = new URL("https://api.telegram.org/file/bot" + bot_token + "/" + filePath).openStream();
            System.out.println("is");
            if (caption != null && !caption.isEmpty()) {
                Files.copy(is, Paths.get( userPath + delimiter + caption + "." + extension));
            } else {
                Files.copy(is, Paths.get(userPath + delimiter + document.getFileName()));
                System.out.println("copy");
            }
        } catch (TelegramApiException e) {
            System.err.println("Error FilesControllerClass (method FilesController(TgAPIException)) " + e);
        } catch (IOException e) {
            System.err.println("Error FilesControllerClass (method FilesController(IOException)) " + e);
        }
    }
}
