package org.example.site;

import org.example.site.fetcher.DocumentFetcher;
import org.example.site.info.ScheduleDocumentInfo;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduleDocumentController {
    private final Map<String, ScheduleDocumentInfo> savedDocuments = new ConcurrentHashMap<>();

    public ScheduleDocumentInfo getScheduleDocumentInfo(String group) throws IOException {
        if (!savedDocuments.containsKey(group)) {
            DocumentFetcher documentFetcher = new DocumentFetcher(group);
            ScheduleDocumentInfo info = documentFetcher.getScheduleDocumentInfo();
            savedDocuments.put(group, info);
        } else if (savedDocuments.get(group).isExpired()) {
            DocumentFetcher documentFetcher = new DocumentFetcher(group);
            ScheduleDocumentInfo info = documentFetcher.getScheduleDocumentInfo();
            if (info != null) {
                savedDocuments.replace(group, info);
            }
        }
        return savedDocuments.get(group);
    }

    public ScheduleDocumentInfo getScheduleDocumentInfoWithoutExceptions(String group) {
        return savedDocuments.get(group);
    }
}
