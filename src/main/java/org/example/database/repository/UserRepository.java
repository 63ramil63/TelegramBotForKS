package org.example.database.repository;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserRepository {
    private final Database dataBaseConnection = Database.getInstance();
    private static final String tableName = "users";

    private static final String GET_USER = "SELECT UserName FROM " +  tableName + " WHERE ChatId=?";
    private static final String GET_FILE_PATH = "SELECT Folder FROM " + tableName + " WHERE ChatId=?";
    private static final String GET_GROUP_ID = "SELECT GroupId FROM " + tableName + " WHERE ChatId=?";
    private static final String GET_CAN_ADD_FOLDER = "SELECT CanAddFolder FROM " + tableName + " WHERE ChatId=?";
    private static final String GET_CAN_ADD_GROUP = "SELECT CanAddGroup FROM " + tableName + " WHERE ChatId=?";
    private static final String GET_CAN_ADD_LINK = "SELECT CanAddLink FROM " + tableName + " WHERE ChatId=?";
    private static final String GET_GROUP_FOR_LINKS = "SELECT GroupForLinks FROM " + tableName + " WHERE ChatId=?";
    private static final String ADD_USER = "INSERT INTO " + tableName + " (ChatId) values (?)";
    private static final String UPDATE_GROUP_ID = "UPDATE " + tableName + " SET GroupId=? WHERE ChatId=?";
    private static final String UPDATE_FILE_PATH = "UPDATE " + tableName + " SET Folder=? WHERE ChatId=?";
    private static final String UPDATE_CAN_ADD_FOLDER = "UPDATE " + tableName + " SET CanAddFolder=? WHERE ChatId=?";
    private static final String UPDATE_CAN_ADD_GROUP = "UPDATE " + tableName + " SET CanAddGroup=? WHERE ChatId=?";
    private static final String UPDATE_CAN_ADD_LINK = "UPDATE " + tableName + " SET CanAddLink=? WHERE ChatId=?";
    private static final String UPDATE_USER_NAME = "UPDATE " + tableName + " SET UserName=? WHERE ChatId=?";
    private static final String UPDATE_GROUP_FOR_LINKS = "UPDATE " + tableName + " SET GroupForLinks=? WHERE ChatId=?";
    private static final String GET_ALL_CHAT_ID = "SELECT ChatId FROM " + tableName;

    // Метод для выполнения запросов с 1 переменной в запросе
    private String executeSQLQuery(String sql, long chatId) {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String result = resultSet.getNString(1);
                if (result != null && !result.isEmpty()) {
                    resultSet.close();
                    return result;
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            System.err.printf("Error (UserRepositoryClass (method executeSQLQuery(sql : %s, chatId : %d))) %n%s%n", sql, chatId, e);
        }
        return "";
    }

    public boolean getUser(long chatId) {
        try (Connection connection = dataBaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_USER)) {
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                resultSet.close();
                return true;
            }
            resultSet.close();
        } catch (SQLException e) {
            System.err.printf("Error (UserRepositoryClass (method getUser(chatId : %d))) %n%s%n", chatId, e);
        }
        return false;
    }

    public String getUserName(long chatId) {
        try (Connection connection = dataBaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_USER)) {
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getNString("UserName");
            }
        } catch (SQLException e) {
            System.err.printf("Error (UserRepositoryClass (method getUserName(chatId : %d))) %n%s%n", chatId, e);
        }
        return "";
    }

    public String getFilePath(long chatId) {
        return executeSQLQuery(GET_FILE_PATH, chatId);
    }

    public String getGroupId(long chatId) {
        return executeSQLQuery(GET_GROUP_ID, chatId);
    }

    public String getGroupForLinks(long chatId) {
        return executeSQLQuery(GET_GROUP_FOR_LINKS, chatId);
    }

    public boolean executeSQLQueryWithBoolean(String query, long chatId) {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int result = resultSet.getInt(1);
                if (result == 1) {
                    resultSet.close();
                    return true;
                }
            }
            resultSet.close();
            return false;
        } catch (SQLException e) {
            System.err.printf("Error (UserRepositoryClass (method executeSQLQueryWithBoolean(query : %s, chatId : %d))) %n%s%n", query, chatId, e);
        }
        return false;
    }

    public boolean getCanAddFolder(long chatId) {
        return executeSQLQueryWithBoolean(GET_CAN_ADD_FOLDER, chatId);
    }

    public boolean getCanAddGroup(long chatId) {
        return executeSQLQueryWithBoolean(GET_CAN_ADD_GROUP, chatId);
    }

    public boolean getCanAddLink(long chatId) {
        return executeSQLQueryWithBoolean(GET_CAN_ADD_LINK, chatId);
    }

    public List<Long> getAllUsersChatId() {
        List<Long> chatIdArray = new ArrayList<>();
        try (Connection connection = dataBaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_CHAT_ID)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                chatIdArray.add(resultSet.getLong("ChatId"));
            }
        } catch (SQLException e) {
            System.err.printf("Error (UserRepositoryClass (method getAllUsersChatId())) %s%n", e);
        }
        return chatIdArray;
    }

    private void executeSQLUpdate(String sql, Object ... params) {
        try (Connection connection = dataBaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Long) {
                    preparedStatement.setLong(i + 1, (Long) params[i]);
                } else if (params[i] instanceof String) {
                    preparedStatement.setString(i + 1, (String) params[i]);
                } else if (params[i] instanceof Byte) {
                    preparedStatement.setByte(i + 1, (Byte) params[i]);
                }
            }
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                System.err.printf("Execute sql update error sql : %s %nParams: %s%n", sql, Arrays.toString(params));
            }
        } catch (SQLException e) {
            System.err.printf("Error (UserRepositoryClass (method executeSQLUpdate(sql : %s, params : %s))) %n%s%n", sql, Arrays.toString(params), e);
        }
    }

    public void addUser(long chatId) {
        executeSQLUpdate(ADD_USER, chatId);
        System.out.println("Add user with chatId: " + chatId);
    }

    public void updateGroupId(long chatId, String groupId) {
        executeSQLUpdate(UPDATE_GROUP_ID, groupId, chatId);
    }

    public void updateFilePath(long chatId, String filePath) {
        executeSQLUpdate(UPDATE_FILE_PATH, filePath, chatId);
    }

    public void updateCanAddFolder(long chatId, byte bool) {
        executeSQLUpdate(UPDATE_CAN_ADD_FOLDER, bool, chatId);
    }

    public void updateCanAddGroup(long chatId, byte bool) { executeSQLUpdate(UPDATE_CAN_ADD_GROUP, bool, chatId); }

    public void updateCanAddLink(long chatId, byte bool) { executeSQLUpdate(UPDATE_CAN_ADD_LINK, bool, chatId); }

    public void updateUserName(long chatId, String userName) {
        executeSQLUpdate(UPDATE_USER_NAME, userName, chatId);
    }

    public void updateGroupForLinks(long chatId, String groupForLinks) { executeSQLUpdate(UPDATE_GROUP_FOR_LINKS, groupForLinks, chatId); }
}