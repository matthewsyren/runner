package com.matthewsyren.runner.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.matthewsyren.runner.services.FirebaseService;

public class PreferenceUtilities {
    private static final String USER_KEY = "user_key";

    /**
     * Returns the user's unique key (which is stored in SharedPreferences)
     */
    public static String getUserKey(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(USER_KEY, null);
    }

    /**
     * Saves the user's unique key in SharedPreferences
     * @param key The user's unique key for Firebase
     */
    public static void setUserKey(Context context, String key){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putString(USER_KEY, key);
        sharedPreferencesEditor.apply();
    }

    /**
     * Requests the user's unique key from the Firebase Database
     * @param resultReceiver The ResultReceiver that will process the result returned from the Service
     */
    public static void requestUserKey(Context context, ResultReceiver resultReceiver){
        String emailAddress = getUserEmailAddress();
        Intent intent = new Intent(context, FirebaseService.class);
        intent.setAction(FirebaseService.ACTION_GET_USER_KEY);
        intent.putExtra(Intent.EXTRA_EMAIL, emailAddress);
        intent.putExtra(FirebaseService.RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    /**
     * Returns the user's email address
     */
    public static String getUserEmailAddress(){
        return FirebaseAuth.getInstance()
                .getCurrentUser()
                .getEmail();
    }
}