package gpsplus.rtkgps.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.SolutionOptions;
import gpsplus.rtklib.constants.GeoidModel;
import gpsplus.rtklib.constants.TimeSystem;

public class SolutionOutputSettingsFragment extends PreferenceFragment {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = SolutionOutputSettingsFragment.class.getSimpleName();

    public static final String SHARED_PREFS_NAME = "SolutionOutputSettings";

    public static final String KEY_OUTPUT_HEADER = "output_header";
    public static final String KEY_TIME_FORMAT = "time_format";
    public static final String KEY_LAT_LON_FORMAT = "lat_lon_format";
    public static final String KEY_FIELD_SEPARATOR = "field_separator";
    public static final String KEY_HEIGHT = "height";
    public static final String KEY_GEOID_MODEL = "geoid_model";
    public static final String KEY_NMEA_INTERVAL_RMC_GGA = "nmea_interval_rmc_gga";
    public static final String KEY_NMEA_INTERVAL_GSA_GSV = "nmea_interval_gsa_gsv";
    public static final String KEY_OUTPUT_SOLUTION_STATUS = "output_solution_status";
    public static final String KEY_DEBUG_TRACE ="debug_trace";
    public static final String KEY_OUTPUT_MOCK_LOCATION = "output_mocklocation";
    public static final String KEY_ENABLE_TEST_MODE = "enable_testmode";
    public static final String KEY_CUSTOM_PROJ4 = "customproj4";

    private CheckBoxPreference mOutputHeaderPref;
    private ListPreference mTimeFormatPref;
    private ListPreference mLatLonFormatPref;
    private EditTextPreference mFieldSeparatorPref;
    private ListPreference mHeightPref;
    private ListPreference mGeoidModelPref;
    private EditTextPreference mNmeaIntervalRmcPref;
    private EditTextPreference mNmeaIntervalGsaPref;
    private ListPreference mOutputSolutionStatusPref;
    private ListPreference mDebugTracePref;


