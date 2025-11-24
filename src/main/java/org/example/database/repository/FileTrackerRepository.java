package org.example.database.repository;

import org.example.bot.TBot;
import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FileTrackerRepository {
    private static final Database databaseConnection = Database.getInstance();
    private static final String tableName = "file_tracker";
    private static final String filesHistoryTable = "files_history";

    private static final String GET_FILE_INFO = "SELECT ChatId FROM " + tableName + " WHERE Folder = ? AND FileName = ?";
    private static final String PUT_FILE_INFO = "INSERT INTO " + tableName + " (ChatId, Folder, FileName) values (?, ?, ?)";
    private static final String GET_ALL_USER_FILES = "SELECT Folder, FileName FROM " + tableName + " WHERE ChatId = ?";
    private static final String DELETE_USER_FILE = "DELETE FROM " + tableName + " WHERE Folder = ? AND FileName = ?";
    private static final String PUT_FILE_INFO_TO_FILES_HISTORY = "INSERT INTO " + filesHistoryTable + " (ChatId, FilePath) values (? ,?)";
    private static final String GET_ALL_FILES_BY_FOLDER_NAME = "SELECT FileName FROM " + tableName + " WHERE Folder = ?";

    public void putFileInfo(long chatId, String folder, String fileName) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement1 = connection.prepareStatement(PUT_FILE_INFO);
             PreparedStatement preparedStatement2 = connection.prepareStatement(PUT_FILE_INFO_TO_FILES_HISTORY)) {
            // Для основной таблицы
            preparedStatement1.setLong(1, chatId);
            preparedStatement1.setNString(2, folder);
            preparedStatement1.setNString(3, fileName);
            int rowsAffected = preparedStatement1.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Add file info for file " + folder + " / " + fileName);
            } else {
                System.err.println("Error add file info " + folder + " / " + fileName);
            }

            // Для таблицы истории файлов
            preparedStatement2.setLong(1, chatId);
            preparedStatement2.setString(2, folder + "/" + fileName);
            rowsAffected = preparedStatement2.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Add file info in history for file " + folder + "/" + fileName);
            } else {
                System.err.println("Error add file info in history  " + folder + "/" + fileName);
            }
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method putFileInfo(chatId : %d, folder : %s, fileName : %s)))%n",
                    chatId, folder, fileName);
        }
    }

    public long getFileInfo(String filePath) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_FILE_INFO)) {
            int delimiterIndex = filePath.indexOf(TBot.delimiter);
            String folder = filePath.substring(0, delimiterIndex);
            String file = filePath.substring(delimiterIndex + 1);
            preparedStatement.setString(1, folder);
            preparedStatement.setString(2, file);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                System.err.println("Error getFileInfo " + filePath);
            }
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method getFileInfo(filePath : %s)))%n%s%n", filePath, e);
        }
        return 0;
    }

    public List<String> getAllUserFiles(long chatId) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_USER_FILES)) {
            List<String> userFiles = new ArrayList<>();
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                userFiles.add(resultSet.getNString(1) + TBot.delimiter + resultSet.getNString(2));
            }
            return userFiles;
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method getAllUserFiles(chatId : %d)))%n%s%n", chatId, e);
        }
        return null;
    }

    public boolean deleteUserFileFromRepository(String folder, String fileName) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(DELETE_USER_FILE)) {
            preparedStatement.setNString(1, folder);
            preparedStatement.setNString(2, fileName);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method deleteUserFilesFromRepository(folder : %s, fileName : %s)))%n%s%n", folder, fileName, e);
        }
        return false;
    }

    public List<String> getFilesByFolderName(String folder) {
        List<String> files = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_FILES_BY_FOLDER_NAME)) {
            preparedStatement.setNString(1, folder);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                files.add(resultSet.getNString(1));
            }
            return files;
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method getAllFiles(folder : %s)))%n%s%n", folder, e);
        }
        return files;
    }
}
