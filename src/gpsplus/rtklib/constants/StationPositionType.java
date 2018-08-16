package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

public enum StationPositionType implements IHasRtklibId {

    POS_IN_PRCOPT(0, R.string.stapos_in_prcopt),

    AVG_OF_SINGLE_POS(1, R.string.stapos_avg_of_single),

    READ_FROM_FILE(2, R.string.stapos_read_from_file),

    RINEX_HEADER(3, R.string.stapos_rinex_hdr),

    RTCM_POS(4, R.string.stapos_rtcm_pos)

    ;

    private final int mRtklibId;
    private final int mNameResId;

    private StationPositionType(int solqId, int nameResId) {
        mRtklibId = solqId;
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

    public static StationPositionType valueOf(int type) {
        for (StationPositionType v: StationPositionType.values()) {
            if (v.mRtklibId == type) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }
}
