package org.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private static final Database dataBaseConnection = Database.getInstance();
    private static final String tableName = dataBaseConnection.tableName;

    private static final String GET_USER = "SELECT UserName FROM " +  tableName + " WHERE ChatId=?";
    private static final String GET_FILE_PATH = "SELECT FilePath FROM " + tableName + " WHERE ChatId=?";
    private static final String GET_GROUP_ID = "SELECT GroupId FROM " + tableName + " WHERE ChatId=?";
    private static final String GET_CAN_ADD_FOLDER = "SELECT CanADDFolder FROM " + tableName + " WHERE ChatId=?";
    private static final String ADD_USER = "INSERT INTO " + tableName + " (ChatId) values (?)";
    private static final String UPDATE_GROUP_ID = "UPDATE " + tableName + " SET GroupId=? WHERE ChatId=?";
    private static final String UPDATE_FILE_PATH = "UPDATE " + tableName + " SET FilePath=? WHERE ChatId=?";
    private static final String UPDATE_CAN_ADD_FOLDER = "UPDATE " + tableName + " SET CanAddFolder=? WHERE ChatId=?";
    private static final String UPDATE_USER_NAME = "UPDATE " + tableName + " SET UserName=? WHERE ChatId=?";

    // Метод для выполнения запросов с 1 переменной в запросе
    private String executeSQLQuery(String sql, long chatId) {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String result = resultSet.getNString(1);
                if (result != null) {
                    resultSet.close();
                    return result;
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return "Not found";
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
            System.err.println("Error (UserRepositoryClass (method getUser()))" + e);
        }
        return false;
    }

    public String getFilePath(long chatId) {
        return executeSQLQuery(GET_FILE_PATH, chatId);
    }

    public String getGroupId(long chatId) {
        return executeSQLQuery(GET_GROUP_ID, chatId);
    }

    public boolean getCanAddFolder(long chatId) {
        try (Connection connection = dataBaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_CAN_ADD_FOLDER)) {
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int result = resultSet.getInt("CanAddFolder");
                if (result == 1) {
                    resultSet.close();
                    return true;
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error (UserRepositoryClass (method getCanAddFolder))" + e);
        }
        return false;
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
                System.out.println("Execute update error");
            }
        } catch (SQLException e) {
            System.err.println("Error (UserRepositoryClass (method executeSQLUpdate())) " + e);
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

    public void updateUserName(long chatId, String userName) {
        executeSQLUpdate(UPDATE_USER_NAME, userName, chatId);
    }
}