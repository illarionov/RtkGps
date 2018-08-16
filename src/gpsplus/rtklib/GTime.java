package gpsplus.rtklib;

import proguard.annotation.Keep;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class GTime {

    /**
     * time (s) expressed by standard time_t
     */
    // Used in native code
    @Keep
    private long time;

    /**
     * fraction of second under 1 s
     */
    // Used in native code
    @Keep
    private double sec;

    public GTime() {
        this(0, 0.0);
    }

    public GTime(long time, double sec) {
        setGTime(time, sec);
    }

    public void copyTo(GTime dst) {
        if (dst == null) throw new IllegalArgumentException();
        dst.setGTime(time, sec);
    }

    // Used in native code
    @Keep
    void setGTime(long time, double sec) {
        this.time = time;
        this.sec = sec;
    }

    /**
     *
     * @return UTC time in milliseconds since January 1, 1970 00:00:00 UTC
     */
    public native long getUtcTimeMillis();

    /**
     *
     * @return GPS time in milliseconds since January 1, 1970 00:00:00 UTC
     */
    public long getGpsTimeMillis() {
        return this.time * 1000 + Math.round(this.sec * 1000.0);
    }

    /**
     * @return GPS week number
     */
    public native int getGpsWeek();

    /**
     * @return GPS time of week (s)
     */
    public native double getGpsTow();

    @Override
    public String toString() {
        return "time: " + time + " sec: " + sec;
    }

    public String getUtcXMLTime() {
        SimpleDateFormat sdtFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date dt = new Date(getUtcTimeMillis());
        String szDate = sdtFormat.format(dt);
        return szDate;
    }

}
