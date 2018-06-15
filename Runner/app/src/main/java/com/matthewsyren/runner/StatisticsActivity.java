package com.matthewsyren.runner;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.PreferenceUtilities;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.matthewsyren.runner.utilities.WidgetUtilities;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StatisticsActivity
        extends BaseActivity {
    //View bindings
    @BindView(R.id.tv_total_number_of_runs) TextView mTvTotalNumberOfRuns;
    @BindView(R.id.tv_total_time_spent_running) TextView mTvTotalTimeSpentRunning;
    @BindView(R.id.tv_average_run_duration) TextView mTvAverageRunDuration;
    @BindView(R.id.tv_total_distance_travelled) TextView mTvTotalDistanceTravelled;
    @BindView(R.id.tv_average_distance_travelled) TextView mTvAverageDistanceTravelled;
    @BindView(R.id.tv_average_speed) TextView mTvAverageSpeed;
    @BindView(R.id.tv_no_runs_completed) TextView mTvNoRunsCompleted;
    @BindView(R.id.pb_runs) ProgressBar mPbRuns;
    @BindView(R.id.sv_statistics) ScrollView mSvStatistics;

    //Variables
    private int mTotalNumberOfRuns;
    private int mTotalTimeSpentRunning = 0;
    private int mAverageRunDuration;
    private double mTotalDistanceTravelled = 0;
    private double mAverageDistanceTravelled;
    private ArrayList<Run> mRuns;
    private static final String RUNS_BUNDLE_KEY = "runs_bundle_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        super.onCreateDrawer();
        ButterKnife.bind(this);

        if(savedInstanceState != null && savedInstanceState.containsKey(RUNS_BUNDLE_KEY)){
            //Restores the user's data and calculates their statistics
            mRuns = savedInstanceState.getParcelableArrayList(RUNS_BUNDLE_KEY);
            calculateStatistics(mRuns);
        }
        else{
            if(PreferenceUtilities.getUserKey(this) != null){
                //Fetches the runs for the user from Firebase
                new Run().requestRuns(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
            }
            else{
                //Requests the user's key if it hasn't already been set
                PreferenceUtilities.requestUserKey(this, new DataReceiver(new Handler()));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Sets the selected item in the Navigation Drawer to the statistics page
        super.setSelectedNavItem(R.id.nav_statistics);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mRuns != null){
            outState.putParcelableArrayList(RUNS_BUNDLE_KEY, mRuns);
        }
    }

    //Calculates the user's statistics
    private void calculateStatistics(ArrayList<Run> runs) {
        //Hides the ProgressBar
        mPbRuns.setVisibility(View.INVISIBLE);

        //Calculates the statistics if the user has taken runs before, or displays a message to the user saying they haven't taken any runs
        if(runs != null && runs.size() > 0){
            mTotalNumberOfRuns = runs.size();

            //Loops through the user's runs and calculates the totals
            for(Run run : runs){
                mTotalTimeSpentRunning += run.getRunDuration();
                mTotalDistanceTravelled += run.getDistanceTravelled();
            }

            //Calculates averages based on the user's totals
            mAverageDistanceTravelled = mTotalDistanceTravelled / mTotalNumberOfRuns;
            mAverageRunDuration = mTotalTimeSpentRunning / mTotalNumberOfRuns;

            //Displays the statistics
            displayStatistics();
        }
        else{
            mTvNoRunsCompleted.setVisibility(View.VISIBLE);
        }
    }

    //Displays the user's statistics
    private void displayStatistics() {
        mTvTotalNumberOfRuns.setText(String.valueOf(mTotalNumberOfRuns));

        mTvTotalTimeSpentRunning.setText(RunInformationFormatUtilities.getFormattedRunDuration(mTotalTimeSpentRunning));

        mTvAverageRunDuration.setText(RunInformationFormatUtilities.getFormattedRunDuration(mAverageRunDuration));

        mTvTotalDistanceTravelled.setText(RunInformationFormatUtilities.getFormattedRunDistance(mTotalDistanceTravelled, this));

        mTvAverageDistanceTravelled.setText(RunInformationFormatUtilities.getFormattedRunDistance(mAverageDistanceTravelled, this));

        mTvAverageSpeed.setText(RunInformationFormatUtilities.getFormattedRunAverageSpeed(mTotalDistanceTravelled, mTotalTimeSpentRunning, this));

        //Displays the ScrollView
        mSvStatistics.setVisibility(View.VISIBLE);
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

            if(resultCode == FirebaseService.ACTION_GET_RUNS_RESULT_CODE){
                mRuns = resultData.getParcelableArrayList(FirebaseService.RUNS_EXTRA);
                calculateStatistics(mRuns);
            }
            else if(resultCode == FirebaseService.ACTION_GET_USER_KEY_RESULT_CODE){
                //Gets the user's key
                String key = resultData.getString(FirebaseService.USER_KEY_EXTRA);

                if(key != null){
                    //Saves the key to SharedPreferences
                    PreferenceUtilities.setUserKey(getApplicationContext(), key);

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