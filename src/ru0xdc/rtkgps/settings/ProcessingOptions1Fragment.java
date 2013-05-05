package ru0xdc.rtkgps.settings;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import ru0xdc.rtkgps.settings.widget.EarthTideCorrectionPreference;
import ru0xdc.rtkgps.settings.widget.MultiSelectListPreferenceWorkaround;
import ru0xdc.rtklib.ProcessingOptions;
import ru0xdc.rtklib.constants.EarthTideCorrectionType;
import ru0xdc.rtklib.constants.EphemerisOption;
import ru0xdc.rtklib.constants.IonosphereOption;
import ru0xdc.rtklib.constants.NavigationSystem;
import ru0xdc.rtklib.constants.PositioningMode;
import ru0xdc.rtklib.constants.TroposphereOption;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;


public class ProcessingOptions1Fragment extends PreferenceFragment {

	private static final boolean DBG = BuildConfig.DEBUG & true;
	static final String TAG = ProcessingOptions1Fragment.class.getSimpleName();

	static final String SHARED_PREFS_NAME = "ProcessingOptions1";

	static final String KEY_POSITIONING_MODE = "positioning_mode";
	static final String KEY_NUMBER_OF_FREQUENCIES = "number_of_frequencies";
	static final String KEY_NAVIGATION_SYSTEM = "navigation_system";
	static final String KEY_ELEVATION_MASK = "elevation_mask";
	static final String KEY_SNR_MASK = "snr_mask";
	static final String KEY_REC_DYNAMICS = "rec_dynamics";
	static final String KEY_EARTH_TIDES_CORRECTION = "rec_earth_tides_correction";
	static final String KEY_IONOSPHERE_CORRECTION = "ionosphere_correction";
	static final String KEY_TROPOSPHERE_CORRECTION = "troposphere_correction";
	static final String KEY_SAT_EPHEM_CLOCK = "satellite_ephemeris_clock";
	static final String KEY_SAT_ANTENNA_PCV = "sat_antenna_pcv";
	static final String KEY_RECEIVER_ANTENNA_PCV = "receiver_antenna_pcv";
	static final String KEY_PHASE_WINDUP_CORRECTION = "phase_windup_correction";
	static final String KEY_EXCLUDE_ECLIPSING = "exclude_eclipsing_sat_measurements";
	static final String KEY_RAIM_FDE = "raim_fde";

	// Settings 1
	private ListPreference mPositioningModePref;
	private ListPreference mNumberOfFrequenciesPref;
	private MultiSelectListPreferenceWorkaround mNavigationSystem;
	private ListPreference mElevationMaskPref;
	private ListPreference mSnrMaskPref;
	private EarthTideCorrectionPreference mEarthTidesCorrPref;
	private ListPreference mIonosphereCorrectionPref;
	private ListPreference mTroposphereCorrectionPref;
	private ListPreference mSatEphemClockPref;

	private final PreferenceChangeListener mPreferenceChangeListener;

	public ProcessingOptions1Fragment() {
		mPreferenceChangeListener = new PreferenceChangeListener();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.processing_options1);

