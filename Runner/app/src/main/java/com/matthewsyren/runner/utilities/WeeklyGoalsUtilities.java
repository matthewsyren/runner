package com.matthewsyren.runner.utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.matthewsyren.runner.services.WeeklyGoalsService;

import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

public class WeeklyGoalsUtilities {
    /**
     * Returns the distance progress as a percentage
     * @param totalDistance The total distance travelled in metres
     * @param targetDistance The target distance in metres
     */
    public static int getDistanceProgress(double totalDistance, int targetDistance){
        return (int)(totalDistance / targetDistance * 100);
    }

    /**
     * Returns the duration progress as a percentage
     * @param totalDuration The total duration in seconds
     * @param targetDuration The target duration in seconds
     */
    public static int getDurationProgress(int totalDuration, int targetDuration){
        return (int)((double)totalDuration / targetDuration * 100);
    }

    /**
     * Returns the average speed progress as a percentage
     * @param averageSpeed The average speed in kilometres per hour
     * @param targetAverageSpeed The target average speed in kilometres per hour
     */
    public static int getAverageSpeedProgress(int averageSpeed, int targetAverageSpeed){
        return (int)((double)averageSpeed / targetAverageSpeed * 100);
    }

    /**
     * Schedules a Service to run every Monday morning at midnight to check if the user made their weekly target for the week
     */
    public static void scheduleWeeklyGoalsService(Context context){
        //Sets up a PendingIntent to start the Service
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        String userKey = PreferenceUtilities.getUserKey(context);
        Intent serviceIntent = new Intent(context, WeeklyGoalsService.class);
        serviceIntent.putExtra(WeeklyGoalsService.USER_KEY_EXTRA, userKey);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, serviceIntent, 0);

        //Cancels any previous scheduled PendingIntents
        alarmManager.cancel(pendingIntent);

        //Sets the execution date to the start of Monday
        Calendar calendar = Calendar.getInstance();

        do{
            calendar.add(Calendar.DAY_OF_WEEK, 1);
        } while(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY);

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        //Schedules the Service to run on Monday morning at midnight
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}