package gpsplus.rtkgps.settings;

import android.content.Context;

import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkServerSettings.LogStream;

import javax.annotation.Nonnull;


public class LogBaseFragment extends LogRoverFragment {

    public static final String SHARED_PREFS_NAME = "LogBase";

    private static final SettingsHelper.LogStreamDefaults DEFAULTS = new SettingsHelper.LogStreamDefaults();

    static {
        DEFAULTS
            .setEnabled(false)
            .setFileClientDefaults(
                new StreamFileClientFragment.Value()
                    .setFilename("base_%Y%m%d%h%M%S.log")
                );
    }


    public LogBaseFragment() {
        super();
    }

    @Override
    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    @Override
    protected void initPreferenceScreen() {
        super.initPreferenceScreen();
        findPreference(KEY_ENABLE).setTitle(R.string.log_streams_settings_enable_base_title);
    }

    @Nonnull
    public static LogStream readPrefs(Context ctx) {
        return SettingsHelper.readLogStreamPrefs(ctx, SHARED_PREFS_NAME);
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SettingsHelper.setLogStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }

}
