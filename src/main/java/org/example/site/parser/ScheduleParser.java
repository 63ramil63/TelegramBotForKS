package org.example.site.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ScheduleParser {
    private static final int MAX_ROWS = 60;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Найти все расписания с датами из документа
     */
    public Map<LocalDate, String> parseAllSchedules(Document document) {
        Map<LocalDate, String> schedules = new HashMap<>();

        for (int row = 1; row <= MAX_ROWS; row++) {
            Element rowElement = document.select(createRowSelector(row)).first();
            if (rowElement != null) {
                String rowText = rowElement.text();

                // Ищем дату в строке
                LocalDate date = extractDateFromText(rowText);
                if (date != null) {
                    // Следующие строки содержат расписание
                    String schedule = extractScheduleForDate(row, document);
                    if (!schedule.trim().isEmpty()) {
                        schedules.put(date, schedule);
                    }
                    // Пропускаем строки с расписанием
                    row += countScheduleRows(row, document);
                }
            }
        }

        return schedules;
    }

    /**
     * Старый метод для обратной совместимости
     */
    public String findScheduleForDay(String day, Document document) {
        for (int row = 1; row <= MAX_ROWS; row++) {
            if (isDayRow(day, row, document)) {
                return formatDaySchedule(day, row + 1, document);
            }
        }
        return "Not found";
    }

    /**
     * Извлечь дату из текста
     */
    private LocalDate extractDateFromText(String text) {
        // Ищем подстроку с форматом даты dd.MM.yyyy
        String[] words = text.split("\\s+");
        for (String word : words) {
            word = word.trim();
            if (word.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                try {
                    return LocalDate.parse(word, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    // Продолжаем поиск
                }
            }
        }
        return null;
    }

    /**
     * Извлечь расписание для найденной даты
     */
    private String extractScheduleForDate(int dateRow, Document document) {
        StringBuilder schedule = new StringBuilder();
        int currentRow = dateRow + 2;

        while (hasScheduleRow(currentRow, document)) {
            schedule.append(extractLessonInfo(currentRow, document)).append("\n");
            currentRow++;
        }

        return cleanScheduleText(schedule.toString());
    }

    /**
     * Подсчитать количество строк с расписанием
     */
    private int countScheduleRows(int startRow, Document document) {
        int count = 0;
        int currentRow = startRow + 1;

        while (hasScheduleRow(currentRow, document)) {
            count++;
            currentRow++;
        }

        return count;
    }

    private boolean isDayRow(String day, int row, Document document) {
        String selector = createRowSelector(row);
        return document.select(selector).text().contains(day);
    }

    private String formatDaySchedule(String day, int startRow, Document document) {
        StringBuilder schedule = new StringBuilder(day);
        int currentRow = startRow;
        currentRow++;

        while (hasScheduleRow(currentRow, document)) {
            schedule.append(extractLessonInfo(currentRow, document));
            currentRow++;
        }

        return cleanScheduleText(schedule.toString());
    }

    private boolean hasScheduleRow(int row, Document document) {
        String selector = createRowSelector(row);
        Elements elements = document.select(selector);
        return !elements.isEmpty() && !elements.text().trim().isEmpty();
    }

    private String extractLessonInfo(int row, Document document) {
        String number = extractCellText(document, row, 1);
        String time = extractCellText(document, row, 2);
        String schedule = extractCellText(document, row, 4);
        String normalizedSchedule = normalizeScheduleText(schedule);

        return String.format("\n%s) %s\n%s", number, time, normalizedSchedule);
    }

    private String extractCellText(Document document, int row, int column) {
        String selector = String.format(
                "body > table:nth-child(5) > tbody > tr:nth-child(%d) > td:nth-child(%d)",
                row, column
        );
        Element element = document.select(selector).first();
        return element != null ? element.text().trim() : "";
    }

    private String createRowSelector(int row) {
        return String.format("body > table:nth-child(5) > tbody > tr:nth-child(%d)", row);
    }

    private String normalizeScheduleText(String schedule) {
        return schedule.replace("Кабинет:", "\nКабинет:")
                .replace("дистанционно", "(дистант)")
                .trim();
    }

    private String cleanScheduleText(String schedule) {
        return schedule.replaceAll("\\s+$", "")
                .replaceAll("Московское шоссе, 120", "")
                .replaceAll(" Замена Свободное время на:", "")
                .trim();
    }
}