package gpsplus.rtkgps.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import gpsplus.rtkgps.R;

public class NTRIPCasterSettingsFragment  extends PreferenceFragment {
    public static final String SHARED_PREFS_NAME = "CasterOptions";
    public static final String KEY_ENABLE_CASTER = "ntripcaster_enabled";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.caster_options);

    }

}
