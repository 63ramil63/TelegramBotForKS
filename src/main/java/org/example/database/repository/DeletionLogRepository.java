package org.example.database.repository;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeletionLogRepository {
    private final Database database = Database.getInstance();
    private final String table_name = "deletion_log";

    private final String ADD_NEW_LOG = "INSERT INTO " + table_name + " (UserChatId, DescriptionOfAction) VALUES (?, ?)";

    public void addDeletionLog(long chatId, String descriptionOfAction) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_NEW_LOG)) {
            preparedStatement.setLong(1, chatId);
            preparedStatement.setNString(2, descriptionOfAction);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.printf("Error (DeletionLogClass (addDeletionLog(chatId : %d, String : descriptionOfAction : %s)))%n%s%n", chatId, descriptionOfAction, e);
        }
    }
}
