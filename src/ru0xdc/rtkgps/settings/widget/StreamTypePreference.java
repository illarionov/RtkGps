package ru0xdc.rtkgps.settings.widget;

import ru0xdc.rtklib.constants.StreamType;
import android.content.Context;
import android.util.AttributeSet;

public class StreamTypePreference extends EnumListPreference<StreamType> {

	public StreamTypePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public StreamTypePreference(Context context) {
		super(context);
	}

}
