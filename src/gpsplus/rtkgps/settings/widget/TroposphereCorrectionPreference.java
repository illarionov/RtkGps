package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import gpsplus.rtklib.constants.TroposphereOption;

public class TroposphereCorrectionPreference extends EnumListPreference<TroposphereOption> {

    public TroposphereCorrectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaults();
    }

    public TroposphereCorrectionPreference(Context context) {
        super(context);
        setDefaults();
    }

    private void setDefaults() {
        setValues(TroposphereOption.values());
        setDefaultValue(TroposphereOption.OFF);
    }

}
