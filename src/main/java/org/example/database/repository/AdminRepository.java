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

    private static final String GET_ADMINS = "SELECT Username FROM " + tableName;
    private static final String ADD_ADMIN = "INSERT INTO " + tableName + " (Username, Role) values (?, ?)";
    private static final String GET_ADMIN = "SELECT Username FROM " + tableName + " WHERE Username = ?";
    private static final String GET_ADMIN_ROLE = "SELECT Role FROM " + tableName + " WHERE Username = ?";


    public List<String> getAdminsUsername() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_ADMINS)) {
            List<String> adminsChatId = new ArrayList<>();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                adminsChatId.add(resultSet.getNString(1));
            }
            return adminsChatId;
        } catch (SQLException e) {
            System.err.println("Error (AdminRepositoryClass (method getAdmins())) " + e);
        }
        return null;
    }

    public boolean addAdmin(String username, String role) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(ADD_ADMIN)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, role);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Add admin with username: " + username);
                return true;
            } else {
                System.out.println("Error admin with username: " + username);
            }
        } catch (SQLException e) {
            System.err.println("Error (AdminRepositoryClass (method addAdmin())) " + username + " " + e);
        }
        return false;
    }

    public boolean getAdmin(String username) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ADMIN)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error (AdminRepositoryClass (method getAdmin())) username: " + username + " " + e);
        }
        return false;
    }

    public String getAdminRole(String username) {
        try (Connection connection = databaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ADMIN_ROLE)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getNString(1);
            }
        } catch (SQLException e) {
            System.err.println("Error (AdminRepositoryClass (method getAdminRole())) username: " + username + " " + e);
        }
        return "";
    }
}
