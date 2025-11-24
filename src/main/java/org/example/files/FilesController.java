package org.example.files;

import org.example.bot.TBot;
import org.example.bot.message.markup.button.ButtonSetter;
import org.example.database.repository.FileTrackerRepository;
import org.example.database.repository.FolderRepository;
import org.example.files.exception.FileSizeException;
import org.example.files.exception.IncorrectExtensionException;
import org.example.files.exception.InvalidCallbackDataException;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FilesController {
    private final TBot tBot;
    private final FolderRepository folderRepository;
    private final FileTrackerRepository fileTrackerRepository;
    private final String bot_token;
    private final String delimiter;
    private final String path;
    private final long maxFileSize;

    public FilesController(TBot tBot, FolderRepository folderRepository, FileTrackerRepository fileTrackerRepository, String bot_token, String delimiter, String path, long maxFileSize) {
        this.tBot = tBot;
        this.folderRepository = folderRepository;
        this.fileTrackerRepository = fileTrackerRepository;
        this.bot_token = bot_token;
        this.delimiter = delimiter;
        this.path = path;
        this.maxFileSize = maxFileSize;
    }

    private List<InlineKeyboardButton> setRow(String data) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(ButtonSetter.setButton(data, data + "Folder"));
        return row;
    }

    public List<String> getFoldersFromTable() {
        return folderRepository.getFolders();
    }

    public InlineKeyboardMarkup getFoldersFromDatabaseMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> folders = getFoldersFromTable();
        if (folders != null && !folders.isEmpty()) {
            for (String folder : folders) {
                keyboard.add(setRow(folder));
            }
        }
        InlineKeyboardButton deleteButton = ButtonSetter.setButton("Удалить файл", "DeleteFileButtonPressed");
        InlineKeyboardButton addFolder = ButtonSetter.setButton("Добавить папку", "AddFolderButtonPressed");
        keyboard.add(ButtonSetter.setRow(addFolder, deleteButton));

        InlineKeyboardButton back = ButtonSetter.setButton("Назад", "BackButtonPressed");
        keyboard.add(ButtonSetter.setRow(back));

        markup.setKeyboard(keyboard);
        return markup;
    }

    private List<String> getFoldersFromPath() throws IOException{
        List<String> folders = new ArrayList<>();
        try (Stream<Path> paths = Files.list(Path.of(this.path))) {
            paths.filter(Files::isDirectory)
                    .forEach(p -> folders.add(p.getFileName().toString()));
        }
        return folders;
    }

    public void synchronizeFoldersWithDatabase() {
        try {
            List<String> folders = getFoldersFromPath();
            if (!folders.isEmpty()) {
                for (String folder : folders) {
                    if (!folderRepository.checkFolderByName(folder)) {
                        folderRepository.addFolder(folder);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error (FilesControllerClass(method synchronizeFoldersWithDatabase()))");
        }
    }

    private List<String> getFilesFromFolder(String folder) {
        List<String> files = new ArrayList<>();
        String fullPath = this.path + delimiter + folder;
        try (Stream<Path> paths = Files.list(Path.of(fullPath))) {
            paths.forEach(p -> files.add(p.getFileName().toString()));
        } catch (IOException e) {
            System.err.printf("Error (FilesControllerClass (method getFilesFromFolder(data : %s))) %n%s%n", files, e);
        }
        return files;
    }

    public List<String> getFilesFromDatabaseByFolder(String folder) {
        List<String> files;
        files = fileTrackerRepository.getFilesByFolderName(folder);
        return files;
    }

    public InlineKeyboardMarkup getFilesFromFolderMarkup(String folder) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> files = getFilesFromDatabaseByFolder(folder);
        if (!files.isEmpty()) {
            for (String file : files) {
                InlineKeyboardButton button = ButtonSetter.setButton(file, folder + delimiter + file + "File");
                keyboard.add(ButtonSetter.setRow(button));
            }
        }
        InlineKeyboardButton back = ButtonSetter.setButton("Назад", "BackButtonPressed");
        keyboard.add(ButtonSetter.setRow(back));
        markup.setKeyboard(keyboard);
        return markup;
    }

    private void addFileToDatabase(String folder, String fileName) {
        fileTrackerRepository.putFileInfo(0, folder, fileName);
    }

    public void synchronizeFilesWithDatabase() {
        try {
            List<String> folders = getFoldersFromPath();
            for (String folder : folders) {
                List<String> filesInFolder = getFilesFromFolder(folder);
                List<String> filesInDatabase = fileTrackerRepository.getFilesByFolderName(folder);
                for (String fileInF : filesInFolder) {
                    if (!filesInDatabase.contains(fileInF)) {
                        addFileToDatabase(folder, fileInF);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error (FilesControllerClass (method synchronizeFilesWithDatabase())) " + e);
        }
    }

    public void addFolder(String text) {
        try {
            if (Files.isDirectory(Path.of(this.path))) {
                Files.createDirectory(Path.of(this.path + text));
                folderRepository.addFolder(text);
            }
        } catch (IOException e) {
            System.err.printf("Error (FilesControllerClass (method addFolder(data : %s))) %s%n", text, e);
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

    private static boolean isValidCallbackData(String callbackData) {
        // Можно и 64(макс ограничение), но лучше взять с запасом
        return callbackData.getBytes(StandardCharsets.UTF_8).length <= 54;
    }

    public String saveDocument(Document document, String caption, String extension, String userPath)
            throws FileSizeException, TelegramApiException, IOException, InvalidCallbackDataException {
        if (checkMaxSize(document)) {
            throw new FileSizeException("File too large");
        }

        String fileId = document.getFileId();

        String filePath = tBot.getFilePath(fileId);
        InputStream is = new URL("https://api.telegram.org/file/bot" + bot_token + "/" + filePath).openStream();
        String path;
        if (caption != null && !caption.isEmpty()) {
            path = userPath + delimiter + caption + "." + extension;
        } else {
            path = userPath + delimiter + document.getFileName();
        }
        if (isValidCallbackData(path)) {
            Files.copy(is, Paths.get(path));
            is.close();
        } else {
            throw new InvalidCallbackDataException("File name too large for as call back data of button " + path.getBytes(StandardCharsets.UTF_8).length);
        }
        return path;
    }

    public void deleteFile(String rawFilePath) throws IOException {
        String normalizedPath = path + rawFilePath;
        Path file = Path.of(normalizedPath);
        Files.delete(file);
    }
}
