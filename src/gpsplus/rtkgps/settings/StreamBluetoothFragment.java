package gpsplus.rtkgps.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.MainActivity;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.constants.StreamType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StreamBluetoothFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String KEY_DEVICE_ADDRESS = "stream_bluetooth_address";
    private static final String KEY_DEVICE_NAME = "stream_bluetooth_name";

    private String mSharedPrefsName;

    private BluetoothAdapter mBluetoothAdapter;

    public static final class Value implements TransportSettings {

        public static final String ADDRESS_DEVICE_IS_NOT_SELECTED = "";

        private String address;
        private String name;
        private String mPath;

        public Value() {
            address = ADDRESS_DEVICE_IS_NOT_SELECTED;
            name = ADDRESS_DEVICE_IS_NOT_SELECTED;
        }

        Value(String address, String name) {
            this.address = address;
            this.name = name;
        }

        @Nonnull
        public static String bluetoothLocalSocketName(@Nonnull String address, String stream) {
            return "bt_" + stream; // + "_" + address.replaceAll("\\W", "_");
        }

        public Value setAddress(@Nonnull String address) {
            if (address == null) throw new NullPointerException();
            this.address = address.toUpperCase();
            this.name = address;
            this.mPath = null;
            return this;
        }

        @Override
        public StreamType getType() {
            return StreamType.BLUETOOTH;
        }

        public String getAddress() {
            return address.toUpperCase();
        }

        public String getName() {
            return name;
        }

        @Override
        public String getPath() {
            if (mPath == null) throw new IllegalStateException("Path not initialized. Call updatePath()");
            return mPath;
        }

        public void updatePath(Context context, String sharedPrefsName) {
            mPath = MainActivity.getLocalSocketPath(context,
                    bluetoothLocalSocketName(address, sharedPrefsName)).getAbsolutePath();
        }

        @Override
        public Value copy() {
            Value v = new Value();
            v.address = address;
            v.name = name;
            v.mPath = mPath;
            return v;
        }
    }

    public StreamBluetoothFragment() {
        super();
        mSharedPrefsName = StreamBluetoothFragment.class.getSimpleName();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments;

        if (DBG) Log.v(mSharedPrefsName, "onCreate()");

        arguments = getArguments();
        if (arguments == null || !arguments.containsKey(StreamDialogActivity.ARG_SHARED_PREFS_NAME)) {
            throw new IllegalArgumentException("ARG_SHARED_PREFFS_NAME argument not defined");
        }

        mSharedPrefsName = arguments.getString(StreamDialogActivity.ARG_SHARED_PREFS_NAME);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        getPreferenceManager().setSharedPreferencesName(mSharedPrefsName);

        initPreferenceScreen();

        if (savedInstanceState == null) {
            askToTurnOnBluetooth();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(mSharedPrefsName, "onResume()");
        reloadSummaries();

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        getActivity().registerReceiver(mBluetoothChangeListener,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(mSharedPrefsName, "onPause()");
        getActivity().unregisterReceiver(mBluetoothChangeListener);
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    private void askToTurnOnBluetooth() {
        if (mBluetoothAdapter.isEnabled())
            return;

        final Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(i);
    }

    protected void initPreferenceScreen() {
        final ListPreference deviceSelectorPref;
        if (DBG) Log.v(mSharedPrefsName, "initPreferenceScreen()");
        addPreferencesFromResource(R.xml.stream_bluetooth_settings);

        deviceSelectorPref = (ListPreference) findPreference(KEY_DEVICE_ADDRESS);
        deviceSelectorPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String name = mBluetoothAdapter.getRemoteDevice(newValue.toString()).getName();
                if (name == null) name = newValue.toString();
                preference.getEditor().putString(KEY_DEVICE_NAME, name).commit();
                return true;
            }
        });

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

    private static class BluetoothDeviceComparator implements Comparator<BluetoothDevice> {
        @Override
        public int compare(BluetoothDevice arg0, BluetoothDevice arg1) {
            String name0, name1;
            name0 = arg0.getName();
            name1 = arg1.getName();
            if (name0 == null) name0="";
            if (name1 == null) name1="";
            return name0.compareToIgnoreCase(name1);
        }
    }

    void reloadSummaries() {
        ListPreference deviceSelectorPref;
        Set<BluetoothDevice> pairedDevicesSet;

        deviceSelectorPref = (ListPreference) findPreference(KEY_DEVICE_ADDRESS);

        if (!mBluetoothAdapter.isEnabled()) {
            pairedDevicesSet = null;
        }else {
            pairedDevicesSet = mBluetoothAdapter.getBondedDevices();
        }

        if (pairedDevicesSet == null) {
            deviceSelectorPref.setEnabled(false);
            deviceSelectorPref.setSummary(R.string.bluetooth_disabled_summary);
            return;
        }


        final List<BluetoothDevice> pairedDevices;
        final String[] entries;
        final String[] values;

        pairedDevices = new ArrayList<BluetoothDevice>(pairedDevicesSet);
        Collections.sort(pairedDevices, new BluetoothDeviceComparator());

        entries = new String[pairedDevices.size()];
        values = new String[pairedDevices.size()];

        for (int i=0; i<entries.length; ++i) {
            final BluetoothDevice dev = pairedDevices.get(i);
            entries[i] = dev.getName();
            values[i] = dev.getAddress();
        }

        deviceSelectorPref.setEnabled(true);
        deviceSelectorPref.setEntries(entries);
        deviceSelectorPref.setEntryValues(values);

        final CharSequence entry = deviceSelectorPref.getEntry();
        if (entry == null) {
            deviceSelectorPref.setSummary(R.string.bluetooth_device_not_selected);
        }else {
            deviceSelectorPref.setSummary(entry);
        }
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            reloadSummaries();
        }
    };

    private final BroadcastReceiver mBluetoothChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadSummaries();
        }
    };

    @Nonnull
    public static Value readSettings(Context context, SharedPreferences prefs, String sharedPrefsName) {
        String address;

        address = prefs.getString(KEY_DEVICE_ADDRESS, null);
        if (address == null)  throw new IllegalStateException("setDefaultValues() must be called");

        final Value v = new Value(address, prefs.getString(KEY_DEVICE_NAME, ""));
        v.updatePath(context, sharedPrefsName);

        return v;
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
