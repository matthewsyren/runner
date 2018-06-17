package com.matthewsyren.runner.utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtilities {
    /**
     * Formats the given date to a yyyy-MM-dd format
     * @param date The date that is to be formatted
     */
    public static String formatDate(Date date){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return simpleDateFormat.format(date);
    }

    /**
     * Returns a String array with all dates for the current week
     */
    public static String[] getDatesForCurrentWeek(){
        //Sets the date to today
        Calendar calendar = Calendar.getInstance();

        //Returns the dates for this week
        return getDatesForSpecificWeek(calendar);
    }

    /**
     * Returns a String array containing all dates for the week of the date passed in
     * @param calendar A Calendar object set to a date in the week that you would like to get the dates for
     */
    public static String[] getDatesForSpecificWeek(Calendar calendar){
        String[] dates = new String[7];

        //Sets the start of the week to Monday
        while(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY){
            calendar.add(Calendar.DAY_OF_WEEK, -1);
        }

        //Loops through all dates for the week and adds them to the array
        for(int i = 0; i < 7; i++) {
            dates[i] = formatDate(calendar.getTime());
            calendar.add(Calendar.DAY_OF_WEEK, 1);
        }

        return dates;
    }

    /**
     * Returns a String array with all dates for the previous week
     */
    public static String[] getDatesForPreviousWeek(){
        //Sets the date to a week ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -7);

        //Returns the dates in the previous week
        return getDatesForSpecificWeek(calendar);
    }
}