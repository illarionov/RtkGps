package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import gpsplus.rtklib.constants.PositioningMode;

public class PositioningModePreference extends EnumListPreference<PositioningMode> {

    public PositioningModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaults();
    }

    public PositioningModePreference(Context context) {
        super(context);
        setDefaults();
    }

    private void setDefaults() {
        setValues(PositioningMode.values());
        setDefaultValue(PositioningMode.SINGLE);
    }

}
