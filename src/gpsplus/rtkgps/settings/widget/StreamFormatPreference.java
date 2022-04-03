package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import gpsplus.rtklib.constants.StreamFormat;

public class StreamFormatPreference extends EnumListPreference<StreamFormat> {

    public StreamFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StreamFormatPreference(Context context) {
        super(context);
    }

}
