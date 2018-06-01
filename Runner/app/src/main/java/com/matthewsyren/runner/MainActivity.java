package com.matthewsyren.runner;

import android.os.Bundle;

public class MainActivity
        extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        super.onCreateDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        super.setSelectedNavItem(R.id.nav_home);
    }
}