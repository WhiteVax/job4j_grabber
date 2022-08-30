package ru.job4j.grabber;

import ru.job4j.model.Post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private Connection connection;

    private static final Logger LOG = LoggerFactory.getLogger(PsqlStore.class.getName());

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("rabbit.driver"));
            connection = DriverManager.getConnection(
                    cfg.getProperty("rabbit.url"),
                    cfg.getProperty("rabbit.username"),
                    cfg.getProperty("rabbit.password"));
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void save(Post post) {
        try (var statement = connection.prepareStatement(
                "INSERT INTO post(name, link, description, created) VALUES (?, ?, ?, ?) ON CONFLICT(link) DO NOTHING")) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getLink());
            statement.setString(3, post.getDescription());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
        } catch (SQLException e) {
            LOG.error("Exception in log.", e);
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (var statement = connection.prepareStatement(
                "SELECT * FROM post")) {
            var set = statement.executeQuery();
            while (set.next()) {
                posts.add(getPost(set));
            }
        } catch (SQLException e) {
            LOG.error("Exception in log.", e);
        }
        return posts;
    }

    public Post getPost(ResultSet set) throws SQLException {
        return new Post(set.getInt("id"),
                set.getString("name"),
                set.getString("link"),
                set.getString("description"),
                set.getTimestamp("created").toLocalDateTime());
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (var statement = connection.prepareStatement(
                "SELECT * FROM post WHERE id = ?")) {
            statement.setInt(1, id);
            var set = statement.executeQuery();
            if (set.next()) {
                post = getPost(set);
            }
        } catch (SQLException e) {
            LOG.error("Exception in log.", e);
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        var first = new Post("Middle Java разработчик", "https://career.habr.com/vacancies/1000.1.",
                "Уверенное владение Java 8+, Spring Framework 5, Spring Boot 2", LocalDateTime.now());
        var second = new Post("Middle Java разработчик", "https://career.habr.com/vacancies/1000.23.",
                "Уверенное владение Java 8+, Spring Framework 5, Spring Boot 2", LocalDateTime.now());
        try (var in = PsqlStore.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            var properties = new Properties();
            properties.load(in);
            try (var store = new PsqlStore(properties)) {
                store.save(first);
                store.save(second);
                store.getAll().forEach(System.out::println);
                System.out.println(store.findById(1).toString());
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }
}
