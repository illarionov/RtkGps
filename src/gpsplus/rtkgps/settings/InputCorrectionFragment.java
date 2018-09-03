package gpsplus.rtkgps.settings;

import android.content.Context;
import android.util.Log;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.settings.widget.StreamFormatPreference;
import gpsplus.rtkgps.settings.widget.StreamTypePreference;
import gpsplus.rtklib.RtkServerSettings.InputStream;
import gpsplus.rtklib.constants.StreamFormat;
import gpsplus.rtklib.constants.StreamType;

import javax.annotation.Nonnull;


public class InputCorrectionFragment extends InputRoverFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = InputBaseFragment.class.getSimpleName();

    public static final String SHARED_PREFS_NAME = "InputCorrection";

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
        StreamFormat.RT17,
        StreamFormat.SS2,
        StreamFormat.LEXR,
        StreamFormat.SEPT,
        StreamFormat.CRES,
        StreamFormat.STQ,
        StreamFormat.GW10,
        StreamFormat.JAVAD,
        StreamFormat.NVS,
        StreamFormat.BINEX,
        StreamFormat.SP3
    };

    private static final StreamFormat DEFAULT_STREAM_FORMAT = StreamFormat.RTCM3;

    private static final SettingsHelper.InputStreamDefaults DEFAULTS = new SettingsHelper.InputStreamDefaults();

    static {
        DEFAULTS
            .setEnabled(false)
            .setFileClientDefaults(
                new StreamFileClientFragment.Value()
                    .setFilename("input_correction.log")
                );
    }


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

        addPreferencesFromResource(R.xml.input_stream_settings_correction);

        findPreference(KEY_ENABLE).setTitle(R.string.input_streams_settings_enable_correction_title);

        typePref = (StreamTypePreference)findPreference(KEY_TYPE);
        typePref.setTitle(R.string.input_streams_settings_correction_tab_title);
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
        SettingsHelper.setInputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }

}


