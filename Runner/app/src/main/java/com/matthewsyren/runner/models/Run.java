package com.matthewsyren.runner.models;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.DateUtilities;

public class Run
        implements Parcelable{
    private String runDate;
    private int runDuration;
    private double distanceTravelled;
    private String imageUrl;

    public Run(){

    }

    public Run(String runDate, int runDuration, double distanceTravelled, String imageUrl) {
        this.runDate = runDate;
        this.runDuration = runDuration;
        this.distanceTravelled = distanceTravelled;
        this.imageUrl = imageUrl;
    }

    Run(Parcel in) {
        runDate = in.readString();
        runDuration = in.readInt();
        distanceTravelled = in.readDouble();
        imageUrl = in.readString();
    }

    public static final Creator<Run> CREATOR = new Creator<Run>() {
        @Override
        public Run createFromParcel(Parcel in) {
            return new Run(in);
        }

        @Override
        public Run[] newArray(int size) {
            return new Run[size];
        }
    };

    public String getRunDate() {
        return runDate;
    }

    public int getRunDuration() {
        return runDuration;
    }

    public double getDistanceTravelled() {
        return distanceTravelled;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(runDate);
        dest.writeInt(runDuration);
        dest.writeDouble(distanceTravelled);
        dest.writeString(imageUrl);
    }

    /**
     * Sends the Run to FirebaseService to be uploaded
     * @param userKey The user's unique key for Firebase
     * @param imageKey The unique key generated when an image is uplaoded
     */
    public void requestUpload(Context context, String userKey, String imageKey, ResultReceiver resultReceiver){
        Intent intent = new Intent(context, FirebaseService.class);
        Bundle bundle = new Bundle();
        intent.setAction(FirebaseService.ACTION_UPLOAD_RUN_INFORMATION);
        bundle.putParcelable(FirebaseService.RUN_EXTRA, this);
        bundle.putString(FirebaseService.USER_KEY_EXTRA, userKey);
        bundle.putString(FirebaseService.IMAGE_KEY_EXTRA, imageKey);
        intent.putExtra(FirebaseService.RESULT_RECEIVER, resultReceiver);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    /**
     * Requests all runs that the user has done
     * @param userKey The user's unique key for Firebase
     */
    public void requestRuns(Context context, String userKey, ResultReceiver resultReceiver){
        Intent intent = new Intent(context, FirebaseService.class);
        Bundle bundle = new Bundle();
        intent.setAction(FirebaseService.ACTION_GET_RUNS);
        bundle.putParcelable(FirebaseService.RUN_EXTRA, this);
        bundle.putString(FirebaseService.USER_KEY_EXTRA, userKey);
        bundle.putStringArray(FirebaseService.DATES_EXTRA, null);
        intent.putExtra(FirebaseService.RESULT_RECEIVER, resultReceiver);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    /**
     * Requests the runs that the user has completed for the current week
     * @param userKey The user's unique key for Firebase
     */
    public void requestRunsForWeek(Context context, String userKey, ResultReceiver resultReceiver){
        Intent intent = new Intent(context, FirebaseService.class);
        Bundle bundle = new Bundle();
        intent.setAction(FirebaseService.ACTION_GET_RUNS);
        bundle.putParcelable(FirebaseService.RUN_EXTRA, this);
        bundle.putString(FirebaseService.USER_KEY_EXTRA, userKey);
        bundle.putStringArray(FirebaseService.DATES_EXTRA, DateUtilities.getDatesForCurrentWeek());
        intent.putExtra(FirebaseService.RESULT_RECEIVER, resultReceiver);
        intent.putExtras(bundle);
        context.startService(intent);
    }
}