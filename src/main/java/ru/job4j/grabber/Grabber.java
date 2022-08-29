package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;
import ru.job4j.model.Post;
import ru.job4j.parse.HabrCareerParse;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private final Properties properties = new Properties();

    public Store store() throws SQLException {
        return new PsqlStore(properties);
    }

    public Scheduler scheduler() throws SchedulerException {
        var scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    public void config() throws IOException {
        try (var in = Grabber.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            properties.load(in);
        }
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        var data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        var job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(Integer.parseInt(properties.getProperty("rabbit.interval")))
                .repeatForever();
        Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            var map = context.getJobDetail().getJobDataMap();
            var store = (Store) map.get("store");
            var parse = (Parse) map.get("parse");
            List<Post> posts = new ArrayList<>();
            try {
                posts = parse.list("https://career.habr.com/vacancies/java_developer?page=");
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (var post : posts) {
                store.save(post);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var grab = new Grabber();
        grab.config();
        var scheduler = grab.scheduler();
        var store = grab.store();
        grab.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
    }
}
