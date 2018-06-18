package com.matthewsyren.runner.utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.services.WeeklyGoalsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import static android.content.Context.ALARM_SERVICE;

public class WeeklyGoalsUtilities {
    /**
     * Returns the distance progress as a percentage
     * @param totalDistance The total distance travelled in metres
     * @param targetDistance The target distance in metres
     */
    public static int getDistanceProgress(double totalDistance, double targetDistance){
        return (int)(NumberUtilities.roundOffToThreeDecimalPlaces(totalDistance) / NumberUtilities.roundOffToThreeDecimalPlaces(targetDistance) * 100);
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
    public static int getAverageSpeedProgress(double averageSpeed, double targetAverageSpeed){
        return (int)(NumberUtilities.roundOffToOneDecimalPlace(averageSpeed) / NumberUtilities.roundOffToOneDecimalPlace(targetAverageSpeed) * 100);
    }

    /**
     * Schedules a Service to run every Monday morning at midnight to check if the user made their weekly target for the week
     */
    public static void scheduleWeeklyGoalsService(Context context){
        if(UserAccountUtilities.getUserKey(context) != null){
            //Sets up a PendingIntent to start the Service
            AlarmManager alarmManager = (AlarmManager)context.getSystemService(ALARM_SERVICE);
            String userKey = UserAccountUtilities.getUserKey(context);
            Intent serviceIntent = new Intent(context, WeeklyGoalsService.class);
            serviceIntent.putExtra(WeeklyGoalsService.USER_KEY_EXTRA, userKey);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, serviceIntent, 0);

            //Cancels any previous scheduled PendingIntents
            if(alarmManager != null){
                alarmManager.cancel(pendingIntent);
            }

            //Sets the execution date to the start of Monday
            Calendar calendar = Calendar.getInstance();

            do{
                calendar.add(Calendar.DAY_OF_WEEK, 1);
            } while(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY);

            calendar.set(Calendar.AM_PM, Calendar.AM);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            //Schedules the Service to run on Monday morning at midnight
            if(alarmManager != null){
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    /**
     * Returns an ArrayList containing the runs for the current week
     * @param runs Runs that have been taken
     */
    public static ArrayList<Run> getRunsForThisWeek(ArrayList<Run> runs){
        ArrayList<Run> runsForThisWeek = new ArrayList<>();
        String[] dates = DateUtilities.getDatesForCurrentWeek();

        for(Run run : runs){
            if(Arrays.asList(dates).contains(run.getRunDate())){
                runsForThisWeek.add(run);
            }
        }

        return runsForThisWeek;
    }

    /**
     * Updates the user's consecutiveTargetsMet and dateOfLastTargetMet information and returns the updated target
     * @param runs An ArrayList containing all runs a user has taken
     * @param target A Target object containing the target information for a user
     */
    public static Target updateConsecutiveTargetsMet(ArrayList<Run> runs, Target target) {
        //Initialises the pastRuns ArrayList and sets the date to last week
        ArrayList<Run> pastRuns = new ArrayList<>(runs);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -7);

        //Gets the dates for the previous week
        String[] dates = DateUtilities.getDatesForSpecificWeek((Calendar) calendar.clone());

        //Reverses the order of the runs (so the latest runs will appear first)
        Collections.reverse(pastRuns);

        //Sets up the appropriate variables
        double totalDistance = 0;
        int totalDuration = 0;
        double averageSpeedTarget = target.getAverageSpeedTarget();
        averageSpeedTarget = RunInformationFormatUtilities.getDistanceInKilometres(averageSpeedTarget);
        averageSpeedTarget = NumberUtilities.roundOffToOneDecimalPlace(averageSpeedTarget);
        double distanceTarget = target.getDistanceTarget();
        int consecutiveTargetsMet = 0;
        double durationTarget = target.getDurationTarget();
        String dateOfLastMetTarget = target.getDateOfLastMetTarget();
        int weekCount = 0;

        //Removes all runs from the current week
        pastRuns.removeAll(WeeklyGoalsUtilities.getRunsForThisWeek(pastRuns));

        //Loops through past runs and increments the consecutiveTargetsMet variable until a week that didn't meet the target is found
        for(Run run : pastRuns){
            if(Arrays.asList(dates).contains(run.getRunDate())){
                //Adds the distance and duration of the run if it falls within the date range for the week that is being calculated
                totalDistance += run.getDistanceTravelled();
                totalDuration += run.getRunDuration();
            }
            else{
                //Calculates the average speed
                double averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInKilometresPerHour(totalDistance, totalDuration);

                //Increments the consecutiveTargetsMet value if the user meets their target, otherwise exits the loop
                if(totalDistance >= distanceTarget && totalDuration >= durationTarget && averageSpeed >= averageSpeedTarget){
                    //Increments the consecutiveTargetsMet and updates the dateOfLastMetTarget
                    consecutiveTargetsMet++;

                    if(weekCount == 0){
                        dateOfLastMetTarget = dates[6];
                        weekCount++;
                    }

                    //Starts totalling the values for the previous week
                    totalDistance = run.getDistanceTravelled();
                    totalDuration = run.getRunDuration();

                    //Gets the dates for the previous week
                    calendar.add(Calendar.DATE, -7);
                    dates = DateUtilities.getDatesForSpecificWeek(calendar);
                }
                else{
                    //Exits loop when the target is not met
                    break;
                }
            }
        }
        //Calculates the averageSpeed of the final week
        double averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInKilometresPerHour(totalDistance, totalDuration);

        //Determines if the user met their target in the final week that was looped through
        if(totalDistance >= distanceTarget && totalDuration >= target.getDurationTarget() && averageSpeed >= averageSpeedTarget){
            //Increments the consecutiveTargetsMet and updates the dateOfLastMetTarget
            consecutiveTargetsMet++;

            if(weekCount == 0){
                dateOfLastMetTarget = dates[6];
            }
        }

        //Updates the target object
        target.setConsecutiveTargetsMet(consecutiveTargetsMet);
        target.setDateOfLastMetTarget(dateOfLastMetTarget);

        return target;
    }
}