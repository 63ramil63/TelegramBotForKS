package org.example.site;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebSite {
    public final String scheduleUrl = "https://lk.ks.psuti.ru/?mn=2&obj=";
    public final String yearsAndGroupsUrl = "https://lk.ks.psuti.ru/?mn=2";

    public List<String> getYears() {
        try {
            Document document = Jsoup.connect(yearsAndGroupsUrl).userAgent("Chrome").timeout(10_000).get();
            List<String> years = new ArrayList<>();
            int i = 1;
            while (i != 11) {
                String year = document.select("body > table:nth-child(5) > tbody > tr:nth-child(6) > td:nth-child(" + i + ")").text();
                if (!year.isEmpty()) {
                    years.add(year + "Year" + i);
                }
                i++;
            }
            return years;
        } catch (IOException e) {
            System.err.println("Error: WebSiteClass (method getYear()) " + e);
        }
        return null;
    }

    private void getGroupsElement(Elements elements, List<String> groups, int i) {
        Elements element = elements.select("tr:nth-child(" + i + ")");
        // Получаем строку в столбце
        Elements groupId = element.select("td > a");
        // Получаем <a> c атрибутом href
        String attribute = groupId.attr("href");
        // Удаляем ненужное и оставляем только obj
        int index = attribute.indexOf("obj");
        attribute = attribute.substring(index);
        attribute = attribute.replace("obj", "");
        groups.add(element.text() + "Group" + attribute);
    }

    public List<String> getGroups(int num) {
        try {
            Document document = Jsoup.connect(yearsAndGroupsUrl).userAgent("Chrome").timeout(10_000).get();
            List<String> groups = new ArrayList<>();
            Elements elementsSize = document.select("body > table:nth-child(5) > tbody > tr:nth-child(7) > td:nth-child(" + num + ") > table > tbody > tr:nth-child(1) > td > table > tbody > tr");
            for (int i = 1; i < elementsSize.size() + 1; i++) {
                getGroupsElement(elementsSize, groups, i);
            }
            return groups;
        } catch (IOException e) {
            System.err.printf("Error WebSiteClass (method getGroups(num : %d))%n%s%n", num, e);
        }
        return null;
    }

    private String getNextWeek(Document document) {
        // Получаем неделю из страницы
        Elements element = document.select("body > table:nth-child(4) > tbody > tr:nth-child(4) > td > table > tbody > tr > td:nth-child(8) > a");
        String week = element.attr("href");
        int index = week.indexOf("wk");
        // Удаление ненужного 'wk'
        week = week.substring(index);
        return week;
    }

    private String getNormalizedSchedule(Elements schedule) {
        return schedule.text().replace("Кабинет:", "\nКабинет:").replace("дистанционно", "(дистант)");
    }

    private String getScheduleInfo(int num, Document document) {
        Elements number = document.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(1)");
        Elements time = document.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(2)");
        Elements rawSchedule = document.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(4)");
        String normalizedSchedule = getNormalizedSchedule(rawSchedule);
        return "\n" + number.text() + ") " + time.text() + "\n" + normalizedSchedule;
    }

    private String parseSchedule(int num, Document document) {
        // пропуск ненужного поля
        num++;
        Elements currentElement = document.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
        StringBuilder schedule = new StringBuilder();

        //проверка на последний элемент расписания на день, который всегда пустой
        while (!currentElement.text().isEmpty()) {

            schedule.append("\n").append(getScheduleInfo(num, document));
            num++;
            currentElement = document.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");

        }
        //переводим StringBuilder в String
        String schedules = schedule.toString();
        //удаление всех пробелов в конце строки, убираем лишний '(', убираем наименование места
        schedules = schedules.replaceAll("\\s+$", "");

        schedules = schedules.replaceAll("Московское шоссе, 120", "");
        schedules = schedules.replaceAll(" Замена Свободное время на:", "");
        return schedules;
    }

    private boolean isEquals(String day, int num, Document document) {
        // Проверка на совпадение с заданной датой
        Elements element = document.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
        return element.text().contains(day);
    }

    private String findCurrentDay(String day, Document document) {
        int num = 1;
        // Перебор эл страницы на поиск нужного дня
        while (num < 60) {
            // Проверка даты
            if (isEquals(day, num, document)) {
                num++;
                return day + parseSchedule(num, document);
            }
            num++;
        }
        return "Not found";
    }

    public String getSchedule(String day, String groupId) {
        try {
            Document document = Jsoup.connect(scheduleUrl + groupId).userAgent("Chrome").timeout(10_000).get();

            // Получение расписание на эту неделю
            String schedule = findCurrentDay(day, document);
            if (!schedule.equals("Not found")) {
                return schedule;
            }

            // Получение след недели
            String week = getNextWeek(document);
            document = Jsoup.connect(scheduleUrl + groupId + "&" + week).get();

            // Расписание на след неделю
            schedule = findCurrentDay(day, document);
            if (!schedule.equals("Not found")) {
                return schedule;
            }
            return "Нет расписания на нужную дату \n https://lk.ks.psuti.ru/?mn=2&obj=" + groupId;
        } catch (IOException e) {
            System.err.printf("Error WebSiteClass (method getSchedule(day : %s, groupId : %s))%n%s%n", day, groupId, e);
        }
        return "Not found";
    }
}
