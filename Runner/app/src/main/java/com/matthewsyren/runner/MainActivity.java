package com.matthewsyren.runner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.DateUtilities;
import com.matthewsyren.runner.utilities.NetworkUtilities;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.matthewsyren.runner.utilities.UserAccountUtilities;
import com.matthewsyren.runner.utilities.WidgetUtilities;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity
        extends BaseActivity
        implements OnMapReadyCallback {
    //View bindings
    @BindView(R.id.fab_toggle_run) FloatingActionButton mFabToggleRun;
    @BindView(R.id.cl_run_information) ConstraintLayout mClRunInformation;
    @BindView(R.id.tv_run_average_speed) TextView mTvRunAverageSpeed;
    @BindView(R.id.tv_run_distance) TextView mTvRunDistance;
    @BindView(R.id.tv_run_duration) TextView mTvRunDuration;
    @BindView(R.id.pb_run_upload_progress_bar) ProgressBar mPbRunUploadProgressBar;

    //Variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private GoogleMap mGoogleMap;
    private boolean mIsRunning = false;
    private boolean mIsBackPressedOnce = false;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private PolylineOptions mPolylineOptions;
    private LatLngBounds.Builder mLatLngBoundsBuilder;
    private int mLatLngBuilderCount = 0;
    private double mDistanceTravelled = 0;
    private int mRunDuration = -1;
    private Timer mTimer;
    private Location mPreviousLocation;
    private AlertDialog mStartRunningDialog = null;
    private boolean mIsRunPaused = false;

    //Time constants
    private static final int DOUBLE_BACK_PRESS_TIME_WINDOW = 6000;

    //Request codes
    private static final int SIGN_IN_REQUEST_CODE = 1;
    private static final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        super.onCreateDrawer();
        ButterKnife.bind(this);

        //Checks if the user is signed in, and signs them in if they aren't
        setUpAuthListener();

        //Displays the FloatingActionButton if the user's unique key has been saved
        if(UserAccountUtilities.getUserKey(this) != null){
            mFabToggleRun.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        super.displayUserDetails();

        //Sets the selected item in the Navigation Drawer to the home page
        super.setSelectedNavItem(R.id.nav_home);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Removes the AuthStateListener
        if(mAuthStateListener != null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SIGN_IN_REQUEST_CODE){
            if(resultCode == RESULT_CANCELED){
                //Closes the app if the user cancels the sign in
                Toast.makeText(getApplicationContext(), getString(R.string.sign_in_cancelled), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == FINE_LOCATION_PERMISSION_REQUEST_CODE){
            //Begins tracking the user's location if they grant location access permission, or displays an error message if they don't
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startLocationTracking();
            }
            else{
                Toast.makeText(getApplicationContext(), getString(R.string.error_location_permission_not_granted), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();

        if(mIsRunning){
            //Creates an AlertDialog that asks the user to confirm that they want to leave the page while they are on a run
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            if(id == R.id.nav_sign_out){
                                //Signs the user out
                                AuthUI.getInstance()
                                        .signOut(getApplicationContext());

                                //Resets the Activity
                                stopLocationTracking();
                                restartActivity();
                            }
                            else{
                                //Sends the click to BaseActivity to handle
                                handleNavigationDrawerClick(item);
                            }
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            //Keeps the user on this Activity
                            Toast.makeText(getApplicationContext(), getString(R.string.action_cancelled), Toast.LENGTH_LONG).show();

                            //Sets the selected Navigation Drawer item to home
                            MainActivity.super.setSelectedNavItem(R.id.nav_home);
                            break;
                    }
                }
            };

            //Sets the content for the AlertDialog
            alertDialogBuilder.setMessage(R.string.leave_page_while_running_confirmation)
                    .setPositiveButton(R.string.yes, onClickListener)
                    .setNegativeButton(R.string.no, onClickListener);

            //Displays the AlertDialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //Sets the selected Navigation Drawer item to home
                    setSelectedNavItem(R.id.nav_home);
                }
            });
            alertDialog.show();
        }
        else{
            switch(id){
                case R.id.nav_sign_out:
                    //Signs the user out
                    AuthUI.getInstance()
                            .signOut(this);

                    //Resets the Activity
                    restartActivity();

                    //Displays a message to the user and closes the Navigation Drawer
                    Toast.makeText(getApplicationContext(), getString(R.string.signed_out), Toast.LENGTH_LONG).show();
                    break;
                default:
                    //Sends the click to BaseActivity to handle
                    return handleNavigationDrawerClick(item);
            }
        }

        //Closes the Navigation Drawer
        super.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
    }

    @Override
    public void onBackPressed() {
        /*
         * Makes the user click the back button twice to close the app while running (to make sure the user doesn't delete their run accidentally)
         * Adapted from https://stackoverflow.com/questions/8430805/clicking-the-back-button-twice-to-exit-an-activity?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
         */
        if(mIsRunning){
            if(mIsBackPressedOnce){
                super.onBackPressed();
            }
            else{
                Toast.makeText(getApplicationContext(), R.string.back_pressed_during_run_confirmation, Toast.LENGTH_LONG).show();
                mIsBackPressedOnce = true;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mIsBackPressedOnce = false;
                    }
                }, DOUBLE_BACK_PRESS_TIME_WINDOW);
            }
        }
        else{
            super.onBackPressed();
        }
    }

    //Sends the click to BaseActivity to handle
    private boolean handleNavigationDrawerClick(MenuItem item){
        return super.onNavigationItemSelected(item);
    }

    //Checks if the user is signed in, and signs them in if they aren't signed in already
    private void setUpAuthListener(){
        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if(firebaseUser == null){
                    //Performs sign out tasks
                    signOut();

                    //Takes the user to the sign in screen
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            SIGN_IN_REQUEST_CODE
                    );
                }
                else{
                    if(TextUtils.isEmpty(UserAccountUtilities.getPreferredDistanceUnit(getApplicationContext()))){
                        //Sets the user's default distance preference to km if it hasn't been set already
                        UserAccountUtilities.setDistanceUnitPreference(getApplicationContext());
                    }

                    if(UserAccountUtilities.getUserKey(getApplicationContext()) == null){
                        //Requests the user's unique key from Firebase
                        UserAccountUtilities.requestUserKey(getApplicationContext(), new DataReceiver(new Handler()));
                    }
                    else{
                        initialiseMap();
                    }
                }
            }
        };

        //Adds the AuthStateListener
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    //Performs tasks when the user signs out
    private void signOut(){
        //Clears the user's key from SharedPreferences
        UserAccountUtilities.setUserKey(this, null);

        //Updates the Widgets
        WidgetUtilities.updateWidgets(this);

        //Hides the FloatingActionButton
        mFabToggleRun.setVisibility(View.GONE);
    }

    //Initialises the map and the appropriate variables
    private void initialiseMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    //Starts/stops a run
    public void toggleRunOnClick(View view) {
        //Toggles variable
        mIsRunning = !mIsRunning;

        //Starts or stops a run, depending on whether a run has been started or not
        if(mIsRunning){
            mIsRunning = true;
            mIsRunPaused = false;
            checkLocationTrackingPermission();
        }
        else {
            pauseRun();
        }
    }

    //Checks to see if the user has granted permission for location tracking
    private void checkLocationTrackingPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            mIsRunning = false;

            //Requests the ACCESS_FINE_LOCATION permission from the user
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSION_REQUEST_CODE);
        }
        else{
            //Starts tracking the user's run
            startLocationTracking();
        }
    }

    //Starts tracking the user's run
    private void startLocationTracking() throws SecurityException {
        //Sets the user's status to running
        mIsRunning = true;

        //Updates the icon for the FloatingActionButton
        mFabToggleRun.setImageResource(R.drawable.ic_stop_black_24dp);

        //Initialises the PolylineOptions and the LatLngBounds Builder
        mPolylineOptions = new PolylineOptions();
        mLatLngBoundsBuilder = new LatLngBounds.Builder();

        //Displays the run information
        displayRunInformation();

        //Sets up the location tracking
        mGoogleMap.setMyLocationEnabled(true);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(mLocationManager != null){
            //Checks if the user has location tracking turned on
            if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                //Displays an AlertDialog to the user telling them to start running
                displayStartRunningDialog();

                //Adapted from https://stackoverflow.com/questions/17591147/how-to-get-current-location-in-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                mLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        //Updates location and draws a polyline from the previous location to the current location
                        LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        //Displays a marker at the start of the route
                        if(mLatLngBuilderCount == 0){
                            addMarkerToLocation(newLocation, getString(R.string.start));
                        }

                        //Hides the Dialog and zooms to the user's location
                        if(mStartRunningDialog != null){
                            mStartRunningDialog.hide();
                            mStartRunningDialog = null;
                        }

                        drawPolyline(newLocation);
                        zoomToLocation(newLocation);

                        //Includes the location in the LatLngBoundsBuilder and increments the count
                        mLatLngBoundsBuilder.include(newLocation);
                        mLatLngBuilderCount++;

                        //Updates the distance travelled
                        if(mPreviousLocation != null){
                            mDistanceTravelled += location.distanceTo(mPreviousLocation);
                        }
                        mPreviousLocation = location;
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        /*
                         * Vibrates the phone and tells the user to turn on their location tracking
                         * Adapted from https://stackoverflow.com/questions/13950338/how-to-make-an-android-device-vibrate
                         */
                        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                        if(vibrator != null){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                                vibrator.vibrate(VibrationEffect.createOneShot(750, VibrationEffect.DEFAULT_AMPLITUDE));
                            }
                            else{
                                vibrator.vibrate(750);
                            }
                        }

                        Toast.makeText(getApplicationContext(), R.string.error_no_location_provider, Toast.LENGTH_LONG).show();
                    }
                };

                //Starts the location tracking
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            }
            else{
                //Stops the timer and resets the duration
                if(mTimer != null){
                    mTimer.cancel();
                    mRunDuration = -1;
                }

                //Stops location tracking and tells the user to turn their location on
                Toast.makeText(getApplicationContext(), R.string.error_no_location_provider, Toast.LENGTH_LONG).show();
                stopLocationTracking();
            }
        }
    }

    //Stops location tracking
    private void stopLocationTracking() throws SecurityException{
        mIsRunPaused = false;
        mIsRunning = false;
        LatLng currentLocation = getCurrentLocation();

        if(currentLocation != null){
            //Gets the current location and adds a marker to it
            addMarkerToLocation(currentLocation, getString(R.string.end));
        }

        //Stops the timer
        mTimer.cancel();

        //Hides the user's location on the map and the run's information
        mGoogleMap.setMyLocationEnabled(false);
        mClRunInformation.setVisibility(View.GONE);

        //Removes the LocationListener and changes the FloatingActionButton icon
        if(mLocationManager != null && mLocationListener != null){
            mLocationManager.removeUpdates(mLocationListener);
        }

        mLocationManager = null;
        mLocationListener = null;
        mFabToggleRun.setImageResource(R.drawable.ic_baseline_directions_run_24px);
    }

    //Displays the run's information
    private void displayRunInformation(){
        //Displays the run information
        mClRunInformation.setVisibility(View.VISIBLE);

        /*
         * Updates the time for the run
         * Adapted from https://stackoverflow.com/questions/41499362/how-to-display-a-timer-in-a-textview-in-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
         */
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!mIsRunPaused){
                            //Displays the appropriate data
                            displayRunDuration();
                            displayRunDistance();
                            displayRunAverageSpeed();
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    //Displays the run duration
    private void displayRunDuration(){
        ++mRunDuration;
        mTvRunDuration.setText(RunInformationFormatUtilities.getFormattedRunDuration(mRunDuration));
    }

    //Displays the run distance
    private void displayRunDistance(){
        mTvRunDistance.setText(RunInformationFormatUtilities.getFormattedRunDistance(mDistanceTravelled, this));
    }

    //Displays the average speed
    private void displayRunAverageSpeed(){
        mTvRunAverageSpeed.setText(RunInformationFormatUtilities.getFormattedRunAverageSpeed(mDistanceTravelled, mRunDuration, this));
    }

    //Pauses the run
    private void pauseRun(){
        mIsRunPaused = true;
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager = null;

        if(mLatLngBuilderCount > 1 && mDistanceTravelled >= 1){
            /*
             * Zooms out to display the entire route taken by the user
             * Adapted from https://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
             */
            LatLngBounds bounds = mLatLngBoundsBuilder.build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 300);
            mGoogleMap.animateCamera(cameraUpdate);

            //Displays an AlertDialog that asks the user if they'd like to save their run
            displaySummaryDialog();
        }
        else {
            //Resets the map and tells the user that no movement was detected
            Toast.makeText(getApplicationContext(), getString(R.string.error_no_movement), Toast.LENGTH_LONG).show();
            stopLocationTracking();
            restartActivity();
        }
    }

    //Adds a marker to the specified location
    private void addMarkerToLocation(LatLng location, String markerTitle){
        mGoogleMap.addMarker( new MarkerOptions()
                .position(location)
                .title(markerTitle))
                .showInfoWindow();

        //Prevents clicks on the Markers
        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });
    }

    //Returns the current location in a LatLng object
    private LatLng getCurrentLocation() throws SecurityException{
        if(mPreviousLocation != null){
            return new LatLng(mPreviousLocation.getLatitude(), mPreviousLocation.getLongitude());
        }
        else{
            return null;
        }
    }

    //Draws a polyline between the points that the user has visited while the app has been open
    private void drawPolyline(LatLng position){
        if(position != null){
            /*
             * Adds a polyline to the map
             * Adapted from https://stackoverflow.com/questions/17425499/how-to-draw-interactive-polyline-on-route-google-maps-v2-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
             */
            mPolylineOptions.add(position);

            mPolylineOptions.width(7)
                    .color(Color.BLUE);

            mGoogleMap.addPolyline(mPolylineOptions);
        }
    }

    //Zooms the camera to the specified LatLng location
    private void zoomToLocation(LatLng location){
        if(location != null){
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 16);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    /*
     * Displays an AlertDialog telling the user to start their run
     */
    private void displayStartRunningDialog(){
        //Creates an AlertDialog to ask the user if they'd like to save their run
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.dialog_start_running, null);
        alertDialogBuilder.setView(view);
        mStartRunningDialog = alertDialogBuilder.create();
        mStartRunningDialog.show();
    }

    /*
     * Displays an AlertDialog which allows the user to save their run
     * Adapted from https://developer.android.com/guide/topics/ui/dialogs.html
     */
    private void displaySummaryDialog(){
        //Displays the appropriate data
        displayRunDuration();
        displayRunDistance();
        displayRunAverageSpeed();

        //Creates an AlertDialog to ask the user if they'd like to save their run
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.dialog_run_summary, null);
        alertDialogBuilder.setView(view);

        //View assignments
        TextView tvPopupRunDuration = view.findViewById(R.id.tv_popup_run_duration);
        TextView tvPopupRunDistance = view.findViewById(R.id.tv_popup_run_distance);
        TextView tvPopupRunAverageSpeed = view.findViewById(R.id.tv_popup_run_average_speed);

        //Performs the appropriate action based on the user's input
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Stops location tracking
                        stopLocationTracking();

                        //Displays the appropriate message (based on whether there is an Internet connection)
                        if(NetworkUtilities.isOnline(getApplicationContext())){
                            Toast.makeText(getApplicationContext(), getString(R.string.uploading), Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(getApplicationContext(), getString(R.string.error_no_internet_connection), Toast.LENGTH_LONG).show();
                        }

                        //Begins the process of saving the run's information
                        mPbRunUploadProgressBar.setVisibility(View.VISIBLE);
                        mFabToggleRun.setVisibility(View.GONE);
                        saveMapScreenshot();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //Stops location tracking
                        stopLocationTracking();

                        //Displays a message to the user telling them they didn't save their run
                        Toast.makeText(getApplicationContext(), getString(R.string.run_not_saved), Toast.LENGTH_LONG).show();

                        //Restarts the Activity
                        restartActivity();
                        break;
                }
            }
        };

        //Displays the information
        tvPopupRunDuration.setText(RunInformationFormatUtilities
                .getFormattedRunDuration(mRunDuration));

        tvPopupRunDistance.setText(RunInformationFormatUtilities
                .getFormattedRunDistance(mDistanceTravelled, this));

        tvPopupRunAverageSpeed.setText(RunInformationFormatUtilities
                .getFormattedRunAverageSpeed(mDistanceTravelled, mRunDuration, this));

        alertDialogBuilder.setTitle(getString(R.string.run_summary));
        alertDialogBuilder.setPositiveButton(R.string.yes, onClickListener)
                .setNegativeButton(R.string.no, onClickListener);

        //Displays the AlertDialog and adds a Listener to require two clicks on the back button to close the AlertDialog
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //Restarts the run if the user cancels the AlertDialog
                mIsRunPaused = false;
                mIsRunning = true;

                try{
                    //Requests location updates
                    mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                    if(mLocationManager != null){
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
                    }
                }
                catch(SecurityException s){
                    Toast.makeText(getApplicationContext(), R.string.error_location_permission_not_granted, Toast.LENGTH_LONG).show();
                }
            }
        });
        alertDialog.show();
    }

    //Uploads a screenshot of the map to Firebase Storage
    private void saveMapScreenshot(){
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                //Converts the Bitmap image (screenshot of the map) to a byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, byteArrayOutputStream);

                //Generates a unique key for the image
                final String imageKey = FirebaseDatabase.getInstance()
                        .getReference()
                        .push()
                        .getKey();

                //Saves the image to Firebase Storage (with the name of [image's unique key].jpg)
                final StorageReference storageReference = FirebaseStorage.getInstance()
                        .getReference()
                        .child(imageKey + ".jpg");

                storageReference.putBytes(byteArrayOutputStream.toByteArray())
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                storageReference.getDownloadUrl()
                                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(final Uri uri) {
                                                //Saves the Run details to the Firebase Database once the screenshot has been uploaded
                                                Date date = new Date();
                                                String formattedDate = DateUtilities.formatDate(date);

                                                //Creates a Run object with the appropriate data
                                                Run run = new Run(formattedDate, mRunDuration, Math.round(mDistanceTravelled), uri.toString());

                                                //Sends a request to FirebaseService to upload the run's data
                                                run.requestUpload(getApplicationContext(), UserAccountUtilities.getUserKey(getApplicationContext()), imageKey, new DataReceiver(new Handler()));
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        };

        //Sets a callback for the map
        mGoogleMap.snapshot(callback);
    }

    //Restarts the Activity by resetting the appropriate variables
    private void restartActivity(){
        //Resets the variables
        mDistanceTravelled = 0;
        mRunDuration = -1;
        mPolylineOptions = null;
        mGoogleMap.clear();
        mLatLngBuilderCount = 0;

        //Displays the appropriate data
        displayRunDuration();
        displayRunDistance();
        displayRunAverageSpeed();

        //Zooms to last known location
        if(mPreviousLocation != null){
            zoomToLocation(new LatLng(mPreviousLocation.getLatitude(), mPreviousLocation.getLongitude()));
        }
    }

    //Used to retrieve results from the FirebaseService
    private class DataReceiver
            extends ResultReceiver{

        //Constructor
        DataReceiver(Handler handler) {
            super(handler);
        }

        //Performs the appropriate action based on the result
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            if(resultCode == FirebaseService.ACTION_GET_USER_KEY_RESULT_CODE){
                //Gets the user's key
                String key = resultData.getString(FirebaseService.USER_KEY_EXTRA);

                if(key != null){
                    //Saves the key to SharedPreferences and initialises the map
                    UserAccountUtilities.setUserKey(getApplicationContext(), key);
                    initialiseMap();

                    //Displays the FloatingActionButton once the user's unique key has been saved
                    mFabToggleRun.setVisibility(View.VISIBLE);

                    //Updates the Widgets
                    WidgetUtilities.updateWidgets(getApplicationContext());
                }
            }
            else if(resultCode == FirebaseService.ACTION_UPLOAD_RUN_INFORMATION_RESULT_CODE){
                //Updates the Widgets
                WidgetUtilities.updateWidgets(getApplicationContext());

                //Tells the user that their upload was successful and resets the Activity
                Toast.makeText(getApplicationContext(), getString(R.string.run_saved), Toast.LENGTH_LONG).show();
                mPbRunUploadProgressBar.setVisibility(View.GONE);
                mFabToggleRun.setVisibility(View.VISIBLE);
                restartActivity();
            }
        }
    }
}