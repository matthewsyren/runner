package com.matthewsyren.runner;

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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.DateUtilities;
import com.matthewsyren.runner.utilities.PreferenceUtilities;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
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
            //Requests the targets and runs for the week from Firebase
            new Target().requestTargetsAndRuns(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
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
            new Target().requestTargetsAndRuns(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
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
            new Target().requestTargetsAndRuns(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
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
            //Displays the ProgressBar and hides the other Views
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
        double totalDistance = 0;
        int totalDuration = 0;

        //Calculates the totals
        for(Run run : runs){
            totalDistance += run.getDistanceTravelled();
            totalDuration += run.getRunDuration();
        }

        //Calculates the user's average speed
        int averageSpeed = RunInformationFormatUtilities.getUsersAverageSpeedInKilometresPerHour(totalDistance, totalDuration);

        //Converts the totalDistance to kilometres and the duration to minutes
        totalDistance /= 1000;
        totalDuration /= 60;

        //Displays the user's progress towards their targets
        mTvDistanceTarget.setText(getString(R.string.distance_target_progress, totalDistance, mTarget.getDistanceTarget()));
        mTvDurationTarget.setText(getString(R.string.duration_target_progress, totalDuration, mTarget.getDurationTarget()));
        mTvAverageSpeedTarget.setText(getString(R.string.average_speed_target_progress, averageSpeed, mTarget.getAverageSpeedTarget()));

        //Displays the user's progress towards their targets in the ProgressBars
        displayProgressInProgressBars(totalDistance, totalDuration, averageSpeed);
    }

    //Displays the user's progress towards their targets in the ProgressBars
    private void displayProgressInProgressBars(double totalDistance, int totalDuration, int averageSpeed){
        //Displays the user's progress in the ProgressBars with the appropriate colours (green if the target has been met, otherwise the accent colour)
        if(mTarget.getDistanceTarget() > 0){
            int progress = WeeklyGoalsUtilities.getDistanceProgress(totalDistance, mTarget.getDistanceTarget());
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
            int progress = WeeklyGoalsUtilities.getAverageSpeedProgress(averageSpeed, mTarget.getAverageSpeedTarget());
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

                //Gets dates for the previous week
                String[] datesInPreviousWeek = DateUtilities.getDatesForPreviousWeek();

                //Sets consecutiveTargetsMet to 0 if the user didn't meet their target in the previous week
                if(!Arrays.asList(datesInPreviousWeek).contains(mTarget.getDateOfLastMetTarget()) && mTarget.getConsecutiveTargetsMet() > 0){
                    mTarget.setConsecutiveTargetsMet(0);
                    mTarget.updateTargets(getApplicationContext(), PreferenceUtilities.getUserKey(getApplicationContext()), null);
                }

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
                new Target().requestTargetsAndRuns(getApplicationContext(), PreferenceUtilities.getUserKey(getApplicationContext()), this);

                //Updates the Widgets
                WidgetUtilities.updateWidgets(getApplicationContext());
            }
        }
    }
}