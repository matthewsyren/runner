package com.matthewsyren.runner;

import android.os.Bundle;

public class SettingsActivity
        extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        super.onCreateDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Sets the selected item in the Navigation Drawer to the settings page
        super.setSelectedNavItem(R.id.nav_settings);
    }
}