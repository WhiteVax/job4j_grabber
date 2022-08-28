package ru.job4j.parse;

import org.jsoup.Jsoup;
import ru.job4j.grabber.Parse;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;
import ru.job4j.model.Post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public static void main(String[] args) throws IOException {
        var habrParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        habrParse.list(PAGE_LINK).forEach(System.out :: println);
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

    @Override
    public List<Post> list(String link) throws IOException {
        var dateParse = new HabrCareerDateTimeParser();
        List<Post> vacancies = new ArrayList<>();
        int pages = 5;
        for (int i = 1; i <= pages; i++) {
            var connection = Jsoup.connect(String.format("%s%d", PAGE_LINK, i));
            var document = connection.get();
            var rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                var titleElement = row.select(".vacancy-card__title").first();
                var linkElement = titleElement.child(0);
                var vacancyName = titleElement.text();
                var linkVacancy = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                var date = dateParse.parse(row.select(".basic-date").attr("datetime"));
                var description = retrieveDescription(linkVacancy);
                vacancies.add(new Post(vacancyName, linkVacancy, description, date));
            });
        }
        return vacancies;
    }
}

