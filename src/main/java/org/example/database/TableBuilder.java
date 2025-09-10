package org.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TableBuilder {
    private final Database dataBaseConnection;
    private final String sql;

    public TableBuilder() {
        dataBaseConnection = Database.getInstance();
        sql = "create table if not exists " + dataBaseConnection.tableName + " (" +
                "ChatId bigint not null," +
                "UserName nvarchar(64) null," +
                "FilePath nvarchar(100) null," +
                "CanAddFolder tinyint default 0," +
                "GroupId varchar(4) null," +
                "primary key (Id))";
    }

    public void createTable() {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int i = preparedStatement.executeUpdate();
            if (i > 0) {
                System.out.println("Table created");
            } else {
                System.out.println("Table found");
            }
        } catch (SQLException e) {
            System.err.println("Error (TableBuilderClass (method - createTable()))" + e);
            System.exit(103);
        }
    }
}
