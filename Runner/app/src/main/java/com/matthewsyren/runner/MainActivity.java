package com.matthewsyren.runner;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.matthewsyren.runner.services.FirebaseService;
import com.matthewsyren.runner.utilities.PreferenceUtilities;

import java.util.Arrays;

public class MainActivity
        extends BaseActivity {
    //Variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private static final int SIGN_IN_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        super.onCreateDrawer();

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
                String key = resultData.getString(FirebaseService.ACTION_GET_USER_KEY);

                if(key != null){
                    PreferenceUtilities.setUserKey(getApplicationContext(), key);
                }
            }
        }
    }
}