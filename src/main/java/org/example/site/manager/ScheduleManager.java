package org.example.site.manager;

import org.example.bot.config.BotConfig;
import org.example.site.fetcher.DocumentFetcher;
import org.example.site.info.GroupScheduleInfo;
import org.example.site.parser.ScheduleParser;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduleManager {
    private final Map<String, GroupScheduleInfo> scheduleCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduleParser scheduleParser = new ScheduleParser();

    public ScheduleManager() {
        startScheduledUpdates();
    }

    /**
     * Получить расписание на конкретный день для группы
     */
    public String getScheduleForDate(String groupId, LocalDate date) {
        GroupScheduleInfo groupInfo = scheduleCache.get(groupId);

        // Если нет в кэше или истек срок, пытаемся обновить
        if (groupInfo == null || groupInfo.isExpired()) {
            try {
                updateGroupSchedule(groupId);
                groupInfo = scheduleCache.get(groupId);
            } catch (IOException e) {
                System.err.printf("Не удалось обновить расписание для группы %s: %s%n",
                        groupId, e.getMessage());
                return getFallbackSchedule(groupId, date, groupInfo);
            }
        }

        // Если после обновления всё равно нет информации
        if (groupInfo == null) {
            return String.format("Нет данных о расписании для группы %s", groupId);
        }

        // Ищем расписание на конкретную дату
        String schedule = groupInfo.getScheduleForDate(date);

        if (schedule != null) {
            return schedule;
        } else {
            // Если расписание не найдено для точной даты
            return String.format("Нет данных о расписании на %s",
                    date.format(BotConfig.formatter));
        }
    }

    /**
     * Получить все доступные даты для группы
     */
    public List<LocalDate> getAvailableDates(String groupId) {
        GroupScheduleInfo groupInfo = scheduleCache.get(groupId);
        if (groupInfo == null || groupInfo.getAllSchedules().isEmpty() || groupInfo.isExpired()) {
            try {
                updateGroupSchedule(groupId);
                groupInfo = scheduleCache.get(groupId);
            } catch (IOException e) {
                System.err.printf("Не удалось обновить расписание для группы %s: %s%n",
                        groupId, e.getMessage());
            }
        }

        if (groupInfo == null || groupInfo.getAllSchedules().isEmpty()) {
            return new ArrayList<>();
        }

        List<LocalDate> dates = new ArrayList<>(groupInfo.getAllSchedules().keySet());
        dates.sort(LocalDate::compareTo);
        return dates;
    }

    /**
     * Резервное расписание при ошибке обновления
     */
    private String getFallbackSchedule(String groupId, LocalDate date, GroupScheduleInfo oldInfo) {
        if (oldInfo != null) {
            String schedule = oldInfo.getScheduleForDate(date);
            if (schedule != null) {
                return schedule + "\n(может быть не актуально)";
            }
        }
        return String.format("Не удалось получить расписание на %s для группы %s",
                date.format(BotConfig.formatter), groupId);
    }

    /**
     * Принудительное обновление расписания для группы
     */
    public boolean updateGroupSchedule(String groupId) throws IOException {
        try {
            DocumentFetcher documentFetcher = new DocumentFetcher(groupId);
            org.example.site.info.ScheduleDocumentInfo docInfo = documentFetcher.getScheduleDocumentInfo();

            Map<LocalDate, String> schedules = new HashMap<>();

            // Парсим текущую неделю
            Map<LocalDate, String> currentWeekSchedules =
                    scheduleParser.parseAllSchedules(docInfo.getThisWeekDocument());
            schedules.putAll(currentWeekSchedules);

            // Парсим следующую неделю
            Map<LocalDate, String> nextWeekSchedules =
                    scheduleParser.parseAllSchedules(docInfo.getNextWeekDocument());
            schedules.putAll(nextWeekSchedules);

            // Создаем или обновляем информацию о группе
            GroupScheduleInfo groupInfo = new GroupScheduleInfo(groupId, schedules);
            scheduleCache.put(groupId, groupInfo);

            System.out.printf("Обновлено расписание для группы %s. Найдено %d дней%n",
                    groupId, schedules.size());
            return true;

        } catch (IOException e) {
            throw new IOException("Ошибка при обновлении расписания для группы " + groupId, e);
        }
    }

    /**
     * Запуск периодических обновлений
     */
    private void startScheduledUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            for (String groupId : scheduleCache.keySet()) {
                GroupScheduleInfo groupInfo = scheduleCache.get(groupId);
                if (groupInfo != null && groupInfo.isExpired()) {
                    try {
                        updateGroupSchedule(groupId);
                        System.out.printf("Автоматически обновлено расписание для группы: %s%n", groupId);
                    } catch (IOException e) {
                        System.err.printf("Не удалось обновить расписание для группы %s: %s%n",
                                groupId, e.getMessage());
                    }
                }
            }
        }, 1, 5, TimeUnit.MINUTES);
    }

    /**
     * Проверить, есть ли группа в кэше
     */
    public boolean hasGroupInCache(String groupId) {
        return scheduleCache.containsKey(groupId);
    }

    /**
     * Получить информацию о конкретной группе из кэша
     */
    public GroupScheduleInfo getGroupInfo(String groupId) {
        return scheduleCache.get(groupId);
    }

    /**
     * Очистить кэш для конкретной группы
     */
    public void clearGroupCache(String groupId) {
        scheduleCache.remove(groupId);
    }

    /**
     * Очистить весь кэш
     */
    public void clearAllCache() {
        scheduleCache.clear();
    }

    /**
     * Остановка менеджера
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
