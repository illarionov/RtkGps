package gpsplus.rtkgps.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.settings.widget.SolutionFormatPreference;
import gpsplus.rtkgps.settings.widget.StreamTypePreference;
import gpsplus.rtklib.RtkServerSettings.OutputStream;
import gpsplus.rtklib.SolutionOptions;
import gpsplus.rtklib.constants.SolutionFormat;
import gpsplus.rtklib.constants.StreamFormat;
import gpsplus.rtklib.constants.StreamType;

import javax.annotation.Nonnull;


public class OutputSolution1Fragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    public static final String SHARED_PREFS_NAME = "OutputSolution1";

    protected static final String KEY_ENABLE = "enable";
    protected static final String KEY_TYPE = "type";
    protected static final String KEY_FORMAT = "format";
    protected static final String KEY_STREAM_SETTINGS_BUTTON = "stream_settings_button";

    private final PreferenceChangeListener mPreferenceChangeListener;

    private final StreamType INPUT_STREAM_TYPES[] = new StreamType[] {
        StreamType.TCPCLI,
        StreamType.NTRIPSVR,
        StreamType.FILE
    };

    static final StreamType DEFAULT_STREAM_TYPE = StreamType.FILE;

    private static final SolutionFormat SOLUTION_FORMATS[] = new SolutionFormat[] {
        SolutionFormat.LLH,
        SolutionFormat.XYZ,
        SolutionFormat.ENU,
        SolutionFormat.NMEA
    };

    private static final SolutionFormat DEFAULT_SOLUTION_FORMAT = SolutionFormat.LLH;

    private static final SettingsHelper.OutputStreamDefaults DEFAULTS = new SettingsHelper.OutputStreamDefaults();

    static {
        DEFAULTS.setFileClientDefaults(
                new StreamFileClientFragment.Value()
                    .setFilename("solution1_%Y%m%d%h%M%S.pos")
                );
    }


    public OutputSolution1Fragment() {
        mPreferenceChangeListener = new PreferenceChangeListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DBG) Log.v(getSharedPreferenceName(), "onCreate() bundle: " + savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(getSharedPreferenceName());

        initPreferenceScreen();

        findPreference(KEY_STREAM_SETTINGS_BUTTON).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                streamSettingsButtonClicked();
                return true;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(getSharedPreferenceName(), "onResume()");
        refresh();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(getSharedPreferenceName(), "onPause()");
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    protected void initPreferenceScreen() {
        final StreamTypePreference typePref;
        final SolutionFormatPreference formatPref;

        if (DBG) Log.v(getSharedPreferenceName(), "initPreferenceScreen()");

        addPreferencesFromResource(R.xml.output_stream_settings);

        typePref = (StreamTypePreference)findPreference(KEY_TYPE);
        typePref.setValues(INPUT_STREAM_TYPES);
        typePref.setDefaultValue(DEFAULT_STREAM_TYPE);

        formatPref = (SolutionFormatPreference)findPreference(KEY_FORMAT);
        formatPref.setValues(SOLUTION_FORMATS);
        formatPref.setDefaultValue(DEFAULT_SOLUTION_FORMAT);
    }

    protected void streamSettingsButtonClicked() {
        final Intent intent;
        final Bundle fragmentArgs;
        final StreamTypePreference typePref;

        intent = new Intent(getActivity(), StreamDialogActivity.class);

        typePref = (StreamTypePreference) findPreference(KEY_TYPE);

        fragmentArgs = new Bundle(1);
        fragmentArgs.putString(StreamDialogActivity.ARG_SHARED_PREFS_NAME, getSharedPreferenceName());

        intent.putExtra(StreamDialogActivity.ARG_FRAGMENT_ARGUMENTS, fragmentArgs);
        intent.putExtra(StreamDialogActivity.ARG_FRAGMENT_TYPE, typePref.getValueT().name());

        startActivity(intent);
    }

    private void refresh() {
        final StreamTypePreference typePref;
        final SolutionFormatPreference formatPref;
        final Preference settingsPref;

        if (DBG) Log.v(getSharedPreferenceName(), "refresh()");

        typePref = (StreamTypePreference) findPreference(KEY_TYPE);
        formatPref = (SolutionFormatPreference) findPreference(KEY_FORMAT);

        typePref.setSummary(getString(typePref.getValueT().getNameResId()));
        formatPref.setSummary(getString(formatPref.getValueT().getNameResId()));

        settingsPref = findPreference(KEY_STREAM_SETTINGS_BUTTON);
        settingsPref.setSummary(SettingsHelper.readOutputStreamSumary(getResources(), getPreferenceManager().getSharedPreferences()));

    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            refresh();
        }
    };

    protected void setDefaultValues(boolean force) {

        if (DBG) Log.v(getSharedPreferenceName(), "setDefaultValues()");

        final SharedPreferences prefs;

        prefs = getPreferenceManager().getSharedPreferences();

        final boolean needUpdate = force || !prefs.contains(KEY_ENABLE);

        if (needUpdate) {
            SharedPreferences.Editor e = prefs.edit();
            e.putBoolean(KEY_ENABLE, false)
            .putString(KEY_TYPE, StreamType.NTRIPSVR.name())
            .putString(KEY_FORMAT, StreamFormat.RTCM3.name())
            ;
            e.commit();
        }
    }

    @Nonnull
    public static OutputStream readPrefs(Context ctx, @Nonnull SolutionOptions base) {
        return SettingsHelper.readOutputStreamPrefs(ctx, SHARED_PREFS_NAME, base);
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SettingsHelper.setOutputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }
}
