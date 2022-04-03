package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import gpsplus.rtklib.constants.SolutionFormat;

public class SolutionFormatPreference extends EnumListPreference<SolutionFormat> {

    public SolutionFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SolutionFormatPreference(Context context) {
        super(context);
    }


}
