package ru0xdc.rtkgps.settings;

import java.util.List;

import ru0xdc.rtkgps.R;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

}
