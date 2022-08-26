package ru.job4j.parse;

import org.jsoup.Jsoup;
import ru.job4j.grabber.utils.DateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;

public class HabrCareerParse implements DateTimeParser {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public static void main(String[] args) throws IOException {
        var habr = new HabrCareerParse();
        var connection = Jsoup.connect(PAGE_LINK);
        var document = connection.get();
        var rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            var titleElement = row.select(".vacancy-card__title").first();
            var linkElement = titleElement.child(0);
            var vacancyName = titleElement.text();
            var link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            var date = habr.parse(row.select(".basic-date").attr("datetime"));
            System.out.printf("%s %s %s%n", vacancyName, date, link);
        });
    }

    @Override
    public  LocalDateTime parse(String parse) {
        return  LocalDateTime.parse(parse.substring(0, 19));
    }
}

