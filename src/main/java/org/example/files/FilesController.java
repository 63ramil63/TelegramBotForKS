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
    private final long maxFileSize;

    public FilesController(TBot tBot, String bot_token, String delimiter, String path, long maxFileSize) {
        this.tBot = tBot;
        this.bot_token = bot_token;
        this.delimiter = delimiter;
        this.path = path;
        this.maxFileSize = maxFileSize;
    }


    private List<InlineKeyboardButton> setRow(Path path) {

        List<InlineKeyboardButton> row = new ArrayList<>();

        // Удаление пути из названия файла
        String fileName = path.toString().replace(this.path, "");
        InlineKeyboardButton button;

        if (!Files.isDirectory(path)) {
            // Удаление лишней \
            int index = fileName.indexOf(this.delimiter);
            String buttonName = fileName.substring(index).replace(this.delimiter, "");
            button = ButtonSetter.setButton(buttonName, fileName + "File");
            row.add(button);
        } else {
            button = ButtonSetter.setButton(fileName, fileName + "Folder");
            row.add(button);
        }
        return row;
    }

    public InlineKeyboardMarkup getFilesFromFolder(String rawPath) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        if (!rawPath.equals(this.path)) {
            rawPath = this.path + rawPath;
        }

        Path normalizedPath = Path.of(rawPath);

        if (!Files.exists(normalizedPath)) {
            try {
                Files.createDirectory(normalizedPath);
            } catch (IOException e) {
                System.err.println("Error (FilesControllerClass (method getFilesFromFolder()))");
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(normalizedPath)) {
            for (Path _path: stream) {
                keyboard.add(setRow(_path));
            }

            InlineKeyboardButton back = ButtonSetter.setButton("Назад", "BackButtonPressed");
            InlineKeyboardButton deleteButton = ButtonSetter.setButton("Удалить файл", "DeleteFileButtonPressed");
            // Добавление различных кнопок
            if (normalizedPath.equals(Path.of(this.path))) {
                InlineKeyboardButton addFolder = ButtonSetter.setButton("Добавить папку", "AddFolderButtonPressed");
                // Установка кнопки назад и добавить папку
                keyboard.add(ButtonSetter.setRow(addFolder, deleteButton));
                keyboard.add(ButtonSetter.setRow(back));
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
            if (Files.isDirectory(Path.of(this.path))) {
                Files.createDirectory(Path.of(this.path + text));
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
        return document.getFileSize() > maxFileSize * 1024 * 1024;
    }

    public String saveDocument(Document document, String caption, String extension, String userPath)
            throws FileSizeException, TelegramApiException, IOException {
        if (checkMaxSize(document)) {
            throw new FileSizeException("File too large");
        }

        String fileId = document.getFileId();

        String filePath = tBot.getFilePath(fileId);
        InputStream is = new URL("https://api.telegram.org/file/bot" + bot_token + "/" + filePath).openStream();
        String path;
        if (caption != null && !caption.isEmpty()) {
            path = userPath + delimiter + caption + "." + extension;
            Files.copy(is, Paths.get(path));
        } else {
            path = userPath + delimiter + document.getFileName();
            Files.copy(is, Paths.get(path));
        }
        is.close();
        return path;
    }

    public void deleteFile(String rawFilePath) throws IOException {
        String normalizedPath = path + rawFilePath;
        Path file = Path.of(normalizedPath);
        System.out.println("before delete file");
        Files.delete(file);
        System.out.println("after delete file");
    }
}
