package com.matthewsyren.runner;

import android.os.Bundle;

public class HelpActivity
        extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        super.onCreateDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Sets the selected item to the help page
        super.setSelectedNavItem(R.id.nav_help);
    }
}