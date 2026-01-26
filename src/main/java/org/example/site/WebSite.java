package org.example.site;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebSite {
    private static final String BASE_URL = "https://lk.ks.psuti.ru/?mn=2";
    private static final String SCHEDULE_URL = BASE_URL + "&obj=";
    private static final String USER_AGENT = "Chrome";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_YEARS = 10;
    private static final int MAX_ROWS = 60;

    private static final int YEAR_ROW_INDEX = 6;
    private static final int GROUP_ROW_INDEX = 7;
    private static final int NEXT_WEEK_ROW_INDEX = 4;
    private static final int NEXT_WEEK_COL_INDEX = 8;

    private static final String YEAR_PREFIX = "Year";
    private static final String GROUP_PREFIX = "Group";

    public List<String> getYears() {
        try {
            Document document = fetchDocument(BASE_URL);
            return extractYears(document);
        } catch (IOException e) {
            handleError("getYears", e);
            return null;
        }
    }

    public List<String> getGroups(int yearColumn) {
        try {
            Document document = fetchDocument(BASE_URL);
            return extractGroups(document, yearColumn);
        } catch (IOException e) {
            handleError("getGroups", e);
            return null;
        }
    }

    public String getSchedule(String day, String groupId) {
        try {
            String schedule = findScheduleForCurrentWeek(day, groupId);

            if (!schedule.equals("Not found")) {
                return schedule;
            }

            schedule = findScheduleForNextWeek(day, groupId);

            if (!schedule.equals("Not found")) {
                return schedule;
            }

            return createNoScheduleMessage(groupId);
        } catch (IOException e) {
            handleError("getSchedule", e);
            return "Not found";
        }
    }

    private List<String> extractYears(Document document) {
        List<String> years = new ArrayList<>();

        for (int column = 1; column <= MAX_YEARS; column++) {
            String yearText = extractYearText(document, column);
            if (!yearText.isEmpty()) {
                years.add(yearText + YEAR_PREFIX + column);
            }
        }
        return years;
    }

    private String extractYearText(Document document, int column) {
        String selector = String.format("body > table:nth-child(5) > tbody > tr:nth-child(%d) > td:nth-child(%d)",
                YEAR_ROW_INDEX, column);
        return document.select(selector).text();
    }

    private List<String> extractGroups(Document document, int yearColumn) {
        List<String> groups = new ArrayList<>();
        Elements groupElements = getGroupElements(document, yearColumn);

        for (int row = 1; row <= groupElements.size(); row++) {
            String groupInfo = extractGroupInfo(groupElements, row);
            groups.add(groupInfo);
        }
        return groups;
    }

    private Elements getGroupElements(Document document, int yearColumn) {
        String selector = String.format(
                "body > table:nth-child(5) > tbody > tr:nth-child(%d) > td:nth-child(%d) > table > tbody > tr:nth-child(1) > td > table > tbody > tr",
                GROUP_ROW_INDEX, yearColumn
        );
        return document.select(selector);
    }

    private String extractGroupInfo(Elements groupElements, int rowIndex) {
        Elements row = groupElements.select("tr:nth-child(" + rowIndex + ")");
        Elements groupLink = row.select("td > a");
        String href = groupLink.attr("href");
        String groupId = extractGroupIdFromHref(href);

        return row.text() + GROUP_PREFIX + groupId;
    }

    private String extractGroupIdFromHref(String href) {
        int index = href.indexOf("obj");
        if (index != -1) {
            return href.substring(index).replace("obj", "");
        }
        return "";
    }

    private String findScheduleForCurrentWeek(String day, String groupId) throws IOException {
        Document document = fetchDocument(SCHEDULE_URL + groupId);
        return searchDayInSchedule(day, document);
    }

    private String findScheduleForNextWeek(String day, String groupId) throws IOException {
        Document currentWeekDoc = fetchDocument(SCHEDULE_URL + groupId);
        String weekParam = extractNextWeekParameter(currentWeekDoc);

        Document nextWeekDoc = fetchDocument(SCHEDULE_URL + groupId + "&" + weekParam);
        return searchDayInSchedule(day, nextWeekDoc);
    }

    private String extractNextWeekParameter(Document document) {
        String selector = String.format(
                "body > table:nth-child(%d) > tbody > tr:nth-child(%d) > td > table > tbody > tr > td:nth-child(%d) > a",
                NEXT_WEEK_ROW_INDEX, NEXT_WEEK_ROW_INDEX, NEXT_WEEK_COL_INDEX
        );

        String href = document.select(selector).attr("href");
        int index = href.indexOf("wk");
        return (index != -1) ? href.substring(index) : "";
    }

    private String searchDayInSchedule(String day, Document document) {
        for (int row = 1; row <= MAX_ROWS; row++) {
            if (isDayRow(day, row, document)) {
                return formatDaySchedule(day, row + 1, document);
            }
        }
        return "Not found";
    }

    private boolean isDayRow(String day, int row, Document document) {
        String selector = createRowSelector(row);
        return document.select(selector).text().contains(day);
    }

    private String formatDaySchedule(String day, int startRow, Document document) {
        StringBuilder schedule = new StringBuilder(day);
        int currentRow = startRow;

        while (hasScheduleRow(currentRow, document)) {
            schedule.append(extractLessonInfo(currentRow, document));
            currentRow++;
        }

        return cleanScheduleText(schedule.toString());
    }

    private boolean hasScheduleRow(int row, Document document) {
        String selector = createRowSelector(row);
        return !document.select(selector).text().isEmpty();
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
        return document.select(selector).text();
    }

    private String createRowSelector(int row) {
        return String.format("body > table:nth-child(5) > tbody > tr:nth-child(%d)", row);
    }

    private String normalizeScheduleText(String schedule) {
        return schedule.replace("Кабинет:", "\nКабинет:")
                .replace("дистанционно", "(дистант)");
    }

    private String cleanScheduleText(String schedule) {
        return schedule.replaceAll("\\s+$", "")
                .replaceAll("Московское шоссе, 120", "")
                .replaceAll(" Замена Свободное время на:", "");
    }

    private String createNoScheduleMessage(String groupId) {
        return String.format("Нет расписания на нужную дату \n %s%s", SCHEDULE_URL, groupId);
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
    }

    private void handleError(String method, Exception e) {
        System.err.printf("Error WebSiteClass (method %s): %s%n", method, e.getMessage());
    }
}