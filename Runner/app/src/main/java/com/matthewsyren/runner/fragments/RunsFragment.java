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
        extends Fragment
        implements IRecyclerViewOnItemClickListener {
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
    private IOnRunsDownloadedListener mOnRunsDownloadedListener;
    private boolean mIsTwoPane = false;
    private int mSelectedPosition = 0;
    private static final String SELECTED_POSITION_BUNDLE_KEY = "selected_position_bundle_key";
    private static final String SCROLL_VIEW_POSITION_BUNDLE_KEY = "scroll_view_position_bundle_key";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflates the layout for the Fragment
        View view = inflater.inflate(R.layout.fragment_runs, container, false);
        ButterKnife.bind(this, view);

        if(savedInstanceState != null){
            //Restores the data
            restoreData(savedInstanceState);
        }
        else{
            //Requests the data from Firebase
            new Run().requestRuns(getContext(), PreferenceUtilities.getUserKey(getContext()), new DataReceiver(new Handler()));
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mRuns != null){
            outState.putParcelableArrayList(RUNS_BUNDLE_KEY, mRuns);
        }

        outState.putInt(SELECTED_POSITION_BUNDLE_KEY, mSelectedPosition);

        /*
         * Puts the RecyclerView's scroll information into the Bundle
         * Adapted from https://stackoverflow.com/questions/29208086/save-the-position-of-scrollview-when-the-orientation-changes?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
         */
        outState.putIntArray(SCROLL_VIEW_POSITION_BUNDLE_KEY, new int[]{
                mRvRuns.getScrollX(),
                mRvRuns.getScrollY()});
    }

    //Restores the data
    private void restoreData(Bundle savedInstanceState){
        //Restores the data
        if(savedInstanceState.containsKey(RUNS_BUNDLE_KEY)){
            mRuns = savedInstanceState.getParcelableArrayList(RUNS_BUNDLE_KEY);
            displayRuns(mRuns);
        }
        else{
            //Requests the data from Firebase
            new Run().requestRuns(getContext(), PreferenceUtilities.getUserKey(getContext()), new DataReceiver(new Handler()));
        }

        if(savedInstanceState.containsKey(SELECTED_POSITION_BUNDLE_KEY)){
            mSelectedPosition = savedInstanceState.getInt(SELECTED_POSITION_BUNDLE_KEY);
        }

        /*
         * Restores the RecyclerViews's scroll position
         * Adapted from https://stackoverflow.com/questions/29208086/save-the-position-of-scrollview-when-the-orientation-changes?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
         */
        if(savedInstanceState.containsKey(SCROLL_VIEW_POSITION_BUNDLE_KEY)){
            final int[] scrollViewPosition = savedInstanceState.getIntArray(SCROLL_VIEW_POSITION_BUNDLE_KEY);
            if(scrollViewPosition != null)
                mRvRuns.post(new Runnable(){
                    public void run() {
                        mRvRuns.scrollTo(scrollViewPosition[0], scrollViewPosition[1]);
                    }
                });
        }
    }

    //Displays the runs in the RecyclerView
    private void displayRuns(ArrayList<Run> runs){
        //Hides the ProgressBar
        mPbRuns.setVisibility(View.GONE);

        //Displays the runs, or a message if there are no runs completed
        if(runs != null && runs.size() > 0){
            RunsAdapter runsAdapter = new RunsAdapter(runs, this, mIsTwoPane);
            runsAdapter.setSelectedPosition(mSelectedPosition);
            mRvRuns.setLayoutManager(new LinearLayoutManager(getContext()));
            mRvRuns.setAdapter(runsAdapter);
        }
        else{
            //Displays a message saying no runs have been completed
            mTvNoRunsCompleted.setVisibility(View.VISIBLE);
        }

        //Sends the selected run to the appropriate Activity
        if(mOnRunsDownloadedListener != null){
            if(mRuns != null && mRuns.size() > 0){
                mOnRunsDownloadedListener.runsDownloaded(mRuns.get(mSelectedPosition));
            }
            else{
                mOnRunsDownloadedListener.runsDownloaded(null);
            }
        }
    }

    public void setOnItemClickListener(IRecyclerViewOnItemClickListener onItemClickListener){
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnRunsDownloadedListener(IOnRunsDownloadedListener onRunsDownloadedListener){
        mOnRunsDownloadedListener = onRunsDownloadedListener;
    }

    public void setIsTwoPane(boolean isTwoPane){
        mIsTwoPane = isTwoPane;
    }

    //Returns the run at the specified position
    public Run getRunAtPosition(int position){
        if(mRuns != null && mRuns.size() > position){
            return mRuns.get(position);
        }
        return null;
    }

    @Override
    public void onItemClick(int position) {
        mSelectedPosition = position;

        if(mOnItemClickListener != null){
            mOnItemClickListener.onItemClick(position);
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