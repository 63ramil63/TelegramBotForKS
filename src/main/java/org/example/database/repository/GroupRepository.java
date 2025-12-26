package org.example.database.repository;

import org.example.database.Database;
import org.example.dto.GroupDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupRepository {
    private final Database database = Database.getInstance();
    private static final String tableName = "edu_groups";

    private static final String GET_ALL_GROUPS = "SELECT Id, GroupName FROM " + tableName;
    private static final String GET_GROUP_NAME_BY_ID = "SELECT GroupName FROM " + tableName + " WHERE Id = ?";
    private static final String ADD_NEW_GROUP = "INSERT INTO " + tableName + " (GroupName) values (?)";
    private static final String DELETE_GROUP_BY_ID = "DELETE FROM " + tableName + " WHERE Id = ?";

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
            System.err.printf("Error (GroupRepositoryClass (method addNewGroup(%s))) %s%n", groupName, e);
        }
    }

    public List<GroupDTO> getAllGroups() {
        List<GroupDTO> groups = new ArrayList<>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(GET_ALL_GROUPS);
            while (resultSet.next()) {
                long groupId = resultSet.getLong(1);
                String groupName = resultSet.getNString(2);
                GroupDTO groupDTO = GroupDTO.builder()
                        .id(groupId)
                        .groupName(groupName)
                        .build();
                groups.add(groupDTO);
            }
            return groups;
        } catch (SQLException e) {
            System.err.println("Error (GroupRepositoryClass (method getAllGroups())) " + e);
        }
        return groups;
    }

    public String getGroupNameById(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_GROUP_NAME_BY_ID)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getNString(1);
            }
        } catch (SQLException e) {
            System.err.printf("Error (GroupRepositoryClass (method getGroupNameById(%d))) %s%n", id, e);
        }
        throw new IllegalArgumentException("edu_group doesn't has a group with id : " + id);
    }

    public boolean deleteGroupById(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_GROUP_BY_ID)) {
            preparedStatement.setLong(1, id);
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.printf("Error (GroupRepositoryClass (method getGroupNameById(%d))) %s%n", id, e);
        }
        return false;
    }
}
