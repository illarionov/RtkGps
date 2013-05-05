package ru0xdc.rtkgps.settings;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import ru0xdc.rtkgps.settings.widget.EarthTideCorrectionPreference;
import ru0xdc.rtkgps.view.MultiSelectListPreferenceWorkaround;
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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;


public class ProcessingOptions1Fragment extends PreferenceFragment {

	private static final boolean DBG = BuildConfig.DEBUG & true;
	static final String TAG = ProcessingOptions1Fragment.class.getSimpleName();

	static final String SHARED_PREFS_NAME = "Settings1";

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
	private CheckBoxPreference mRecDynamicsPref;
	private EarthTideCorrectionPreference mEarthTidesCorrPref;
	private ListPreference mIonosphereCorrectionPref;
	private ListPreference mTroposphereCorrectionPref;
	private ListPreference mSatEphemClockPref;
	private CheckBoxPreference mSatAntennaPcv, mReceiverAntennaPcv, mPhaseWindupCorrection,
		mExcludeEclipsing, mRaimFde;

	private ProcessingOptions mProcessingOptions;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.processing_options1);

		mProcessingOptions = readPrefs(getActivity());

		initSettings1();
	}

	@Override
    public void onResume() {
        super.onResume();
        reloadSummaries();
    }

    private final OnPreferenceChangeListener mOnPreferenceChangeListener = new OnPreferenceChangeListener() {

		@SuppressWarnings("unchecked")
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (mPositioningModePref.equals(preference)) {
				mProcessingOptions.setPositioningMode(PositioningMode.valueOf(newValue.toString()));
			}else if(mNumberOfFrequenciesPref.equals(preference)) {
				mProcessingOptions.setNumberOfFrequencies(Integer.valueOf(newValue.toString()));
			}else if (mNavigationSystem.equals(preference)) {
				final Set<NavigationSystem> navsys = EnumSet.noneOf(NavigationSystem.class);
				final Set<String> vSet = (Set<String>)newValue;
				for (Object v0: vSet.toArray()) {
					Log.v(TAG, v0.toString());
					navsys.add(NavigationSystem.valueOf(v0.toString()));
				}
		    	mProcessingOptions.setNavigationSystem(navsys);
			}else if(mElevationMaskPref.equals(preference)) {
				mProcessingOptions.setElevationMask(Math.toRadians(Double.valueOf(newValue.toString())));
			}else if (mSnrMaskPref.equals(preference)) {
				mProcessingOptions.setSnrMask(Integer.valueOf(newValue.toString()));
			}else if (mRecDynamicsPref.equals(preference)) {
				mProcessingOptions.setRecDynamics((Boolean)newValue);
			}else if (mEarthTidesCorrPref.equals(preference)) {
				mProcessingOptions.setEarthTidesCorrection(EarthTideCorrectionType.valueOf(newValue.toString()));
			}else if (mIonosphereCorrectionPref.equals(preference)) {
				mProcessingOptions.setIonosphereCorrection(IonosphereOption.valueOf(newValue.toString()));
			}else if (mTroposphereCorrectionPref.equals(preference)) {
				mProcessingOptions.setTroposphereCorrection(TroposphereOption.valueOf(newValue.toString()));
			}else if (mSatEphemClockPref.equals(preference)) {
				mProcessingOptions.setSatEphemerisOption(EphemerisOption.valueOf(newValue.toString()));
			}else if (mSatAntennaPcv.equals(preference)) {
				mProcessingOptions.setSatAntennaPcvEnabled((Boolean)newValue);
			}else if (mReceiverAntennaPcv.equals(preference)) {
				mProcessingOptions.setReceiverAntennaPcvEnabled((Boolean)newValue);
			}else if (mPhaseWindupCorrection.equals(preference)) {
				mProcessingOptions.setPhaseWindupCorrectionEnabled((Boolean)newValue);
			}else if (mExcludeEclipsing.equals(preference)) {
				mProcessingOptions.setExcludeEclipsingSatMeasurements((Boolean)newValue);
			}else if (mRaimFde.equals(preference)) {
				mProcessingOptions.setRaimFdeEnabled((Boolean)newValue);
			}else {
				throw new IllegalStateException();
			}

			reloadSummaries();
			return true;
		}
    };

    @SuppressLint("NewApi")
	private void initSettings1() {
    	final Resources r = getResources();

    	// Positioning mode
		mPositioningModePref = (ListPreference)findPreference(KEY_POSITIONING_MODE);
		mPositioningModePref.setEntries(PositioningMode.getEntries(r));
		mPositioningModePref.setEntryValues(PositioningMode.getEntryValues());
		mPositioningModePref.setValue(mProcessingOptions.getPositioningMode().name());
		mPositioningModePref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// Number of frequencies
		mNumberOfFrequenciesPref = (ListPreference)findPreference(KEY_NUMBER_OF_FREQUENCIES);
		mNumberOfFrequenciesPref.setValue(String.valueOf(mProcessingOptions.getNumberOfFrequencies()));
		mNumberOfFrequenciesPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// Navigation system
		mNavigationSystem = (MultiSelectListPreferenceWorkaround)findPreference(KEY_NAVIGATION_SYSTEM);
		mNavigationSystem.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
		mNavigationSystem.setEntries(NavigationSystem.getEntries(r));
		mNavigationSystem.setEntryValues(NavigationSystem.getEntryValues());
		final Set<String> navsysSet = new HashSet<String>(7);
		for (NavigationSystem ns: mProcessingOptions.getNavigationSystem()) {
			navsysSet.add(ns.name());
		}
		mNavigationSystem.setValues(navsysSet);

		// Elevation mask
		mElevationMaskPref = (ListPreference)findPreference(KEY_ELEVATION_MASK);
		int elmin = (int)Math.toDegrees(mProcessingOptions.getElevationMask());
		elmin = (elmin / 5) * 5;
		mElevationMaskPref.setValue(String.valueOf(elmin));
		mElevationMaskPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// SNR mask
		mSnrMaskPref = (ListPreference)findPreference(KEY_SNR_MASK);
		mSnrMaskPref.setValue(String.valueOf(mProcessingOptions.getSnrMask()));
		mSnrMaskPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// Rec dynamics
		mRecDynamicsPref = (CheckBoxPreference)findPreference(KEY_REC_DYNAMICS);
		mRecDynamicsPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
		mRecDynamicsPref.setChecked(mProcessingOptions.getRecDynamics());

		// Earth tides correction
		mEarthTidesCorrPref = (EarthTideCorrectionPreference)findPreference(KEY_EARTH_TIDES_CORRECTION);
		mEarthTidesCorrPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// Ionosphere correction
		mIonosphereCorrectionPref = (ListPreference)findPreference(KEY_IONOSPHERE_CORRECTION);
		mIonosphereCorrectionPref.setEntries(IonosphereOption.getEntries(r));
		mIonosphereCorrectionPref.setEntryValues(IonosphereOption.getEntryValues());
		mIonosphereCorrectionPref.setValue(mProcessingOptions.getIonosphereCorrection().name());
		mIonosphereCorrectionPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// Troposphere correction
		mTroposphereCorrectionPref = (ListPreference)findPreference(KEY_TROPOSPHERE_CORRECTION);
		mTroposphereCorrectionPref.setEntries(TroposphereOption.getEntries(r));
		mTroposphereCorrectionPref.setEntryValues(TroposphereOption.getEntryValues());
		mTroposphereCorrectionPref.setValue(mProcessingOptions.getTroposphereCorrection().name());
		mTroposphereCorrectionPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// Satellite Ephemeris/Clock
		mSatEphemClockPref = (ListPreference)findPreference(KEY_SAT_EPHEM_CLOCK);
		mSatEphemClockPref.setEntries(EphemerisOption.getEntries(r));
		mSatEphemClockPref.setEntryValues(EphemerisOption.getEntryValues());
		mSatEphemClockPref.setValue(mProcessingOptions.getSatEphemerisOption().name());
		mSatEphemClockPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// Satellite antenna PCV
		mSatAntennaPcv = (CheckBoxPreference)findPreference(KEY_SAT_ANTENNA_PCV);
		mSatAntennaPcv.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
		mSatAntennaPcv.setChecked(mProcessingOptions.isSatAntennaPcvEnabled());

		// Receiver antenna PCV
		mReceiverAntennaPcv = (CheckBoxPreference)findPreference(KEY_RECEIVER_ANTENNA_PCV);
		mReceiverAntennaPcv.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
		mReceiverAntennaPcv.setChecked(mProcessingOptions.isReceiverAntennaPcvEnabled());

		// Phase windup correction
		mPhaseWindupCorrection = (CheckBoxPreference)findPreference(KEY_PHASE_WINDUP_CORRECTION);
		mPhaseWindupCorrection.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
		mPhaseWindupCorrection.setChecked(mProcessingOptions.isPhaseWindupCorrectionEnabled());

		// Exclude eclipsing satellite
		mExcludeEclipsing = (CheckBoxPreference)findPreference(KEY_EXCLUDE_ECLIPSING);
		mExcludeEclipsing.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
		mExcludeEclipsing.setChecked(mProcessingOptions.isExcludeEclipsingSatMeasurements());

		// RAIM FDE
		mRaimFde = (CheckBoxPreference)findPreference(KEY_RAIM_FDE);
		mRaimFde.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
		mRaimFde.setChecked(mProcessingOptions.isRaimFdeEnabled());

    }

    private void reloadSummaries() {
    	final Resources r = getResources();
    	CharSequence summary;

    	summary = r.getString(mProcessingOptions.getPositioningMode().getNameResId());
    	mPositioningModePref.setSummary(summary);

    	summary = r.getStringArray(R.array.procopt_number_of_frequencies_entries)
    			[mProcessingOptions.getNumberOfFrequencies()-1];
    	mNumberOfFrequenciesPref.setSummary(summary);

    	final ArrayList<String> navsys = new ArrayList<String>(10);
    	for (NavigationSystem ns: mProcessingOptions.getNavigationSystem()) {
    		navsys.add(r.getString(ns.getNameResId()));
    	}
    	summary = TextUtils.join(", ",  navsys);
    	mNavigationSystem.setSummary(summary);

    	int elmin = (int)Math.toDegrees(mProcessingOptions.getElevationMask());
    	elmin = (elmin / 5) * 5;
    	mElevationMaskPref.setSummary(String.valueOf(elmin));

    	summary = String.valueOf(mProcessingOptions.getSnrMask());
    	mSnrMaskPref.setSummary(summary);

    	summary = r.getString(mProcessingOptions.getIonosphereCorrection().getNameResId());
    	mIonosphereCorrectionPref.setSummary(summary);

    	summary = r.getString(mProcessingOptions.getTroposphereCorrection().getNameResId());
    	mTroposphereCorrectionPref.setSummary(summary);

    	summary = r.getString(mProcessingOptions.getSatEphemerisOption().getNameResId());
    	mSatEphemClockPref.setSummary(summary);

    	summary = r.getString(mProcessingOptions.getEarthTidersCorrection().getNameResId());
    	mEarthTidesCorrPref.setSummary(summary);
    }

    public static ProcessingOptions readPrefs(Context ctx) {
    	final ProcessingOptions opts;
    	final SharedPreferences prefs;
    	String v;
    	Set<String> vSet;

    	opts = new ProcessingOptions();
    	prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Activity.MODE_PRIVATE);

    	v = prefs.getString(KEY_POSITIONING_MODE, null);
    	if (DBG) Log.v(TAG, "positioning mode: " + v);
    	if (v != null) opts.setPositioningMode(PositioningMode.valueOf(v));

    	v = prefs.getString(KEY_NUMBER_OF_FREQUENCIES, null);
    	if (v != null) opts.setNumberOfFrequencies(Integer.valueOf(v));

    	vSet = prefs.getStringSet(KEY_NAVIGATION_SYSTEM, null);
    	if (vSet != null) {
    		Set<NavigationSystem> navsys = EnumSet.noneOf(NavigationSystem.class);
    		for (String v0: vSet) navsys.add(NavigationSystem.valueOf(v0));
    		opts.setNavigationSystem(navsys);
    		if (DBG) Log.v(TAG, "navsystems: " + navsys.toString());
    	}

    	v = prefs.getString(KEY_ELEVATION_MASK, null);
    	if (v != null) opts.setElevationMask(Math.toRadians(Double.valueOf(v)));

    	v = prefs.getString(KEY_SNR_MASK, null);
    	if (v != null) opts.setSnrMask(Integer.valueOf(v));

    	opts.setRecDynamics(prefs.getBoolean(KEY_REC_DYNAMICS, opts.getRecDynamics()));

    	try {
    		v = prefs.getString(KEY_EARTH_TIDES_CORRECTION, null);
    	}catch (ClassCastException cce) {
    		cce.printStackTrace();
    		v = null;
    	}
    	if (v != null) opts.setEarthTidesCorrection(EarthTideCorrectionType.valueOf(v));

    	v = prefs.getString(KEY_IONOSPHERE_CORRECTION, null);
    	if (v != null) opts.setIonosphereCorrection(IonosphereOption.valueOf(v));

    	v = prefs.getString(KEY_TROPOSPHERE_CORRECTION, null);
    	if (v != null) opts.setTroposphereCorrection(TroposphereOption.valueOf(v));

    	v = prefs.getString(KEY_SAT_EPHEM_CLOCK, null);
    	if (v != null) opts.setSatEphemerisOption(EphemerisOption.valueOf(v));

    	opts.setSatAntennaPcvEnabled(prefs.getBoolean(KEY_SAT_ANTENNA_PCV, opts.isSatAntennaPcvEnabled()));
    	opts.setReceiverAntennaPcvEnabled(prefs.getBoolean(KEY_RECEIVER_ANTENNA_PCV, opts.isReceiverAntennaPcvEnabled()));
    	opts.setPhaseWindupCorrectionEnabled(prefs.getBoolean(KEY_PHASE_WINDUP_CORRECTION, opts.isPhaseWindupCorrectionEnabled()));
    	opts.setExcludeEclipsingSatMeasurements(prefs.getBoolean(KEY_EXCLUDE_ECLIPSING, opts.isExcludeEclipsingSatMeasurements()));
    	opts.setRaimFdeEnabled(prefs.getBoolean(KEY_RAIM_FDE, opts.isRaimFdeEnabled()));

    	return opts;
    }

}
