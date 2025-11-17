package org.example.database.repository;

import org.example.database.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupRepository {
    private static final Database database = Database.getInstance();
    private static final String tableName = "GROUPS";

    private static final String GET_ALL_GROUPS = "SELECT GroupName FROM " + tableName;
    private static final String ADD_NEW_GROUP = "INSERT INTO " + tableName + " (GroupName) values (?)";

    public void addNewGroup(String groupName) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_NEW_GROUP)) {
            preparedStatement.setString(1, groupName);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Added new group: " + groupName);
            } else {
                System.out.println("Failed to add new group: " + groupName);
            }
        } catch (SQLException e) {
            System.err.println("Error (GroupRepositoryClass (method addNewGroup())) " + e);
        }
    }

    public List<String> getAllGroups() {
        List<String> groups = new ArrayList<>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(GET_ALL_GROUPS);
            while (resultSet.next()) {
                groups.add(resultSet.getNString(1));
            }
            return groups;
        } catch (SQLException e) {
            System.err.println("Error (GroupRepositoryClass (method getAllGroups())) " + e);
        }
        return groups;
    }
}
