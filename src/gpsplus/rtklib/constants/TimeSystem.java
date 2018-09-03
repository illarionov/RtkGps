package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;
import android.content.res.Resources;

/** Time system (TIMES_XXX) */
public enum TimeSystem implements IHasRtklibId {

    /** time system: gps time */
    GPST(0, R.string.times_gpst),

    /** time system: utc */
    UTC(1, R.string.times_utc),

    /** time system: jst */
    JST(2, R.string.times_jst)

    ;

    private final int mRtklibId;
    private final int mNameResId;

    private TimeSystem(int rtklibId, int nameResId) {
        mRtklibId = rtklibId;
        mNameResId = nameResId;
    }

    @Override
    public int getRtklibId() {
        return mRtklibId;
    }

    @Override
    public int getNameResId() {
        return mNameResId;
    }

    public static TimeSystem valueOf(int rtklibId) {
        for (TimeSystem v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }

    public static CharSequence[] getEntries(Resources r) {
        final TimeSystem values[] = values();
        final CharSequence res[] = new CharSequence[values.length];
        for (int i=0; i<values.length; ++i) res[i] = r.getString(values[i].mNameResId);
        return res;
    }

    public static CharSequence[] getEntryValues() {
        final TimeSystem values[] = values();
        final CharSequence res[] = new CharSequence[values.length];
        for (int i=0; i<values.length; ++i) res[i] = values[i].name();
        return res;
    }
}
