package ru0xdc.rtkgps.settings;
import javax.annotation.Nonnull;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;


public class StreamNtripClientFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String KEY_HOST = "stream_ntrip_client_host";
    private static final String KEY_PORT = "stream_ntrip_client_port";
    private static final String KEY_MOUNTPOINT = "stream_ntrip_client_mountpoint";
    private static final String KEY_USER = "stream_ntrip_client_user";
    private static final String KEY_PASSWORD = "stream_ntrip_client_password";

    private final PreferenceChangeListener mPreferenceChangeListener;

    private String mSharedPrefsName;

    public static final class Value {
        private String host;
        private int port;
        private String mountpoint;
        private String user;
        private String password;

        public static final String DEFAULT_HOST = "gps.0xdc.ru";
        public static final int DEFULT_PORT = 2101;
        public static final String DEFAULT_MOUNTPOUNT = "gag27-rtcm";
        public static final String DEFAULT_USER = "osm";
        public static final String DEFAULT_PASSWORD = "osm";

        public Value() {
            host = DEFAULT_HOST;
            port = DEFULT_PORT;
            mountpoint = DEFAULT_MOUNTPOUNT;
            user = DEFAULT_USER;
            password = DEFAULT_PASSWORD;
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

        public Value setMountpoint(@Nonnull String mountpoint) {
            if (mountpoint == null) throw new NullPointerException();
            this.mountpoint = mountpoint;
            return this;
        }

        public Value setUser(@Nonnull String user) {
            if (user == null) throw new NullPointerException();
            this.user = user;
            return this;
        }

        public Value setPassword(@Nonnull String password) {
            if (password == null) throw new NullPointerException();
            this.password = password;
            return this;
        }

    }

    public StreamNtripClientFragment() {
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.ntrip_client_dialog_title);
    }

    protected void initPreferenceScreen() {
        if (DBG) Log.v(mSharedPrefsName, "initPreferenceScreen()");
        addPreferencesFromResource(R.xml.stream_ntrip_client_settings);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

        prefs.edit()
            .putString(KEY_HOST, value.host)
            .putString(KEY_PORT, String.valueOf(value.port))
            .putString(KEY_MOUNTPOINT, value.mountpoint)
            .putString(KEY_USER, value.user)
            .putString(KEY_PASSWORD, value.password)
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

        etp = (EditTextPreference) findPreference(KEY_MOUNTPOINT);
        etp.setSummary(etp.getText());

        etp = (EditTextPreference) findPreference(KEY_USER);
        etp.setSummary(etp.getText());

        etp = (EditTextPreference) findPreference(KEY_PASSWORD);
        etp.setSummary(etp.getText());
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            reloadSummaries();
        }
    };

    /**
     *
     * @return [user[:passwd]@]addr[:port][/mntpnt[:str]])
     */
    @Nonnull
    static String encodeNtripTcpPath(String user, String passwd, String host, String port, String mountpoint, String str) {
        StringBuilder path;
        path = new StringBuilder();

        final boolean emptyUser, emptyPasswd;

        emptyUser = TextUtils.isEmpty(user);
        emptyPasswd = TextUtils.isEmpty(passwd);

        if (!emptyUser) {
            path.append(Uri.encode(user));
        }

        if (!emptyPasswd) {
            if (!emptyUser) path.append(':');
            path.append(Uri.encode(passwd));
        }

        if (!emptyUser || !emptyPasswd) {
            path.append('@');
        }

        if (TextUtils.isEmpty(host)) host = "localhost";

        path.append(host);
        if (!TextUtils.isEmpty(port)) {
            path.append(':').append(port);
        }

        path.append('/');

        if (!TextUtils.isEmpty(mountpoint)) path.append(mountpoint);

        if (!TextUtils.isEmpty(str)) {
            path.append(':').append(str);
        }

        return path.toString();
    }

    @Nonnull
    public static String readPath(SharedPreferences prefs) {
        return encodeNtripTcpPath(
                prefs.getString(KEY_USER, ""),
                prefs.getString(KEY_PASSWORD, ""),
                prefs.getString(KEY_HOST, ""),
                prefs.getString(KEY_PORT, ""),
                prefs.getString(KEY_MOUNTPOINT, ""),
                null
                );
    }

    public static String readSummary(SharedPreferences prefs) {
        return "ntrip://" + readPath(prefs);
    }

}
