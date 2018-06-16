package com.matthewsyren.runner.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import com.matthewsyren.runner.R;
import com.matthewsyren.runner.utilities.WidgetUtilities;

public class SettingsFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.distance_unit_preferences);

        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(isAdded()){
            //Displays a message and updates the Widgets
            Toast.makeText(getContext(), getString(R.string.settings_updated), Toast.LENGTH_LONG).show();
            WidgetUtilities.updateWidgets(getContext());
        }
    }
}