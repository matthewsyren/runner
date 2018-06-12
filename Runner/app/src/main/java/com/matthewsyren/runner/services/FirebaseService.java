package com.matthewsyren.runner.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class FirebaseService
        extends IntentService {
    //ResultReceiver
    public static final String RESULT_RECEIVER = "result_receiver";

    //Actions and result codes
    public static final String ACTION_GET_USER_KEY = "action_get_user_key";
    public static final int ACTION_GET_USER_KEY_RESULT_CODE = 101;
    public static final String ACTION_UPLOAD_RUN_INFORMATION = "action_upload_run_information";
    public static final int ACTION_UPLOAD_RUN_INFORMATION_RESULT_CODE = 102;
    public static final String ACTION_GET_RUNS = "action_get_runs";
    public static final int ACTION_GET_RUNS_RESULT_CODE = 103;
    public static final String ACTION_GET_TARGETS_AND_RUNS = "action_get_targets_and_runs";
    public static final int ACTION_GET_TARGETS_AND_RUNS_RESULT_CODE = 104;
    public static final String ACTION_UPDATE_TARGETS = "action_update_targets";
    public static final int ACTION_UPDATE_TARGETS_RESULT_CODE = 105;

    //Extras
    public static final String RUN_EXTRA = "run_extra";
    public static final String TARGET_EXTRA = "target_extra";
    public static final String USER_KEY_EXTRA = "user_key_extra";
    public static final String IMAGE_KEY_EXTRA = "image_key_extra";
    public static final String DATES_EXTRA = "dates_extra";
    public static final String RUNS_EXTRA = "runs_extra";

    //Variables
    private DatabaseReference mDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    private ResultReceiver mResultReceiver;

    //Default constructor
    public FirebaseService() {
        super("FirebaseService");
    }

    //Calls the appropriate method based on the action passed in the Intent
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent != null){
            mResultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);
            String action = intent.getAction();
            String userKey;

            if(action != null){
                switch(action){
                    case ACTION_GET_USER_KEY:
                        String emailAddress = intent.getStringExtra(Intent.EXTRA_EMAIL);
                        getUserKey(emailAddress);
                        break;
                    case ACTION_UPLOAD_RUN_INFORMATION:
                        Run run = intent.getParcelableExtra(RUN_EXTRA);
                        userKey = intent.getStringExtra(USER_KEY_EXTRA);
                        String imageKey = intent.getStringExtra(IMAGE_KEY_EXTRA);
                        uploadRunInformation(run, userKey, imageKey);
                        break;
                    case ACTION_GET_RUNS:
                        userKey = intent.getStringExtra(USER_KEY_EXTRA);
                        getRuns(userKey);
                        break;
                    case ACTION_GET_TARGETS_AND_RUNS:
                        userKey = intent.getStringExtra(USER_KEY_EXTRA);
                        String[] dates = intent.getStringArrayExtra(DATES_EXTRA);
                        getTargetsAndRuns(userKey, dates);
                        break;
                    case ACTION_UPDATE_TARGETS:
                        userKey = intent.getStringExtra(USER_KEY_EXTRA);
                        Target target = intent.getParcelableExtra(TARGET_EXTRA);
                        updateTargets(target, userKey);
                        break;
                }
            }
        }
    }

    /**
     * Connects to Firebase Database
     */
    private void openFirebaseDatabaseConnection(){
        mFirebaseDatabase = FirebaseDatabase.getInstance();
    }

    /**
     * Gets the user's unique key from the Firebase Database, or generates one if the user doesn't have one
     */
    private void getUserKey(final String emailAddress){
        openFirebaseDatabaseConnection();

        mDatabaseReference = mFirebaseDatabase.getReference()
                .child("users");

        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String key = null;

                //Loops through all email addresses to see if the user's email address is there
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    if(snapshot != null && snapshot.getValue().equals(emailAddress)){
                        key = snapshot.getKey();
                    }
                }

                if(key == null){
                    //Generates a unique key if no key for the user is found
                    key = mDatabaseReference.push()
                            .getKey();

                    //Uploads the user's email address using the key as the parent node
                    if(key != null){
                        mDatabaseReference.child(key)
                                .setValue(emailAddress);
                    }
                }

                //Returns the result
                returnUserKey(key);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Returns null 
                returnUserKey(null);
            }
        });
    }

    /**
     * Uploads the user's run information to the Firebase Database
     * @param run The run to be uploaded
     * @param userKeyExtra The user's unique key for Firebase
     * @param imageKey The unique key generated when the image was uploaded
     */
    private void uploadRunInformation(Run run, String userKeyExtra, String imageKey){
        openFirebaseDatabaseConnection();

        mFirebaseDatabase.getReference()
                .child(userKeyExtra)
                .child("runs")
                .child(imageKey)
                .setValue(run);

        //Marks upload as complete
        returnRunUploadResult();
    }

    /**
     * Fetches an ArrayList of all the runs a user has taken
     * @param userKey The user's unique key for Firebase
     */
    private void getRuns(String userKey){
        openFirebaseDatabaseConnection();
        final ArrayList<Run> runs = new ArrayList<>();

        mDatabaseReference = mFirebaseDatabase.getReference()
                .child(userKey)
                .child("runs");

        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Loops through the runs stored in Firebase and adds them to an ArrayList
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    runs.add(snapshot.getValue(Run.class));
                }

                //Reverses the ArrayList (so the latest run appears first)
                Collections.reverse(runs);

                //Returns the runs to the appropriate Activity
                returnRuns(runs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Fetches a the user's targets and their runs for the week from Firebase
     * @param userKey The user's unique key for Firebase
     * @param dates The date range for the runs (pass in a date range with the format yyyy-MM-dd)
     */
    private void getTargetsAndRuns(String userKey, final String[] dates){
        openFirebaseDatabaseConnection();

        mDatabaseReference = mFirebaseDatabase.getReference()
                .child(userKey);

        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Gets the user's targets for the week
                DataSnapshot targetSnapshot = dataSnapshot.child("targets");
                Target target = targetSnapshot
                        .getValue(Target.class);

                //Creates default targets for the user if they haven't set any yet
                if(target == null){
                    target = new Target();
                    mDatabaseReference.setValue(target);
                }

                ArrayList<Run> runs = new ArrayList<>();
                DataSnapshot runsSnapshot = dataSnapshot.child("runs");

                //Fetches the user's runs for the week and adds them to the runs ArrayList
                for(DataSnapshot snapshot : runsSnapshot.getChildren()){
                    Run run = snapshot.getValue(Run.class);

                    //Adds the run to the runs ArrayList if the date is within the specified date range
                    if(Arrays.asList(dates).contains(run.getRunDate())){
                        runs.add(run);
                    }
                }

                //Sends the targets and runs back to the appropriate Activity
                returnTargetsAndRuns(target, runs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Updates the user's targets
     * @param target The target to be uploaded
     * @param userKey The user's unique key for Firebase
     */
    private void updateTargets(Target target, String userKey){
        openFirebaseDatabaseConnection();

        mDatabaseReference = mFirebaseDatabase.getReference()
                .child(userKey)
                .child("targets");

        //Updates the user's weekly targets
        mDatabaseReference.setValue(target);

        //Returns control to the appropriate Activity
        returnTargetUpdateResult();
    }

    /**
     * Returns the user's key to the appropriate Activity
     */
    private void returnUserKey(String key){
        Bundle bundle = new Bundle();
        bundle.putString(USER_KEY_EXTRA, key);
        mResultReceiver.send(ACTION_GET_USER_KEY_RESULT_CODE, bundle);
    }

    /**
     * Informs the appropriate Activity that the upload was successful
     */
    private void returnRunUploadResult(){
        mResultReceiver.send(ACTION_UPLOAD_RUN_INFORMATION_RESULT_CODE, null);
    }

    /**
     * Returns the ArrayList of runs to the user
     */
    private void returnRuns(ArrayList<Run> runs){
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(RUNS_EXTRA, runs);
        mResultReceiver.send(ACTION_GET_RUNS_RESULT_CODE, bundle);
    }

    /**
     * Returns the targets to the user
     */
    private void returnTargetsAndRuns(Target target, ArrayList<Run> runs){
        Bundle bundle = new Bundle();
        bundle.putParcelable(TARGET_EXTRA, target);
        bundle.putParcelableArrayList(RUNS_EXTRA, runs);
        mResultReceiver.send(ACTION_GET_TARGETS_AND_RUNS_RESULT_CODE, bundle);
    }

    /**
     * Informs the appropriate Activity that the updating of the user's targets was successful
     */
    private void returnTargetUpdateResult(){
        mResultReceiver.send(ACTION_UPDATE_TARGETS_RESULT_CODE, null);
    }
}