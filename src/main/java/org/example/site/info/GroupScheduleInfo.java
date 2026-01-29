package org.example.site.info;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupScheduleInfo {
    private final String groupId;
    private final Map<LocalDate, DailySchedule> dailySchedules;
    private final LocalDateTime lastUpdateTime;
    private static final long CACHE_DURATION_MINUTES = 30;

    public GroupScheduleInfo(String groupId, Map<LocalDate, String> scheduleTexts) {
        this.groupId = groupId;
        this.dailySchedules = new ConcurrentHashMap<>();
        this.lastUpdateTime = LocalDateTime.now();

        // Преобразуем тексты расписаний в объекты DailySchedule
        for (Map.Entry<LocalDate, String> entry : scheduleTexts.entrySet()) {
            dailySchedules.put(entry.getKey(), new DailySchedule(entry.getKey(), entry.getValue()));
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public String getScheduleForDate(LocalDate date) {
        DailySchedule schedule = dailySchedules.get(date);
        return schedule != null ? schedule.getScheduleText() : null;
    }

    public Map<LocalDate, DailySchedule> getAllSchedules() {
        return new ConcurrentHashMap<>(dailySchedules);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(
                lastUpdateTime.plusMinutes(CACHE_DURATION_MINUTES)
        );
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Добавить или обновить расписание на день
     */
    public void addOrUpdateSchedule(LocalDate date, String scheduleText) {
        dailySchedules.put(date, new DailySchedule(date, scheduleText));
    }
}