package org.example.database.repository;

import org.example.database.Database;
import org.example.dto.FolderDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FolderRepository {
    private final Database databaseConnection = Database.getInstance();
    private static final String tableName = "folder_tracker";

    private static final String ADD_FOLDER = "INSERT INTO " + tableName + " (Folder) values (?)";
    private static final String GET_ALL_FOLDERS = "SELECT Id, Folder FROM " + tableName;
    private static final String GET_FOLDER_BY_ID = "SELECT Folder FROM " + tableName + " WHERE Id = ?";
    private static final String GET_FOLDER_BY_NAME = "SELECT Folder FROM " + tableName + " WHERE Folder = ?";
    private static final String DELETE_FOLDER_BY_NAME = "DELETE FROM " + tableName + " WHERE Folder = ?";
    private static final String DELETE_FOLDER_BY_ID = "DELETE FROM " + tableName + " WHERE Id = ?";

    public boolean addFolder(String folderName) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_FOLDER)) {
            preparedStatement.setString(1, folderName);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.printf("Error (FolderRepositoryClass (method addFolder(folderName : %s))) %n%s%n", folderName, e);
        }
        return false;
    }

    public boolean checkFolderByName(String folder) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_FOLDER_BY_NAME)) {
            preparedStatement.setNString(1, folder);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.err.printf("Error (FolderRepositoryClass (method getFolder(folder : %s))) %n%s%n", folder, e);
        }
        return false;
    }

    public String getFolderNameById(long id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_FOLDER_BY_ID)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getNString(1);
            }
        } catch (SQLException e) {
            System.err.printf("Error (FolderRepositoryClass (method getFolder(folderId : %s))) %n%s%n", id, e);
        }
        throw new IllegalArgumentException("Not found folder by Id : " + id);
    }

    public List<FolderDTO> getFolders() {
        List<FolderDTO> folders = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_FOLDERS)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String folder = resultSet.getNString(2);
                FolderDTO folderDTO = FolderDTO.builder()
                                .id(id)
                                .folder(folder)
                                .build();
                folders.add(folderDTO);
            }
            return folders;
        } catch (SQLException e) {
            System.err.println("Error (FolderRepositoryClass (method getFolders())) " + e);
        }
        return folders;
    }

    public boolean deleteFolderByName(String folder) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_FOLDER_BY_NAME)) {
            preparedStatement.setNString(1, folder);
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error (FolderRepositoryClass (method deleteFolder())) " + e);
        }
        return false;
    }

    public boolean deleteFolderById(long id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_FOLDER_BY_ID)) {
            preparedStatement.setLong(1, id);
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error (FolderRepositoryClass (method deleteFolder())) " + e);
        }
        return false;
    }
}
