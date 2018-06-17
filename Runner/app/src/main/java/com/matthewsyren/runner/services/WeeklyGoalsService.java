package com.matthewsyren.runner.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.matthewsyren.runner.R;
import com.matthewsyren.runner.WeeklyGoalsActivity;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.utilities.WeeklyGoalsUtilities;
import com.matthewsyren.runner.utilities.WidgetUtilities;

import java.util.ArrayList;

public class WeeklyGoalsService
        extends Service {
    public static final String USER_KEY_EXTRA = "user_key_extra";
    private String mUserKey;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Requests the user's targets and runs
        mUserKey = intent.getStringExtra(USER_KEY_EXTRA);
        new Target().requestTargetsAndRuns(getApplicationContext(), mUserKey, new DataReceiver(new Handler()));
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Displays a notification to the user about their weekly targets
    private static void displayNotification(Target target, Context context){
        //Creates a PendingIntent for the notification
        Intent intent = new Intent(context, WeeklyGoalsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        String message;

        //Sets the appropriate message for the notification
        if(target.getConsecutiveTargetsMet() > 1){
            message = context.getString(R.string.notification_met_targets, target.getConsecutiveTargetsMet());
        }
        else{
            message = context.getString(R.string.notification_met_target, target.getConsecutiveTargetsMet());
        }

        //Creates the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.notification_channel))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_met_target_title))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        //Displays the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(notificationManager != null){
            notificationManager.notify(1, builder.build());
        }
    }

    //Used to retrieve results from the FirebaseService
    private class DataReceiver
            extends ResultReceiver {

        //Constructor
        DataReceiver(Handler handler) {
            super(handler);
        }

        //Performs the appropriate action based on the result
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            if(resultCode == FirebaseService.ACTION_GET_TARGETS_AND_RUNS_RESULT_CODE){
                //Assigns the data to variables
                Target target = resultData.getParcelable(FirebaseService.TARGET_EXTRA);
                ArrayList<Run> runs = resultData.getParcelableArrayList(FirebaseService.RUNS_EXTRA);

                if(target != null && runs != null){
                    //Updates the user's dateOfLastMetTarget and consecutiveTargetsMet information
                    target = WeeklyGoalsUtilities.updateConsecutiveTargetsMet(runs, target);

                    if(target.getConsecutiveTargetsMet() > 0){
                        //Displays a notification telling the user that they met their target
                        displayNotification(target, getApplicationContext());
                    }

                    //Uploads the updated target information
                    target.updateTargets(getApplicationContext(), mUserKey, this);
                }
            }
            else if(resultCode == FirebaseService.ACTION_UPDATE_TARGETS_RESULT_CODE){
                //Updates the Widgets, schedules this Service to be run next Monday morning and stops this Service
                WidgetUtilities.updateWidgets(getApplicationContext());
                WeeklyGoalsUtilities.scheduleWeeklyGoalsService(getApplicationContext());
                stopSelf();
            }
        }
    }
}