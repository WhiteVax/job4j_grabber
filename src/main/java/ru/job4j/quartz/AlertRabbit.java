package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.*;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {
    public static void main(String[] args) {
        try {
            var path = "rabbit.properties";
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
        try (var in = AlertRabbit.class.getClassLoader().getResourceAsStream(path)) {
            rsl.load(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return rsl;
    }
}