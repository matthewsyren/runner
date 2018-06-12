package com.matthewsyren.runner.utilities;

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
}