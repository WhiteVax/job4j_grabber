package ru.job4j.parse;

import org.jsoup.Jsoup;
import ru.job4j.grabber.utils.DateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;

public class HabrCareerParse implements DateTimeParser {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    public static void main(String[] args) throws IOException {
        var habr = new HabrCareerParse();
        for (int i = 1; i <= 5; i++) {
            var connection = Jsoup.connect(String.format("%s%d", PAGE_LINK, i));
            var document = connection.get();

            var rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                var titleElement = row.select(".vacancy-card__title").first();
                var linkElement = titleElement.child(0);
                var vacancyName = titleElement.text();
                var link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                var date = habr.parse(row.select(".basic-date").attr("datetime"));
                var description = habr.retrieveDescription(link);
                System.out.printf("%s %s %s%n", vacancyName, date, link);
                System.out.println(description);
            });
        }
    }

    @Override
    public  LocalDateTime parse(String parse) {
        return  LocalDateTime.parse(parse.substring(0, 19));
    }

    private String retrieveDescription(String link) {
        var description = "";
        var connection = Jsoup.connect(link);
        try {
            var document = connection.get();
            description = document.select(".style-ugc").text();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return description;
    }
}

