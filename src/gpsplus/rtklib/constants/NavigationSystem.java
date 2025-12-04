package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;
import android.content.res.Resources;


/**
 *
 * Navigation System (SYS_XXX)
 *
 */
public enum NavigationSystem implements IHasRtklibId {

    /** navigation system: GPS */
    GPS(0x01, R.string.navsys_gps),
    /** navigation system: SBAS */
    SBS(0x02, R.string.navsys_sbs),
    /** navigation system: GLONASS */
    GLO(0x04, R.string.navsys_glo),
    /** navigation system: Galileo */
    GAL(0x08, R.string.navsys_gal),
    /** navigation system: QZSS */
    QZS(0x10, R.string.navsys_qzs),
    /** navigation system: BeiDou */
    CMP(0x20, R.string.navsys_cmp),
    ;

    private final int mRtklibId;
    private final int mNameResId;

    private NavigationSystem(int rtklibId, int nameResId) {
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

    public static NavigationSystem valueOf(int rtklibId) {
        for (NavigationSystem v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }


    public static CharSequence[] getEntries(Resources r) {
        final NavigationSystem values[] = values();
        final CharSequence res[] = new CharSequence[values.length];
        for (int i=0; i<values.length; ++i) res[i] = r.getString(values[i].mNameResId);
        return res;
    }

    public static CharSequence[] getEntryValues() {
        final NavigationSystem values[] = values();
        final CharSequence res[] = new CharSequence[values.length];
        for (int i=0; i<values.length; ++i) res[i] = values[i].toString();
        return res;
    }
}
