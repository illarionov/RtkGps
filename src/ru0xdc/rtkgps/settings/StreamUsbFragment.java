package ru0xdc.rtkgps.settings;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.MainActivity;
import ru0xdc.rtkgps.R;
import ru0xdc.rtklib.RtkServerSettings.TransportSettings;
import ru0xdc.rtklib.constants.StreamType;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

public class StreamUsbFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String KEY_DEVICE_BAUDRATE = "stream_usb_baudrate";


    private String mSharedPrefsName;


    public static final class Value implements TransportSettings {

        public static final int DEFAULT_BAUDRATE = 38400;

        private String mPath;

        private int mBaudrate;

        public Value() {
            mPath = null;
            mBaudrate = DEFAULT_BAUDRATE;
        }

        @Override
        public StreamType getType() {
            return StreamType.USB;
        }

        @Override
        public String getPath() {
            if (mPath == null) throw new IllegalStateException("Path not initialized. Call updatePath()");
            return mPath;
        }

        public void updatePath(Context context, String sharedPrefsName) {
            mPath = MainActivity.getLocalSocketPath(context,
                    usbLocalSocketName(sharedPrefsName)).getAbsolutePath();
        }


        @Nonnull
        public static String usbLocalSocketName(String stream) {
            return "usb_" + stream; // + "_" + address.replaceAll("\\W", "_");
        }

        public Value setBaudrate(@Nonnegative int baudrate) {
            if (baudrate == 0) {
                mBaudrate = DEFAULT_BAUDRATE;
            }else {
                mBaudrate = baudrate;
            }
            return this;
        }

        public int getBaudrate() {
            return mBaudrate;
        }

        @Override
        public Value copy() {
            Value v = new Value();
            v.mBaudrate = mBaudrate;
            v.mPath = mPath;
            return v;
        }
    }

    public StreamUsbFragment() {
        super();
        mSharedPrefsName = StreamUsbFragment.class.getSimpleName();
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

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(mSharedPrefsName, "onResume()");
        reloadSummaries();

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                mPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(mSharedPrefsName, "onPause()");
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                mPreferenceChangeListener);
        super.onPause();
    }

    protected void initPreferenceScreen() {
        if (DBG) Log.v(mSharedPrefsName, "initPreferenceScreen()");
        addPreferencesFromResource(R.xml.stream_usb_settings);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        prefs
            .edit()
            .putString(KEY_DEVICE_BAUDRATE, String.valueOf(value.getBaudrate()))
            .apply();
    }

    @Nonnull
    public static Value readSettings(Context context, SharedPreferences prefs, String sharedPrefsName) {
        String baudrate = prefs.getString(KEY_DEVICE_BAUDRATE,
                String.valueOf(Value.DEFAULT_BAUDRATE));

        final Value v = new Value().setBaudrate(Integer.valueOf(baudrate));
        v.updatePath(context, sharedPrefsName);
        return v;
    }

    void reloadSummaries() {
        ListPreference baudratePref;

        baudratePref = (ListPreference) findPreference(KEY_DEVICE_BAUDRATE);
        baudratePref.setSummary(baudratePref.getEntry());
    }

    public static String readSummary(Resources r, SharedPreferences prefs) {
        return "Any USB device, baudrate: " + prefs.getString(KEY_DEVICE_BAUDRATE,
                String.valueOf(Value.DEFAULT_BAUDRATE));
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            reloadSummaries();
        }
    };

}
