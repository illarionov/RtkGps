package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

/**
 * Solution status SOLQ_XXX:  {@link #NONE},
 * {@link #FIX}, {@link #FLOAT}, {@link #SBAS},
 * {@link #DGPS}, {@link #SINGLE}, {@link #PPP},
 * {@link #DR}
 */
public enum SolutionStatus implements IHasRtklibId {

    NONE(0, R.string.solq_none),
    FIX(1, R.string.solq_fix),
    FLOAT(2, R.string.solq_float),
    SBAS(3, R.string.solq_sbas),
    DGPS(4, R.string.solq_dgps),
    SINGLE(5, R.string.solq_single),
    PPP(6, R.string.solq_ppp),
    DR(7, R.string.solq_dr),
    INTERNAL(8,R.string.solq_internal)
    ;

    private final int mRtklibId;
    private final int mNameResId;

    private SolutionStatus(int solqId, int nameResId) {
        mRtklibId = solqId;
        mNameResId = nameResId;
    }

    public static SolutionStatus valueOf(int solqId) {
        for (SolutionStatus v: SolutionStatus.values()) {
            if (v.mRtklibId == solqId) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public int getRtklibId() {
        return mRtklibId;
    }

    @Override
    public int getNameResId() {
        return mNameResId;
    }
};
