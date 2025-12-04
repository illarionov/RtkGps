package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import gpsplus.rtklib.constants.EarthTideCorrectionType;

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
