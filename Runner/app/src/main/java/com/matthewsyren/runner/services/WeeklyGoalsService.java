package com.matthewsyren.runner.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;

import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.utilities.DateUtilities;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.matthewsyren.runner.utilities.WeeklyGoalsUtilities;
import com.matthewsyren.runner.utilities.WidgetUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

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

                //Updates the user's dateOfLastMetTarget and consecutiveTargetsMet information
                if(target != null && runs != null){
                    //Sets up total variables
                    double totalDistance = 0;
                    int totalDuration = 0;

                    //Calculates totals
                    for(Run run : runs){
                        totalDistance += run.getDistanceTravelled();
                        totalDuration += run.getRunDuration();
                    }

                    double averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInKilometresPerHour(totalDistance, totalDuration);

                    //Converts the totalDistance and distanceTarget to kilometres, duration to minutes and averageSpeedTarget to kilometres per hour
                    totalDistance /= 1000.0;
                    totalDuration /= 60;
                    double averageSpeedTarget = target.getAverageSpeedTarget();
                    averageSpeedTarget /= 1000.0;
                    double distanceTarget = target.getDistanceTarget();
                    distanceTarget /= 1000.0;

                    //Updates the target's last met date and consecutive targets met based on whether the user met their target
                    if(totalDistance >= distanceTarget && totalDuration >= target.getDurationTarget() && averageSpeed >= averageSpeedTarget){
                        //Increments the consecutiveTargetsMet field if the user met their target last week as well, otherwise sets consecutiveTargetsMet to 1
                        if(Arrays.asList(DateUtilities.getDatesForPreviousWeek()).contains(target.getDateOfLastMetTarget())){
                            target.setConsecutiveTargetsMet(target.getConsecutiveTargetsMet() + 1);
                        }
                        else{
                            target.setConsecutiveTargetsMet(1);
                        }

                        //Sets the dateOfLastMetTarget to yesterday
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DATE, -1);
                        String currentDate = DateUtilities.formatDate(calendar.getTime());
                        target.setDateOfLastMetTarget(currentDate);
                    }
                    else{
                        //User didn't meet target, sets the consecutiveTargetsMet to 0
                        target.setConsecutiveTargetsMet(0);
                    }

                    //Uploads the new information
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