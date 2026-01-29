package org.example.site;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebSite {
    private static final String BASE_URL = "https://lk.ks.psuti.ru/?mn=2";
    private static final String USER_AGENT = "Chrome";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_YEARS = 10;

    private static final int YEAR_ROW_INDEX = 6;
    private static final int GROUP_ROW_INDEX = 7;

    private static final String YEAR_PREFIX = "Year";
    private static final String GROUP_PREFIX = "Group";

    public List<String> getYears() {
        try {
            Document document = fetchDocument(BASE_URL);
            return extractYears(document);
        } catch (IOException e) {
            handleError("getYears", e);
            return new ArrayList<>();
        }
    }

    public List<String> getGroups(int yearColumn) {
        try {
            Document document = fetchDocument(BASE_URL);
            return extractGroups(document, yearColumn);
        } catch (IOException e) {
            handleError("getGroups", e);
            return new ArrayList<>();
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
        String selector = String.format(
                "body > table:nth-child(5) > tbody > tr:nth-child(%d) > td:nth-child(%d)",
                YEAR_ROW_INDEX, column
        );
        return document.select(selector).text().trim();
    }

    public String getSchedule(String day, String groupId) {
        return "";
    }

    private List<String> extractGroups(Document document, int yearColumn) {
        List<String> groups = new ArrayList<>();
        Elements groupElements = getGroupElements(document, yearColumn);

        for (int row = 1; row <= groupElements.size(); row++) {
            String groupInfo = extractGroupInfo(groupElements, row);
            if (!groupInfo.isEmpty()) {
                groups.add(groupInfo);
            }
        }
        return groups;
    }

    private Elements getGroupElements(Document document, int yearColumn) {
        String selector = String.format(
                "body > table:nth-child(5) > tbody > tr:nth-child(%d) > td:nth-child(%d) > table > tbody > tr",
                GROUP_ROW_INDEX, yearColumn
        );
        return document.select(selector);
    }

    private String extractGroupInfo(Elements groupElements, int rowIndex) {
        Elements row = groupElements.select("tr:nth-child(" + rowIndex + ")");
        Elements groupLink = row.select("td > a");
        String href = groupLink.attr("href");
        String groupId = extractGroupIdFromHref(href);

        String groupName = row.text().trim();
        if (!groupName.isEmpty() && !groupId.isEmpty()) {
            return groupName + GROUP_PREFIX + groupId;
        }
        return "";
    }

    private String extractGroupIdFromHref(String href) {
        int startIndex = href.indexOf("obj=");
        if (startIndex != -1) {
            String substring = href.substring(startIndex + 4);
            int endIndex = substring.indexOf("&");
            if (endIndex != -1) {
                return substring.substring(0, endIndex);
            }
            return substring;
        }
        return "";
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