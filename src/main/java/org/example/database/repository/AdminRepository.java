package org.example.database.repository;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminRepository {
    private static final Database databaseConnection = Database.getInstance();
    private static final String tableName = "admins";

    private static final String GET_ADMINS = "SELECT ChatId FROM " + tableName;
    private static final String ADD_ADMIN = "INSERT INTO " + tableName + " (ChatId, Role) values (?, ?)";

    public List<Long> getAdminsChatId() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_ADMINS)) {
            List<Long> adminsChatId = new ArrayList<>();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                adminsChatId.add(resultSet.getLong(1));
            }
            return adminsChatId;
        } catch (SQLException e) {
            System.err.println("Error (AdminRepositoryClass (method getAdmins())) " + e);
        }
        return null;
    }

    public void addAdmin(long chatId, String role) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(ADD_ADMIN)) {
            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, role);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Add admin with chatId: " + chatId);
            } else {
                System.out.println("Error admin with chatId: " + chatId);
            }
        } catch (SQLException e) {
            System.err.println("Error (AdminRepositoryClass (method addAdmin())) " + chatId);
        }
    }
}
