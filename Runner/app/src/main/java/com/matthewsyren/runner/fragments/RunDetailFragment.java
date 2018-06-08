package com.matthewsyren.runner.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler;
import com.matthewsyren.runner.R;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RunDetailFragment extends Fragment {
    //View bindings
    @BindView(R.id.iv_run_route) ImageView mIvRunRoute;
    @BindView(R.id.tv_run_duration) TextView mTvRunDuration;
    @BindView(R.id.tv_run_distance) TextView mTvRunDistance;
    @BindView(R.id.tv_run_average_speed) TextView mTvRunAverageSpeed;

    //Variables
    private static final String RUN_ARGUMENT_KEY = "run_argument_key";
    private Run mRun;

    public RunDetailFragment() {
        // Required empty public constructor
    }

    //Returns a new instance of this Fragment
    public static RunDetailFragment newInstance(Run run) {
        RunDetailFragment fragment = new RunDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(RUN_ARGUMENT_KEY, run);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            //Fetches the run to display
            mRun = getArguments().getParcelable(RUN_ARGUMENT_KEY);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Inflates the layout for the Fragment
        View view = inflater.inflate(R.layout.fragment_run_detail, container, false);
        ButterKnife.bind(this, view);

        //Displays the information
        displayRunInformation();

        return view;
    }

    //Displays the run's information in the appropriate Views
    private void displayRunInformation() {
        //Makes the ImageView zoomable
        mIvRunRoute.setOnTouchListener(new ImageMatrixTouchHandler(getContext()));

        if(mRun != null){
            //Loads the image
            Picasso.with(getContext())
                    .load(mRun.getImageUrl())
                    .placeholder(R.color.colorGrey)
                    .into(mIvRunRoute);

            //Displays the appropriate text
            mTvRunDuration.setText(RunInformationFormatUtilities.getFormattedRunDuration(mRun.getRunDuration()));

            mTvRunDistance.setText(RunInformationFormatUtilities.getFormattedRunDistance(mRun.getDistanceTravelled(), getContext()));

            mTvRunAverageSpeed.setText(RunInformationFormatUtilities.getFormattedRunAverageSpeed(mRun.getDistanceTravelled(), mRun.getRunDuration(), getContext()));
        }
    }
}