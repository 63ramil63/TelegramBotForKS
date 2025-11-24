package org.example.database.repository;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FolderRepository {
    private static final Database databaseConnection = Database.getInstance();
    private static final String tableName = "folder_tracker";

    private static final String ADD_FOLDER = "INSERT INTO " + tableName + " (Folder) values (?)";
    private static final String GET_ALL_FOLDERS = "SELECT Folder FROM " + tableName;
    private static final String GET_FOLDER_BY_NAME = "SELECT Folder FROM " + tableName + " WHERE Folder = ?";

    public boolean addFolder(String folderName) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_FOLDER)) {
            preparedStatement.setString(1, folderName);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.printf("Error FolderRepositoryClass (method addFolder(folderName : %s)) %n%s%n", folderName, e);
        }
        return false;
    }

    public boolean checkFolderByName(String folder) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_FOLDER_BY_NAME)) {
            preparedStatement.setString(1, folder);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.err.printf("Error (FolderRepositoryClass (method getFolder(folder : %s))) %n%s%n", folder, e);
        }
        return false;
    }

    public List<String> getFolders() {
        List<String> folders = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_FOLDERS)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                folders.add(resultSet.getNString(1));
            }
            return folders;
        } catch (SQLException e) {
            System.err.println("Error (FolderRepositoryClass (method getFolders())) " + e);
        }
        return folders;
    }
}
