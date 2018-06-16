package com.matthewsyren.runner.utilities;

import android.content.Context;

import com.matthewsyren.runner.R;

import java.util.Locale;

public class RunInformationFormatUtilities {
    //Time constants
    private static final int SECONDS_IN_ONE_MINUTE = 60;
    private static final int SECONDS_IN_ONE_HOUR = 3600;
    private static final int METRES_IN_ONE_KILOMETRE = 1000;
    private static final double METRES_IN_ONE_MILE = 1609.3440;
    private static final double YARDS_IN_ONE_METRE = 1.0936132983;
    private static final int YARDS_IN_ONE_MILE = 1760;

    /**
     * Returns a formatted run duration
     * @param runDuration The duration in seconds
     */
    public static String getFormattedRunDuration(int runDuration){
        //Formats the duration of the run in the appropriate format
        if(runDuration < SECONDS_IN_ONE_HOUR){
            int minutes = runDuration / SECONDS_IN_ONE_MINUTE;
            int seconds = runDuration % SECONDS_IN_ONE_MINUTE;
            return String.format(Locale.getDefault(),"%02d", minutes) + ":" + String.format(Locale.getDefault(),"%02d", seconds);
        }
        else{
            int hours = runDuration / SECONDS_IN_ONE_HOUR;
            int minutes = (runDuration - (hours * SECONDS_IN_ONE_HOUR)) / SECONDS_IN_ONE_MINUTE;
            int seconds = (runDuration - (hours * SECONDS_IN_ONE_HOUR)) % SECONDS_IN_ONE_MINUTE;
            return hours + ":" + String.format(Locale.getDefault(), "%02d", minutes) + ":" + String.format(Locale.getDefault(),"%02d", seconds);
        }
    }

    /**
     * Returns a formatted run distance
     * @param distanceTravelled The distance travelled in metres
     */
    public static String getFormattedRunDistance(double distanceTravelled, Context context){
        String unit = UserAccountUtilities.getPreferredDistanceUnit(context);

        if(unit.equals(context.getString(R.string.unit_kilometres_key))){
            //Formats the distance travelled with the correct units
            if(distanceTravelled < METRES_IN_ONE_KILOMETRE){
                return context.getString(R.string.metres, String.valueOf(Math.round(distanceTravelled)));
            }
            else{
                double kilometresTravelled = distanceTravelled / METRES_IN_ONE_KILOMETRE;
                return context.getString(R.string.kilometres, kilometresTravelled );
            }
        }
        else{
            //Formats the distance travelled with the correct units
            if(distanceTravelled < METRES_IN_ONE_MILE){
                double yardsTravelled = distanceTravelled * YARDS_IN_ONE_METRE;
                return context.getString(R.string.yards, String.valueOf(Math.round(yardsTravelled)));
            }
            else{
                distanceTravelled *= YARDS_IN_ONE_METRE;
                double milesTravelled = (distanceTravelled / YARDS_IN_ONE_MILE);
                return context.getString(R.string.miles, milesTravelled);
            }
        }
    }

    /**
     * Returns the user's average speed in kilometres per hour
     * @param totalDistance The distance travelled in metres
     * @param totalDuration The duration in seconds
     */
    public static int getUsersAverageSpeedInKilometresPerHour(double totalDistance, int totalDuration){
        return (int) Math.round((totalDistance / totalDuration * (SECONDS_IN_ONE_HOUR / METRES_IN_ONE_KILOMETRE)));
    }


    /**
     * Returns the user's average speed in miles per hour
     * @param totalDistance The distance travelled in metres
     * @param totalDuration The duration in seconds
     */
    public static int getUsersAverageSpeedInMilesPerHour(double totalDistance, int totalDuration){
        double yardsTravelled = totalDistance * YARDS_IN_ONE_METRE;
        return (int) Math.round(((yardsTravelled / totalDuration) * (SECONDS_IN_ONE_HOUR / YARDS_IN_ONE_MILE)));
    }

    /**
     * Returns a formatted run average speed
     * @param distanceTravelled The distance travelled in metres
     * @param runDuration The duration in seconds
     */
    public static String getFormattedRunAverageSpeed(double distanceTravelled, int runDuration, Context context){
        String unit = UserAccountUtilities.getPreferredDistanceUnit(context);

        if(unit.equals(context.getString(R.string.unit_kilometres_key))){
            //Calculates the speed in km/h
            return context.getString(
                    R.string.kilometres_per_hour,
                    String.valueOf(getUsersAverageSpeedInKilometresPerHour(distanceTravelled, runDuration)));
        }
        else{
            //Calculates the speed in mph
            return context.getString(
                    R.string.miles_per_hour,
                    String.valueOf(getUsersAverageSpeedInMilesPerHour(distanceTravelled, runDuration)));
        }
    }

    /**
     * Returns the distance in the appropriate unit
     * @param distanceTravelled The distance travelled in metres
     */
    public static double getDistance(double distanceTravelled, Context context){
        String unit = UserAccountUtilities.getPreferredDistanceUnit(context);

        if(unit.equals(context.getString(R.string.unit_kilometres_key))){
            //Calculates the speed in km/h
            return distanceTravelled /= METRES_IN_ONE_KILOMETRE;
        }
        else{
            //Calculates the speed in mph
            distanceTravelled *= YARDS_IN_ONE_METRE;
            return distanceTravelled / YARDS_IN_ONE_MILE;
        }
    }

    /**
     * Returns the distance in metres
     * @param totalDistance The distance travelled in metres
     */
    public static int getDistanceInMetres(double totalDistance, Context context){
        String unit = UserAccountUtilities.getPreferredDistanceUnit(context);

        if(unit.equals(context.getString(R.string.unit_kilometres_key))){
            //Calculates the speed in km/h
            return (int) Math.round(totalDistance * METRES_IN_ONE_KILOMETRE);
        }
        else{
            //Calculates the speed in mph
            totalDistance *= YARDS_IN_ONE_MILE;
            totalDistance /= YARDS_IN_ONE_METRE;
            return (int) Math.round(totalDistance);
        }
    }
}