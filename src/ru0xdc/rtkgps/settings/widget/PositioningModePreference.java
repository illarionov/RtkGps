package ru0xdc.rtkgps.settings.widget;

import ru0xdc.rtklib.constants.PositioningMode;
import android.content.Context;
import android.util.AttributeSet;

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
