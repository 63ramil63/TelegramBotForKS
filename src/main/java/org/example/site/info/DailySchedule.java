package org.example.site.info;

import org.example.bot.config.BotConfig;

import java.time.LocalDate;

public class DailySchedule {
    private final LocalDate date;
    private final String scheduleText;

    public DailySchedule(LocalDate date, String scheduleText) {
        this.date = date;
        this.scheduleText = scheduleText;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getScheduleText() {
        return scheduleText;
    }

    @Override
    public String toString() {
        return String.format("Расписание на %s:\n%s", BotConfig.formatter, scheduleText);
    }
}