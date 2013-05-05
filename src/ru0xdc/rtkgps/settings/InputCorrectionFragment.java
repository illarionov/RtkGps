package ru0xdc.rtkgps.settings;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import ru0xdc.rtkgps.settings.widget.StreamFormatPreference;
import ru0xdc.rtkgps.settings.widget.StreamTypePreference;
import ru0xdc.rtklib.RtkServerSettings.InputStream;
import ru0xdc.rtklib.constants.StreamFormat;
import ru0xdc.rtklib.constants.StreamType;
import android.content.Context;
import android.util.Log;


public class InputCorrectionFragment extends InputRoverFragment {

	private static final boolean DBG = BuildConfig.DEBUG & true;
	static final String TAG = InputBaseFragment.class.getSimpleName();

	static final String SHARED_PREFS_NAME = "InputCorrection";

	static final StreamType CORRECTION_STREAM_TYPES[] = new StreamType[] {
		StreamType.TCPCLI,
		StreamType.NTRIPCLI,
		StreamType.FILE
	};

	static final StreamType DEFAULT_STREAM_TYPE = StreamType.NTRIPCLI;

	private static final StreamFormat CORRECTION_STREAM_FORMATS[] = new StreamFormat[] {
		StreamFormat.RTCM2,
		StreamFormat.RTCM3,
		StreamFormat.OEM3,
		StreamFormat.OEM4,
		StreamFormat.UBX,
		StreamFormat.SS2,
		StreamFormat.CRES,
		StreamFormat.STQ,
		StreamFormat.GW10,
		StreamFormat.JAVAD,
		StreamFormat.NVS,
		StreamFormat.BINEX,
		StreamFormat.SP3
	};

	private static final StreamFormat DEFAULT_STREAM_FORMAT = StreamFormat.RTCM3;

	public InputCorrectionFragment() {
		super();
	}

	@Override
	protected String getSharedPreferenceName() {
		return SHARED_PREFS_NAME;
	}

	@Override
	protected void initPreferenceScreen() {
		final StreamTypePreference typePref;
		final StreamFormatPreference formatPref;

		if (DBG) Log.v(getSharedPreferenceName(), "initPreferenceScreen()");

		addPreferencesFromResource(R.xml.input_stream_settings);

		findPreference(KEY_ENABLE).setTitle(R.string.input_streams_settings_enable_correction_title);

		typePref = (StreamTypePreference)findPreference(KEY_TYPE);
		typePref.setTitle(R.string.input_streams_settings_correction_category_title);
		typePref.setValues(CORRECTION_STREAM_TYPES);
		typePref.setDefaultValue(DEFAULT_STREAM_TYPE);

		formatPref = (StreamFormatPreference)findPreference(KEY_FORMAT);
		formatPref.setValues(CORRECTION_STREAM_FORMATS);
		formatPref.setDefaultValue(DEFAULT_STREAM_FORMAT);
	}

	@Nonnull
	public static InputStream readPrefs(Context ctx) {
		return SettingsHelper.readInputStreamPrefs(ctx, SHARED_PREFS_NAME);
	}

    public static void setDefaultValues(Context ctx, boolean force) {
    	SettingsHelper.setInputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force);
    }

}


