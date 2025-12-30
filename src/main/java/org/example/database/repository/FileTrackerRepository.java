package org.example.database.repository;

import org.example.bot.TBot;
import org.example.database.Database;
import org.example.dto.FileDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FileTrackerRepository {
    private final Database databaseConnection = Database.getInstance();
    private static final String tableName = "file_tracker";

    private static final String PUT_FILE_INFO = "INSERT INTO " + tableName + " (ChatId, Folder, FileName) values (?, ?, ?)";
    private static final String GET_ALL_USER_FILES = "SELECT Id, Folder, FileName FROM " + tableName + " WHERE ChatId = ?";
    private static final String GET_ALL_FILES_BY_FOLDER_NAME = "SELECT Id, FileName FROM " + tableName + " WHERE Folder = ?";


    private static final String GET_FILE_CHAT_ID_BY_ID = "SELECT ChatId FROM " + tableName + " WHERE Id = ?";
    private static final String DELETE_USER_FILE_BY_ID = "DELETE FROM " + tableName + " WHERE Id = ?";
    private static final String GET_FILE_INFO_BY_FILE_ID = "SELECT Folder, FileName FROM " + tableName + " WHERE Id = ?";

    public void putFileInfo(long chatId, String folder, String fileName) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement1 = connection.prepareStatement(PUT_FILE_INFO)) {
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
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method putFileInfo(chatId : %d, folder : %s, fileName : %s)))%n",
                    chatId, folder, fileName);
        }
    }

    public long getFilesChatIdById(long fileId) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_FILE_CHAT_ID_BY_ID)) {

            preparedStatement.setLong(1, fileId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                System.err.println("Error getFileInfo fileId : " + fileId);
            }
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method getFileInfo(fileId : %s)))%n%s%n", fileId, e);
        }
        return 0;
    }

    public List<FileDTO> getAllUserFiles(long chatId) {
        List<FileDTO> userFilesDTO = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_USER_FILES)) {
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String folder = resultSet.getNString(2);
                String fileName = resultSet.getNString(3);
                FileDTO fileDTO = FileDTO.builder()
                        .id(id)
                        .folder(folder)
                        .fileName(fileName)
                        .build();
                userFilesDTO.add(fileDTO);
            }
            return userFilesDTO;
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method getAllUserFiles(chatId : %d)))%n%s%n", chatId, e);
        }
        return null;
    }

    public FileDTO getFileInfoByFileId(long fileId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_FILE_INFO_BY_FILE_ID)){
            preparedStatement.setLong(1, fileId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String folder = resultSet.getNString(1);
                String fileName = resultSet.getNString(2);
                FileDTO fileDTO = FileDTO.builder()
                        .folder(folder)
                        .fileName(fileName)
                        .build();
                return fileDTO;
            }
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method getFileInfoByFileId(fileId %d)))%n%s%n", fileId, e);
        }
        return null;
    }

    public boolean deleteUserFileFromRepository(long fileId) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(DELETE_USER_FILE_BY_ID)) {
            preparedStatement.setLong(1, fileId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method deleteUserFilesFromRepository(fileId : %d)))%n%s%n",fileId, e);
        }
        return false;
    }

    public List<FileDTO> getFilesByFolderName(String folder) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_FILES_BY_FOLDER_NAME)) {
            List<FileDTO> filesDTO = new ArrayList<>();
            preparedStatement.setNString(1, folder);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String fileName = resultSet.getNString(2);
                FileDTO fileDTO = FileDTO.builder()
                        .id(id)
                        .folder(folder)
                        .fileName(fileName)
                        .build();
                filesDTO.add(fileDTO);
            }
            return filesDTO;
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method getAllFiles(folder : %s)))%n%s%n", folder, e);
        }
        return null;
    }
}
