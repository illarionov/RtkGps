package ru0xdc.rtklib.constants;

import ru0xdc.rtkgps.R;
import android.content.res.Resources;


/** Stream type STR_XXX */
public enum StreamType implements IHasRtklibId {

    /** stream type: none */
    NONE(0, R.string.str_none),

    /** stream type: serial */
    SERIAL(1, R.string.str_serial),

    /** stream type: file */
    FILE(2, R.string.str_file),

    /** stream type: TCP server */
    TCPSVR(3, R.string.str_tcpsvr),

    /** stream type: TCP client */
    TCPCLI(4, R.string.str_tcpcli),

    /** stream type: UDP */
    UDP(5, R.string.str_udp),

    /** stream type: NTRIP server */
    NTRIPSVR(6, R.string.str_ntripsvr),

    /** stream type: NTRIP client */
    NTRIPCLI(7, R.string.str_ntripcli),

    /** stream type: ftp */
    FTP(8, R.string.str_ftp),

    /** stream type: http */
    HTTP(9, R.string.str_http)
    ;

    private final int mRtklibId;
    private final int mNameResId;

    private StreamType(int rtklibId, int nameResId) {
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

    public static StreamType valueOf(int rtklibId) {
        for (StreamType v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }

    public static CharSequence[] getEntries(Resources r) {
        final StreamType values[] = values();
        final CharSequence res[] = new CharSequence[values.length];
        for (int i=0; i<values.length; ++i) res[i] = r.getString(values[i].mNameResId);
        return res;
    }

    public static CharSequence[] getEntryValues() {
        final StreamType values[] = values();
        final CharSequence res[] = new CharSequence[values.length];
        for (int i=0; i<values.length; ++i) res[i] = values[i].name();
        return res;
    }
}
