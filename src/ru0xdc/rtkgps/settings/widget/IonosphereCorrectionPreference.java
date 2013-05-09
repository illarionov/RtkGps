package ru0xdc.rtkgps.settings.widget;

import ru0xdc.rtklib.constants.IonosphereOption;
import android.content.Context;
import android.util.AttributeSet;

public class IonosphereCorrectionPreference extends EnumListPreference<IonosphereOption> {

    public IonosphereCorrectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaults();
    }

    public IonosphereCorrectionPreference(Context context) {
        super(context);
        setDefaults();
    }

    private void setDefaults() {
        setValues(IonosphereOption.values());
        setDefaultValue(IonosphereOption.OFF);
    }

}
