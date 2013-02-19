package ru0xdc.rtklib;

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

	void setGTime(long time, double sec) {
		this.time = time;
		this.sec = sec;
	}

}
