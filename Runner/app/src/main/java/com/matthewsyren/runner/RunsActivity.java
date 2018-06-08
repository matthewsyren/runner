package com.matthewsyren.runner;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.widget.FrameLayout;

import com.matthewsyren.runner.adapters.IRecyclerViewOnItemClickListener;
import com.matthewsyren.runner.fragments.IOnRunsDownloadedListener;
import com.matthewsyren.runner.fragments.RunDetailFragment;
import com.matthewsyren.runner.fragments.RunsFragment;
import com.matthewsyren.runner.models.Run;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RunsActivity
        extends BaseActivity
        implements IRecyclerViewOnItemClickListener,
        IOnRunsDownloadedListener {
    //View bindings
    @Nullable
    @BindView(R.id.fl_run_detail) FrameLayout mFlRunDetail;

    //Variables
    private boolean mIsFragmentAttached;
    private static final String IS_FRAGMENT_ATTACHED_BUNDLE_KEY = "is_fragment_attached_bundle_key";
    private boolean mIsTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runs);
        super.onCreateDrawer();
        ButterKnife.bind(this);

        //Determines whether the device is a tablet
        if(mFlRunDetail != null){
            mIsTwoPane = true;
        }

        //Determines whether the Fragment has been attached already
        if(savedInstanceState != null){
            restoreData(savedInstanceState);
        }

        //Attaches the appropriate Fragments
        attachFragments();
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

        outState.putBoolean(IS_FRAGMENT_ATTACHED_BUNDLE_KEY, mIsFragmentAttached);
    }

    //Restores data
    private void restoreData(Bundle savedInstanceState){
        if(savedInstanceState.containsKey(IS_FRAGMENT_ATTACHED_BUNDLE_KEY)){
            mIsFragmentAttached = savedInstanceState.getBoolean(IS_FRAGMENT_ATTACHED_BUNDLE_KEY);
        }

        if(mIsFragmentAttached){
            //Resets the appropriate variables for the RunsFragment
            RunsFragment runsFragment = (RunsFragment) getSupportFragmentManager().findFragmentById(R.id.fl_runs);
            runsFragment.setOnItemClickListener(this);
            runsFragment.setOnRunsDownloadedListener(this);
            runsFragment.setIsTwoPane(mIsTwoPane);
        }
    }

    //Attaches the appropriate Fragments to the FrameLayout
    private void attachFragments() {
        if(!mIsFragmentAttached){
            //Attaches the RunsFragment
            RunsFragment runsFragment = new RunsFragment();
            runsFragment.setOnItemClickListener(this);
            runsFragment.setOnRunsDownloadedListener(this);
            runsFragment.setIsTwoPane(mIsTwoPane);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fl_runs, runsFragment)
                    .commit();

            mIsFragmentAttached = true;
        }
    }

    //Opens the RunDetailFragment with the specified run
    private void displayRun(Run run){
        RunDetailFragment runDetailFragment = RunDetailFragment.newInstance(run);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_run_detail, runDetailFragment)
                .commit();
    }

    @Override
    public void onItemClick(int position) {
        //Fetches the run that was clicked on
        RunsFragment runsFragment = (RunsFragment) getSupportFragmentManager().findFragmentById(R.id.fl_runs);
        Run run = runsFragment.getRunAtPosition(position);

        if(!mIsTwoPane){
            //Sends the run to the RunsActivity
            Intent intent = new Intent(RunsActivity.this, RunDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelable(RunDetailActivity.RUN_BUNDLE_KEY, run);
            intent.putExtras(bundle);
            startActivity(intent);
        }
        else{
            displayRun(run);
        }
    }

    @Override
    public void runsDownloaded(Run selectedRun) {
        //Displays the run in a two-pane layout
        if(mIsTwoPane && selectedRun != null){
            displayRun(selectedRun);
        }
    }
}