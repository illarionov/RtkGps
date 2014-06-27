package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

/**
 * Satellite ephemeris / clock option (EPHOPT_XXX)
 *
 */
public enum EphemerisOption implements IHasRtklibId {

    /** ephemeris option: broadcast ephemeris */
    BRDC(0, R.string.ephopt_brdc),
    /** ephemeris option: precise ephemeris */
    PREC(1, R.string.ephopt_prec),
    /** ephemeris option: broadcast + SBAS */
    SBAS(2, R.string.ephopt_sbas),
    /** ephemeris option: broadcast + SSR APC correction (antenna phase  center value) */
    SSRAPC(3, R.string.ephopt_ssrapc),
    /** ephemeris option: broadcast + SSR CoM correction (satellite center of mass value)*/
    SSRCOM(4, R.string.ephopt_ssrcom),
    /** ephemeris option: QZSS LEX ephemeris */
    LEX(5, R.string.ephopt_lex)
    ;

    private final int mRtklibId;
    private final int mNameResId;

    private EphemerisOption(int rtklibId, int nameResId) {
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

    public static EphemerisOption valueOf(int rtklibId) {
        for (EphemerisOption v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
