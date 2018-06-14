package com.matthewsyren.runner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.matthewsyren.runner.utilities.WeeklyGoalsUtilities;

public class Receiver
        extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            /*
             * Schedules a Service to check if the user has met their weekly targets
             * Adapted from http://blog.teamtreehouse.com/scheduling-time-sensitive-tasks-in-android-with-alarmmanager
             */
            WeeklyGoalsUtilities.scheduleWeeklyGoalsService(context);
        }
    }
}