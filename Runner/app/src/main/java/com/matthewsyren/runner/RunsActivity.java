package com.matthewsyren.runner;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.matthewsyren.runner.adapters.IRecyclerViewOnItemClickListener;
import com.matthewsyren.runner.adapters.RunsAdapter;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.PreferenceUtilities;

import java.util.ArrayList;

import butterknife.BindView;

public class RunsActivity
        extends BaseActivity
        implements IRecyclerViewOnItemClickListener {
    //View bindings
    @BindView(R.id.rv_runs) RecyclerView mRvRuns;
    @BindView(R.id.pb_runs) ProgressBar mPbRuns;
    @BindView(R.id.tv_no_runs_completed) TextView mTvNoRunsCompleted;

    //Variables
    private ArrayList<Run> mRuns;
    private static final String RUNS_BUNDLE_KEY = "runs_bundle_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runs);
        super.onCreateDrawer();

        if(savedInstanceState != null && savedInstanceState.containsKey(RUNS_BUNDLE_KEY)){
            //Restores the data
            mRuns = savedInstanceState.getParcelableArrayList(RUNS_BUNDLE_KEY);
            displayRuns(mRuns);
        }
        else{
            //Requests the data from Firebase
            new Run().requestUserRuns(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Sets the selected item in the Navigation Drawer to the runs page
        super.setSelectedNavItem(R.id.nav_runs);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mRuns != null){
            outState.putParcelableArrayList(RUNS_BUNDLE_KEY, mRuns);
        }
    }

    @Override
    public void onItemClick(int position) {

    }

    //Displays the runs in the RecyclerView
    private void displayRuns(ArrayList<Run> runs){
        //Hides the ProgressBar
        mPbRuns.setVisibility(View.GONE);

        //Displays the runs, or a message if there are no runs completed
        if(runs != null && runs.size() > 0){
            RunsAdapter runsAdapter = new RunsAdapter(runs, RunsActivity.this);
            mRvRuns.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            mRvRuns.setAdapter(runsAdapter);
        }
        else{
            //Displays a message saying no runs have been completed
            mTvNoRunsCompleted.setVisibility(View.VISIBLE);
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

            if(resultCode == FirebaseService.ACTION_GET_RUNS_RESULT_CODE){
                mRuns = resultData.getParcelableArrayList(FirebaseService.ACTION_GET_RUNS);

                //Displays the runs
                displayRuns(mRuns);
            }
        }
    }
}