		initSettings1();
	}

	@Override
    public void onResume() {
        super.onResume();
        reloadSummaries();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

	@Override
	public void onPause() {
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
		super.onPause();
	}

    @SuppressLint("NewApi")
	private void initSettings1() {
    	final Resources r = getResources();
    	ProcessingOptions opts = readPrefs(getActivity());

    	// Positioning mode
		mPositioningModePref = (ListPreference)findPreference(KEY_POSITIONING_MODE);
		mPositioningModePref.setEntries(PositioningMode.getEntries(r));
		mPositioningModePref.setEntryValues(PositioningMode.getEntryValues());

		// Number of frequencies
		mNumberOfFrequenciesPref = (ListPreference)findPreference(KEY_NUMBER_OF_FREQUENCIES);

		// Navigation system
		mNavigationSystem = (MultiSelectListPreferenceWorkaround)findPreference(KEY_NAVIGATION_SYSTEM);
		mNavigationSystem.setEntries(NavigationSystem.getEntries(r));
		mNavigationSystem.setEntryValues(NavigationSystem.getEntryValues());
		final Set<String> navsysSet = new HashSet<String>(7);
		for (NavigationSystem ns: opts.getNavigationSystem()) {
			navsysSet.add(ns.name());
		}
		mNavigationSystem.setValues(navsysSet);

		// Elevation mask
		mElevationMaskPref = (ListPreference)findPreference(KEY_ELEVATION_MASK);

		// Earth tides correction
		mEarthTidesCorrPref = (EarthTideCorrectionPreference)findPreference(KEY_EARTH_TIDES_CORRECTION);

		// Ionosphere correction
		mIonosphereCorrectionPref = (ListPreference)findPreference(KEY_IONOSPHERE_CORRECTION);
		mIonosphereCorrectionPref.setEntries(IonosphereOption.getEntries(r));
		mIonosphereCorrectionPref.setEntryValues(IonosphereOption.getEntryValues());

		// Troposphere correction
		mTroposphereCorrectionPref = (ListPreference)findPreference(KEY_TROPOSPHERE_CORRECTION);
		mTroposphereCorrectionPref.setEntries(TroposphereOption.getEntries(r));
		mTroposphereCorrectionPref.setEntryValues(TroposphereOption.getEntryValues());

		// Satellite Ephemeris/Clock
		mSatEphemClockPref = (ListPreference)findPreference(KEY_SAT_EPHEM_CLOCK);
		mSatEphemClockPref.setEntries(EphemerisOption.getEntries(r));
		mSatEphemClockPref.setEntryValues(EphemerisOption.getEntryValues());

		mSnrMaskPref = (ListPreference)findPreference(KEY_SNR_MASK);
    }

    private void reloadSummaries() {
    	final Resources r = getResources();
    	CharSequence summary;

    	summary = mPositioningModePref.getEntry();
    	mPositioningModePref.setSummary(summary);

    	summary = mNumberOfFrequenciesPref.getEntry();
    	mNumberOfFrequenciesPref.setSummary(summary);

    	final ArrayList<String> navsys = new ArrayList<String>(10);
    	final EnumSet<NavigationSystem> navsys0 = EnumSet.noneOf(NavigationSystem.class);
    	// sort
    	for (String v: mNavigationSystem.getValues()) navsys0.add(NavigationSystem.valueOf(v));

    	for (NavigationSystem ns: navsys0) navsys.add(r.getString(ns.getNameResId()));

    	summary = TextUtils.join(", ",  navsys);
    	mNavigationSystem.setSummary(summary);

    	summary = mElevationMaskPref.getEntry();
    	mElevationMaskPref.setSummary(summary);

    	summary = mSnrMaskPref.getEntry();
    	mSnrMaskPref.setSummary(summary);

    	mIonosphereCorrectionPref.setSummary(mIonosphereCorrectionPref.getEntry());
    	mTroposphereCorrectionPref.setSummary(mTroposphereCorrectionPref.getEntry());
    	mSatEphemClockPref.setSummary(mSatEphemClockPref.getEntry());
    	mEarthTidesCorrPref.setSummary(mEarthTidesCorrPref.getEntry());
    }

    public static ProcessingOptions readPrefs(Context ctx) {
    	final ProcessingOptions opts;
    	final SharedPreferences prefs;
    	String v;
    	Set<String> vSet;

    	opts = new ProcessingOptions();
    	prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Activity.MODE_PRIVATE);

    	v = prefs.getString(KEY_POSITIONING_MODE, null);
    	opts.setPositioningMode(PositioningMode.valueOf(v));

    	v = prefs.getString(KEY_NUMBER_OF_FREQUENCIES, null);
    	opts.setNumberOfFrequencies(Integer.valueOf(v));

    	vSet = prefs.getStringSet(KEY_NAVIGATION_SYSTEM, null);
    	Set<NavigationSystem> navsys = EnumSet.noneOf(NavigationSystem.class);
    	for (String v0: vSet) navsys.add(NavigationSystem.valueOf(v0));
    	opts.setNavigationSystem(navsys);

    	v = prefs.getString(KEY_ELEVATION_MASK, null);
    	opts.setElevationMask(Math.toRadians(Double.valueOf(v)));

    	v = prefs.getString(KEY_SNR_MASK, null);
    	opts.setSnrMask(Integer.valueOf(v));

    	opts.setRecDynamics(prefs.getBoolean(KEY_REC_DYNAMICS, opts.getRecDynamics()));

   		v = prefs.getString(KEY_EARTH_TIDES_CORRECTION, null);
    	opts.setEarthTidesCorrection(EarthTideCorrectionType.valueOf(v));

    	v = prefs.getString(KEY_IONOSPHERE_CORRECTION, null);
    	opts.setIonosphereCorrection(IonosphereOption.valueOf(v));

    	v = prefs.getString(KEY_TROPOSPHERE_CORRECTION, null);
    	opts.setTroposphereCorrection(TroposphereOption.valueOf(v));

    	v = prefs.getString(KEY_SAT_EPHEM_CLOCK, null);
    	opts.setSatEphemerisOption(EphemerisOption.valueOf(v));

    	opts.setSatAntennaPcvEnabled(prefs.getBoolean(KEY_SAT_ANTENNA_PCV, opts.isSatAntennaPcvEnabled()));
    	opts.setReceiverAntennaPcvEnabled(prefs.getBoolean(KEY_RECEIVER_ANTENNA_PCV, opts.isReceiverAntennaPcvEnabled()));
    	opts.setPhaseWindupCorrectionEnabled(prefs.getBoolean(KEY_PHASE_WINDUP_CORRECTION, opts.isPhaseWindupCorrectionEnabled()));
    	opts.setExcludeEclipsingSatMeasurements(prefs.getBoolean(KEY_EXCLUDE_ECLIPSING, opts.isExcludeEclipsingSatMeasurements()));
    	opts.setRaimFdeEnabled(prefs.getBoolean(KEY_RAIM_FDE, opts.isRaimFdeEnabled()));

    	return opts;
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			reloadSummaries();
		}
    };

    public static void setValue(Context ctx, ProcessingOptions opts) {
    	SharedPreferences prefs;

		prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

		Set<String> navSystemSet = new HashSet<String>();
		for (NavigationSystem ns: opts.getNavigationSystem()) navSystemSet.add(ns.name());

		prefs
			.edit()
			.putString(KEY_POSITIONING_MODE, opts.getPositioningMode().name())
			.putString(KEY_NUMBER_OF_FREQUENCIES, String.valueOf(opts.getNumberOfFrequencies()))
			.putStringSet(KEY_NAVIGATION_SYSTEM, navSystemSet)
			.putString(KEY_ELEVATION_MASK, String.valueOf(Math.toDegrees(opts.getElevationMask())))
			.putString(KEY_SNR_MASK, String.valueOf(opts.getSnrMask()))
			.putBoolean(KEY_REC_DYNAMICS, opts.getRecDynamics())
			.putString(KEY_EARTH_TIDES_CORRECTION, opts.getEarthTidersCorrection().name())
			.putString(KEY_IONOSPHERE_CORRECTION, opts.getIonosphereCorrection().name())
			.putString(KEY_TROPOSPHERE_CORRECTION, opts.getTroposphereCorrection().name())
			.putString(KEY_SAT_EPHEM_CLOCK, opts.getSatEphemerisOption().name())
			.putBoolean(KEY_SAT_ANTENNA_PCV, opts.isSatAntennaPcvEnabled())
			.putBoolean(KEY_RECEIVER_ANTENNA_PCV, opts.isReceiverAntennaPcvEnabled())
			.putBoolean(KEY_PHASE_WINDUP_CORRECTION, opts.isPhaseWindupCorrectionEnabled())
			.putBoolean(KEY_EXCLUDE_ECLIPSING, opts.isExcludeEclipsingSatMeasurements())
			.putBoolean(KEY_RAIM_FDE, opts.isRaimFdeEnabled())
			.commit()
			;
		if (DBG) {
			ProcessingOptions opts2 = readPrefs(ctx);
			if (!opts.equals(opts2)) {
				Log.e(TAG, "saved processing options differ from the original");
			}
		}
    }

    public static void setDefaultValues(Context ctx, boolean force) {
    	SharedPreferences prefs;

		prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

		final boolean needUpdate = force || !prefs.contains(InputRoverFragment.KEY_ENABLE);

		if (needUpdate) {
			setValue(ctx, new ProcessingOptions());
		}
    }

}
