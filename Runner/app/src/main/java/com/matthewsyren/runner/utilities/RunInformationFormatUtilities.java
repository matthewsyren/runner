package com.matthewsyren.runner.utilities;

import android.content.Context;

import com.matthewsyren.runner.R;

import java.util.Locale;

public class RunInformationFormatUtilities {
    //Time constants
    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 3600;

    /**
     * Returns a formatted run duration
     * @param runDuration The time taken in seconds
     */
    public static String getFormattedRunDuration(int runDuration){
        //Formats the duration of the run in the appropriate format
        if(runDuration < ONE_HOUR){
            int minutes = runDuration / ONE_MINUTE;
            int seconds = runDuration % ONE_MINUTE;
            return String.format(Locale.getDefault(),"%02d", minutes) + ":" + String.format(Locale.getDefault(),"%02d", seconds);
        }
        else{
            int hours = runDuration / ONE_HOUR;
            int minutes = (runDuration - (hours * ONE_HOUR)) / ONE_MINUTE;
            int seconds = (runDuration - (hours * ONE_HOUR)) % ONE_MINUTE;
            return hours + ":" + String.format(Locale.getDefault(), "%02d", minutes) + ":" + String.format(Locale.getDefault(),"%02d", seconds);
        }
    }

    /**
     * Returns a formatted run distance
     * @param distanceTravelled The distance travelled in metres
     */
    public static String getFormattedRunDistance(double distanceTravelled, Context context){
        //Formats the distance travelled with the correct units
        if(distanceTravelled < 1000){
            return context.getString(R.string.metres, String.valueOf(Math.round(distanceTravelled)));
        }
        else{
            int kilometresTravelled = (int) distanceTravelled / 1000;
            int metresTravelled = (int) distanceTravelled % 1000;
            return context.getString(R.string.kilometres, kilometresTravelled + "." + metresTravelled);
        }
    }

    /**
     * Returns the user's average speed in kilometres per hour
     * @param totalDistance The distance travelled in metres
     * @param totalDuration The time taken in seconds
     */
    public static int getUsersAverageSpeedInKilometresPerHour(double totalDistance, int totalDuration){
        return (int) Math.round((totalDistance / totalDuration * 3.6));
    }

    /**
     * Returns a formatted run average speed
     * @param distanceTravelled The distance travelled in metres
     * @param runDuration The time taken in seconds
     */
    public static String getFormattedRunAverageSpeed(double distanceTravelled, int runDuration, Context context){
        //Calculates the speed in km/h
        return context.getString(
                R.string.kilometres_per_hour,
                String.valueOf(getUsersAverageSpeedInKilometresPerHour(distanceTravelled, runDuration)));
    }
}