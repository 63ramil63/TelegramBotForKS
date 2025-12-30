package org.example.database.repository;

import org.example.bot.ban.types.BanType;
import org.example.bot.ban.types.ban.info.BanInfo;
import org.example.database.Database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserBansRepository {
    private final Database database = Database.getInstance();
    private final String table_name = "user_bans";

    private final String BAN_USER = "INSERT INTO " + table_name + " (UserChatId, Reason, BanType, AdminChatId) VALUES (?, ?, ?, ?)";
    private final String UNBAN_USER = "DELETE FROM " + table_name + " WHERE UserChatId = ?";
//    private final String IS_USER_BANNED = "SELECT UserChatId FROM " + table_name + " WHERE UserChatId = ?";
//    private final String GET_USER_BAN_TYPE = "SELECT BanType FROM " + table_name + " WHERE UserChatId = ?";
    private final String GET_ALL_BANNED_USERS = "SELECT UserChatId, Reason, BanType, AdminChatId FROM " + table_name;



    public void banUser(long userChatId, String reason, String banType, long adminChatId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(BAN_USER)) {
            preparedStatement.setLong(1, userChatId);
            preparedStatement.setNString(2, reason);
            preparedStatement.setNString(3, banType);
            preparedStatement.setLong(4, adminChatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.printf("User with chatId (%d) has been banned%n", userChatId);
            }
        } catch (SQLException e) {
            System.err.printf("Error (UserBansRepository (method banUser(usersChatId : %d, reason : %s, banType : %s, adminsChatId : %d)))%n%s%n",
                    userChatId, reason, banType, adminChatId, e);
        }
    }

    public void unbanUser(long userChatId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UNBAN_USER)) {
            preparedStatement.setLong(1, userChatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.printf("User with chatId (%d) has been unbanned%n", userChatId);
            }
        } catch (SQLException e) {
            System.err.printf("Error (UserBansRepository (method unbanUser(usersChatId : %d)))%n%s%n",
                    userChatId, e);
        }
    }

//    public boolean isUserBanned(long userChatId) {
//        try (Connection connection = database.getConnection();
//             PreparedStatement preparedStatement = connection.prepareStatement(IS_USER_BANNED)) {
//            preparedStatement.setLong(1, userChatId);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            return resultSet.next();
//        } catch (SQLException e) {
//            System.err.printf("Error (UserBansRepository (method unbanUser(usersChatId : %d)))%n%s%n",
//                    userChatId, e);
//        }
//        return false;
//    }
//
//    public String getBanType(long userChatId) {
//        try (Connection connection = database.getConnection();
//             PreparedStatement preparedStatement = connection.prepareStatement(GET_USER_BAN_TYPE)) {
//            preparedStatement.setLong(1, userChatId);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            if (resultSet.next()) {
//                return resultSet.getNString(1);
//            }
//        } catch (SQLException e) {
//            System.err.printf("Error (UserBansRepository (method unbanUser(usersChatId : %d)))%n%s%n",
//                    userChatId, e);
//        }
//        return "";
//    }

    public Map<Long, BanInfo> getAllBannedUsers() {
        Map<Long, BanInfo> map = new HashMap<>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(GET_ALL_BANNED_USERS);
            while (resultSet.next()) {
                Long chatId = resultSet.getLong(1);
                String reason = resultSet.getNString(2);
                String banType = resultSet.getNString(3);
                long adminChatId = resultSet.getLong(4);
                BanInfo banInfo = BanInfo.builder()
                        .reason(reason)
                        .banType(banType)
                        .adminChatId(adminChatId)
                        .build();
                map.put(chatId, banInfo);
            }
            return map;
        } catch (SQLException e) {
            System.err.printf("Error (UserBansRepository (method getAllBannedUsers()))%n%s%n", e);
        }
        return map;
    }
}
