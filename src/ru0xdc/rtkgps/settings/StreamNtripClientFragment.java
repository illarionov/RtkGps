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
import android.preference.PreferenceManager;
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

    public static void setDefaultValues(Context ctx, String sharedPrefsName, boolean force) {
        PreferenceManager.setDefaultValues(ctx, sharedPrefsName,
                Context.MODE_PRIVATE, R.xml.stream_ntrip_client_settings, force);
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
