package ru0xdc.rtkgps.settings;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.R;
import ru0xdc.rtklib.RtkServerSettings.OutputStream;
import ru0xdc.rtklib.SolutionOptions;
import android.content.Context;

public class OutputSolution2Fragment extends OutputSolution1Fragment {

    static final String SHARED_PREFS_NAME = "OutputSolution2";

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
        SettingsHelper.setOutputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force);
    }
}
