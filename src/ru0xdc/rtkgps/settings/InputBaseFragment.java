package ru0xdc.rtkgps.settings;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import ru0xdc.rtkgps.settings.widget.StreamFormatPreference;
import ru0xdc.rtkgps.settings.widget.StreamTypePreference;
import ru0xdc.rtklib.RtkCommon;
import ru0xdc.rtklib.RtkServerSettings.InputStream;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.TwoStatePreference;
import android.text.TextUtils;
import android.util.Log;


public class InputBaseFragment extends InputRoverFragment {

	private static final boolean DBG = BuildConfig.DEBUG & true;
	static final String TAG = InputBaseFragment.class.getSimpleName();

	static final String SHARED_PREFS_NAME = "InputBase";

	protected static final String KEY_TRANSMIT_GPGGA_TO_BASE = "transmit_gpgga_to_base";
	protected static final String KEY_TRANSMIT_GPGGA_LAT = "transmit_gpgga_latitude";
	protected static final String KEY_TRANSMIT_GPGGA_LON = "transmit_gpgga_longitude";

	public InputBaseFragment() {
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

		addPreferencesFromResource(R.xml.input_stream_settings_base);

		findPreference(KEY_ENABLE).setTitle(R.string.input_streams_settings_enable_base_title);


		typePref = (StreamTypePreference)findPreference(KEY_TYPE);
		typePref.setValues(INPUT_STREAM_TYPES);
		typePref.setDefaultValue(DEFAULT_STREAM_TYPE);
		typePref.setTitle(R.string.input_streams_settings_base_category_title);

		formatPref = (StreamFormatPreference)findPreference(KEY_FORMAT);
		formatPref.setValues(INPUT_STREAM_FORMATS);
		formatPref.setDefaultValue(DEFAULT_STREAM_FORMAT);
	}

	@Override
	protected void refresh() {
		super.refresh();
		final ListPreference transmitPref;
		final EditTextPreference gpggaLatPref, gpggaLonPref;

		transmitPref = (ListPreference) findPreference(KEY_TRANSMIT_GPGGA_TO_BASE);
		gpggaLatPref = (EditTextPreference) findPreference(KEY_TRANSMIT_GPGGA_LAT);
		gpggaLonPref = (EditTextPreference) findPreference(KEY_TRANSMIT_GPGGA_LON);

		final boolean enabled = ((TwoStatePreference)findPreference(KEY_ENABLE)).isChecked();
		final boolean transmitLatLon = TextUtils.equals("1", transmitPref.getValue());

		gpggaLatPref.setEnabled(transmitLatLon && enabled);
		gpggaLonPref.setEnabled(transmitLatLon && enabled);

		transmitPref.setSummary(transmitPref.getEntry());
		gpggaLatPref.setSummary(gpggaLatPref.getText());
		gpggaLonPref.setSummary(gpggaLonPref.getText());
	}

	@Nonnull
	public static InputStream readPrefs(Context ctx) {
		final InputStream stream;
		final SharedPreferences prefs;
		int type;
		double lat, lon;

		stream = SettingsHelper.readInputStreamPrefs(ctx, SHARED_PREFS_NAME);
		prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

		type = Integer.valueOf(prefs.getString(KEY_TRANSMIT_GPGGA_TO_BASE, "0"));
		lat = Double.valueOf(prefs.getString(KEY_TRANSMIT_GPGGA_LAT, "0"));
		lon = Double.valueOf(prefs.getString(KEY_TRANSMIT_GPGGA_LON, "0"));

		stream.setTransmitNmeaPosition(type, RtkCommon.pos2ecef(lat,lon,0,null));

		return stream;
	}

    public static void setDefaultValues(Context ctx, boolean force) {
    	final SharedPreferences prefs;

    	SettingsHelper.setInputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force);

		prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

    	final boolean needUpdate = force || !prefs.contains(KEY_TRANSMIT_GPGGA_TO_BASE);

		if (needUpdate) {
			SharedPreferences.Editor e = prefs.edit();
			e
			 .putString(KEY_TRANSMIT_GPGGA_TO_BASE, "0")
			 .putString(KEY_TRANSMIT_GPGGA_LAT, "0.0")
			 .putString(KEY_TRANSMIT_GPGGA_LON, "0.0")
			  ;
			e.commit();
		}
    }
}
