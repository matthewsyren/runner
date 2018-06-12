package com.matthewsyren.runner.models;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.DateUtilities;

public class Target
        implements Parcelable {
    private int consecutiveTargetsMet;
    private String dateOfLastMetTarget;
    private int distanceTarget;
    private int durationTarget;
    private int averageSpeedTarget;

    public Target(int consecutiveTargetsMet, String dateOfLastMetTarget, int distanceTarget, int durationTarget, int averageSpeedTarget) {
        this.consecutiveTargetsMet = consecutiveTargetsMet;
        this.dateOfLastMetTarget = dateOfLastMetTarget;
        this.distanceTarget = distanceTarget;
        this.durationTarget = durationTarget;
        this.averageSpeedTarget = averageSpeedTarget;
    }

    //Creates a default Target object (used if the user hasn't set any targets yet)
    public Target(){
        consecutiveTargetsMet = 0;
        dateOfLastMetTarget = "";
        distanceTarget = 21;
        durationTarget = 180;
        averageSpeedTarget = 7;
    }

    Target(Parcel in) {
        consecutiveTargetsMet = in.readInt();
        dateOfLastMetTarget = in.readString();
        distanceTarget = in.readInt();
        durationTarget = in.readInt();
        averageSpeedTarget = in.readInt();
    }

    public static final Creator<Target> CREATOR = new Creator<Target>() {
        @Override
        public Target createFromParcel(Parcel in) {
            return new Target(in);
        }

        @Override
        public Target[] newArray(int size) {
            return new Target[size];
        }
    };

    public int getConsecutiveTargetsMet() {
        return consecutiveTargetsMet;
    }

    public String getDateOfLastMetTarget() {
        return dateOfLastMetTarget;
    }

    public int getDistanceTarget() {
        return distanceTarget;
    }

    public int getDurationTarget() {
        return durationTarget;
    }

    public int getAverageSpeedTarget() {
        return averageSpeedTarget;
    }

    public void setConsecutiveTargetsMet(int consecutiveTargetsMet) {
        this.consecutiveTargetsMet = consecutiveTargetsMet;
    }

    public void setDateOfLastMetTarget(String dateOfLastMetTarget) {
        this.dateOfLastMetTarget = dateOfLastMetTarget;
    }

    public void setDistanceTarget(int distanceTarget) {
        this.distanceTarget = distanceTarget;
    }

    public void setDurationTarget(int durationTarget) {
        this.durationTarget = durationTarget;
    }

    public void setAverageSpeedTarget(int averageSpeedTarget) {
        this.averageSpeedTarget = averageSpeedTarget;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(consecutiveTargetsMet);
        dest.writeString(dateOfLastMetTarget);
        dest.writeInt(distanceTarget);
        dest.writeInt(durationTarget);
        dest.writeInt(averageSpeedTarget);
    }

    /**
     * Requests the user's targets from Firebase
     * @param userKey The user's unique key for Firebase
     */
    public void requestTargetsAndRuns(Context context, String userKey, ResultReceiver resultReceiver){
        Intent intent = new Intent(context, FirebaseService.class);
        Bundle bundle = new Bundle();
        intent.setAction(FirebaseService.ACTION_GET_TARGETS_AND_RUNS);
        bundle.putString(FirebaseService.USER_KEY_EXTRA, userKey);
        intent.putExtra(FirebaseService.RESULT_RECEIVER, resultReceiver);
        intent.putExtra(FirebaseService.DATES_EXTRA, DateUtilities.getDatesForCurrentWeek());
        intent.putExtras(bundle);
        context.startService(intent);
    }

    /**
     * Uploads the user's new targets
     * @param userKey The user's unique key for Firebase
     */
    public void updateTargets(Context context, String userKey, ResultReceiver resultReceiver){
        Intent intent = new Intent(context, FirebaseService.class);
        Bundle bundle = new Bundle();
        intent.setAction(FirebaseService.ACTION_UPDATE_TARGETS);
        bundle.putString(FirebaseService.USER_KEY_EXTRA, userKey);
        bundle.putParcelable(FirebaseService.TARGET_EXTRA, this);
        intent.putExtra(FirebaseService.RESULT_RECEIVER, resultReceiver);
        intent.putExtras(bundle);
        context.startService(intent);
    }
}