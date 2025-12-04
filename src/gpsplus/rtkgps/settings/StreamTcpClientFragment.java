package gpsplus.rtkgps.settings;

import javax.annotation.Nonnull;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.constants.StreamType;


public class StreamTcpClientFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String KEY_HOST = "stream_tcp_client_host";
    private static final String KEY_PORT = "stream_tcp_client_port";

    private final PreferenceChangeListener mPreferenceChangeListener;

    private String mSharedPrefsName;

    public static final class Value implements TransportSettings, Cloneable {
        private String host;
        private int port;

        public static final String DEFAULT_HOST = "localhost";
        public static final int DEFAULT_PORT = 1020;

        public Value() {
            host = DEFAULT_HOST;
            port = DEFAULT_PORT;
        }

        public Value setHost(@Nonnull String host) {
            if (host == null) throw new NullPointerException();
            this.host = host;
            return this;
        }

        public Value setPort(int port) {
            if (port <= 0 || port > 65535) throw new IllegalArgumentException();
            this.port = port;
            return this;
        }

        @Override
        public StreamType getType() {
            return StreamType.TCPCLI;
        }

        @Override
        public String getPath() {
            return SettingsHelper.encodeNtripTcpPath(
                    null,
                    null,
                    host,
                    String.valueOf(port),
                    null,
                    null
                    );
        }

        @Override
        protected Value clone() {
            try {
                return (Value)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Value copy() {
            return clone();
        }
    }

    public StreamTcpClientFragment() {
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
        addPreferencesFromResource(R.xml.stream_tcp_client_settings);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

        prefs.edit()
            .putString(KEY_HOST, value.host)
            .putString(KEY_PORT, String.valueOf(value.port))
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
        EditTextPreference etp;

        etp = (EditTextPreference) findPreference(KEY_HOST);
        etp.setSummary(etp.getText());

        etp = (EditTextPreference) findPreference(KEY_PORT);
        etp.setSummary(etp.getText());
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            reloadSummaries();
        }
    };

    public static Value readSettings(SharedPreferences prefs) {
        return new Value()
            .setHost(prefs.getString(KEY_HOST, ""))
            .setPort(Integer.valueOf(prefs.getString(KEY_PORT, "0")))
            ;
    }

    public static String readSummary(SharedPreferences prefs) {
        return "tcp:" + readSettings(prefs).getPath();
    }


}
