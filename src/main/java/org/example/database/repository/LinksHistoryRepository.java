package org.example.database.repository;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LinksHistoryRepository {
    private final Database database = Database.getInstance();
    private final String historyTable = "links_history";

    private final String ADD_LINK_IN_HISTORY_TABLE = "INSERT INTO " + historyTable + " (LinkName, GroupName, Link, UserChatId) values (?, ?, ?, ?)";

    private void prepareStatementForUsage(PreparedStatement preparedStatement, String linkName, String link, String groupName, long chatId) throws SQLException {
        preparedStatement.setString(1, linkName);
        preparedStatement.setString(2, groupName);
        preparedStatement.setString(3, link);
        preparedStatement.setLong(4, chatId);
    }

    public void addLink(String linkName, String link, String groupName,  long chatId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatementForHistory = connection.prepareStatement(ADD_LINK_IN_HISTORY_TABLE)) {
            prepareStatementForUsage(preparedStatementForHistory, linkName, link, groupName, chatId);
            int rowsAffected = preparedStatementForHistory.executeUpdate();
            if (rowsAffected > 0) {
                System.out.printf("New link added in history table, linkName: %s, user's chat id is: %d%n", linkName, chatId);
            } else {
                System.out.printf("Failed to add new link, linkName: %s, user's chat id is: %d%n", linkName, chatId);
            }
        } catch (SQLException e) {
            System.err.println("Error (LinksHistoryRepositoryClass (method addLink())) " + e);
        }
    }
}