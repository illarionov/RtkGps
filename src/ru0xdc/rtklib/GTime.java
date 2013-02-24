package ru0xdc.rtklib;

import android.text.format.Time;

public class GTime {

	/**
	 * time (s) expressed by standard time_t
	 */
	public long time;

	/**
	 * fraction of second under 1 s
	 */
	public double sec;

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

	void setGTime(long time, double sec) {
		this.time = time;
		this.sec = sec;
	}

	public Time getGpsTime(Time dst) {
		if (dst == null) dst = new Time("UTC");
		dst.set(this.time * 1000 + (Math.round(this.sec * 1000.0)));
		return dst;
	}

	public Time getTime(Time dst) {
		if (dst == null) dst = new Time("UTC");
		_getAndroidUtcTime(dst);
		return dst;
	}

	/**
	 * @return GPS week number
	 */
	public native int getGpsWeek();

	/**
	 * @return GPS time of week (s)
	 */
	public native double getGpsTow();

	native void _getAndroidUtcTime(Time dst);

}
