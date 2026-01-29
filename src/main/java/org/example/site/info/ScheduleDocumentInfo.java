package org.example.site.info;

import org.jsoup.nodes.Document;

import java.util.concurrent.TimeUnit;

public class ScheduleDocumentInfo {
    private final Document thisWeekDocument;
    private final Document nextWeekDocument;
    private final long timestamp;
    private String weekParam;

    public ScheduleDocumentInfo(Document thisWeekDocument, Document nextWeekDocument, String weekParam) {
        this.thisWeekDocument = thisWeekDocument;
        this.nextWeekDocument = nextWeekDocument;
        this.weekParam = weekParam;
        this.timestamp = System.currentTimeMillis();
    }

    public Document getThisWeekDocument() {
        return thisWeekDocument;
    }

    public Document getNextWeekDocument() {
        return nextWeekDocument;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMinutes(5);
    }

    public String getWeekParam() {
        return weekParam;
    }
}
