package com.matthewsyren.runner.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler;
import com.matthewsyren.runner.R;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.squareup.picasso.Callback;
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
    private boolean mIsImageLoaded = false;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 3;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflates the layout for the Fragment
        View view = inflater.inflate(R.layout.fragment_run_detail, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);

        //Displays the information
        displayRunInformation();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_run_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.mi_share){
            checkStorageWritingPermission();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE){
            //Shares the user's run if they grant the writing to storage permission, or displays an error message if they don't
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                shareRun();
            }
            else{
                Toast.makeText(getContext(), getString(R.string.error_storage_permission_not_granted), Toast.LENGTH_LONG).show();
            }
        }
    }

    /*
     * Allows the user to share their run
     * Adapted from https://stackoverflow.com/questions/33222918/sharing-bitmap-via-android-intent?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
     */
    private void shareRun(){
        if(mIsImageLoaded){
            if(getContext() != null){
                Bitmap bitmap = ((BitmapDrawable)mIvRunRoute.getDrawable()).getBitmap();

                //Saves the image to storage
                String path = MediaStore.Images.Media.insertImage(
                        getContext().getContentResolver(),
                        bitmap,
                        "",
                        null);

                Uri uri = Uri.parse(path);

                //Assembles the text to send to the user
                String text = getString(R.string.share_run_introduction) +
                        getString(R.string.run_duration, RunInformationFormatUtilities.getFormattedRunDuration(mRun.getRunDuration())) + "\n" +
                        getString(R.string.run_distance, RunInformationFormatUtilities.getFormattedRunDistance(mRun.getDistanceTravelled(), getContext())) + "\n" +
                        getString(R.string.run_average_speed, RunInformationFormatUtilities.getFormattedRunAverageSpeed(mRun.getDistanceTravelled(), mRun.getRunDuration(), getContext()));

                //Adds the run image and information to the Intent
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.putExtra(Intent.EXTRA_TEXT, text);
                intent.setType("image/png");
                startActivity(Intent.createChooser(intent,getString(R.string.share)));
            }
            else{
                Toast.makeText(getContext(), R.string.error_null_bitmap, Toast.LENGTH_LONG).show();
            }
        }
    }

    //Checks to see if the user has granted permission for writing to storage
    private void checkStorageWritingPermission() {
        if(getContext() != null && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //Requests the WRITE_EXTERNAL_STORAGE permission from the user
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        }
        else{
            //Shares the user's run
            shareRun();
        }
    }

    //Displays the run's information in the appropriate Views
    @SuppressLint("ClickableViewAccessibility")
    private void displayRunInformation() {
        //Makes the ImageView zoomable
        if(getContext() != null){
            mIvRunRoute.setOnTouchListener(new ImageMatrixTouchHandler(getContext()));
        }

        if(mRun != null){
            //Loads the image
            Picasso.with(getContext())
                    .load(mRun.getImageUrl())
                    .placeholder(R.color.colorGrey)
                    .into(mIvRunRoute, new Callback() {
                        @Override
                        public void onSuccess() {
                            mIsImageLoaded = true;
                        }

                        @Override
                        public void onError() {

                        }
                    });

            //Displays the appropriate text
            mTvRunDuration.setText(RunInformationFormatUtilities.getFormattedRunDuration(mRun.getRunDuration()));

            mTvRunDistance.setText(RunInformationFormatUtilities.getFormattedRunDistance(mRun.getDistanceTravelled(), getContext()));

            mTvRunAverageSpeed.setText(RunInformationFormatUtilities.getFormattedRunAverageSpeed(mRun.getDistanceTravelled(), mRun.getRunDuration(), getContext()));
        }
    }
}