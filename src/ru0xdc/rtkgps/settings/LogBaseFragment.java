package ru0xdc.rtkgps.settings;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.R;
import ru0xdc.rtklib.RtkServerSettings.LogStream;
import android.content.Context;


public class LogBaseFragment extends LogRoverFragment {

	static final String SHARED_PREFS_NAME = "LogBase";

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
    	SettingsHelper.setLogStreamDefaultValues(ctx, SHARED_PREFS_NAME, force);
    }

}
