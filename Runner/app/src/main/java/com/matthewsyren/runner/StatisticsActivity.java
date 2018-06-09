package com.matthewsyren.runner;

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
            //Fetches the runs for the user from Firebase
            new Run().requestUserRuns(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
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
        if(runs != null){
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

        //Sets the visibility of the ScrollView to true
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
                mRuns = resultData.getParcelableArrayList(FirebaseService.ACTION_GET_RUNS);
                calculateStatistics(mRuns);
            }
        }
    }
}