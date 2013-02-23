package ru0xdc.rtklib;

/**
 * DOP (dilution of precision)
 *
 */
public class Dops {

	public double gdop;

	public double pdop;

	public double hdop;

	public double vdop;

	public Dops() {
		this(0.0, 0.0, 0.0, 0.0);
	}

	public Dops(double gdop, double pdop, double hdop, double vdop) {
		setDops(gdop, pdop, hdop, vdop);
	}

	public void copyTo(Dops dst) {
		if (dst == null) throw new IllegalArgumentException();
		dst.setDops(gdop, pdop, hdop, vdop);
	}

	void setDops(double gdop, double pdop, double hdop, double vdop) {
		this.gdop = gdop;
		this.pdop = pdop;
		this.hdop = hdop;
		this.vdop = vdop;
	}

}
