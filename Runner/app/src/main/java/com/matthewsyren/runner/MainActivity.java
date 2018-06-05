package com.matthewsyren.runner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.view.LayoutInflater;
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
import com.matthewsyren.runner.utilities.PreferenceUtilities;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
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

    //Time constants
    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 3600;

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
    }

    @Override
    protected void onResume() {
        super.onResume();

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
                Toast.makeText(getApplicationContext(), getString(R.string.error_permission_not_granted), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.nav_sign_out){
            //Signs the user out
            AuthUI.getInstance()
                    .signOut(this);

            //Displays a message to the user and closes the Navigation Drawer
            Toast.makeText(getApplicationContext(), getString(R.string.signed_out), Toast.LENGTH_LONG).show();
            super.closeDrawer(GravityCompat.START);
            return true;
        }
        else{
            return super.onNavigationItemSelected(item);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
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
                    if(PreferenceUtilities.getUserKey(getApplicationContext()) == null){
                        //Requests the user's unique key from Firebase
                        requestUserKey(firebaseUser.getEmail());
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

    //Requests the user's unique key from the Firebase Database
    private void requestUserKey(String emailAddress){
        Intent intent = new Intent(getApplicationContext(), FirebaseService.class);
        intent.setAction(FirebaseService.ACTION_GET_USER_KEY);
        intent.putExtra(Intent.EXTRA_EMAIL, emailAddress);
        intent.putExtra(FirebaseService.RESULT_RECEIVER, new DataReceiver(new Handler()));
        startService(intent);
    }

    //Performs tasks when the user signs out
    private void signOut(){
        //Clears the user's key from SharedPreferences
        PreferenceUtilities.setUserKey(this, null);
    }

    //Initialises the map and the appropriate variables
    private void initialiseMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mLatLngBoundsBuilder = new LatLngBounds.Builder();
    }

    //Starts/stops a run
    public void toggleRunOnClick(View view) {
        //Toggles variable
        mIsRunning = !mIsRunning;

        //Starts or stops a run, depending on whether a run has been started or not
        if(mIsRunning){
            checkLocationTrackingPermission();
        }
        else {
            stopLocationTracking();
        }
    }

    //Checks to see if the user has granted permission for location tracking
    private void checkLocationTrackingPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
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
        //Updates the icon for the FloatingActionButton
        mFabToggleRun.setImageResource(R.drawable.ic_stop_black_24dp);

        //Initialises the PolylineOptions
        mPolylineOptions = new PolylineOptions();

        //Displays the run information
        displayRunInformation();

        //Sets up the location tracking
        mGoogleMap.setMyLocationEnabled(true);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(mLocationManager != null){
            //Gets current location
            LatLng currentLocation = getCurrentLocation();
            mPreviousLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(currentLocation != null){
                //Centers the current location and draws a polyline to the location
                zoomToLocation(currentLocation);
                drawPolyline(currentLocation);

                //Includes the location in the LatLngBuilder
                mLatLngBoundsBuilder.include(currentLocation);
                mLatLngBuilderCount++;

                //Displays a marker at the start of the route
                addMarkerToLocation(currentLocation, getString(R.string.start));
            }

            //Adapted from https://stackoverflow.com/questions/17591147/how-to-get-current-location-in-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
            mLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //Updates location and draws a polyline from the previous location to the current location
                    LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
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

                }
            };

            //Starts the location tracking
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        }
    }

    //Stops location tracking
    private void stopLocationTracking() throws SecurityException{
        LatLng currentLocation = getCurrentLocation();

        if(currentLocation != null){
            //Gets the current location and adds a marker to it
            addMarkerToLocation(getCurrentLocation(), getString(R.string.end));
        }

        //Stops the timer
        mTimer.cancel();

        //Hides the user's location on the map and the run's information
        mGoogleMap.setMyLocationEnabled(false);
        mClRunInformation.setVisibility(View.GONE);

        //Removes the LocationListener and changes the FloatingActionButton icon
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager = null;
        mLocationListener = null;
        mFabToggleRun.setImageResource(R.drawable.ic_baseline_directions_run_24px);

        if(mLatLngBuilderCount > 1){
            /*
             * Zooms out to display the entire route taken by the user
             * Adapted from https://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
             */
            LatLngBounds bounds = mLatLngBoundsBuilder.build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 300);
            mGoogleMap.animateCamera(cameraUpdate);

            //Displays Dialog that asks the user if they'd like to save their run
            displaySummaryDialog();
        }
        else {
            //Resets the map and tells the user that no movement was detected
            Toast.makeText(getApplicationContext(), getString(R.string.error_no_movement), Toast.LENGTH_LONG).show();
            restartActivity();
        }
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
                        //Displays the appropriate data
                        displayRunDuration();
                        displayRunDistance();
                        displayRunAverageSpeed();
                    }
                });
            }
        }, 0, 1000);
    }

    //Displays the run duration
    private void displayRunDuration(){
        ++mRunDuration;
        mTvRunDuration.setText(getFormattedRunDuration());
    }

    //Returns a formatted run duration
    private String getFormattedRunDuration(){
        //Formats the duration of the run in the appropriate format
        if(mRunDuration < ONE_HOUR){
            int minutes = mRunDuration / ONE_MINUTE;
            int seconds = mRunDuration % ONE_MINUTE;
            return String.format(Locale.getDefault(),"%02d", minutes) + ":" + String.format(Locale.getDefault(),"%02d", seconds);
        }
        else{
            int hours = mRunDuration / ONE_HOUR;
            int minutes = (mRunDuration - (hours * ONE_HOUR)) / ONE_MINUTE;
            int seconds = (mRunDuration - (hours * ONE_HOUR)) % ONE_MINUTE;
            return hours + ":" + String.format(Locale.getDefault(), "%02d", minutes) + ":" + String.format(Locale.getDefault(),"%02d", seconds);
        }
    }

    //Displays the run distance
    private void displayRunDistance(){
        mTvRunDistance.setText(getFormattedRunDistance());
    }

    //Returns a formatted run distance
    private String getFormattedRunDistance(){
        //Formats the distance travelled with the correct units
        if(mDistanceTravelled < 1000){
            return getString(R.string.metres, String.valueOf(Math.round(mDistanceTravelled)));
        }
        else{
            int kilometresTravelled = (int) mDistanceTravelled / 1000;
            int metresTravelled = (int) mDistanceTravelled % 1000;
            return getString(R.string.kilometres, kilometresTravelled + "." + metresTravelled);
        }
    }

    //Displays the average speed
    private void displayRunAverageSpeed(){
        mTvRunAverageSpeed.setText(getFormattedRunAverageSpeed());
    }

    //Returns a formatted run average speed
    private String getFormattedRunAverageSpeed(){
        //Calculates the speed in km/h
        double averageSpeed = (mDistanceTravelled / mRunDuration) * 3.6;
        return getString(R.string.kilometres_per_hour, String.valueOf(Math.round(averageSpeed)));
    }

    //Adds a marker to the specified location
    private void addMarkerToLocation(LatLng location, String markerTitle){
        mGoogleMap.addMarker( new MarkerOptions()
                .position(location)
                .title(markerTitle))
                .showInfoWindow();
    }

    //Returns the current location in a LatLng object
    private LatLng getCurrentLocation() throws SecurityException{
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(location != null){
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        else{
            return null;
        }
    }

    //Draws a polyline between the points that the user has visited while the app has been open
    public void drawPolyline(LatLng position){
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
    public void zoomToLocation(LatLng location){
        if(location != null){
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 16);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    /* Displays a Dialog which allows the user to save their run
     * Adapted from https://developer.android.com/guide/topics/ui/dialogs.html
     */
    private void displaySummaryDialog(){
        //Creates an AlertDialog to ask the user if they'd like to save their run
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.run_summary_popup, null);
        builder.setView(dialogView);

        //View assignments
        TextView tvPopupRunDuration = dialogView.findViewById(R.id.tv_popup_run_duration);
        TextView tvPopupRunDistance = dialogView.findViewById(R.id.tv_popup_run_distance);
        TextView tvPopupRunAverageSpeed = dialogView.findViewById(R.id.tv_popup_run_average_speed);

        //Performs the appropriate action based on the user's input
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Begins the process of saving the run's information
                        Toast.makeText(getApplicationContext(), getString(R.string.uploading), Toast.LENGTH_LONG).show();
                        mPbRunUploadProgressBar.setVisibility(View.VISIBLE);
                        mFabToggleRun.setVisibility(View.GONE);
                        saveMapScreenshot();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        Toast.makeText(getApplicationContext(), getString(R.string.run_not_saved), Toast.LENGTH_LONG).show();

                        //Restarts the Activity
                        restartActivity();
                        break;
                }
            }
        };

        //Displays the information
        tvPopupRunDuration.setText(getFormattedRunDuration());
        tvPopupRunDistance.setText(getFormattedRunDistance());
        tvPopupRunAverageSpeed.setText(getFormattedRunAverageSpeed());
        builder.setTitle(getString(R.string.run_summary));
        builder.setPositiveButton(R.string.yes, onClickListener)
                .setNegativeButton(R.string.no, onClickListener);

        //Displays the Dialog and adds a Listener to require two clicks on the back button to close the Dialog
        final AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                /*
                 * Makes the user click the back button twice to close the Dialog (to make sure the user doesn't delete their run accidentally)
                 * Adapted from https://stackoverflow.com/questions/8430805/clicking-the-back-button-twice-to-exit-an-activity?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                 */
                if(!mIsBackPressedOnce) {
                    //Prompts for confirmation
                    Toast.makeText(getApplicationContext(), getString(R.string.dialog_cancellation_confirmation), Toast.LENGTH_LONG).show();
                    mIsBackPressedOnce = true;
                    alertDialog.show();
                }
                else{
                    //Closes the Dialog and resets the map
                    Toast.makeText(getApplicationContext(), getString(R.string.run_not_saved), Toast.LENGTH_LONG).show();
                    restartActivity();
                }

                //Creates a window of 6 seconds when the user can click the back button for a second time
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mIsBackPressedOnce = false;
                    }
                }, 6000);
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
                                                String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                        .format(date);

                                                //Creates a Run object with the appropriate data
                                                Run run = new Run(formattedDate, mRunDuration, Math.round(mDistanceTravelled), uri.toString());

                                                //Sends a request to FirebaseService to upload the run's data
                                                run.requestUpload(getApplicationContext(), PreferenceUtilities.getUserKey(getApplicationContext()), imageKey, new DataReceiver(new Handler()));
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
                String key = resultData.getString(FirebaseService.ACTION_GET_USER_KEY);

                if(key != null){
                    //Saves the key to SharedPreferences and initialises the map
                    PreferenceUtilities.setUserKey(getApplicationContext(), key);
                    initialiseMap();
                }
            }
            else if(resultCode == FirebaseService.ACTION_UPLOAD_RUN_INFORMATION_RESULT_CODE){
                //Tells the user that their upload was successful and resets the Activity
                Toast.makeText(getApplicationContext(), getString(R.string.run_saved), Toast.LENGTH_LONG).show();
                mPbRunUploadProgressBar.setVisibility(View.GONE);
                mFabToggleRun.setVisibility(View.VISIBLE);
                restartActivity();
            }
        }
    }
}