package com.matthewsyren.runner.utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtilities {
    //Formats the given date to a yyyy-MM-dd format
    public static String formatDate(Date date){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return simpleDateFormat.format(date);
    }

    //Gets all dates for the current week and returns them as a String array
    public static String[] getDatesForCurrentWeek(){
        String[] dates = new String[7];

        //Sets the start of the week to Monday
        Calendar calendar = Calendar.getInstance();
        while(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY){
            calendar.add(Calendar.DAY_OF_WEEK, -1);
        }

        //Loops through all dates for the current week and adds them to the array
        for(int i = 0; i < 7; i++) {
            dates[i] = formatDate(calendar.getTime());
            calendar.add(Calendar.DAY_OF_WEEK, 1);
        }

        return dates;
    }
}