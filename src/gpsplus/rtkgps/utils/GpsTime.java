package gpsplus.rtkgps.utils;

import java.util.Calendar;
import java.util.TimeZone;

import static java.lang.Math.floor;

public class GpsTime {
    private static final int LEAP_SECOND = 17;
    private static final int SEC_IN_DAY = 86400;
    private static final int SEC_IN_WEEK = 604800;
    private double fmjd;
    private long mGpsWeek;
    private double mSecOfWeek;
    private Calendar mTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private long mjd;

    public GpsTime() {}

    private long dayOfYear(long paramLong1, long paramLong2, long paramLong3)
    {
        int i = 1;
        if (paramLong1 % 4L != 0L) {
            i = 0;
        }
        long l2 = i;
        long l1 = l2;
        if (paramLong1 % 100L == 0L)
        {
            l1 = l2;
            if (paramLong1 != 2000L) {
                l1 = 0L;
            }
        }
        i = (short)(int)l1;
        return new long[][] { { 0L, 31L, 59L, 90L, 120L, 151L, 181L, 212L, 243L, 273L, 304L, 334L }, { 0L, 31L, 60L, 91L, 121L, 152L, 182L, 213L, 244L, 274L, 305L, 335L } }[i][((short)(int)(paramLong2 - 1L))] + paramLong3;
    }

    private void mjdTOgps()
    {
        long l = this.mjd - 44244L;
        this.mGpsWeek = (l / 7L);
        this.mSecOfWeek = ((l - this.mGpsWeek * 7L + this.fmjd) * 86400.0D);
        int i;
        if ((this.mjd < 57754L) && ((this.mjd != 57753L) || (1.0D - this.fmjd >= 1.1574074074074074E-9D))) {
            i = 17;
        } else {
            i = 18;
        }
        this.mSecOfWeek += i;
        if (this.mSecOfWeek >= 604800.0D)
        {
            this.mSecOfWeek -= 604800.0D;
            this.mGpsWeek += 1L;
        }
    }

    private void dyhmsTOmjd()
    {
        long d = this.mTime.get(Calendar.DAY_OF_YEAR);
        long y = this.mTime.get(Calendar.YEAR);
        long h = this.mTime.get(Calendar.HOUR_OF_DAY);
        long m = this.mTime.get(Calendar.MINUTE);
        long s = this.mTime.get(Calendar.SECOND);
        long t = (y - 1901L) / 4L;
        this.fmjd = ((h * 3600L + m * 60L + s) * 1.157407407407407E-5D);
        this.mjd = (15385L + t * 1461L + (y - 1L) % 4L * 365L + d - 1L);
    }

    private void ymdhmsTOgps()
    {
        dyhmsTOmjd();
        mjdTOgps();
    }

    public long getGpsWeek()
    {
        return this.mGpsWeek;
    }
    public String getStringGpsWeek(){
        return String.format("%d",mGpsWeek);
    }

    public String getStringGpsTOW(){
        return String.format("%.0f",floor(this.mSecOfWeek));
    }

    public double getSecondsOfWeek()
    {
        return floor(this.mSecOfWeek);
    }

    public void setTime(long timeInMillis)
    {
        this.mTime.setTimeInMillis(timeInMillis);
        ymdhmsTOgps();
    }
}
