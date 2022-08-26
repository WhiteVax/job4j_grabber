package ru.job4j.parse;

import org.jsoup.Jsoup;

import java.io.IOException;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public static void main(String[] args) throws IOException {
        var connection = Jsoup.connect(PAGE_LINK);
        var document = connection.get();
        var rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            var titleElement = row.select(".vacancy-card__title").first();
            var linkElement = titleElement.child(0);
            var vacancyName = titleElement.text();
            var link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            var date = row.select(".basic-date").attr("datetime");
            System.out.printf("%s %s %s%n", vacancyName, date, link);
        });
    }
}

