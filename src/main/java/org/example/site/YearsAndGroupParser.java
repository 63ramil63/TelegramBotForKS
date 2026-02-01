package org.example.site;

import org.example.site.fetcher.DocumentFetcher;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YearsAndGroupParser {
    private final int MAX_YEARS = 10;

    private final int YEAR_ROW_INDEX = 6;
    private final int GROUP_ROW_INDEX = 7;

    private final String YEAR_PREFIX = "Year";
    private final String GROUP_PREFIX = "Group";

    private Document document;


    public YearsAndGroupParser() {
        try {
            DocumentFetcher documentFetcher = new DocumentFetcher();
            document = documentFetcher.getBaseDocument();
        } catch (IOException e) {
            System.out.printf("Не удалось получить группы и годы %n%s%n", e);
        }
    }

    public List<String> getYears() {
        System.out.println("Document isn't null ? " + document != null);
        if (document != null) {
            return extractYears(document);
        } else {
            handleError("getYears", new RuntimeException("Base document is null"));
            return new ArrayList<>();
        }
    }

    public List<String> getGroups(int yearColumn) {
        if (document != null) {
            return extractGroups(document, yearColumn);
        } else {
            handleError("getGroups", new RuntimeException("Base document is null"));
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

    private List<String> extractGroups(Document document, int yearColumn) {
        List<String> groups = new ArrayList<>();

        // Простой селектор: находим все ссылки с группами в нужном столбце
        String selector = String.format(
                "body > table:nth-child(5) > tbody > tr:nth-child(%d) > td:nth-child(%d) a",
                GROUP_ROW_INDEX, yearColumn
        );


        Elements groupLinks = document.select(selector);

        for (org.jsoup.nodes.Element link : groupLinks) {
            String href = link.attr("href");
            String groupName = link.text().trim();
            String groupId = extractGroupIdFromHref(href);

            if (!groupName.isEmpty() && !groupId.isEmpty()) {
                // ВЕРНУТЬ СТАРЫЙ ФОРМАТ: "названиеGroupID"
                groups.add(groupName + GROUP_PREFIX + groupId);
                System.out.println("DEBUG: Added group: " + groupName + GROUP_PREFIX + groupId);
            }
        }

        System.out.println("DEBUG: Total groups: " + groups);
        return groups;
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

    private void handleError(String method, Exception e) {
        System.err.printf("Error WebSiteClass (method %s): %s%n", method, e.getMessage());
    }
}