package com.sps.main;

import com.sps.bullhorn.BullhornAPI;
import com.sps.bullhorn.BullhornTask;
import org.joda.time.DateTime;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class NewHireProcessor {
    private BullhornAPI bullhorn;

    public NewHireProcessor() {
        bullhorn = new BullhornAPI();

        // Schedule task
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.start();

            JobDataMap jobData = new JobDataMap();
            jobData.put("bullhorn", bullhorn);
            JobDetail job = JobBuilder.newJob(BullhornTask.class)
                    .withIdentity("myJob", "group1")
                    .usingJobData(jobData)
                    .storeDurably()
                    .build();


            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("myTrigger", "group1")
                    .forJob(job)
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 45 7 ? * WED *"))
                    .build();
            Trigger trigger2 = TriggerBuilder.newTrigger()
                    .withIdentity("myTrigger2", "group1")
                    .forJob(job)
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 15 ? * FRI *"))
                    .build();


            scheduler.addJob(job, false);
            scheduler.scheduleJob(trigger);
            scheduler.scheduleJob(trigger2);
            testTrigger(scheduler, job);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void testTrigger(Scheduler scheduler, JobDetail job) throws SchedulerException {
        DateTime dt = new DateTime();
        int min = (dt.getMinuteOfDay() % 60) + 1;
        int hr = dt.getHourOfDay();
        String day = new SimpleDateFormat("E").format(Calendar.getInstance().getTime());
        dt.dayOfWeek();

        // String expr = "0 " + min + " " + hr + " ? * MON *";
        String expr = "0 " + min + " " + hr + " ? * " + day + " *";

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("testTrigger", "group1")
                .forJob(job)
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(expr))
                .build();

        scheduler.scheduleJob(trigger);
    }

    public static void main(String[] args) {
        try {
            int startingID = Integer.parseInt(args[0]);
            new NewHireProcessor();
        } catch (NumberFormatException e) {
            System.out.println("Argument must be an Integer.");
            System.exit(1);
        }
    }
}
