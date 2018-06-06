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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runs);
        super.onCreateDrawer();

        new Run().requestUserRuns(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Sets the selected item in the Navigation Drawer to the runs page
        super.setSelectedNavItem(R.id.nav_runs);
    }

    @Override
    public void onItemClick(int position) {

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
                ArrayList<Run> runs = resultData.getParcelableArrayList(FirebaseService.ACTION_GET_RUNS);

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
        }
    }
}