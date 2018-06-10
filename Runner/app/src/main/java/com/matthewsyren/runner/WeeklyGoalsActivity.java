package com.matthewsyren.runner;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

public class WeeklyGoalsActivity
        extends BaseActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_goals);
        super.onCreateDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Sets the selected item in the Navigation Drawer to the weekly goals page
        super.setSelectedNavItem(R.id.nav_weekly_goals);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_weekly_goals, menu);
        return super.onCreateOptionsMenu(menu);
    }
}