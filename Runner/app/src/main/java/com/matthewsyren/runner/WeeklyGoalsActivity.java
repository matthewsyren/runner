package com.matthewsyren.runner;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.DateUtilities;
import com.matthewsyren.runner.utilities.NumberUtilities;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.matthewsyren.runner.utilities.UserAccountUtilities;
import com.matthewsyren.runner.utilities.WeeklyGoalsUtilities;
import com.matthewsyren.runner.utilities.WidgetUtilities;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;

public class WeeklyGoalsActivity
        extends BaseActivity{
    //View bindings
    @BindView(R.id.et_duration_target) TextInputEditText mEtDurationTarget;
    @BindView(R.id.et_distance_target) TextInputEditText mEtDistanceTarget;
    @BindView(R.id.et_average_speed_target) TextInputEditText mEtAverageSpeedTarget;
    @BindView(R.id.tv_duration_target) TextView mTvDurationTarget;
    @BindView(R.id.tv_distance_target) TextView mTvDistanceTarget;
    @BindView(R.id.tv_distance_target_label) TextView mTvDistanceTargetLabel;
    @BindView(R.id.tv_average_speed_target_label) TextView mTvAverageSpeedTargetLabel;
    @BindView(R.id.tv_average_speed_target) TextView mTvAverageSpeedTarget;
    @BindView(R.id.tv_consecutive_targets_met) TextView mTvConsecutiveTargetsMet;
    @BindView(R.id.sv_weekly_goals) ScrollView mSvWeeklyGoals;
    @BindView(R.id.pb_weekly_goals) ProgressBar mPbWeeklyGoals;
    @BindView(R.id.pb_distance_target) ProgressBar mPbDistanceTarget;
    @BindView(R.id.pb_duration_target) ProgressBar mPbDurationTarget;
    @BindView(R.id.pb_average_speed_target) ProgressBar mPbAverageSpeedTarget;

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
            if(UserAccountUtilities.getUserKey(this) != null) {
                //Requests the targets and runs for the week from Firebase
                new Target().requestTargetsAndRuns(this, UserAccountUtilities.getUserKey(this), new DataReceiver(new Handler()));
            }
            else {
                //Requests the user's key if it hasn't already been set
                UserAccountUtilities.requestUserKey(this, new DataReceiver(new Handler()));
            }
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
            new Target().requestTargetsAndRuns(this, UserAccountUtilities.getUserKey(this), new DataReceiver(new Handler()));
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
            new Target().requestTargetsAndRuns(this, UserAccountUtilities.getUserKey(this), new DataReceiver(new Handler()));
        }
    }

    //Displays the user's targets
    private void displayTargets(Target target) {
        //Displays the user's targets in the appropriate Views
        mEtDurationTarget.setText(
                String.valueOf(RunInformationFormatUtilities.getDurationInMinutes(target.getDurationTarget())));

        mEtDistanceTarget.setText(
                getString(R.string.target_three_decimal_places,
                        RunInformationFormatUtilities.getDistance(target.getDistanceTarget(), this)));

        mEtAverageSpeedTarget.setText(
                String.valueOf(NumberUtilities.roundOffToOneDecimalPlace(RunInformationFormatUtilities.getDistance(target.getAverageSpeedTarget(), this))));

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
            //Displays the ProgressBar and hides the other Views
            mPbWeeklyGoals.setVisibility(View.VISIBLE);
            mSvWeeklyGoals.setVisibility(View.GONE);

            //Ensures the decimal separator is a full stop
            String distanceTarget = mEtDistanceTarget.getText().toString();
            distanceTarget = distanceTarget.replaceAll(",", ".");
            String averageSpeed = mEtAverageSpeedTarget.getText().toString();
            averageSpeed = averageSpeed.replaceAll(",", ".");

            //Updates the values
            mTarget.setDistanceTarget(
                    RunInformationFormatUtilities.getDistanceInMetres(
                            Double.parseDouble(distanceTarget),
                            this));

            mTarget.setDurationTarget(
                    RunInformationFormatUtilities.getDurationInSeconds(
                            Integer.parseInt(mEtDurationTarget.getText().toString())));

            mTarget.setAverageSpeedTarget(
                    RunInformationFormatUtilities.getDistanceInMetres(
                            Double.parseDouble(averageSpeed),
                            this));

            //Sends the updated targets to Firebase
            mTarget.updateTargets(this, UserAccountUtilities.getUserKey(this), new DataReceiver(new Handler()));
        }
    }

    //Calculates the user's progress towards their targets and displays it
    private void displayProgress(ArrayList<Run> runs){
        double totalDistance = 0;
        int totalDuration = 0;

        //Calculates the totals
        for(Run run : runs){
            totalDistance += run.getDistanceTravelled();
            totalDuration += run.getRunDuration();
        }

        //Gets the user's preferred distance unit
        String unit = UserAccountUtilities.getPreferredDistanceUnit(this);

        double averageSpeed;

        //Calculates the user's average speed in the appropriate unit
        if(unit.equals(getString(R.string.unit_kilometres_key))){
            //Calculates the user's average speed in km/h
            averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInKilometresPerHour(totalDistance, totalDuration);
            mTvDistanceTargetLabel.setText(R.string.distance_travelled_km);
            mTvAverageSpeedTargetLabel.setText(R.string.average_speed_kmh);
        }
        else{
            //Calculates the user's average speed in mph
            averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInMilesPerHour(totalDistance, totalDuration);
            mTvDistanceTargetLabel.setText(R.string.distance_travelled_mi);
            mTvAverageSpeedTargetLabel.setText(R.string.average_speed_mph);
        }

        //Converts the totalDistance to the appropriate unit and the duration to minutes
        totalDistance = RunInformationFormatUtilities.getDistance(totalDistance, this);

        //Displays the user's progress towards their targets
        mTvDistanceTarget.setText(
                getString(R.string.distance_target_progress,
                        totalDistance,
                        RunInformationFormatUtilities.getDistance(mTarget.getDistanceTarget(), this)));

        mTvDurationTarget.setText(
                getString(R.string.duration_target_progress,
                        RunInformationFormatUtilities.getDurationInMinutes(totalDuration),
                        RunInformationFormatUtilities.getDurationInMinutes(mTarget.getDurationTarget())));

        mTvAverageSpeedTarget.setText(
                getString(R.string.average_speed_target_progress,
                        averageSpeed,
                        RunInformationFormatUtilities.getDistance(mTarget.getAverageSpeedTarget(), this)));

        //Displays the user's progress towards their targets in the ProgressBars
        displayProgressInProgressBars(totalDistance, totalDuration, averageSpeed);
    }

    //Displays the user's progress towards their targets in the ProgressBars
    private void displayProgressInProgressBars(double totalDistance, int totalDuration, double averageSpeed){
        //Displays the user's progress in the ProgressBars with the appropriate colours (green if the target has been met, otherwise the accent colour)
        if(mTarget.getDistanceTarget() > 0){
            int progress = WeeklyGoalsUtilities.getDistanceProgress(totalDistance, RunInformationFormatUtilities.getDistance(mTarget.getDistanceTarget(), this));
            setProgressBarColour(progress, mPbDistanceTarget);
        }
        else{
            setProgressBarColour(100, mPbDistanceTarget);
        }

        if(mTarget.getDurationTarget() > 0){
            int progress = WeeklyGoalsUtilities.getDurationProgress(totalDuration, mTarget.getDurationTarget());
            setProgressBarColour(progress, mPbDurationTarget);
        }
        else{
            setProgressBarColour(100, mPbDurationTarget);
        }

        if(mTarget.getAverageSpeedTarget() > 0){
            int progress = WeeklyGoalsUtilities.getAverageSpeedProgress(averageSpeed, NumberUtilities.roundOffToOneDecimalPlace(RunInformationFormatUtilities.getDistance(mTarget.getAverageSpeedTarget(), this)));
            setProgressBarColour(progress, mPbAverageSpeedTarget);
        }
        else{
            setProgressBarColour(100, mPbAverageSpeedTarget);
        }
    }

    //Sets the passed in ProgressBar's colour based on the user's progress (green if the progress is 100, otherwise the app's accent colour)
    private void setProgressBarColour(int progress, ProgressBar progressBar){
        //Increases the ProgressBar's height
        progressBar.setScaleY(3);

        //Sets the progress and colour of the ProgressBar
        progressBar.setProgress(progress);
        if(progress >= 100){
            progressBar.getProgressDrawable()
                    .setColorFilter(getColor(R.color.colorGreen), PorterDuff.Mode.SRC_IN);
        }
        else{
            progressBar.getProgressDrawable()
                    .setColorFilter(getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        }
    }

    /*
     * Hides the keyboard
     * Adapted from https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
     */
    private void hideKeyboard(){
        View view = getCurrentFocus();
        if(view != null){
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if(inputMethodManager != null){
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
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

            if(resultCode == FirebaseService.ACTION_GET_TARGETS_AND_RUNS_RESULT_CODE){
                mTarget = resultData.getParcelable(FirebaseService.TARGET_EXTRA);
                mRuns = resultData.getParcelableArrayList(FirebaseService.RUNS_EXTRA);

                String[] datesInPreviousWeek = DateUtilities.getDatesForPreviousWeek();

                //Updates the consecutiveTargetsMet value for the user if it hasn't been updated already
                if(!Arrays.asList(datesInPreviousWeek).contains(mTarget.getDateOfLastMetTarget())){
                    mTarget = WeeklyGoalsUtilities.updateConsecutiveTargetsMet(mRuns, mTarget);
                    mTarget.updateTargets(getApplicationContext(), UserAccountUtilities.getUserKey(getApplicationContext()), null);
                }

                //Gets the runs for the current week to display
                mRuns = WeeklyGoalsUtilities.getRunsForThisWeek(mRuns);

                if(mTarget != null && mRuns != null){
                    //Displays the user's progress towards their targets
                    displayProgress(mRuns);
                    displayTargets(mTarget);
                }
                else{
                    Toast.makeText(getApplicationContext(), getString(R.string.error_no_weekly_targets_fetched), Toast.LENGTH_LONG).show();
                    mPbWeeklyGoals.setVisibility(View.GONE);
                }
            }
            else if(resultCode == FirebaseService.ACTION_UPDATE_TARGETS_RESULT_CODE){
                Toast.makeText(getApplicationContext(), R.string.targets_successfully_updated, Toast.LENGTH_LONG).show();

                //Updates the targets
                new Target().requestTargetsAndRuns(getApplicationContext(), UserAccountUtilities.getUserKey(getApplicationContext()), this);

                //Updates the Widgets
                WidgetUtilities.updateWidgets(getApplicationContext());

                //Hides the keyboard
                hideKeyboard();
            }
            else if(resultCode == FirebaseService.ACTION_GET_USER_KEY_RESULT_CODE){
                //Gets the user's key
                String key = resultData.getString(FirebaseService.USER_KEY_EXTRA);

                if(key != null){
                    //Saves the key to SharedPreferences
                    UserAccountUtilities.setUserKey(getApplicationContext(), key);

                    //Updates the Widgets
                    WidgetUtilities.updateWidgets(getApplicationContext());

                    //Restarts the Activity
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
        }
    }
}