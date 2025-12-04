package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

/**
 * Solution format (SOLF_XXX):
 * {@link #LLH},
 * {@link #XYZ},
 * {@link #ENU},
 * {@link #NMEA},
 * {@link #GSIF}
 */
public enum SolutionFormat implements IHasRtklibId {

    /** solution format: lat/lon/height */
    LLH(0, R.string.solf_llh),

    /** solution format: x/y/z-ecef */
    XYZ(1, R.string.solf_xyz),

    /** solution format: e/n/u-baseline */
    ENU(2, R.string.solf_enu),

    /** solution format: NMEA-183 */
    NMEA(3, R.string.solf_nmea),

    /** solution format: GSI-F1/2/3 */
    GSIF(4, R.string.solf_gsif)
    ;

    private final int mRtklibId;
    private final int mNameResId;

    private SolutionFormat(int rtklibId, int nameResId) {
        mRtklibId = rtklibId;
        mNameResId = nameResId;
    }

    /* (non-Javadoc)
     * @see gpsplus.rtklib.constants.HasRtklibId#getRtklibId()
     */
    @Override
    public int getRtklibId() {
        return mRtklibId;
    }

    /* (non-Javadoc)
     * @see gpsplus.rtklib.constants.HasRtklibId#getNameResId()
     */
    @Override
    public int getNameResId() {
        return mNameResId;
    }

    public static SolutionFormat valueOf(int rtklibId) {
        for (SolutionFormat v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
