package org.example.database.repository;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LinksRepository {
    private static final Database database = Database.getInstance();
    private static final String tableName = "links";

    private static final String GET_ALL_LINKS_BY_GROUP_NAME = "SELECT LinkName FROM " + tableName + " WHERE GroupName = ?";
    private static final String GET_ALL_LINKS_BY_USERS_CHAT_ID = "SELECT LinkName FROM " + tableName + " WHERE UsersChatId = ?";
    private static final String GET_LINK_BY_LINK_NAME_AND_GROUP = "SELECT Link FROM " + tableName + " WHERE LinkName = ? AND GroupName = ?";
    private static final String ADD_NEW_LINK = "INSERT INTO " + tableName + " (LinkName, GroupName, Link, UsersChatId) values (?, ?, ?, ?)";
    
    public void addLink(String linkName, String link, String groupName,  long chatId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_NEW_LINK)) {
            preparedStatement.setString(1, linkName);
            preparedStatement.setString(2, groupName);
            preparedStatement.setString(3, link);
            preparedStatement.setLong(4, chatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.printf("New link added, linkName: %s, user's chat id is: %d%n", linkName, chatId);
            } else {
                System.out.printf("Failed to add new link, linkName: %s, user's chat id is: %d%n", linkName, chatId);
            }
        } catch (SQLException e) {
            System.err.println("Error (LinksRepositoryClass (method addLink())) " + e);
        }
    }

    public List<String> getAllLinksByGroupName(String groupName) {
        List<String> links = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_LINKS_BY_GROUP_NAME)) {
            preparedStatement.setString(1, groupName);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                links.add(resultSet.getNString(1));
            }
            return links;
        } catch (SQLException e) {
            System.err.printf("Error (LinksRepositoryClass (method getAllLinksByGroupName(data : %s))) %n" + e);
        }
        return links;
    }

    public List<String> getAllLinksByUsersChatId(long chatId) {
        List<String> links = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_LINKS_BY_USERS_CHAT_ID)) {
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                links.add(resultSet.getNString(1));
            }
            return links;
        } catch (SQLException e) {
            System.err.printf("Error (LinksRepositoryClass (method getAllLinksByUsersChatId(data : %d))) %n%s%n", chatId, e);
        }
        return links;
    }

    public String getLinkByNameAndGroup(String linkName, String group) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_LINK_BY_LINK_NAME_AND_GROUP)) {
            preparedStatement.setString(1, linkName);
            preparedStatement.setString(2, group);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getNString(1);
            }
        } catch (SQLException e) {
            System.err.printf("Error (LinksRepositoryClass (method getLinkByNameAndGroup(linkName : %s, group : %s)))%n%s%n", linkName, group, e);
        }
        return null;
    }
}
