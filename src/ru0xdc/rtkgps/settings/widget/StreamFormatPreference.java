package ru0xdc.rtkgps.settings.widget;

import ru0xdc.rtklib.constants.StreamFormat;
import android.content.Context;
import android.util.AttributeSet;

public class StreamFormatPreference extends EnumListPreference<StreamFormat> {

    public StreamFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StreamFormatPreference(Context context) {
        super(context);
    }

}
