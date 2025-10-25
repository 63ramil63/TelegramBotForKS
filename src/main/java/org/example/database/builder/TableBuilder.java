package org.example.database.builder;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TableBuilder {
    private final Database dataBaseConnection;
    private final String CREATE_USERS_TABLE_SQL;
    private final String CREATE_FOLDER_TRACKING_SQL;
    private final String CREATE_FILE_TRACKING_SQL;
    private final String CREATE_FILES_HISTORY_SQL;
    private final String CREATE_ADMINS_TABLE_SQL;

    public TableBuilder() {
        dataBaseConnection = Database.getInstance();
        CREATE_USERS_TABLE_SQL = "CREATE TABLE IF NOT EXISTS users (" +
                "ChatId BIGINT NOT NULL," +
                "UserName NVARCHAR(64) NULL," +
                "Folder NVARCHAR(15) NULL," +
                "CanAddFolder TINYINT DEFAULT 0," +
                "GroupId VARCHAR(4) NULL," +
                "PRIMARY KEY (ChatId))";
        CREATE_FOLDER_TRACKING_SQL = "CREATE TABLE IF NOT EXISTS folder_tracker(" +
                "Folder NVARCHAR(15) NOT NULL," +
                "PRIMARY KEY(Folder))";
        CREATE_FILE_TRACKING_SQL = "CREATE TABLE IF NOT EXISTS file_tracker (" +
                "Id BIGINT NOT NULL AUTO_INCREMENT," +
                "ChatId BIGINT NOT NULL," +
                "Folder NVARCHAR(15) NOT NULL," +
                "FileName NVARCHAR(100) NOT NULL," +
                "PRIMARY KEY (Id))";
        CREATE_FILES_HISTORY_SQL = "CREATE TABLE IF NOT EXISTS files_history (" +
                "Id BIGINT NOT NULL AUTO_INCREMENT," +
                "ChatId BIGINT NOT NULL," +
                "FilePath NVARCHAR(100) NOT NULL," +
                "PRIMARY KEY (Id))";
        CREATE_ADMINS_TABLE_SQL = "CREATE TABLE IF NOT EXISTS admins (" +
                "Username NVARCHAR(128) not null," +
                "Role NVARCHAR(32)," +
                "PRIMARY KEY (Username))";
    }

    public void checkSQLUpdate(int rowsAffected, String tableName) {
        if(rowsAffected > 0) {
            System.out.println(tableName + " table created");
        } else {
            System.out.println(tableName + " table found");
        }
    }

    public void createTables() {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(CREATE_USERS_TABLE_SQL)) {
            int rowsAffected = preparedStatement.executeUpdate();
            checkSQLUpdate(rowsAffected, "Users");
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(CREATE_FOLDER_TRACKING_SQL)) {
            int rowsAffected = preparedStatement.executeUpdate();
            checkSQLUpdate(rowsAffected, "FolderTracking");
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
        try (Connection connection = dataBaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(CREATE_FILE_TRACKING_SQL)) {
            int rowsAffected = preparedStatement.executeUpdate();
            checkSQLUpdate(rowsAffected, "FileTracking");
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(CREATE_FILES_HISTORY_SQL)) {
            int rowsAffected = preparedStatement.executeUpdate();
            checkSQLUpdate(rowsAffected, "FileHistory");
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(CREATE_ADMINS_TABLE_SQL)) {
            int rowsAffected = preparedStatement.executeUpdate();
            checkSQLUpdate(rowsAffected, "Admins");
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
    }
}
