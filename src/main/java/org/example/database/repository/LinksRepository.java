package org.example.database.repository;

import org.example.database.Database;
import org.example.dto.LinkDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LinksRepository {
    private static final Database database = Database.getInstance();
    private static final String tableName = "links";
    private static final String historyTable = "links_history";

    private static final String GET_ALL_LINKS_BY_GROUP_NAME = "SELECT Id, LinkName FROM " + tableName + " WHERE GroupName = ?";
    private static final String GET_ALL_LINKS_BY_USERS_CHAT_ID = "SELECT Id, LinkName, GroupName FROM " + tableName + " WHERE UsersChatId = ?";
    private static final String ADD_NEW_LINK = "INSERT INTO " + tableName + " (LinkName, GroupName, Link, UsersChatId) values (?, ?, ?, ?)";
    private static final String GET_LINK_INFO_BY_ID = "SELECT Link FROM " + tableName + " WHERE Id = ?";
    private static final String GET_USERS_CHAT_ID_BY_LINK_ID = "SELECT UsersChatId FROM " + tableName + " WHERE Id = ?";
    private static final String DELETE_LINK_BY_ID = "DELETE FROM " + tableName + " WHERE Id = ?";

    private static final String ADD_LINK_IN_HISTORY_TABLE = "INSERT INTO " + historyTable + " (LinkName, GroupName, Link, UsersChatId) values (?, ?, ?, ?)";

    private void prepareStatementForUsage(PreparedStatement preparedStatement, String linkName, String link, String groupName, long chatId) throws SQLException {
        preparedStatement.setString(1, linkName);
        preparedStatement.setString(2, groupName);
        preparedStatement.setString(3, link);
        preparedStatement.setLong(4, chatId);
    }

    public void addLink(String linkName, String link, String groupName,  long chatId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_NEW_LINK);
             PreparedStatement preparedStatementForHistory = connection.prepareStatement(ADD_LINK_IN_HISTORY_TABLE)) {
            prepareStatementForUsage(preparedStatement, linkName, link, groupName, chatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.printf("New link added in links table, linkName: %s, user's chat id is: %d%n", linkName, chatId);
            } else {
                System.out.printf("Failed to add new link, linkName: %s, user's chat id is: %d%n", linkName, chatId);
            }
            prepareStatementForUsage(preparedStatementForHistory, linkName, link, groupName, chatId);
            rowsAffected = preparedStatementForHistory.executeUpdate();
            if (rowsAffected > 0) {
                System.out.printf("New link added in history table, linkName: %s, user's chat id is: %d%n", linkName, chatId);
            } else {
                System.out.printf("Failed to add new link, linkName: %s, user's chat id is: %d%n", linkName, chatId);
            }
        } catch (SQLException e) {
            System.err.println("Error (LinksRepositoryClass (method addLink())) " + e);
        }
    }

    public List<LinkDTO> getAllLinksByGroupName(String groupName) {
        List<LinkDTO> links = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_LINKS_BY_GROUP_NAME)) {
            preparedStatement.setString(1, groupName);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String linkName = resultSet.getNString(2);
                LinkDTO linkDTO = LinkDTO.builder()
                        .id(id)
                        .linkName(linkName)
                        .build();
                links.add(linkDTO);
            }
            return links;
        } catch (SQLException e) {
            System.err.printf("Error (LinksRepositoryClass (method getAllLinksByGroupName(data : %s))) %n%s%n", groupName, e);
        }
        return links;
    }

    public List<LinkDTO> getAllLinksByUsersChatId(long chatId) {
        List<LinkDTO> links = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_LINKS_BY_USERS_CHAT_ID)) {
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String linkName = resultSet.getNString(2);
                String groupName = resultSet.getNString(3);
                LinkDTO linkDTO = LinkDTO.builder()
                                .id(id)
                                .groupName(groupName)
                                .linkName(linkName)
                                .build();
                links.add(linkDTO);
            }
            return links;
        } catch (SQLException e) {
            System.err.printf("Error (LinksRepositoryClass (method getAllLinksByUsersChatId(data : %d))) %n%s%n", chatId, e);
        }
        return links;
    }

    public long getUsersChatIdByLinkId(long linkId) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_USERS_CHAT_ID_BY_LINK_ID)) {
            preparedStatement.setLong(1, linkId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        } catch (SQLException e) {
            System.err.printf("Error (LinksRepositoryClass (method getUsersChatIdByLinkId(data : %d))) %n%s%n", linkId, e);
        }
        return -1;
    }

    public String getLinkById(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_LINK_INFO_BY_ID)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getNString(1);
            }
        }  catch (SQLException e) {
            System.err.printf("Error (LinksRepositoryClass (method getLinkById(id : %s))) %n%s%n", id, e);
        }
        return null;
    }

    public boolean deleteLinkById(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_LINK_BY_ID)) {
            preparedStatement.setLong(1, id);
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.printf("Error (LinksRepositoryClass (method deleteLinkById(id : %s))) %n%s%n", id, e);
        }
        return false;
    }
}
