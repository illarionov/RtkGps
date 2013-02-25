package ru0xdc.rtkgps.view;

import java.util.Locale;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import ru0xdc.rtklib.GTime;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GTimeWidget extends LinearLayout {

	public final static int TIME_FORMAT_GPS = 0;

	public final static int TIME_FORMAT_UTC = 1;

	public final static int TIME_FORMAT_LOCAL = 2;

	public final static int TIME_FORMAT_GPS_TOW = 3;

	private final static int DEFAULT_TIME_FORMAT = TIME_FORMAT_LOCAL;

	@SuppressWarnings("unused")
	private static final boolean DBG = BuildConfig.DEBUG & true;
	static final String TAG = GTimeWidget.class.getSimpleName();

	private int mTimeFormat;

	private TextView mGTimeView;

	private Button mFormatSwitcherButton;

	private final GTime mGTime;

	public GTimeWidget(Context context) {
		this(context, null);
	}

    public GTimeWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        mGTime = new GTime();

        // process style attributes
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.GTimeWidget, 0, 0);

        try {
        	mTimeFormat = a.getInt(R
        			.styleable.GTimeWidget_time_format,
        			DEFAULT_TIME_FORMAT
        			);
        }finally {
        	a.recycle();
        }

        inflate(context, R.layout.widget_gtime, this);

        mGTimeView = (TextView) findViewById(R.id.gtime);
        mFormatSwitcherButton = (Button)findViewById(R.id.formatSwitcher);

        mFormatSwitcherButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switchTimeFormat();
			}
        });

        updateValues();
    }

    public void setTimeFormat(int timeFormat) {
    	if (timeFormat != mTimeFormat) {
    		mTimeFormat = timeFormat;
    		updateValues();
    		invalidate();
    	}
    }

    public int getTimeFormat() {
    	return mTimeFormat;
    }

    public void setTime(GTime time) {
    	time.copyTo(mGTime);
    	updateGTimeView();
    	invalidate();
    }

    public void switchTimeFormat() {
    	int nextFormat;
    	switch (mTimeFormat) {
    	case TIME_FORMAT_GPS:
    		nextFormat = TIME_FORMAT_UTC;
    		break;
    	case TIME_FORMAT_UTC:
    		nextFormat = TIME_FORMAT_LOCAL;
    		break;
    	case TIME_FORMAT_LOCAL:
    		nextFormat = TIME_FORMAT_GPS_TOW;
    		break;
    	case TIME_FORMAT_GPS_TOW:
    		nextFormat = TIME_FORMAT_GPS;
    		break;
    	default:
    		throw new IllegalStateException();
    	}
    	setTimeFormat(nextFormat);
    }

    private void updateGTimeView() {
    	Time tm;
    	String time;

    	// XXX
    	if (mGTime.sec == 0) {
    		mGTimeView.setText("");
    		return;
    	}

    	try {
    		switch (mTimeFormat) {
    		case TIME_FORMAT_GPS:
    			time = mGTime.getGpsTime(null).format("%Y/%m/%d %T");
    			break;
    		case TIME_FORMAT_UTC:
    			time = mGTime.getTime(null).format("%Y/%m/%d %T");
    			break;
    		case TIME_FORMAT_LOCAL:
    			tm = new Time();
    			mGTime.getTime(tm);
    			time = tm.format("%Y/%m/%d %T");
    			break;
    		case TIME_FORMAT_GPS_TOW:
    			time = String.format(Locale.US,
    					"week %04d %.1f s", mGTime.getGpsWeek(),
    					mGTime.getGpsTow()
    					);
    			break;
    		default:
    			throw new IllegalStateException();
    		}
    	} catch (UnsatisfiedLinkError e) {
    		Log.e(TAG, "UnsatisfiedLinkError " + e);
    		time="time time time";
    	}

    	mGTimeView.setText(time);
    }

    private void updateFormatSwitcherButtonText() {
    	int textResId;

    	switch (mTimeFormat) {
    	case TIME_FORMAT_GPS:
    		textResId = R.string.gtime_format_gps;
    		break;
    	case TIME_FORMAT_UTC:
    		textResId = R.string.gtime_format_utc;
    		break;
    	case TIME_FORMAT_LOCAL:
    		textResId = R.string.gtime_format_local;
    		break;
    	case TIME_FORMAT_GPS_TOW:
    		textResId = R.string.gtime_format_gps_tow;
    		break;
    	default:
    		throw new IllegalStateException();
    	}
    	mFormatSwitcherButton.setText(textResId);
    }

    private void updateValues() {
    	updateFormatSwitcherButtonText();
    	updateGTimeView();
    }

}
