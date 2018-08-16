package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import gpsplus.rtklib.constants.IonosphereOption;

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
