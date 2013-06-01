package ru0xdc.rtkgps.settings;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import ru0xdc.rtkgps.settings.widget.StreamFormatPreference;
import ru0xdc.rtkgps.settings.widget.StreamTypePreference;
import ru0xdc.rtklib.RtkServerSettings.InputStream;
import ru0xdc.rtklib.constants.StreamFormat;
import ru0xdc.rtklib.constants.StreamType;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;


public class InputRoverFragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String SHARED_PREFS_NAME = "InputRover";
    static final String KEY_ENABLE = "enable";
    static final String KEY_TYPE = "type";
    static final String KEY_FORMAT = "format";
    static final String KEY_STREAM_SETTINGS_BUTTON = "stream_settings_button";
    static final String KEY_COMMANDS_AT_STARTUP_SHUTDOWN_BUTTON = "commands_at_startup_shutdown_button";
    static final String KEY_RECEIVER_OPTION = "receiver_option";

    private static final StreamType INPUT_STREAM_TYPES[] = new StreamType[] {
        StreamType.BLUETOOTH,
        StreamType.USB,
        StreamType.TCPCLI,
        StreamType.NTRIPCLI,
        StreamType.FILE
    };

    static final StreamType DEFAULT_STREAM_TYPE = StreamType.BLUETOOTH;

    protected static final StreamFormat INPUT_STREAM_FORMATS[] = new StreamFormat[] {
        StreamFormat.RTCM2,
        StreamFormat.RTCM3,
        StreamFormat.OEM3,
        StreamFormat.OEM4,
        StreamFormat.UBX,
        StreamFormat.SIRF,
        StreamFormat.SS2,
        StreamFormat.CRES,
        StreamFormat.STQ,
        StreamFormat.GW10,
        StreamFormat.JAVAD,
        StreamFormat.NVS,
        StreamFormat.BINEX
    };

    protected static final StreamFormat DEFAULT_STREAM_FORMAT = StreamFormat.RTCM3;

    private static final SettingsHelper.InputStreamDefaults DEFAULTS = new SettingsHelper.InputStreamDefaults();

    static {
        DEFAULTS.setFileClientDefaults(
                new StreamFileClientFragment.Value()
                    .setFilename("input_rover.rtcm3")
                );
    }

    private final PreferenceChangeListener mPreferenceChangeListener;

    public InputRoverFragment() {
        mPreferenceChangeListener = new PreferenceChangeListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(getSharedPreferenceName());
        initPreferenceScreen();

        findPreference(KEY_STREAM_SETTINGS_BUTTON).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                streamSettingsButtonClicked();
                return true;
            }
        });

        findPreference(KEY_COMMANDS_AT_STARTUP_SHUTDOWN_BUTTON).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                commandsAtStartupShutdownButtonClicked();
                return true;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(getSharedPreferenceName(), "onResume()");
        refresh();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(getSharedPreferenceName(), "onPause()");
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    protected void initPreferenceScreen() {

        final StreamFormatPreference formatPref;

        if (DBG) Log.v(getSharedPreferenceName(), "initPreferenceScreen()");

        addPreferencesFromResource(R.xml.input_stream_settings);

        initStreamTypePref();

        formatPref = (StreamFormatPreference)findPreference(KEY_FORMAT);
        formatPref.setValues(INPUT_STREAM_FORMATS);
        formatPref.setDefaultValue(DEFAULT_STREAM_FORMAT);
    }

    protected void initStreamTypePref() {
        final StreamTypePreference typePref;
        ArrayList<StreamType> types;

        types = new ArrayList<StreamType>(Arrays.asList(INPUT_STREAM_TYPES));

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            types.remove(StreamType.BLUETOOTH);
        }

        typePref = (StreamTypePreference)findPreference(KEY_TYPE);
        typePref.setValues(types.toArray(new StreamType[types.size()]));
        typePref.setDefaultValue(DEFAULT_STREAM_TYPE);
    }

    protected void streamSettingsButtonClicked() {
        final Intent intent;
        final Bundle fragmentArgs;
        final StreamTypePreference typePref;

        intent = new Intent(getActivity(), StreamDialogActivity.class);

        typePref = (StreamTypePreference) findPreference(KEY_TYPE);

        fragmentArgs = new Bundle(1);
        fragmentArgs.putString(StreamDialogActivity.ARG_SHARED_PREFS_NAME, getSharedPreferenceName());

        intent.putExtra(StreamDialogActivity.ARG_FRAGMENT_ARGUMENTS, fragmentArgs);
        intent.putExtra(StreamDialogActivity.ARG_FRAGMENT_TYPE, typePref.getValueT().name());

        startActivity(intent);
    }

    protected void commandsAtStartupShutdownButtonClicked() {
        final Intent intent = new Intent(getActivity(), StartupShutdownSettingsActivity.class);
        intent.putExtra(StartupShutdownSettingsActivity.ARG_SHARED_PREFS_NAME, getSharedPreferenceName());
        startActivity(intent);
    }

    protected void refresh() {
        final StreamTypePreference typePref;
        final StreamFormatPreference formatPref;
        final EditTextPreference startupCommandsPref, shutdownCommandsPref, receiverOptionPref;
        final Preference settingsButtonPref;

        if (DBG) Log.v(getSharedPreferenceName(), "refresh()");

        typePref = (StreamTypePreference) findPreference(KEY_TYPE);
        formatPref = (StreamFormatPreference) findPreference(KEY_FORMAT);
        receiverOptionPref = (EditTextPreference)findPreference(KEY_RECEIVER_OPTION);
        settingsButtonPref = findPreference(KEY_STREAM_SETTINGS_BUTTON);

        typePref.setSummary(getString(typePref.getValueT().getNameResId()));
        formatPref.setSummary(getString(formatPref.getValueT().getNameResId()));
        receiverOptionPref.setSummary(receiverOptionPref.getText());
        settingsButtonPref.setSummary(SettingsHelper.readInputStreamSumary(getResources(), getPreferenceManager().getSharedPreferences()));
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            refresh();
        }
    };

    public static void setDefaultValues(Context ctx, boolean force) {
        SettingsHelper.setInputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }

    @Nonnull
    public static InputStream readPrefs(Context ctx) {
        return SettingsHelper.readInputStreamPrefs(ctx, SHARED_PREFS_NAME);
    }
}
