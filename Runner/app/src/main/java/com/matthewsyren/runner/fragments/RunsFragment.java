package com.matthewsyren.runner.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.matthewsyren.runner.R;
import com.matthewsyren.runner.adapters.IRecyclerViewOnItemClickListener;
import com.matthewsyren.runner.adapters.RunsAdapter;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.PreferenceUtilities;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RunsFragment
        extends Fragment {
    //View bindings
    @BindView(R.id.rv_runs)
    RecyclerView mRvRuns;
    @BindView(R.id.pb_runs)
    ProgressBar mPbRuns;
    @BindView(R.id.tv_no_runs_completed)
    TextView mTvNoRunsCompleted;

    //Variables
    private ArrayList<Run> mRuns;
    private static final String RUNS_BUNDLE_KEY = "runs_bundle_key";
    private IRecyclerViewOnItemClickListener mOnItemClickListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflates the layout for the Fragment
        View view = inflater.inflate(R.layout.fragment_runs, container, false);
        ButterKnife.bind(this, view);

        if(savedInstanceState != null && savedInstanceState.containsKey(RUNS_BUNDLE_KEY)){
            //Restores the data
            mRuns = savedInstanceState.getParcelableArrayList(RUNS_BUNDLE_KEY);
            displayRuns(mRuns);
        }
        else{
            //Requests the data from Firebase
            new Run().requestUserRuns(getContext(), PreferenceUtilities.getUserKey(getContext()), new DataReceiver(new Handler()));
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mRuns != null){
            outState.putParcelableArrayList(RUNS_BUNDLE_KEY, mRuns);
        }
    }

    //Displays the runs in the RecyclerView
    private void displayRuns(ArrayList<Run> runs){
        //Hides the ProgressBar
        mPbRuns.setVisibility(View.GONE);

        //Displays the runs, or a message if there are no runs completed
        if(runs != null && runs.size() > 0){
            RunsAdapter runsAdapter = new RunsAdapter(runs, mOnItemClickListener);
            mRvRuns.setLayoutManager(new LinearLayoutManager(getContext()));
            mRvRuns.setAdapter(runsAdapter);
        }
        else{
            //Displays a message saying no runs have been completed
            mTvNoRunsCompleted.setVisibility(View.VISIBLE);
        }
    }

    public void setOnItemClickListener(IRecyclerViewOnItemClickListener onItemClickListener){
        mOnItemClickListener = onItemClickListener;
    }

    //Returns the run at the specified position
    public Run getRunAtPosition(int position){
        if(mRuns != null && mRuns.size() > position){
            return mRuns.get(position);
        }
        return null;
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