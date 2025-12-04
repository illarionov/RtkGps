package gpsplus.rtkgps.settings;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.utils.IP;

public class NTRIPCasterSettingsFragment  extends PreferenceFragment {
    private static final boolean DBG = BuildConfig.DEBUG & true;

    public static final String SHARED_PREFS_NAME = "CasterOptions";
    public static final String KEY_ENABLE_CASTER = "ntripcaster_enabled";
    public static final String KEY_BRUTAL_ENDING_CASTER = "ntripcaster_force_brutal_ending";
    private final PreferenceChangeListener mPreferenceChangeListener;
    private CheckBoxPreference mCasterEnableCheckbox;
    private CheckBoxPreference mCasterBrutalEnding;


    public NTRIPCasterSettingsFragment() {
        super();
        mPreferenceChangeListener = new PreferenceChangeListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.caster_options);

        initSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        reloadSummaries();
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (key.equalsIgnoreCase(KEY_ENABLE_CASTER) && sharedPreferences.getBoolean(NTRIPCasterSettingsFragment.KEY_ENABLE_CASTER, false) )
            {
                for (int i = 0; i < 2; i++) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.ntripcaster_options_warning),
                            Toast.LENGTH_LONG).show();
                }
            }
            reloadSummaries();
        }
    };

    private void initSettings() {
        final Resources r = getResources();
        mCasterEnableCheckbox = (CheckBoxPreference)findPreference(KEY_ENABLE_CASTER);
        mCasterBrutalEnding = (CheckBoxPreference)findPreference(KEY_BRUTAL_ENDING_CASTER);
        if (!DBG) mCasterBrutalEnding.setEnabled(false);
    }

    private void reloadSummaries() {
        mCasterEnableCheckbox.setSummary(IP.getIPAddress(true));
    }

}
