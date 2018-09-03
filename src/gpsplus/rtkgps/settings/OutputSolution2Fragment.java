package gpsplus.rtkgps.settings;

import android.content.Context;

import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkServerSettings.OutputStream;
import gpsplus.rtklib.SolutionOptions;

import javax.annotation.Nonnull;

public class OutputSolution2Fragment extends OutputSolution1Fragment {

    public static final String SHARED_PREFS_NAME = "OutputSolution2";

    private static final SettingsHelper.OutputStreamDefaults DEFAULTS = new SettingsHelper.OutputStreamDefaults();

    static {
        DEFAULTS
            .setEnabled(false)
            .setFileClientDefaults(
                new StreamFileClientFragment.Value()
                    .setFilename("solution2.pos")
                );
    }


    public OutputSolution2Fragment() {
        super();
    }

    @Override
    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    @Override
    protected void initPreferenceScreen() {
        super.initPreferenceScreen();
        findPreference(KEY_ENABLE).setTitle(R.string.output_streams_settings_enable_solution2_title);
    }

    @Nonnull
    public static OutputStream readPrefs(Context ctx, @Nonnull SolutionOptions base) {
        return SettingsHelper.readOutputStreamPrefs(ctx, SHARED_PREFS_NAME, base);
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SettingsHelper.setOutputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }
}
