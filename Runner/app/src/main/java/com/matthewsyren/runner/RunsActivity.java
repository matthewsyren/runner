package com.matthewsyren.runner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.matthewsyren.runner.adapters.IRecyclerViewOnItemClickListener;
import com.matthewsyren.runner.fragments.RunsFragment;
import com.matthewsyren.runner.models.Run;

public class RunsActivity
        extends BaseActivity
        implements IRecyclerViewOnItemClickListener{
    //Variables
    private boolean mIsFragmentAttached;
    private static final String IS_FRAGMENT_ATTACHED_BUNDLE_KEY = "is_fragment_attached_bundle_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runs);
        super.onCreateDrawer();

        //Determines whether the Fragment has been attached already
        if(savedInstanceState != null && savedInstanceState.containsKey(IS_FRAGMENT_ATTACHED_BUNDLE_KEY)){
            mIsFragmentAttached = savedInstanceState.getBoolean(IS_FRAGMENT_ATTACHED_BUNDLE_KEY);

            if(mIsFragmentAttached){
                //Resets the onItemClickListener
                RunsFragment runsFragment = (RunsFragment) getSupportFragmentManager().findFragmentById(R.id.fl_runs);
                runsFragment.setOnItemClickListener(this);
            }
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

    //Attaches the appropriate Fragments to the FrameLayout
    private void attachFragments() {
        if(!mIsFragmentAttached){
            //Attaches the RunsFragment
            RunsFragment runsFragment = new RunsFragment();
            runsFragment.setOnItemClickListener(this);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(R.id.fl_runs, runsFragment)
                    .commit();

            mIsFragmentAttached = true;
        }
    }

    @Override
    public void onItemClick(int position) {
        //Fetches the run that was clicked on
        RunsFragment runsFragment = (RunsFragment) getSupportFragmentManager().findFragmentById(R.id.fl_runs);
        Run run = runsFragment.getRunAtPosition(position);

        //Passes the run that was clicked on to the RunsActivity
        Intent intent = new Intent(RunsActivity.this, RunDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(RunDetailActivity.RUN_BUNDLE_KEY, run);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}