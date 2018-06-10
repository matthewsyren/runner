package com.matthewsyren.runner;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.matthewsyren.runner.models.Target;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.PreferenceUtilities;

import butterknife.BindView;

public class WeeklyGoalsActivity
        extends BaseActivity{
    //View bindings
    @BindView(R.id.et_duration_target) TextInputEditText mEtDurationTarget;
    @BindView(R.id.et_distance_target) TextInputEditText mEtDistanceTarget;
    @BindView(R.id.et_average_speed_target) TextInputEditText mEtAverageSpeedTarget;
    @BindView(R.id.tv_consecutive_targets_met) TextView mTvConsecutiveTargetsMet;
    @BindView(R.id.sv_weekly_goals) ScrollView mSvWeeklyGoals;
    @BindView(R.id.pb_weekly_goals) ProgressBar mPbWeeklyGoals;

    //Variables
    private Target mTarget;
    private static final String TARGET_BUNDLE_KEY = "target_bundle_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_goals);
        super.onCreateDrawer();

        if(savedInstanceState != null && savedInstanceState.containsKey(TARGET_BUNDLE_KEY)){
            //Restores the targets and displays them
            mTarget = savedInstanceState.getParcelable(TARGET_BUNDLE_KEY);
            displayTargets(mTarget);
        }
        else{
            //Requests the targets from Firebase
            new Target().requestTargets(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Sets the selected item in the Navigation Drawer to the weekly goals page
        super.setSelectedNavItem(R.id.nav_weekly_goals);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_weekly_goals, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.mi_save){
            saveTargets();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mTarget != null){
            outState.putParcelable(TARGET_BUNDLE_KEY, mTarget);
        }
    }

    //Displays the user's targets
    private void displayTargets(Target target) {
        //Displays the user's targets in the appropriate Views
        mEtDurationTarget.setText(String.valueOf(target.getDurationTarget()));
        mEtDistanceTarget.setText(String.valueOf(target.getDistanceTarget()));
        mEtAverageSpeedTarget.setText(String.valueOf(target.getAverageSpeedTarget()));
        mTvConsecutiveTargetsMet.setText(getString(R.string.consecutive_targets_met, target.getConsecutiveTargetsMet()));

        //Hides the ProgressBar and displays the ScrollView
        mSvWeeklyGoals.setVisibility(View.VISIBLE);
        mPbWeeklyGoals.setVisibility(View.GONE);
    }

    //Uploads the user's new targets
    private void saveTargets() {
        //Checks that all EditTexts have data inputted
        if(TextUtils.isEmpty(mEtDurationTarget.getText().toString())){
            Toast.makeText(getApplicationContext(), R.string.error_no_duration_target, Toast.LENGTH_LONG).show();
            mEtDurationTarget.requestFocus();
        }
        else if(TextUtils.isEmpty(mEtDistanceTarget.getText().toString())){
            Toast.makeText(getApplicationContext(), R.string.error_no_distance_target, Toast.LENGTH_LONG).show();
            mEtDistanceTarget.requestFocus();
        }
        else if(TextUtils.isEmpty(mEtAverageSpeedTarget.getText().toString())){
            Toast.makeText(getApplicationContext(), R.string.error_no_average_speed_target, Toast.LENGTH_LONG).show();
            mEtAverageSpeedTarget.requestFocus();
        }
        else{
            //Displays the Progress Bar and hides the other Views
            mPbWeeklyGoals.setVisibility(View.VISIBLE);
            mSvWeeklyGoals.setVisibility(View.GONE);

            //Updates the values
            mTarget.setDistanceTarget(Integer.parseInt(mEtDistanceTarget.getText().toString()));
            mTarget.setDurationTarget(Integer.parseInt(mEtDurationTarget.getText().toString()));
            mTarget.setAverageSpeedTarget(Integer.parseInt(mEtAverageSpeedTarget.getText().toString()));

            //Sends the updated targets to Firebase
            mTarget.updateTargets(this, PreferenceUtilities.getUserKey(this), new DataReceiver(new Handler()));
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

            if(resultCode == FirebaseService.ACTION_GET_TARGETS_RESULT_CODE){
                mTarget = resultData.getParcelable(FirebaseService.ACTION_GET_TARGETS);

                if(mTarget != null){
                    displayTargets(mTarget);
                }
                else{
                    Toast.makeText(getApplicationContext(), getString(R.string.error_no_weekly_targets_fetched), Toast.LENGTH_LONG).show();
                    mPbWeeklyGoals.setVisibility(View.GONE);
                }
            }
            else if(resultCode == FirebaseService.ACTION_UPDATE_TARGETS_RESULT_CODE){
                Toast.makeText(getApplicationContext(), R.string.targets_successfully_updated, Toast.LENGTH_LONG).show();

                //Hides the Progress Bar and displays the other Views
                mPbWeeklyGoals.setVisibility(View.GONE);
                mSvWeeklyGoals.setVisibility(View.VISIBLE);
            }
        }
    }
}