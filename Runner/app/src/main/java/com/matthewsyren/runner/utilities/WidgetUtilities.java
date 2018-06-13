package com.matthewsyren.runner.utilities;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.matthewsyren.runner.widget.WeeklyGoalsWidget;

public class WidgetUtilities {
    /**
     * Sends a broadcast that causes the Widgets to update their data
     */
    public static void updateWidgets(Context context){
        //Adapted from https://stackoverflow.com/questions/3455123/programmatically-update-widget-from-activity-service-receiver?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
        Intent intent = new Intent(context, WeeklyGoalsWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        //Gets the Widgets to update
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, WeeklyGoalsWidget.class));

        //Sends the broadcast to the Widgets
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }
}