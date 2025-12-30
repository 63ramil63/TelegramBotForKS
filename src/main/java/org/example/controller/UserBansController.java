package org.example.controller;

import org.example.bot.ban.types.BanType;
import org.example.bot.ban.types.ban.BanList;
import org.example.bot.ban.types.ban.info.BanInfo;
import org.example.database.repository.UserBansHistoryRepository;
import org.example.database.repository.UserBansRepository;

import java.util.Map;

public class UserBansController {
    private final UserBansRepository userBansRepository;
    private final UserBansHistoryRepository userBansHistoryRepository;
    private final BanList banList;

    public UserBansController() {
        userBansRepository = new UserBansRepository();
        userBansHistoryRepository = new UserBansHistoryRepository();
        Map<Long, BanInfo> map = getAllBannedUsers();
        banList = new BanList(map);
    }

    public void banUser(long userChatId, BanInfo banInfo) {
        String reason = banInfo.getReason();
        String banType = banInfo.getBanType();
        long adminChatId = banInfo.getAdminChatId();

        userBansRepository.banUser(userChatId, reason, banType, adminChatId);
        userBansHistoryRepository.banUser(userChatId, reason, banType, adminChatId);
        banList.addToList(userChatId, banInfo);
    }

    public void unbanUser(long userChatId, long adminChatId) {
        userBansRepository.unbanUser(userChatId);
        userBansHistoryRepository.unbanUser(userChatId, adminChatId);
        banList.removeFromList(userChatId);
    }

    public boolean isUserBanned(long userChatId) {
        return banList.isUserBanned(userChatId);
    }

    public BanInfo getUserBanInfo(long userChatId) {
        return banList.getBanInfo(userChatId);
    }

    private Map<Long, BanInfo> getAllBannedUsers() {
        return userBansRepository.getAllBannedUsers();
    }
}