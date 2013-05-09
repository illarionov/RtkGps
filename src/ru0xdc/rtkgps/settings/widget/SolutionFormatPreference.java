package ru0xdc.rtkgps.settings.widget;

import ru0xdc.rtklib.constants.SolutionFormat;
import android.content.Context;
import android.util.AttributeSet;

public class SolutionFormatPreference extends EnumListPreference<SolutionFormat> {

    public SolutionFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SolutionFormatPreference(Context context) {
        super(context);
    }


}
