package org.example.schedule;

import org.example.site.WebSite;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ScheduleCache {
    private final int cacheDuration;
    private static final Map<String, CachedSchedule> cachedSchedule = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private String formatDate(boolean isToday) {
        if (isToday) {
            return LocalDate.now().format(formatter);
        } else {
            return LocalDate.now().plusDays(1).format(formatter);
        }
    }

    public ScheduleCache(int duration) {
        this.cacheDuration = duration;
    }

    private void parseSchedule(String groupId) {
        String today = formatDate(true);
        String tomorrow = formatDate(false);
        WebSite webSite = new WebSite();
        String scheduleToday = webSite.getSchedule(today, groupId);
        String scheduleTomorrow = webSite.getSchedule(tomorrow, groupId);
        cachedSchedule.put(groupId, new CachedSchedule(scheduleToday, scheduleTomorrow, cacheDuration));
    }

    public synchronized String getScheduleToday(String groupId) {
        if (cachedSchedule.get(groupId).isExpired()) {
            clearExpiredCache();
            parseSchedule(groupId);
        }
        return cachedSchedule.get(groupId).scheduleToday;
    }

    public synchronized String getScheduleTomorrow(String groupId) {
        if (cachedSchedule.get(groupId).isExpired()) {
            clearExpiredCache();
            parseSchedule(groupId);
        }
        return cachedSchedule.get(groupId).scheduleTomorrow;
    }

    private synchronized void putSchedule(String group, String scheduleToday, String scheduleTomorrow) {
        cachedSchedule.put(group, new CachedSchedule(scheduleToday, scheduleTomorrow, cacheDuration));
    }

    public void clearExpiredCache() {
        cachedSchedule.entrySet().removeIf(entry -> entry.getValue().isExpired());
        System.out.println("Cleared cachedSchedule");
    }

    private static class CachedSchedule {
        String scheduleToday;
        String scheduleTomorrow;
        long timeStamp;
        int duration;

        public CachedSchedule(String scheduleToday, String scheduleTomorrow, int duration) {
            this.timeStamp = System.currentTimeMillis();
            this.scheduleToday = scheduleToday;
            this.scheduleTomorrow = scheduleTomorrow;
            this.duration = duration;
        }

        public boolean isExpired() {
            //текущее время - время создания > времени существования
            return System.currentTimeMillis() - timeStamp > TimeUnit.MINUTES.toMillis(duration);
        }
    }
}
