package ru0xdc.rtkgps.settings;

import java.io.File;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.MainActivity;
import ru0xdc.rtkgps.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

public class StreamFileClientFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String KEY_FILENAME = "stream_file_filename";

    private final static String DEFAULT_FILENAME = "stream.log";

    private final PreferenceChangeListener mPreferenceChangeListener;

    private String mSharedPrefsName;

    public StreamFileClientFragment() {
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
        getActivity().setTitle(R.string.file_dialog_title);
    }

    protected void initPreferenceScreen() {
        if (DBG) Log.v(mSharedPrefsName, "initPreferenceScreen()");
        addPreferencesFromResource(R.xml.stream_file_settings);
    }


    public static void setDefaultValues(Context ctx, String sharedPrefsName, boolean force) {
        PreferenceManager.setDefaultValues(ctx, sharedPrefsName,
                Context.MODE_PRIVATE, R.xml.stream_file_settings, force);
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

        etp = (EditTextPreference) findPreference(KEY_FILENAME);
        etp.setSummary(etp.getText());

    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            reloadSummaries();
        }
    };

    @Nonnull
    public static String readPath(SharedPreferences prefs) {
        String path;
        String filename;

        filename = prefs.getString(KEY_FILENAME, null);
        if (filename == null)  throw new IllegalStateException("setDefaultValues() must be called");

        if (filename.length() == 0) filename = DEFAULT_FILENAME;

        path =  (new File(MainActivity.getFileStorageDirectory(), filename)).getAbsolutePath();

        if (DBG) Log.v("StreamFileClientFragment", "file path: " + path);

        return path;
    }

    public static String readSummary(SharedPreferences prefs) {
        return "file://" + readPath(prefs);
    }


}
