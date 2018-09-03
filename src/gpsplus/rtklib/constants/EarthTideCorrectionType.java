package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

/**
 * earth tide correction (0:off,1:solid,2:solid+otl+pole)
 *
 */
public enum EarthTideCorrectionType implements IHasRtklibId {

    OFF(0, R.string.earth_tide_correction_off),

    /** Solid earth tides */
    SOLID(1, R.string.earth_tide_correction_solid),

    /** Solid earth tides, ocean tide loading, pole tide */
    SOLID_OTL_POLE(7, R.string.earth_tide_correction_solid_otl_pole)

    ;

    private final int mRtklibId;
    private final int mNameResId;

    private EarthTideCorrectionType(int rtklibId, int nameResId) {
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

    public static EarthTideCorrectionType valueOf(int rtklibId) {
        for (EarthTideCorrectionType v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
