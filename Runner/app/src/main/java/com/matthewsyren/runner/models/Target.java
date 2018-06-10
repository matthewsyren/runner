package com.matthewsyren.runner.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Target
        implements Parcelable {
    private int consecutiveTargetsMet;
    private String dateOfLastMetTarget;
    private int distanceTarget;
    private int durationTarget;
    private int speedTarget;

    public Target(int consecutiveTargetsMet, String dateOfLastMetTarget, int distanceTarget, int durationTarget, int speedTarget) {
        this.consecutiveTargetsMet = consecutiveTargetsMet;
        this.dateOfLastMetTarget = dateOfLastMetTarget;
        this.distanceTarget = distanceTarget;
        this.durationTarget = durationTarget;
        this.speedTarget = speedTarget;
    }

    Target(Parcel in) {
        consecutiveTargetsMet = in.readInt();
        dateOfLastMetTarget = in.readString();
        distanceTarget = in.readInt();
        durationTarget = in.readInt();
        speedTarget = in.readInt();
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

    public int getSpeedTarget() {
        return speedTarget;
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
        dest.writeInt(speedTarget);
    }
}