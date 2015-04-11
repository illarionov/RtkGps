package gpsplus.rtkgps.settings;

import javax.annotation.Nonnull;

import gpsplus.rtkgps.R;

import android.content.Context;

import gpsplus.rtklib.RtkServerSettings.LogStream;

public class LogCorrectionFragment extends LogRoverFragment {

    static final String SHARED_PREFS_NAME = "LogCorrection";

    private static final SettingsHelper.LogStreamDefaults DEFAULTS = new SettingsHelper.LogStreamDefaults();

    static {
        DEFAULTS
            .setEnabled(false)
            .setFileClientDefaults(
                new StreamFileClientFragment.Value()
                    .setFilename("correction_%Y%m%d%h%M%S.log")
                );
    }


    public LogCorrectionFragment() {
        super();
    }

    @Override
    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    @Override
    protected void initPreferenceScreen() {
        super.initPreferenceScreen();
        findPreference(KEY_ENABLE).setTitle(R.string.log_streams_settings_enable_correction_title);
    }

    @Nonnull
    public static LogStream readPrefs(Context ctx) {
        return SettingsHelper.readLogStreamPrefs(ctx, SHARED_PREFS_NAME);
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SettingsHelper.setLogStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }

}
