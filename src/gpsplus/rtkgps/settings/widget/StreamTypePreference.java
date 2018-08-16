package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import gpsplus.rtklib.constants.StreamType;

public class StreamTypePreference extends EnumListPreference<StreamType> {

    public StreamTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StreamTypePreference(Context context) {
        super(context);
    }

}
