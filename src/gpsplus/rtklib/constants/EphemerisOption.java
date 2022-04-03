package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;
import gpsplus.rtkgps.utils.PreciseEphemerisProvider;

/**
 * Satellite ephemeris / clock option (EPHOPT_XXX)
 *
 */
public enum EphemerisOption implements IHasRtklibId {

    /** ephemeris option: broadcast ephemeris */
    BRDC(0, R.string.ephopt_brdc, null),
    /** ephemeris option: precise ephemeris */
    PREC_ESU_ESA(1, R.string.ephopt_prec,PreciseEphemerisProvider.ESU_ESA),
    /** ephemeris option: precise ephemeris */
    PREC_IGU_IGN(1, R.string.ephopt_prec_ign,PreciseEphemerisProvider.IGU_IGN),
    /** ephemeris option: precise ephemeris */
    PREC_IGU_NASA(1, R.string.ephopt_prec_nasa,PreciseEphemerisProvider.IGU_NASA),
    /** ephemeris option: broadcast + SBAS */
    SBAS(2, R.string.ephopt_sbas,null),
    /** ephemeris option: broadcast + SSR APC correction (antenna phase  center value) */
    SSRAPC(3, R.string.ephopt_ssrapc,null),
    /** ephemeris option: broadcast + SSR CoM correction (satellite center of mass value)*/
    SSRCOM(4, R.string.ephopt_ssrcom,null),
    /** ephemeris option: QZSS LEX ephemeris */
    LEX(5, R.string.ephopt_lex,null)
    ;

    private final int mRtklibId;
    private final int mNameResId;
    private final PreciseEphemerisProvider mProvider;

    EphemerisOption(int rtklibId, int nameResId, PreciseEphemerisProvider provider) {
        mRtklibId = rtklibId;
        mNameResId = nameResId;
        mProvider = provider;
    }

    @Override
    public int getRtklibId() {
        return mRtklibId;
    }

    @Override
    public int getNameResId() {
        return mNameResId;
    }

    public PreciseEphemerisProvider getProvider(){ return mProvider;}

    public static EphemerisOption valueOf(int rtklibId) {
        for (EphemerisOption v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
