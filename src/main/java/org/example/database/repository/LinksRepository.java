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
    private static final String ADD_NEW_LINK = "INSERT INTO link (LinkName, GroupName, Link, UsersChatId) values (?, ?, ?, ?)";
    
    public void addLink(String linkName, String link, String groupName,  long chatId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_NEW_LINK)) {
            preparedStatement.setString(1, linkName);
            preparedStatement.setString(2, groupName);
            preparedStatement.setString(3, link);
            preparedStatement.setLong(4, chatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("New link added, linkName: " + linkName + ", user's chat id is: " + chatId);
            } else {
                System.out.println("Failed to add new link, linkName: " + linkName + ", user's chat id is: " + chatId);
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
            System.err.println("Error (LinksRepositoryClass (method getAllLinksByGroupName())) " + e);
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
            System.err.println("Error (LinksRepositoryClass (method getAllLinksByUsersChatId())) " + e);
        }
        return links;
    }
}
