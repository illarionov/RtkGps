package ru0xdc.rtkgps.settings;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.MainActivity;
import ru0xdc.rtklib.RtkServerSettings.TransportSettings;
import ru0xdc.rtklib.constants.StreamType;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

public class StreamUsbFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private String mSharedPrefsName;

    public static final class Value implements TransportSettings {

        private String path;

        public Value(Context context) {
            path = MainActivity.getLocalSocketPath(context, usbLocalSocketName("receiver")).getAbsolutePath();
        }

        @Override
        public StreamType getType() {
            return StreamType.USB;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public Value copy() {
            return this;
        }
    }

    public StreamUsbFragment() {
        super();
        mSharedPrefsName = StreamNtripClientFragment.class.getSimpleName();
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
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName) {
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(mSharedPrefsName, "onResume()");
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(mSharedPrefsName, "onPause()");
        super.onPause();
    }


    @Nonnull
    public static String usbLocalSocketName(@Nonnull String address) {
        return "usb_" + address.replaceAll("\\W", "_");
    }

    @Nonnull
    public static Value readSettings(Context context, SharedPreferences prefs) {
        return new Value(context);
    }

    public static String readSummary(Resources r, SharedPreferences prefs) {
        return "Any USB device";
    }
}
