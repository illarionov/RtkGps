package ru0xdc.rtklib;

import java.text.DecimalFormat;
import java.util.Arrays;

import android.annotation.SuppressLint;

public class RtkServerObservationStatus {

	/**
	 * receiver:
	 * {@link RtkServer.RECEIVER_ROVER},
	 * {@link RtkServer.RECEIVER_BASE},
	 * {@link RtkServer.RECEIVER_EPHEM}
	 */
	public int receiver;

	/**
	 * Number of satellites
	 */
	public int ns;

	/**
	 * time of observation data
	 */
	public final GTime time;

	/**
	 * satellite prn numbers
	 */
	public final int sat[];

	/**
	 * satellite azimuth angles (rad)
	 */
	public final double az[];

	/**
	 * satellite elevation angles (rad)
	 */
	public final double el[];

	/**
	 * satellite snr for freq1 (dBHz)
	 */
	public final int freq1Snr[];

	/**
	 * satellite snr for freq2 (dBHz)
	 */
	public final int freq2Snr[];

	/**
	 * satellite snr for freq3 (dBHz)
	 */
	public final int freq3Snr[];

	/**
	 * valid satellite flag
	 */
	public final int vsat[];

	public RtkServerObservationStatus() {
		ns = 0;
		time = new GTime();
		sat = new int[Constants.MAXSAT];
		az = new double[Constants.MAXSAT];
		el = new double[Constants.MAXSAT];
		freq1Snr = new int[Constants.MAXSAT];
		freq2Snr = new int[Constants.MAXSAT];
		freq3Snr = new int[Constants.MAXSAT];
		vsat = new int[Constants.MAXSAT];
	}

	@SuppressLint("DefaultLocale")
	@Override
	public String toString() {
		StringBuffer sb;
		String receiverName;

		switch (this.receiver) {
		case RtkServer.RECEIVER_ROVER:
			receiverName = "Rover";
			break;
		case  RtkServer.RECEIVER_BASE:
			receiverName = "Base";
			break;
		case RtkServer.RECEIVER_EPHEM:
			receiverName = "Ephem";
			break;
		default:
			receiverName = String.valueOf(this.receiver);
		}

		String header = String.format("RtkServerObservationStatus %s %d %.3f %d sat-s ",
				receiverName,
				this.time.time, this.time.sec,
				this.ns);
		if (this.ns == 0)
			return header;

		sb = new StringBuffer(header);
		sb.append("\nprn: ");
		sb.append(Arrays.toString(Arrays.copyOf(this.sat, this.ns)));
		sb.append("\nf1 snr: ");
		sb.append(Arrays.toString(Arrays.copyOf(this.freq1Snr, this.ns)));
		sb.append("\nvalid: ");
		sb.append(Arrays.toString(Arrays.copyOf(this.vsat, this.ns)));

		String tmp[] = new String[this.ns];
		DecimalFormat df = new DecimalFormat("###");

		for (int i=0; i<this.ns; ++i) {
			tmp[i] = df.format(Math.toDegrees(this.az[i]));
		}
		sb.append("\naz: ");
		sb.append(Arrays.toString(tmp));

		for (int i=0; i<this.ns; ++i) {
			tmp[i] = df.format(Math.toDegrees(this.el[i]));
		}
		sb.append("\nel: ");
		sb.append(Arrays.toString(tmp));

		return sb.toString();
	}

}
