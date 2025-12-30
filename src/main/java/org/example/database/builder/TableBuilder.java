package org.example.database.builder;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TableBuilder {
    private final Database dataBaseConnection;
    private final String CREATE_USERS_TABLE_SQL;
    private final String CREATE_FOLDER_TRACKING_TABLE_SQL;
    private final String CREATE_FILE_TRACKING_TABLE_SQL;
    private final String CREATE_FILES_HISTORY_TABLE_SQL;
    private final String CREATE_ADMINS_TABLE_SQL;
    private final String CREATE_LINKS_TABLE_SQL;
    private final String CREATE_GROUP_TABLE_SQL;
    private final String CREATE_LINKS_HISTORY_TABLE_SQL;
    private final String CREATE_DELETION_LOG_TABLE;
    private final String CREATE_USER_BANS_TABLE;
    private final String CREATE_USER_BANS_HISTORY_TABLE;

    public TableBuilder() {
        dataBaseConnection = Database.getInstance();
        CREATE_USERS_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS users (
                Id BIGINT NOT NULL AUTO_INCREMENT,
                ChatId BIGINT NOT NULL,
                UserName NVARCHAR(64) NULL,
                FirstName NVARCHAR(255) NULL,
                LastName NVARCHAR(255) NULL,
                Folder NVARCHAR(15) NULL,
                CanAddFolder TINYINT DEFAULT 0,
                CanAddGroup TINYINT DEFAULT 0,
                CanAddLink TINYINT DEFAULT 0,
                GroupId NVARCHAR(4) NULL,
                GroupForLinks NVARCHAR(20) NULL,
                PRIMARY KEY (Id))
                """;
        CREATE_FOLDER_TRACKING_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS folder_tracker(
                Id BIGINT NOT NULL AUTO_INCREMENT,
                Folder NVARCHAR(20) NOT NULL UNIQUE,
                ChatId BIGINT NOT NULL,
                PRIMARY KEY(Id))
                """;
        CREATE_FILE_TRACKING_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS file_tracker (
                Id BIGINT NOT NULL AUTO_INCREMENT,
                ChatId BIGINT NOT NULL,
                Folder NVARCHAR(20) NOT NULL,
                FileName NVARCHAR(100) NOT NULL,
                PRIMARY KEY (Id))
                """;
        CREATE_FILES_HISTORY_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS files_history(
                Id BIGINT NOT NULL AUTO_INCREMENT,
                ChatId BIGINT NOT NULL,
                FilePath NVARCHAR(100) NOT NULL,
                PRIMARY KEY (Id))
                """;
        CREATE_ADMINS_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS admins(
                Username NVARCHAR(128) not null,
                Role NVARCHAR(32),
                PRIMARY KEY (Username))
                """;
        CREATE_LINKS_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS links(
                Id BIGINT NOT NULL AUTO_INCREMENT,
                LinkName NVARCHAR(50) NOT NULL,
                GroupName NVARCHAR(20) NOT NULL,
                Link NVARCHAR(255) NOT NULL,
                UserChatId BIGINT NOT NULL,
                PRIMARY KEY(Id))
                """;
        CREATE_GROUP_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS edu_groups(
                Id BIGINT NOT NULL AUTO_INCREMENT,
                GroupName NVARCHAR(20) NOT NULL UNIQUE,
                ChatId BIGINT NOT NULL,
                PRIMARY KEY(Id))
                """;
        CREATE_LINKS_HISTORY_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS links_history(
                Id BIGINT NOT NULL AUTO_INCREMENT,
                LinkName NVARCHAR(50) NOT NULL,
                GroupName NVARCHAR(20) NOT NULL,
                Link NVARCHAR(255) NOT NULL,
                UserChatId BIGINT NOT NULL,
                PRIMARY KEY (Id))
                """;
        CREATE_DELETION_LOG_TABLE = """
                CREATE TABLE IF NOT EXISTS deletion_log (
                Id BIGINT NOT NULL AUTO_INCREMENT,
                UserChatId BIGINT NOT NULL,
                DescriptionOfAction NVARCHAR(255) NOT NULL,
                PRIMARY KEY(Id))
                """;
        CREATE_USER_BANS_TABLE = """
                CREATE TABLE IF NOT EXISTS user_bans (
                Id BIGINT NOT NULL AUTO_INCREMENT,
                UserChatId BIGINT NOT NULL,
                Reason NVARCHAR(255) NOT NULL,
                BanType NVARCHAR(20) NOT NULL,
                AdminChatId BIGINT NOT NULL,
                PRIMARY KEY(Id))
                """;
        CREATE_USER_BANS_HISTORY_TABLE = """
                CREATE TABLE IF NOT EXISTS user_bans_history (
                Id BIGINT NOT NULL AUTO_INCREMENT,
                UserChatId BIGINT NOT NULL,
                Reason NVARCHAR(255) NULL,
                BanType NVARCHAR(20) NULL,
                AdminChatId BIGINT NULL,
                Action NVARCHAR(10) NOT NULL,
                PRIMARY KEY(Id))
                """;
    }

    private void checkSQLUpdate(int rowsAffected, String tableName) {
        if(rowsAffected > 0) {
            System.out.println(tableName + " table created");
        } else {
            System.out.println(tableName + " table found");
        }
    }

    private void executeSQL(String sql, String tableName) {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int rowsAffected = preparedStatement.executeUpdate();
            checkSQLUpdate(rowsAffected, tableName);
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTables()))" + e);
            System.exit(103);
        }
    }

    public void createTables() {
        executeSQL(CREATE_USERS_TABLE_SQL, "USERS");
        executeSQL(CREATE_FOLDER_TRACKING_TABLE_SQL, "FOLDER TRACKING");
        executeSQL(CREATE_FILE_TRACKING_TABLE_SQL, "FILE TRACKING");
        executeSQL(CREATE_FILES_HISTORY_TABLE_SQL, "FILES HISTORY");
        executeSQL(CREATE_ADMINS_TABLE_SQL, "ADMINS");
        executeSQL(CREATE_LINKS_TABLE_SQL, "LINKS TABLE");
        executeSQL(CREATE_GROUP_TABLE_SQL, "GROUPS TABLE");
        executeSQL(CREATE_LINKS_HISTORY_TABLE_SQL, "LINKS HISTORY TABLE");
        executeSQL(CREATE_DELETION_LOG_TABLE, "DELETION LOG TABLE");
        executeSQL(CREATE_USER_BANS_TABLE, "USER BANS TABLE");
        executeSQL(CREATE_USER_BANS_HISTORY_TABLE, "USER BANS HISTORY TABLE");
    }
}
