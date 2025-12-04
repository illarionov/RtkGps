package gpsplus.rtkgps.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

import gpsplus.rtkgps.MainActivity;
import gpsplus.rtkgps.usb.SerialLineConfiguration;
import gpsplus.rtkgps.usb.SerialLineConfiguration.Parity;
import gpsplus.rtkgps.usb.SerialLineConfiguration.StopBits;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.constants.StreamType;
import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;

import javax.annotation.Nonnull;

public class StreamUsbFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String KEY_DEVICE_BAUDRATE = "stream_usb_baudrate";
    private static final String KEY_DATA_BITS = "stream_usb_data_bits";
    private static final String KEY_PARITY = "stream_usb_parity";
    private static final String KEY_STOP_BITS = "stream_usb_stop_bits";


    private String mSharedPrefsName;


    public static final class Value implements TransportSettings {

        private String mPath;

        private SerialLineConfiguration mSerialLineConfiguration;

        public Value() {
            mPath = null;
            mSerialLineConfiguration = new SerialLineConfiguration();
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

        public Value setSerialLineConfiguration(SerialLineConfiguration conf) {
            mSerialLineConfiguration.set(conf);
            return this;
        }

        public SerialLineConfiguration getSerialLineConfiguration() {
            return new SerialLineConfiguration(mSerialLineConfiguration);
        }

        @Override
        public Value copy() {
            Value v = new Value();
            v.mSerialLineConfiguration.set(mSerialLineConfiguration);
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
        final SerialLineConfiguration conf = value.getSerialLineConfiguration();
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        prefs
            .edit()
            .putString(KEY_DEVICE_BAUDRATE, String.valueOf(conf.getBaudrate()))
            .putString(KEY_DATA_BITS, String.valueOf(conf.getDataBits()))
            .putString(KEY_PARITY, String.valueOf(conf.getParity().getCharVal()))
            .putString(KEY_STOP_BITS, conf.getStopBits().getStringVal())
            .apply();
    }

    private static SerialLineConfiguration readSerialLineConfiguration(SharedPreferences prefs) {
        final SerialLineConfiguration conf;
        final String baudrate, dataBits, parity, stopBits;

        conf = new SerialLineConfiguration();

        baudrate = prefs.getString(KEY_DEVICE_BAUDRATE, null);
        dataBits = prefs.getString(KEY_DATA_BITS, null);
        parity = prefs.getString(KEY_PARITY, null);
        stopBits = prefs.getString(KEY_STOP_BITS, null);

        if (baudrate != null) conf.setBaudrate(Integer.valueOf(baudrate));
        if (dataBits != null) conf.setDataBits(Integer.valueOf(dataBits));
        if (parity != null) conf.setParity(Parity.valueOfChar(parity.charAt(0)));
        if (stopBits != null) conf.setStopBits(StopBits.valueOfString(stopBits));

        return conf;
    }

    @Nonnull
    public static Value readSettings(Context context,
            SharedPreferences prefs, String sharedPrefsName) {
        final Value v;
        final SerialLineConfiguration conf;

        conf = readSerialLineConfiguration(prefs);

        v = new Value();
        v.setSerialLineConfiguration(conf);
        v.updatePath(context, sharedPrefsName);

        return v;
    }

    void reloadSummaries() {
        for (String lpKey: new String[]{
                KEY_DEVICE_BAUDRATE,
                KEY_DATA_BITS,
                KEY_PARITY,
                KEY_STOP_BITS}) {
            final ListPreference lpref = (ListPreference)findPreference(lpKey);
            lpref.setSummary(lpref.getEntry());
        }
    }

    public static String readSummary(Resources r, SharedPreferences prefs) {
        final SerialLineConfiguration conf;
        conf = readSerialLineConfiguration(prefs);
        return "Any USB device, " + conf.toString();
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            reloadSummaries();
        }
    };

}
