package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

/**
 * Ionosphere option (IONOOPT_XXX)
 */
public enum IonosphereOption implements IHasRtklibId {

    /** correction off */
    OFF(0, R.string.ionoopt_off),
    /** broadcast model */
    BRDC(1, R.string.ionoopt_brdc),
    /** SBAS model */
    SBAS(2, R.string.ionoopt_sbas),
    /** L1/L2 or L1/L5 iono-free LC */
    IFLC(3, R.string.ionoopt_iflc),
    /** estimation */
    EST(4, R.string.ionoopt_est),
    /** IONEX TEC model */
    TEC(5, R.string.ionoopt_tec),
    /** QZSS broadcast model */
    QZS(6, R.string.ionoopt_qzs),
    /** QZSS LEX ionospehre */
    LEX(7, R.string.ionoopt_lex),
    /** SLANT TEC model */
    STEC(8, R.string.ionoopt_stec)
    ;

    private final int mRtklibId;
    private final int mNameResId;

    private IonosphereOption(int rtklibId, int nameResId) {
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

    public static IonosphereOption valueOf(int rtklibId) {
        for (IonosphereOption v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
