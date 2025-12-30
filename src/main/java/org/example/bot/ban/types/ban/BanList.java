package org.example.bot.ban.types.ban;

import org.example.bot.ban.types.ban.info.BanInfo;

import java.util.HashMap;
import java.util.Map;

public class BanList {
    private final Map<Long, BanInfo> banList;

    public BanList(Map<Long, BanInfo> map) {
        banList = new HashMap<>(map);
    }

    public boolean isUserBanned(long chatId) {
        return banList.containsKey(chatId);
    }

    public BanInfo getBanInfo(long chatId) {
        return banList.get(chatId);
    }

    public void addToList(long chatId, BanInfo banInfo) {
        banList.put(chatId, banInfo);
    }

    public void removeFromList(long chatId) {
        banList.remove(chatId);
    }
}
