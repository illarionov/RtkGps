package gpsplus.rtkgps.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import javax.annotation.Nonnull;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.MainActivity;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.constants.StreamType;

public class StreamMobileMapperFragment extends PreferenceFragment {

    public static final String MOBILEMAPPER_INTERNAL_SENSOR_STR = "U-Blox M8030";
    public static final String MOBILEMAPPER_INTERNAL_SENSOR_POGOPIN_PORT = "/dev/ttyHSL1";
    public static final String MOBILEMAPPER_RAW_AUTOCAPTURE = "stream_mobilemapper_autocapture";
    public static final String MOBILEMAPPER_DYNAMIC_MODEL = "stream_mobilemapper_dynmodel";
    public static final String MOBILEMAPPER_FORCE_COLDSTART = "stream_mobilemapper_force_coldstart";
    public static final String MOBILEMAPPER_ENABLE_NATIVE_SBAS = "stream_mobilemapper_enable_native_sbas";
    public static final String MOBILEMAPPER_DISBALE_NMEA = "stream_mobilemapper_disable_NMEA";
    private static final boolean DBG = BuildConfig.DEBUG & true;

    private String mSharedPrefsName;


    public static final class Value implements TransportSettings {

        private String mPath;
        private int mDynamicModel = 1;
        private boolean mAutocapture = false;
        private boolean mForceColdStart = true;
        private boolean mEnableNativeSBAS = true;
        private boolean mDisableNMEA = true;

        public Value() {
            mPath = null;
        }

        @Override
        public StreamType getType() {
            return StreamType.MOBILEMAPPER;
        }

        @Override
        public String getPath() {
            if (mPath == null) throw new IllegalStateException("Path not initialized. Call updatePath()");
            return mPath;
        }

        public void updatePath(Context context, String sharedPrefsName) {
            mPath = MainActivity.getLocalSocketPath(context,
                    mobileMapperLocalSocketName(sharedPrefsName)).getAbsolutePath();
        }


        @Nonnull
        public static String mobileMapperLocalSocketName(String stream) {
            return "mm_" + stream; // + "_" + address.replaceAll("\\W", "_");
        }

        public boolean isAutocapture() {
            return mAutocapture;
        }

        public void setAutocapture(boolean mAutocapture) {
            this.mAutocapture = mAutocapture;
        }

        public void setForceColdStart(boolean coldStart){
            this.mForceColdStart = coldStart;
        }

        public boolean isForceColdStart(){
            return mForceColdStart;
        }

        public void setDisableNMEA(boolean disableNMEA){
            this.mDisableNMEA = disableNMEA;
        }

        public boolean isDisableNMEA(){
            return mDisableNMEA;
        }

        public void setEnableNativeSBAS(boolean enableNativeSBAS){
            this.mEnableNativeSBAS = enableNativeSBAS;
        }

        public boolean isEnableNativeSBAS(){
            return mEnableNativeSBAS;
        }

        public int getDynamicModel() {
            return mDynamicModel;
        }

        public void setDynamicModel(int mDynamicModel) {
            this.mDynamicModel = mDynamicModel;
        }

        @Override
        public Value copy() {
            Value v = new Value();
            v.mPath = mPath;
            v.setDynamicModel(mDynamicModel);
            v.setForceColdStart(mForceColdStart);
            v.setAutocapture(mAutocapture);
            v.setEnableNativeSBAS(mEnableNativeSBAS);
            v.setDisableNMEA(mDisableNMEA);
            return v;
        }
    }

    public StreamMobileMapperFragment() {
        super();
        mSharedPrefsName = StreamMobileMapperFragment.class.getSimpleName();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments;

        arguments = getArguments();
        if (arguments == null || !arguments.containsKey(StreamDialogActivity.ARG_SHARED_PREFS_NAME)) {
            throw new IllegalArgumentException("ARG_SHARED_PREFFS_NAME argument not defined");
        }

        mSharedPrefsName = arguments.getString(StreamDialogActivity.ARG_SHARED_PREFS_NAME);

        if (DBG) Log.v(mSharedPrefsName, "onCreate()");

        getPreferenceManager().setSharedPreferencesName(mSharedPrefsName);

        initPreferenceScreen();
    }



    protected void initPreferenceScreen() {
        if (DBG) Log.v(mSharedPrefsName, "initPreferenceScreen()");
        addPreferencesFromResource(R.xml.stream_mobilemapper_settings);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        prefs
                .edit()
                .putBoolean(MOBILEMAPPER_RAW_AUTOCAPTURE,true)
                .putBoolean(MOBILEMAPPER_FORCE_COLDSTART, true)
                .putString(MOBILEMAPPER_DYNAMIC_MODEL, "1")
                .putBoolean(MOBILEMAPPER_ENABLE_NATIVE_SBAS, true)
                .putBoolean(MOBILEMAPPER_DISBALE_NMEA,true)
                .apply();
    }

    public static String readSummary(SharedPreferences prefs) {
        return MOBILEMAPPER_INTERNAL_SENSOR_STR;
    }

    public static Value readSettings(Context context,
                                     SharedPreferences prefs, String sharedPrefsName) {
        final Value v;
        v = new Value();
        v.updatePath(context, sharedPrefsName);
        String strDynamicModel= prefs.getString(MOBILEMAPPER_DYNAMIC_MODEL,null);
        v.setAutocapture(prefs.getBoolean(MOBILEMAPPER_RAW_AUTOCAPTURE,false));
        v.setForceColdStart(prefs.getBoolean(MOBILEMAPPER_FORCE_COLDSTART,true));
        v.setEnableNativeSBAS(prefs.getBoolean(MOBILEMAPPER_ENABLE_NATIVE_SBAS,true));
        v.setDisableNMEA(prefs.getBoolean(MOBILEMAPPER_DISBALE_NMEA,true));

        if (strDynamicModel != null) v.setDynamicModel(Integer.parseInt(strDynamicModel));

        return v;
    }
}
