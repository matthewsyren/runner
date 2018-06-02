package com.matthewsyren.runner.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtilities {
    private static final String USER_KEY = "user_key";

    //Returns the user's unique key (which is stored in SharedPreferences)
    public static String getUserKey(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(USER_KEY, null);
    }

    //Saves the user's unique key in SharedPreferences
    public static void setUserKey(Context context, String key){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putString(USER_KEY, key);
        sharedPreferencesEditor.apply();
    }
}