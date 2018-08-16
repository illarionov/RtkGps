package gpsplus.rtkgps.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.GTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GTimeView extends TextView {

    public enum Format {

        GPS(0, R.string.gtime_format_gps),

        UTC(1, R.string.gtime_format_utc),

        LOCAL(2, R.string.gtime_format_local),

        GPS_TOW(3, R.string.gtime_format_gps_tow);

        final int mStyledAttributeValue;
        final int mDescriptionId;

        private Format(int styledAttributeValue, int descriptionId) {
            mStyledAttributeValue = styledAttributeValue;
            mDescriptionId = descriptionId;
        }

        static Format valueOfStyledAttr(int val) {
            for (Format f: Format.values()) {
                if (f.mStyledAttributeValue == val)
                    return f;
            }
            throw new IllegalArgumentException();
        }

        public int getDescriptionResId() {
            return mDescriptionId;
        }
    }

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = GTimeView.class.getSimpleName();

    private final static Format DEFAULT_TIME_FORMAT = Format.LOCAL;

    private final static String DEFAULT_TIME_TEMPLATE = "yyyy/MM/dd HH:mm:ss.SSS";

    private Format mTimeFormat;

    private String mTimeTemplate;

    private final GTime mGTime;

    private final Date mGTimeDate;

    private SimpleDateFormat mGTimeFormatter;

    public GTimeView(Context context) {
        this(context, null);
    }

    public GTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mGTime = new GTime();
        mGTimeDate = new Date();

        // process style attributes
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.GTimeView, 0, 0);

        try {
            final int formatVal = a.getInt(R.styleable.GTimeView_time_format,
                    DEFAULT_TIME_FORMAT.mStyledAttributeValue
                    );
            mTimeFormat = Format.valueOfStyledAttr(formatVal);

            mTimeTemplate = a.getString(R.styleable.GTimeView_time_template);
            if (mTimeTemplate == null) mTimeTemplate = DEFAULT_TIME_TEMPLATE;
        }finally {
            a.recycle();
        }

        updateTimeFormatter();
        updateTextViewValue();
    }

    public void setTimeFormat(Format timeFormat) {
        if (timeFormat != mTimeFormat) {
            mTimeFormat = timeFormat;
            updateTimeFormatter();
            updateTextViewValue();
            invalidate();
        }
    }

    public void setTimeTemplate(String timeTemplate) {
        if (!TextUtils.equals(timeTemplate, mTimeTemplate)) {
            mTimeTemplate = timeTemplate;
            updateTimeFormatter();
            updateTextViewValue();
            invalidate();
        }
    }

    public Format getTimeFormat() {
        return mTimeFormat;
    }

    public String getTimeTemplate() {
        return mTimeTemplate;
    }

    public void setTime(GTime time) {
        time.copyTo(mGTime);
        updateTextViewValue();
        invalidate();
    }

    private void updateTimeFormatter() {
        switch (mTimeFormat) {
        case GPS:
            mGTimeFormatter = new SimpleDateFormat(mTimeTemplate, Locale.US);
            mGTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            break;
        case UTC:
            mGTimeFormatter = new SimpleDateFormat(mTimeTemplate, Locale.US);
            mGTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            break;
        case LOCAL:
            mGTimeFormatter = new SimpleDateFormat(mTimeTemplate, Locale.US);
            break;
        case GPS_TOW:
            mGTimeFormatter = null;
            break;
        }
    }

    private void updateTextViewValue() {
        String time;

        try {
            switch (mTimeFormat) {
            case GPS:
                mGTimeDate.setTime(mGTime.getGpsTimeMillis());
                time = mGTimeFormatter.format(mGTimeDate)
                        + " "
                        + getResources().getString(R.string.gtime_format_gps)
                        ;
                break;
            case UTC:
                mGTimeDate.setTime(mGTime.getUtcTimeMillis());
                time = mGTimeFormatter.format(mGTimeDate)
                        + " "
                        + getResources().getString(R.string.gtime_format_utc)
                        ;
                break;
            case LOCAL:
                mGTimeDate.setTime(mGTime.getUtcTimeMillis());
                mGTimeFormatter.setTimeZone(TimeZone.getDefault());
                time = mGTimeFormatter.format(mGTimeDate)
                        + " "
                        + getResources().getString(R.string.gtime_format_local)
                        ;
                break;
            case GPS_TOW:
                time = String.format(Locale.US,
                        "week %04d %.3f s", mGTime.getGpsWeek(),
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

        setText(time);
    }
}
