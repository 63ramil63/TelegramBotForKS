package org.example.site.fetcher;

import org.example.site.info.ScheduleDocumentInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class DocumentFetcher {
    private final String BASE_URL = "https://lk.ks.psuti.ru/?mn=2";
    private final String SCHEDULE_URL = BASE_URL + "&obj=";
    private static final String USER_AGENT = "Chrome";
    private static final int TIMEOUT_MS = 10_000;

    private static final int NEXT_WEEK_ROW_INDEX = 4;
    private static final int NEXT_WEEK_COL_INDEX = 8;

    private Document documentThisWeek;
    private Document documentNextWeek;
    private Document baseDocument;
    private String weekParam;

    public DocumentFetcher(String groupId) throws IOException {
        documentThisWeek = fetchDocumentsWithSchedule(groupId);
        documentNextWeek = fetchDocumentsWithScheduleNextWeek(groupId);
    }

    public DocumentFetcher() throws IOException {
        baseDocument = fetchDocumentWithYearsAndGroups();
    }

    public ScheduleDocumentInfo getScheduleDocumentInfo() {
        return new ScheduleDocumentInfo(documentThisWeek, documentNextWeek, weekParam);
    }

    public Document getBaseDocument() {
        return baseDocument;
    }

    private Document fetchDocumentsWithSchedule(String groupId) throws IOException {
        String formattedUrl = SCHEDULE_URL + groupId;
        Document document = Jsoup.connect(formattedUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
        weekParam = extractNextWeekParameter(document);
        return document;
    }

    private Document fetchDocumentsWithScheduleNextWeek(String groupId) throws IOException {
        String formattedUrl = SCHEDULE_URL + groupId + "&" + weekParam;
        return Jsoup.connect(formattedUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
    }

    private Document fetchDocumentWithYearsAndGroups() throws IOException {
        return Jsoup.connect(BASE_URL).get();
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
}
