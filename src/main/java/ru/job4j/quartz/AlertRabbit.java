package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {
    public static void main(String[] args) {
        try {
            var path = "rabbit.properties";
            var property = getProperties(path);
            Class.forName(property.getProperty("rabbit.driver"));
            try (var connection = DriverManager.getConnection(
                    property.getProperty("rabbit.url"),
                    property.getProperty("rabbit.username"),
                    property.getProperty("rabbit.password"))) {
                var scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                var data = new JobDataMap();
                data.put("connection", connection);
                var job = newJob(Rabbit.class)
                        .usingJobData(data)
                        .build();
                var times = simpleSchedule()
                        .withIntervalInSeconds(Integer.parseInt(property.getProperty("rabbit.interval")))
                        .repeatForever();
                var trigger = newTrigger()
                        .startNow()
                        .withSchedule(times)
                        .build();
                scheduler.scheduleJob(job, trigger);
                Thread.sleep(Integer.parseInt(property.getProperty("rabbit.sleep")));
                scheduler.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Rabbit implements Job {
        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            try (var connection = (Connection) context.getJobDetail().getJobDataMap().get("connection")) {
                try (var statement = connection.prepareStatement(
                        "INSERT INTO grabber(created_date) VALUES (now())")) {
                    statement.execute();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Properties getProperties(String path) {
        Properties rsl = new Properties();
        try (var in = AlertRabbit.class.getClassLoader().getResourceAsStream(path)) {
            rsl.load(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return rsl;
    }
}