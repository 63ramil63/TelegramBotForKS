package org.example.database.repository;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FileTrackerRepository {
    private static final Database databaseConnection = Database.getInstance();
    private static final String tableName = "file_tracker";

    private static final String GET_FILE_INFO = "SELECT ChatId FROM " + tableName + " WHERE FilePath = ?";
    private static final String PUT_FILE_INFO = "INSERT INTO " + tableName + " (ChatId, FilePath) values (?, ?)";

    public void putFileInfo(long chatId, String filePath) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(PUT_FILE_INFO)) {
            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, filePath);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Add file info for file " + filePath);
            } else {
                System.err.println("Error add file info " + filePath);
            }
        } catch (SQLException e) {
            System.err.println("Error (FileTrackerClass (method putFileInfo)) " + e);
        }
    }

    public long getFileInfo(String filePath) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_FILE_INFO)) {
            preparedStatement.setString(1, filePath);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                System.err.println("Error getFileInfo " + filePath);
            }
        } catch (SQLException e) {
            System.err.println("Error (FileTrackerClass (method getFileInfo))");
        }
        return 0;
    }
}
