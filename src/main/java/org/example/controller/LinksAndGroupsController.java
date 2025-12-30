package org.example.controller;

import org.example.database.repository.GroupRepository;
import org.example.database.repository.LinksHistoryRepository;
import org.example.database.repository.LinksRepository;
import org.example.dto.GroupDTO;
import org.example.dto.LinkDTO;

import java.util.List;

public class LinksAndGroupsController {
    private final LinksRepository linksRepository;
    private final LinksHistoryRepository linksHistoryRepository;
    private final GroupRepository groupRepository;

    public LinksAndGroupsController() {
        linksRepository = new LinksRepository();
        linksHistoryRepository = new LinksHistoryRepository();
        groupRepository = new GroupRepository();
    }

    public void addLink(String linkName, String link, String groupName,  long chatId) {
        linksRepository.addLink(linkName, link, groupName, chatId);
        linksHistoryRepository.addLink(linkName, link, groupName, chatId);
    }

    public List<LinkDTO> getAllLinksByGroupName(String groupName) {
        return linksRepository.getAllLinksByGroupName(groupName);
    }

    public List<LinkDTO> getAllLinksByUsersChatId(long chatId) {
        return linksRepository.getAllLinksByUsersChatId(chatId);
    }

    public long getUsersChatIdByLinkId(long linkId) {
        return linksRepository.getUsersChatIdByLinkId(linkId);
    }

    public String getLinkById(long id) {
        return linksRepository.getLinkById(id);
    }

    public boolean deleteLinkById(long id) {
        return linksRepository.deleteLinkById(id);
    }

    public void deleteAllLinksByGroup(String group) {
        linksRepository.deleteAllLinksByGroup(group);
    }

    public void addNewGroup(long chatId, String groupName) {
        groupRepository.addNewGroup(chatId, groupName);
    }

    public List<GroupDTO> getAllGroups() {
        return groupRepository.getAllGroups();
    }

    public String getGroupNameById(long id) {
        return groupRepository.getGroupNameById(id);
    }

    public boolean deleteGroupById(long id) {
        return groupRepository.deleteGroupById(id);
    }

    public void deleteGroupWithLinksByGroupId(long id) {
        deleteGroupById(id);
        String groupName = getGroupNameById(id);
        deleteAllLinksByGroup(groupName);
    }
}
