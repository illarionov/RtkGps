package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import gpsplus.rtklib.constants.EphemerisOption;

public class EphemerisOptionPreference extends EnumListPreference<EphemerisOption> {

    public EphemerisOptionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaults();
    }

    public EphemerisOptionPreference(Context context) {
        super(context);
        setDefaults();
    }

    private void setDefaults() {
        setValues(EphemerisOption.values());
        setDefaultValue(EphemerisOption.BRDC);
    }

}
