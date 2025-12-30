package org.example.database.repository;

import org.example.bot.ban.types.BanType;
import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserBansHistoryRepository {
    private final Database database = Database.getInstance();
    private final String history_table_name = "user_bans_history";

    private final String BAN_USER = "INSERT INTO " + history_table_name + " (UserChatId, Reason, BanType, AdminChatId, Action) VALUES (?, ?, ?, ?, ?)";
    private final String UNBAN_USER = "INSERT INTO " + history_table_name + " (UserChatID, AdminChatId, Action) VALUES (?, ?, ?)";

    public void banUser(long userChatId, String reason, String banType, long adminChatId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(BAN_USER)) {
            preparedStatement.setLong(1, userChatId);
            preparedStatement.setNString(2, reason);
            preparedStatement.setNString(3, banType);
            preparedStatement.setLong(4, adminChatId);
            preparedStatement.setNString(5, "BAN");
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.printf("User with chatId (%d) has been banned%n", userChatId);
            }
        } catch (SQLException e) {
            System.err.printf("Error (UserBansHistoryRepository (method banUser(usersChatId : %d, reason : %s, banType : %s, adminsChatId : %d)))%n%s%n",
                    userChatId, reason, banType, adminChatId, e);
        }
    }

    public void unbanUser(long userChatId, long adminChatId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UNBAN_USER)) {
            preparedStatement.setLong(1, userChatId);
            preparedStatement.setLong(2, adminChatId);
            preparedStatement.setNString(3, "UNBAN");
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.printf("User with chatId (%d) has been unbanned%n", userChatId);
            }
        } catch (SQLException e) {
            System.err.printf("Error (UserBansHistoryRepository (method unbanUser(userChatId : %d, adminChatId : %d)))%n%s%n",
                    userChatId, adminChatId, e);
        }
    }
}