    private SolutionOptions mSolutionOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);

        addPreferencesFromResource(R.xml.solution_output_settings);

        mSolutionOptions = readPrefs(getActivity());

        initSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadSummaries();
    }

    public static SolutionOptions readPrefs(Context ctx) {
        final SolutionOptions opts;
        final SharedPreferences prefs;
        String v;
        mSzEllipsoidal = ctx.getResources().getStringArray(R.array.solopt_height_entries)[0];
        mSzGeodetic = ctx.getResources().getStringArray(R.array.solopt_height_entries)[1];

        opts = new SolutionOptions();
        prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Activity.MODE_PRIVATE);

        try {
            opts.setOutHead(prefs.getBoolean(KEY_OUTPUT_HEADER, opts.getOutHead()));
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_TIME_FORMAT, null);
            if (v != null) opts.setTimeSystem(TimeSystem.valueOf(v));
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_LAT_LON_FORMAT, null);
            if (v != null) {
                if (!"degree".equals(v) && !"dms".equals(v)) {
                    throw new IllegalArgumentException("Wrong lat_lon format");
                }
                opts.setLatLonFormat("degree".equals(v) ? 0 : 1);
            }
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_FIELD_SEPARATOR, null);
            if (v != null) opts.setFieldSeparator(v);
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_HEIGHT, null);
            if (v != null) {

                if (!mSzEllipsoidal.equals(v) && !mSzGeodetic.equals(v)) {
                    throw new IllegalArgumentException("Wrong height");
                }
                opts.setIsEllipsoidalHeight(mSzEllipsoidal.equals(v));
            }
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_GEOID_MODEL, null);
            if (v != null) opts.setGeoidModel(GeoidModel.valueOf(v));
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_NMEA_INTERVAL_RMC_GGA, null);
            if (v != null) opts.setNmeaIntervalRmcGga(Double.valueOf(v));
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }catch(NumberFormatException nfe) {
            nfe.printStackTrace();
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_NMEA_INTERVAL_GSA_GSV, null);
            if (v != null) opts.setNmeaIntervalGsv(Double.valueOf(v));
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }catch(NumberFormatException nfe) {
            nfe.printStackTrace();
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_OUTPUT_SOLUTION_STATUS, null);
            if (v != null) opts.setSolutionStatsLevel(Integer.valueOf(v));
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }catch(NumberFormatException nfe) {
            nfe.printStackTrace();
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }

        try {
            v = prefs.getString(KEY_DEBUG_TRACE, null);
            if (v != null) opts.setDebugTraceLevel(Integer.valueOf(v));
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }catch(NumberFormatException nfe) {
            nfe.printStackTrace();
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }


        return opts;
    }

    private void initSettings() {
        final Resources r = getResources();

        mOutputHeaderPref = (CheckBoxPreference)findPreference(KEY_OUTPUT_HEADER);
        mOutputHeaderPref.setChecked(mSolutionOptions.getOutHead());
        mOutputHeaderPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mTimeFormatPref = (ListPreference)findPreference(KEY_TIME_FORMAT);
        mTimeFormatPref.setEntries(TimeSystem.getEntries(r));
        mTimeFormatPref.setEntryValues(TimeSystem.getEntryValues());
        mTimeFormatPref.setValue(mSolutionOptions.getTimeSystem().name());
        mTimeFormatPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mLatLonFormatPref = (ListPreference)findPreference(KEY_LAT_LON_FORMAT);
        mLatLonFormatPref.setValue(mSolutionOptions.getLatLonFormat() == 0 ? "degree" : "dms");
        mLatLonFormatPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mFieldSeparatorPref = (EditTextPreference)findPreference(KEY_FIELD_SEPARATOR);
        mFieldSeparatorPref.setText(mSolutionOptions.getFieldSeparator());
        mFieldSeparatorPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mHeightPref = (ListPreference)findPreference(KEY_HEIGHT);
        mHeightPref.setValue(mSolutionOptions.isEllipsoidalHeight() ? mSzEllipsoidal : mSzGeodetic);
        mHeightPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mGeoidModelPref = (ListPreference)findPreference(KEY_GEOID_MODEL);
        mGeoidModelPref.setEntries(GeoidModel.getEntries(r));
        mGeoidModelPref.setEntryValues(GeoidModel.getEntryValues());
        mGeoidModelPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        // TODO
        mGeoidModelPref.setEnabled(true);

        mNmeaIntervalGsaPref = (EditTextPreference)findPreference(KEY_NMEA_INTERVAL_GSA_GSV);
        mNmeaIntervalGsaPref.setText(String.valueOf(mSolutionOptions.getNmeaIntervalGsv()));
        mNmeaIntervalGsaPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mNmeaIntervalRmcPref = (EditTextPreference)findPreference(KEY_NMEA_INTERVAL_RMC_GGA);
        mNmeaIntervalRmcPref.setText(String.valueOf(mSolutionOptions.getNmeaIntervalRmcGga()));
        mNmeaIntervalRmcPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mOutputSolutionStatusPref = (ListPreference)findPreference(KEY_OUTPUT_SOLUTION_STATUS);
        mOutputSolutionStatusPref.setValue(String.valueOf(mSolutionOptions.getSolutionStatsLevel()));
        mOutputSolutionStatusPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mDebugTracePref = (ListPreference)findPreference(KEY_DEBUG_TRACE);
        mDebugTracePref.setValue(String.valueOf(mSolutionOptions.getDebugTraceLevel()));
        mDebugTracePref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

    }

    private void reloadSummaries() {
        final Resources r = getResources();
        CharSequence summary;

        summary = r.getString(mSolutionOptions.getTimeSystem().getNameResId());
        mTimeFormatPref.setSummary(summary);

        summary = r.getStringArray(R.array.solopt_lat_lon_format_entries)[
                                                                          mSolutionOptions.getLatLonFormat()];
        mLatLonFormatPref.setSummary(summary);

        mFieldSeparatorPref.setSummary(mSolutionOptions.getFieldSeparator());

        summary = r.getStringArray(R.array.solopt_height_entry_values)[
                                                                       mSolutionOptions.isEllipsoidalHeight() ? 0 : 1];
        mHeightPref.setSummary(summary);

        summary = r.getString(mSolutionOptions.getGeoidModel().getNameResId());
        mGeoidModelPref.setSummary(summary);

        mNmeaIntervalGsaPref.setSummary(String.valueOf(mSolutionOptions.getNmeaIntervalGsv()));

        mNmeaIntervalRmcPref.setSummary(String.valueOf(mSolutionOptions.getNmeaIntervalRmcGga()));

        summary = r.getStringArray(R.array.solopt_output_solution_status_entries
                )[mSolutionOptions.getSolutionStatsLevel()];
        mOutputSolutionStatusPref.setSummary(summary);

        summary = r.getStringArray(R.array.solopt_debug_trace_entries
                )[mSolutionOptions.getDebugTraceLevel()];
        mDebugTracePref.setSummary(summary);

    }

    private final OnPreferenceChangeListener mOnPreferenceChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            if (mOutputHeaderPref.equals(preference)) {
                mSolutionOptions.setOutHead((Boolean)newValue);
            }else if (mTimeFormatPref.equals(preference)) {
                mSolutionOptions.setTimeSystem(TimeSystem.valueOf(String.valueOf(newValue)));
            }else if (mLatLonFormatPref.equals(preference)) {
                mSolutionOptions.setLatLonFormat(TextUtils.equals((CharSequence)newValue, "dms") ? 1 : 0);
            }else if (mFieldSeparatorPref.equals(preference)) {
                mSolutionOptions.setFieldSeparator(newValue.toString());
            }else if (mHeightPref.equals(preference)) {
                mSolutionOptions.setIsEllipsoidalHeight(TextUtils.equals((CharSequence)newValue, mSzEllipsoidal));
            }else if (mGeoidModelPref.equals(preference)) {
                mSolutionOptions.setGeoidModel(GeoidModel.valueOf(newValue.toString()));
            }else if (mNmeaIntervalRmcPref.equals(preference)) {
                mSolutionOptions.setNmeaIntervalRmcGga(Double.valueOf(newValue.toString()));
            }else if (mNmeaIntervalGsaPref.equals(preference)) {
                mSolutionOptions.setNmeaIntervalGsv(Double.valueOf(newValue.toString()));
            }else if (mOutputSolutionStatusPref.equals(preference)) {
                mSolutionOptions.setSolutionStatsLevel(Integer.valueOf(newValue.toString()));
            }else if (mDebugTracePref.equals(preference)) {
                mSolutionOptions.setDebugTraceLevel(Integer.valueOf(newValue.toString()));
            }
            reloadSummaries();
            return true;
        }
    };
    private static String mSzEllipsoidal;
    private static String mSzGeodetic;
}
