package com.matthewsyren.runner.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.RemoteViews;

import com.matthewsyren.runner.R;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.PreferenceUtilities;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.matthewsyren.runner.utilities.WeeklyGoalsUtilities;

import java.util.ArrayList;

/**
 * Creates a Widget that displays a user's progress towards their weekly goals
 */
public class WeeklyGoalsWidget
        extends AppWidgetProvider {
    ArrayList<Run> mRuns;
    Target mTarget;
    Context mContext;

    void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        //Creates the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weekly_goals_widget);

        //Initialises the total variables
        double totalDistance = 0;
        int totalDuration = 0;

        //Calculates the totals
        for(Run run : mRuns){
            totalDistance += run.getDistanceTravelled();
            totalDuration += run.getRunDuration();
        }

        //Calculates the user's average speed
        int averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInKilometresPerHour(totalDistance, totalDuration);

        //Converts distance to kilometres
        totalDistance /= 1000;

        //Displays the user's distance progress
        int distanceProgress = WeeklyGoalsUtilities.getDistanceProgress(totalDistance, mTarget.getDistanceTarget());
        views.setProgressBar(R.id.pb_widget_distance_target, 100, distanceProgress, false);
        views.setTextViewText(R.id.tv_widget_distance_target, context.getString(R.string.distance_target_progress, totalDistance, mTarget.getDistanceTarget()));

        //Displays the user's duration progress
        int durationProgress = WeeklyGoalsUtilities.getDurationProgress(totalDuration, mTarget.getDurationTarget());
        views.setProgressBar(R.id.pb_widget_duration_target, 100, durationProgress, false);
        views.setTextViewText(R.id.tv_widget_duration_target, context.getString(R.string.duration_target_progress, totalDuration, mTarget.getDurationTarget()));

        //Displays the user's duration progress
        int averageSpeedProgress = WeeklyGoalsUtilities.getAverageSpeedProgress(averageSpeed, mTarget.getAverageSpeedTarget());
        views.setProgressBar(R.id.pb_widget_average_speed_target, 100, averageSpeedProgress, false);
        views.setTextViewText(R.id.tv_widget_average_speed_target, context.getString(R.string.average_speed_target_progress, averageSpeed, mTarget.getAverageSpeedTarget()));

        //Displays the updated values in the Widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        mContext = context;

        //Requests the user's targets and runs
        new Target().requestTargetsAndRuns(context, PreferenceUtilities.getUserKey(context), new DataReceiver(new Handler()));
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {

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
                mTarget = resultData.getParcelable(FirebaseService.TARGET_EXTRA);

                //Gets the Widgets to update
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(mContext, WeeklyGoalsWidget.class));

                //Updates the Widgets with the new values
                for(int appWidgetId : appWidgetIds){
                    updateAppWidget(mContext, appWidgetManager, appWidgetId);
                }
            }
        }
    }
}