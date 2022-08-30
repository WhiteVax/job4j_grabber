package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;
import ru.job4j.model.Post;
import ru.job4j.parse.HabrCareerParse;

import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private final Properties properties = new Properties();
    private static final String LINK = "https://career.habr.com/vacancies/java_developer?page=";
    private static final Logger LOG = LoggerFactory.getLogger(Grab.class.getName());

    public Store store() {
        return new PsqlStore(properties);
    }

    public Scheduler scheduler() {
        Scheduler scheduler = null;
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            LOG.error(e.getMessage());
        }
        return scheduler;
    }

    public void config() {
        try (var in = Grabber.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            properties.load(in);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) {
        var data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        var job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        var times = simpleSchedule()
                .withIntervalInSeconds(Integer.parseInt(properties.getProperty("rabbit.interval")))
                .repeatForever();
        var trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            LOG.error(e.getMessage());
        }
    }

    public static class GrabJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            var map = context.getJobDetail().getJobDataMap();
            var store = (Store) map.get("store");
            var parse = (Parse) map.get("parse");
            List<Post> posts = new ArrayList<>();
            try {
                posts = parse.list(LINK);
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Exception in log.", e);
            }
            for (var post : posts) {
                store.save(post);
            }
        }
    }

    public void web(Store store) {
        new Thread(() -> {
            try (var server = new ServerSocket(Integer.parseInt(properties.getProperty("rabbit.port")))) {
                while (!server.isClosed()) {
                    var socket = server.accept();
                    try (var out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        for (var post : store.getAll()) {
                            out.write(post.toString().getBytes(Charset.forName("Windows-1251")));
                            out.write(System.lineSeparator().getBytes());
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                        LOG.error("Exception in log.", io);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Exception in log.", e);
            }
        }).start();
    }

    public static void main(String[] args) {
        var grab = new Grabber();
        grab.config();
        Scheduler scheduler = grab.scheduler();
        var store = grab.store();
        grab.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
        grab.web(store);
    }
}
