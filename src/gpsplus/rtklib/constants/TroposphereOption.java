package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

/**
 * Troposphere option (TROPOPT_XXX)
 */
public enum TroposphereOption implements IHasRtklibId {

    /** correction off */
    OFF(0, R.string.tropopt_off),
    /** Saastamoinen model */
    SAAS(1, R.string.tropopt_saas),
    /** SBAS model */
    SBAS(2, R.string.tropopt_sbas),
    /** ZTD estimation */
    EST(3, R.string.tropopt_est),
    /** ZTD+grad estimation */
    ESTG(4, R.string.tropopt_estg),
    /** ZTD correction */
    COR(5, R.string.tropopt_cor),
    /** ZTD+grad correction */
    CORG(6, R.string.tropopt_corg)
    ;

    private final int mRtklibId;
    private final int mNameResId;

    private TroposphereOption(int rtklibId, int nameResId) {
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

    public static TroposphereOption valueOf(int rtklibId) {
        for (TroposphereOption v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
