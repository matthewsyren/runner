package com.matthewsyren.runner.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.RemoteViews;

import com.matthewsyren.runner.MainActivity;
import com.matthewsyren.runner.R;
import com.matthewsyren.runner.WeeklyGoalsActivity;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.NumberUtilities;
import com.matthewsyren.runner.utilities.UserAccountUtilities;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.matthewsyren.runner.utilities.WeeklyGoalsUtilities;

import java.util.ArrayList;

/**
 * Creates a Widget that displays a user's progress towards their weekly goals
 */
public class WeeklyGoalsWidget
        extends AppWidgetProvider {
    private ArrayList<Run> mRuns;
    private Target mTarget;
    private Context mContext;
    private boolean mIsUserSignedIn;

    void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        //Creates the RemoteViews object
        RemoteViews views;

        //Displays the appropriate Widget layout based on whether the user is signed in or not
        if(mIsUserSignedIn) {
            //Fetches the Widget layout that displays the user's progress towards their targets
            views = new RemoteViews(context.getPackageName(), R.layout.weekly_goals_widget);

            //Initialises the total variables
            double totalDistance = 0;
            int totalDuration = 0;

            //Calculates the totals
            for(Run run : mRuns){
                totalDistance += run.getDistanceTravelled();
                totalDuration += run.getRunDuration();
            }

            //Gets the user's preferred unit of distance
            String unit = UserAccountUtilities.getPreferredDistanceUnit(context);

            double averageSpeed;

            //Calculates the user's average speed in the appropriate unit
            if(unit.equals(context.getString(R.string.unit_kilometres_key))){
                //Calculates the user's average speed in km/h
                averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInKilometresPerHour(totalDistance, totalDuration);
                views.setTextViewText(R.id.tv_widget_distance_target_label, context.getString(R.string.distance_travelled_km));
                views.setTextViewText(R.id.tv_widget_average_speed_target_label, context.getString(R.string.average_speed_kmh));
            }
            else{
                //Calculates the user's average speed in mph
                averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInMilesPerHour(totalDistance, totalDuration);
                views.setTextViewText(R.id.tv_widget_distance_target_label, context.getString(R.string.distance_travelled_mi));
                views.setTextViewText(R.id.tv_widget_average_speed_target_label, context.getString(R.string.average_speed_mph));
            }

            //Converts the totalDistance to the appropriate unit and the duration to minutes
            totalDistance = RunInformationFormatUtilities.getDistance(totalDistance, context);

            //Displays the user's distance progress
            int distanceProgress = WeeklyGoalsUtilities.getDistanceProgress(
                    totalDistance,
                    RunInformationFormatUtilities.getDistance(mTarget.getDistanceTarget(), context));

            views.setProgressBar(R.id.pb_widget_distance_target, 100, distanceProgress, false);

            views.setTextViewText(
                    R.id.tv_widget_distance_target,
                    context.getString(
                            R.string.distance_target_progress,
                            totalDistance,
                            RunInformationFormatUtilities.getDistance(mTarget.getDistanceTarget(), context)));

            //Displays the user's duration progress
            int durationProgress = WeeklyGoalsUtilities.getDurationProgress(totalDuration, mTarget.getDurationTarget());
            views.setProgressBar(R.id.pb_widget_duration_target, 100, durationProgress, false);

            views.setTextViewText(
                    R.id.tv_widget_duration_target,
                    context.getString(
                            R.string.duration_target_progress,
                            RunInformationFormatUtilities.getDurationInMinutes(totalDuration),
                            RunInformationFormatUtilities.getDurationInMinutes(mTarget.getDurationTarget())));

            //Displays the user's duration progress
            int averageSpeedProgress = WeeklyGoalsUtilities.getAverageSpeedProgress(
                    averageSpeed,
                    NumberUtilities.roundOffToOneDecimalPlace(RunInformationFormatUtilities.getDistance(mTarget.getAverageSpeedTarget(), context)));

            views.setProgressBar(R.id.pb_widget_average_speed_target, 100, averageSpeedProgress, false);

            views.setTextViewText(
                    R.id.tv_widget_average_speed_target,
                    context.getString(
                            R.string.average_speed_target_progress,
                            averageSpeed,
                            RunInformationFormatUtilities.getDistance(mTarget.getAverageSpeedTarget(), context)));

            //Creates a PendingIntent that will open WeeklyGoalsActivity when the user clicks on the Widget heading
            Intent intent = new Intent(context, WeeklyGoalsActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.tv_widget_heading, pendingIntent);

            //Displays the Widget when the user is signed in
            views.setViewVisibility(R.id.ll_widget_weekly_goals, View.VISIBLE);
        }
        else{
            //Fetches the Widget layout that displays a signed out message
            views = new RemoteViews(context.getPackageName(), R.layout.weekly_goals_widget_signed_out);

            //Creates a PendingIntent that will open MainActivity when the user clicks on the Widget heading
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.tv_widget_heading, pendingIntent);
        }

        //Displays the updated values in the Widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        mContext = context;

        //Fetches the user's unique key
        String userKey = UserAccountUtilities.getUserKey(context);

        //Determines whether the user is signed in or not based on if their key has a value
        if(userKey != null){
            mIsUserSignedIn = true;

            //Requests the user's targets and runs, as the user is signed in
            new Target().requestTargetsAndRuns(context, userKey, new DataReceiver(new Handler()));
        }
        else{
            mIsUserSignedIn = false;

            //Will display a Widget that tells the user to sign in
            updateWidgets();
        }
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {

    }

    //Updates all Widgets
    private void updateWidgets(){
        //Gets the Widgets to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(mContext, WeeklyGoalsWidget.class));

        //Updates the Widgets with the new values
        for(int appWidgetId : appWidgetIds){
            updateAppWidget(mContext, appWidgetManager, appWidgetId);
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
                mRuns = resultData.getParcelableArrayList(FirebaseService.RUNS_EXTRA);

                if(mRuns != null){
                    mRuns = WeeklyGoalsUtilities.getRunsForThisWeek(mRuns);
                    mTarget = resultData.getParcelable(FirebaseService.TARGET_EXTRA);

                    //Updates all the Widgets
                    updateWidgets();
                }
            }
        }
    }
}