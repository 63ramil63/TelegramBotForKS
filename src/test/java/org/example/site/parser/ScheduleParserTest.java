package org.example.site.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

public class ScheduleParserTest {
    @Test
    void parseAllSchedules() {
        ScheduleParser parser = new ScheduleParser();
        File file = new File("path");
        try {
            Document document = Jsoup.parse(file);
            Map<LocalDate, String> schedules = parser.parseAllSchedules(document);
            for (Map.Entry<LocalDate, String> e : schedules.entrySet()) {
                System.out.println(e.getKey() + " " + e.getValue());
                System.out.println("-------------------------");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
