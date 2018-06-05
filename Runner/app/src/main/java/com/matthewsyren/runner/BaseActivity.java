package com.matthewsyren.runner;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Class provides a base for the Navigation Drawer that is shared amongst the Activities
 * Adapted from https://stackoverflow.com/questions/19451715/same-navigation-drawer-in-different-activities?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
 */

public class BaseActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    //View bindings
    @BindView(R.id.nav_view) NavigationView mNavigationView;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;

    //Sets up the Navigation Drawer
    protected void onCreateDrawer() {
        ButterKnife.bind(this);
        mNavigationView.setNavigationItemSelectedListener(this);
        setSupportActionBar(mToolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                mToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        //Displays the user's details
        displayUserDetails();
    }

    //Method displays the user's details in the Navigation Drawer
    public void displayUserDetails(){
        NavigationView navigationView = findViewById(R.id.nav_view);
        View view =  navigationView.getHeaderView(0);
        TextView tvEmailAddress = view.findViewById(R.id.tv_email_address);
        TextView tvDisplayName = view.findViewById(R.id.tv_display_name);

        //Displays the data
        FirebaseUser firebaseUser = FirebaseAuth.getInstance()
                .getCurrentUser();

        if(firebaseUser != null){
            tvEmailAddress.setText(firebaseUser.getEmail());
            tvDisplayName.setText(firebaseUser.getDisplayName());
        }
    }

    //Sets the selected item in the Navigation Drawer
    public void setSelectedNavItem(int id){
        mNavigationView.setCheckedItem(id);
    }

   @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        //Determines which Activity to open
        switch(id){
            case R.id.nav_home:
                intent = new Intent(getApplicationContext(), MainActivity.class);
                break;
            case R.id.nav_runs:
                intent = new Intent(getApplicationContext(), RunsActivity.class);
                break;
            case R.id.nav_sign_out:
                //Signs the user out
                AuthUI.getInstance()
                        .signOut(this);

                //Displays a message to the user and closes the Navigation Drawer
                Toast.makeText(getApplicationContext(), getString(R.string.signed_out), Toast.LENGTH_LONG).show();

                intent = new Intent(getApplicationContext(), MainActivity.class);
                break;
        }

        //Opens the appropriate Activity
        if(intent != null){
            startActivity(intent);
            finish();
        }

        closeDrawer(GravityCompat.START);
        return true;
    }

    //Closes the Navigation Drawer
    protected void closeDrawer(int gravity){
        mDrawerLayout.closeDrawer(gravity);
    }
}