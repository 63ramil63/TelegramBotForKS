package org.example.database.builder;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TableBuilder {
    private final Database dataBaseConnection;
    private final String CREATE_USERS_TABLE_SQL;
    private final String CREATE_FILE_TRACKING_SQL;
    private final String CREATE_FILES_HISTORY_SQL;

    public TableBuilder() {
        dataBaseConnection = Database.getInstance();
        CREATE_USERS_TABLE_SQL = "CREATE TABLE IF NOT EXISTS users (" +
                "ChatId BIGINT NOT NULL," +
                "UserName NVARCHAR(64) NULL," +
                "FilePath NVARCHAR(100) NULL," +
                "CanAddFolder TINYINT DEFAULT 0," +
                "GroupId VARCHAR(4) NULL," +
                "PRIMARY KEY (ChatId))";
        CREATE_FILE_TRACKING_SQL = "CREATE TABLE IF NOT EXISTS file_tracker (" +
                "Id BIGINT NOT NULL AUTO_INCREMENT," +
                "ChatId BIGINT NOT NULL," +
                "FilePath NVARCHAR(100) NOT NULL," +
                "PRIMARY KEY (Id))";
        CREATE_FILES_HISTORY_SQL = "CREATE TABLE IF NOT EXISTS files_history (" +
                "Id BIGINT NOT NULL AUTO_INCREMENT," +
                "ChatId BIGINT NOT NULL," +
                "FilePath NVARCHAR(100) NOT NULL," +
                "PRIMARY KEY (Id))";
    }

    public void createTables() {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(CREATE_USERS_TABLE_SQL)) {
            int i = preparedStatement.executeUpdate();
            if (i > 0) {
                System.out.println("Users table created");
            } else {
                System.out.println("Users table found");
            }
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
        try (Connection connection = dataBaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(CREATE_FILE_TRACKING_SQL)) {
            int i = preparedStatement.executeUpdate();
            if (i > 0) {
                System.out.println("FileTracking table created");
            } else {
                System.out.println("FileTracking table found");
            }
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(CREATE_FILES_HISTORY_SQL)) {
            int i = preparedStatement.executeUpdate();
            if (i > 0) {
                System.out.println("FilesHistory table created");
            } else {
                System.out.println("FilesHistory table found");
            }
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
    }
}
