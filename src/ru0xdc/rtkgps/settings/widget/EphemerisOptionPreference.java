package ru0xdc.rtkgps.settings.widget;

import ru0xdc.rtklib.constants.EphemerisOption;
import android.content.Context;
import android.util.AttributeSet;

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
