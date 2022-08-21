package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {
    public static void main(String[] args) {
        try {
            var path = "./src/main/resources/rabbit.properties";
            var scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            var job = newJob(Rabbit.class).build();
            var times = simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(getProperties(path)
                            .getProperty("rabbit.interval")))
                    .repeatForever();
            var trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Rabbit runs here ...");
        }
    }

    public static Properties getProperties(String path) {
        Properties rsl = new Properties();
        try (var reader = new BufferedReader(new FileReader(path))) {
            rsl.load(reader);
            reader.lines().forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rsl;
    }
}