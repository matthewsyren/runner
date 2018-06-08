package com.matthewsyren.runner;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.matthewsyren.runner.fragments.RunDetailFragment;
import com.matthewsyren.runner.models.Run;

public class RunDetailActivity extends AppCompatActivity {
    //Variables
    public static final String RUN_BUNDLE_KEY = "run_bundle_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_detail);

        //Displays the run in the RunDetailFragment
        Run run = getIntent().getParcelableExtra(RUN_BUNDLE_KEY);
        FragmentManager fragmentManager = getSupportFragmentManager();
        RunDetailFragment runDetailFragment = RunDetailFragment.newInstance(run);
        fragmentManager.beginTransaction()
                .replace(R.id.fl_run_detail, runDetailFragment)
                .commit();

        //Sets the title to the date
        setTitle(run.getRunDate());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //Returns the user to the previous Activity
        switch(id){
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}