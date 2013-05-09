package ru0xdc.rtkgps.settings.widget;

import ru0xdc.rtklib.constants.TroposphereOption;
import android.content.Context;
import android.util.AttributeSet;

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
