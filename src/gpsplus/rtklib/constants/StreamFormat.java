package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

/**
 * Stream format STRFMT_XXX
 */
public enum StreamFormat implements IHasRtklibId {

    /** stream format: RTCM 2 */
    RTCM2(0, R.string.strfmt_rtcm2),
    /** stream format: RTCM 3 */
    RTCM3(1, R.string.strfmt_rtcm3),
    /** stream format: NovAtel OEMV/4 */
    OEM4(2, R.string.strfmt_oem4),
    /** stream format: NovAtel OEM3 */
    OEM3(3, R.string.strfmt_oem3),
    /** stream format: u-blox LEA-*T */
    UBX(4, R.string.strfmt_ubx),
    /** stream format: NovAtel Superstar II */
    SS2(5, R.string.strfmt_ss2),
    /** stream format: Hemisphere */
    CRES(6, R.string.strfmt_cres),
    /** stream format: SkyTraq S1315F */
    STQ(7, R.string.strfmt_stq),
    /** stream format: Furuno GW10 */
    GW10(8, R.string.strfmt_gw10),
    /** stream format: JAVAD GRIL/GREIS */
    JAVAD(9, R.string.strfmt_javad),
    /** stream format: NVS NVC08C */
    NVS(10, R.string.strfmt_nvs),
    /** stream format: BINEX */
    BINEX(11, R.string.strfmt_binex),
    /** Trimble RT17 */
    RT17(12, R.string.strfmt_rt17),
    /** stream format: SiRF    (reserved) */
    SIRF(13, R.string.strfmt_sirf),
    /** stream format: Furuno LPY-10000 */
    LEXR(14, R.string.strfmt_lexr),
    /** stream format Septentrio */
    SEPT(15, R.string.strfmt_sept),
    /** stream format: RINEX */
    RINEX(16, R.string.strfmt_rinex),
    /** stream format: SP3 */
    SP3(17, R.string.strfmt_sp3),
    /** stream format: RINEX CLK */
    RNXCLK(18, R.string.strfmt_rnxclk),
    /** stream format: SBAS messages */
    SBAS(19, R.string.strfmt_sbas),
    /** stream format: NMEA 0183 */
    NMEA(20, R.string.strfmt_nmea)

    ;

    private final int mRtklibId;
    private final int mNameResId;

    private StreamFormat(int rtklibId, int nameResId) {
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

    public static StreamFormat valueOf(int rtklibId) {
        for (StreamFormat v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
