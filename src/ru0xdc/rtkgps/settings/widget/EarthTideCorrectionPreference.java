package ru0xdc.rtkgps.settings.widget;

import ru0xdc.rtklib.constants.EarthTideCorrectionType;
import android.content.Context;
import android.util.AttributeSet;

public class EarthTideCorrectionPreference extends EnumListPreference<EarthTideCorrectionType> {

	public EarthTideCorrectionPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDefaults();
	}

	public EarthTideCorrectionPreference(Context context) {
		super(context);
		setDefaults();
	}

	private void setDefaults() {
		setValues(EarthTideCorrectionType.values());
		setDefaultValue(EarthTideCorrectionType.OFF);
	}

}
