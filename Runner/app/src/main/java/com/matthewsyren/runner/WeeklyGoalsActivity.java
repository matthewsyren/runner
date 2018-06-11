package com.matthewsyren.runner;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.PreferenceUtilities;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;

import java.util.ArrayList;

import butterknife.BindView;

public class WeeklyGoalsActivity
        extends BaseActivity{
    //View bindings
    @BindView(R.id.et_duration_target) TextInputEditText mEtDurationTarget;
    @BindView(R.id.et_distance_target) TextInputEditText mEtDistanceTarget;
    @BindView(R.id.et_average_speed_target) TextInputEditText mEtAverageSpeedTarget;
    @BindView(R.id.tv_duration_target) TextView mTvDurationTarget;
    @BindView(R.id.tv_distance_target) TextView mTvDistanceTarget;
    @BindView(R.id.tv_average_speed_target) TextView mTvAverageSpeedTarget;
    @BindView(R.id.tv_consecutive_targets_met) TextView mTvConsecutiveTargetsMet;
    @BindView(R.id.sv_weekly_goals) ScrollView mSvWeeklyGoals;
    @BindView(R.id.pb_weekly_goals) ProgressBar mPbWeeklyGoals;

    //Variables
    private Target mTarget;
    private ArrayList<Run> mRuns;
    private static final String TARGET_BUNDLE_KEY = "target_bundle_key";
    private static final String RUNS_BUNDLE_KEY = "runs_bundle_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_goals);
        super.onCreateDrawer();

        if(savedInstanceState != null){
            //Restores the data
            restoreData(savedInstanceState);
        }
        else{
            //Requests the targets and runs for the week from Firebase
            new Target().requestTargets(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
            new Run().requestRunsForWeek(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Sets the selected item in the Navigation Drawer to the weekly goals page
        super.setSelectedNavItem(R.id.nav_weekly_goals);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_weekly_goals, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.mi_save){
            //Saves the user's new targets
            saveTargets();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mTarget != null){
            outState.putParcelable(TARGET_BUNDLE_KEY, mTarget);
        }

        if(mRuns != null){
            outState.putParcelableArrayList(RUNS_BUNDLE_KEY, mRuns);
        }
    }

    //Restores the data
    private void restoreData(Bundle savedInstanceState){
        if(savedInstanceState.containsKey(TARGET_BUNDLE_KEY)){
            //Restores the targets and displays them
            mTarget = savedInstanceState.getParcelable(TARGET_BUNDLE_KEY);

            if(mTarget != null){
                displayTargets(mTarget);
            }
        }
        else{
            //Requests the user's targets from Firebase
            new Target().requestTargets(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
        }

        if(savedInstanceState.containsKey(RUNS_BUNDLE_KEY)){
            //Restores the user's runs
            mRuns = savedInstanceState.getParcelableArrayList(RUNS_BUNDLE_KEY);

            //Displays the user's progress
            if(mRuns != null && mTarget != null){
                displayProgress(mRuns);
            }
        }
        else{
            //Requests the user's runs for the week from Firebase
            new Run().requestRunsForWeek(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
        }
    }

    //Displays the user's targets
    private void displayTargets(Target target) {
        //Displays the user's targets in the appropriate Views
        mEtDurationTarget.setText(String.valueOf(target.getDurationTarget()));
        mEtDistanceTarget.setText(String.valueOf(target.getDistanceTarget()));
        mEtAverageSpeedTarget.setText(String.valueOf(target.getAverageSpeedTarget()));
        mTvConsecutiveTargetsMet.setText(getString(R.string.consecutive_targets_met, target.getConsecutiveTargetsMet()));

        //Hides the ProgressBar and displays the ScrollView
        mSvWeeklyGoals.setVisibility(View.VISIBLE);
        mPbWeeklyGoals.setVisibility(View.GONE);
    }

    //Uploads the user's new targets
    private void saveTargets() {
        //Checks that all EditTexts have data inputted
        if(TextUtils.isEmpty(mEtDurationTarget.getText().toString())){
            Toast.makeText(getApplicationContext(), R.string.error_no_duration_target, Toast.LENGTH_LONG).show();
            mEtDurationTarget.requestFocus();
        }
        else if(TextUtils.isEmpty(mEtDistanceTarget.getText().toString())){
            Toast.makeText(getApplicationContext(), R.string.error_no_distance_target, Toast.LENGTH_LONG).show();
            mEtDistanceTarget.requestFocus();
        }
        else if(TextUtils.isEmpty(mEtAverageSpeedTarget.getText().toString())){
            Toast.makeText(getApplicationContext(), R.string.error_no_average_speed_target, Toast.LENGTH_LONG).show();
            mEtAverageSpeedTarget.requestFocus();
        }
        else{
            //Displays the Progress Bar and hides the other Views
            mPbWeeklyGoals.setVisibility(View.VISIBLE);
            mSvWeeklyGoals.setVisibility(View.GONE);

            //Updates the values
            mTarget.setDistanceTarget(Integer.parseInt(mEtDistanceTarget.getText().toString()));
            mTarget.setDurationTarget(Integer.parseInt(mEtDurationTarget.getText().toString()));
            mTarget.setAverageSpeedTarget(Integer.parseInt(mEtAverageSpeedTarget.getText().toString()));

            //Sends the updated targets to Firebase
            mTarget.updateTargets(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
        }
    }

    //Calculates the user's progress towards their targets and displays it
    private void displayProgress(ArrayList<Run> runs){
        int totalDistance = 0;
        int totalDuration = 0;

        //Calculates the totals
        for(Run run : runs){
            totalDistance += run.getDistanceTravelled();
            totalDuration += run.getRunDuration();
        }

        //Displays the user's progress towards their targets
        int averageSpeed = (int) Math.round(RunInformationFormatUtilities.getUsersAverageSpeed(totalDistance, totalDuration));
        mTvDistanceTarget.setText(getString(R.string.distance_target_progress, totalDistance, mTarget.getDistanceTarget()));
        mTvDurationTarget.setText(getString(R.string.duration_target_progress, totalDuration, mTarget.getDurationTarget()));
        mTvAverageSpeedTarget.setText(getString(R.string.average_speed_target_progress, averageSpeed, mTarget.getAverageSpeedTarget()));
    }

    //Used to retrieve results from the FirebaseService
    private class DataReceiver
            extends ResultReceiver {

        //Constructor
        DataReceiver(Handler handler) {
            super(handler);
        }

        //Performs the appropriate action based on the result
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            if(resultCode == FirebaseService.ACTION_GET_TARGETS_RESULT_CODE){
                mTarget = resultData.getParcelable(FirebaseService.ACTION_GET_TARGETS);

                if(mTarget != null){
                    displayTargets(mTarget);

                    if(mRuns != null){
                        //Displays the user's progress towards their targets
                        displayProgress(mRuns);
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), getString(R.string.error_no_weekly_targets_fetched), Toast.LENGTH_LONG).show();
                    mPbWeeklyGoals.setVisibility(View.GONE);
                }
            }
            else if(resultCode == FirebaseService.ACTION_GET_RUNS_RESULT_CODE){
                mRuns = resultData.getParcelableArrayList(FirebaseService.ACTION_GET_RUNS);

                if(mTarget != null && mRuns != null){
                    //Displays the user's progress towards their targets
                    displayProgress(mRuns);
                }
            }
            else if(resultCode == FirebaseService.ACTION_UPDATE_TARGETS_RESULT_CODE){
                Toast.makeText(getApplicationContext(), R.string.targets_successfully_updated, Toast.LENGTH_LONG).show();

                //Updates the targets
                new Target().requestTargets(getApplicationContext(), PreferenceUtilities.getUserKey(getApplicationContext()), this);
            }
        }
    }
}