package com.matthewsyren.runner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.view.MenuItem;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.PreferenceUtilities;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity
        extends BaseActivity
        implements OnMapReadyCallback {
    //View bindings
    @BindView(R.id.fab_toggle_run) FloatingActionButton mFabToggleRun;

    //Variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private GoogleMap mGoogleMap;
    private boolean mIsRunning = false;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private PolylineOptions mPolylineOptions;
    private LatLngBounds.Builder mLatLngBoundsBuilder;
    private int mLatLngBuilderCount = 0;

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
        mPolylineOptions = new PolylineOptions();
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

        //Sets up the location tracking
        mGoogleMap.setMyLocationEnabled(true);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(mLocationManager != null){
            //Gets current location
            LatLng currentLocation = getCurrentLocation();

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

    //Stops location tracking
    private void stopLocationTracking(){
        LatLng currentLocation = getCurrentLocation();

        if(currentLocation != null){
            //Gets the current location and adds a marker to it
            addMarkerToLocation(getCurrentLocation(), getString(R.string.end));
        }

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
        }
        else {
            //Clears the start marker and tells the user that no movement was detected
            Toast.makeText(getApplicationContext(), getString(R.string.error_no_movement), Toast.LENGTH_LONG).show();
            mGoogleMap.clear();
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
        }
    }
}