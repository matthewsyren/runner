package com.matthewsyren.runner.fragments;


import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.matthewsyren.runner.R;

public class SettingsFragment
        extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.distance_unit_preferences);
    }
}