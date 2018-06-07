package com.matthewsyren.runner;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.matthewsyren.runner.adapters.IRecyclerViewOnItemClickListener;
import com.matthewsyren.runner.fragments.RunsFragment;

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

        if(savedInstanceState != null && savedInstanceState.containsKey(IS_FRAGMENT_ATTACHED_BUNDLE_KEY)){
            mIsFragmentAttached = savedInstanceState.getBoolean(IS_FRAGMENT_ATTACHED_BUNDLE_KEY);
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

    }
}