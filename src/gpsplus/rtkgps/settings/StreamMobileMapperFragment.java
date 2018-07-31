package gpsplus.rtkgps.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

import javax.annotation.Nonnull;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.MainActivity;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.usb.SerialLineConfiguration;
import gpsplus.rtkgps.usb.SerialLineConfiguration.Parity;
import gpsplus.rtkgps.usb.SerialLineConfiguration.StopBits;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.constants.StreamType;

public class StreamMobileMapperFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private String mSharedPrefsName;


    public static final class Value implements TransportSettings {

        private String mPath;

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


        @Override
        public Value copy() {
            Value v = new Value();
            v.mPath = mPath;
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
        addPreferencesFromResource(R.xml.stream_usb_settings);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
    }

    public static String readSummary(SharedPreferences prefs) {
        return "mm:";
    }

    public static Value readSettings(Context context,
                                     SharedPreferences prefs, String sharedPrefsName) {
        final Value v;
        v = new Value();
        v.updatePath(context, sharedPrefsName);
        return v;
    }
}
