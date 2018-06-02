package com.matthewsyren.runner.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseService
        extends IntentService {
    //ResultReceiver
    public static final String RESULT_RECEIVER = "result_receiver";

    //Actions and result codes
    public static final String ACTION_GET_USER_KEY = "action_get_user_key";
    public static final int ACTION_GET_USER_KEY_RESULT_CODE = 101;

    //Variables
    private DatabaseReference mDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    private ResultReceiver mResultReceiver;

    //Default constructor
    public FirebaseService() {
        super("FirebaseService");
    }

    //Calls the appropriate method based on the action passed in the Intent
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent != null){
            mResultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);
            String action = intent.getAction();

            if(action != null){
                switch(action){
                    case ACTION_GET_USER_KEY:
                        String emailAddress = intent.getStringExtra(Intent.EXTRA_EMAIL);
                        getUserKey(emailAddress);
                        break;
                }
            }
        }
    }

    //Connects to Firebase Database and Storage
    private void openFirebaseDatabaseConnection(){
        mFirebaseDatabase = FirebaseDatabase.getInstance();
    }

    //Gets the user's unique key from the Firebase Realtime Database, or generates one if the user doesn't have one
    private void getUserKey(final String emailAddress){
        openFirebaseDatabaseConnection();

        mDatabaseReference = mFirebaseDatabase.getReference()
                .child("users");

        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String key = null;

                //Loops through all email addresses to see if the user's email address is there
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    if(snapshot != null && snapshot.getValue().equals(emailAddress)){
                        key = snapshot.getKey();
                    }
                }

                if(key == null){
                    //Generates a unique key if no key for the user is found
                    key = mDatabaseReference.push().getKey();

                    //Uploads the user's email address using the key as the parent node
                    if(key != null){
                        mDatabaseReference.child(key)
                                .setValue(emailAddress);
                    }
                }

                //Removes the EventListener
                mDatabaseReference.removeEventListener(this);

                //Returns the result
                returnUserKey(key);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Returns null 
                returnUserKey(null);
            }
        });
    }

    //Returns the user's key to the appropriate Activity
    private void returnUserKey(String key){
        Bundle bundle = new Bundle();
        bundle.putString(ACTION_GET_USER_KEY, key);
        mResultReceiver.send(ACTION_GET_USER_KEY_RESULT_CODE, bundle);
    }
}