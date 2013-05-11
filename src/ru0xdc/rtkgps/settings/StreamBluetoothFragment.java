package ru0xdc.rtkgps.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.MainActivity;
import ru0xdc.rtkgps.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;

public class StreamBluetoothFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String KEY_DEVICE_ADDRESS = "stream_bluetooth_address";
    private static final String KEY_DEVICE_NAME = "stream_bluetooth_name";

    private final PreferenceChangeListener mPreferenceChangeListener;

    private String mSharedPrefsName;

    public static final class Value {

        public static final String ADDRESS_DEVICE_IS_NOT_SELECTED = "";

        private String address;
        private String name;

        public Value() {
            address = ADDRESS_DEVICE_IS_NOT_SELECTED;
            name = ADDRESS_DEVICE_IS_NOT_SELECTED;
        }

        public Value setAddress(@Nonnull String address) {
            if (address == null) throw new NullPointerException();
            this.address = address;
            this.name = address;
            return this;
        }
    }

    public StreamBluetoothFragment() {
        super();
        mPreferenceChangeListener = new PreferenceChangeListener();
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
        addPreferencesFromResource(R.xml.stream_bluetooth_settings);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        prefs
            .edit()
            .putString(KEY_DEVICE_NAME, value.name)
            .putString(KEY_DEVICE_ADDRESS, value.address)
            .apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(mSharedPrefsName, "onResume()");
        reloadSummaries();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(mSharedPrefsName, "onPause()");
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    void reloadSummaries() {
        ListPreference pref;

        pref = (ListPreference) findPreference(KEY_DEVICE_ADDRESS);
        pref.setSummary(getSummary(getResources(), pref.getEntry()));

    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            reloadSummaries();
        }
    };

    @Nonnull
    public static String bluetoothLocalSocketName(@Nonnull String address) {
        return "bluetooth_" + address.replaceAll("\\W", "_");
    }

    @Nonnull
    public static String readPath(Context context, SharedPreferences prefs) {
        String path;
        String address;

        address = prefs.getString(KEY_DEVICE_ADDRESS, null);
        if (address == null)  throw new IllegalStateException("setDefaultValues() must be called");

        path = MainActivity.getLocalSocketPath(context, bluetoothLocalSocketName(address)).getAbsolutePath();

        if (DBG) Log.v("StreamFileClientFragment", "bluetooth socket path: " + path);

        return path;
    }

    private static String getSummary(Resources r, @Nullable CharSequence deviceName) {
        if (TextUtils.isEmpty(deviceName)) {
            deviceName = r.getString(R.string.bluetooth_device_not_selected);
        }
        return "Bluetooth: " + deviceName;
    }

    public static String readSummary(Resources r, SharedPreferences prefs) {
        String name=prefs.getString(KEY_DEVICE_NAME, "");
        return getSummary(r, name);
    }


}
