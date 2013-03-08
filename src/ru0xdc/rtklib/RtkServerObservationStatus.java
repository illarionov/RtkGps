package ru0xdc.rtklib;

import java.text.DecimalFormat;
import java.util.Arrays;

import junit.framework.Assert;
import android.annotation.SuppressLint;

public class RtkServerObservationStatus {

	/**
	 * receiver:
	 * {@link RtkServer#RECEIVER_ROVER},
	 * {@link RtkServer#RECEIVER_BASE},
	 * {@link RtkServer#RECEIVER_EPHEM}
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
	 * satellite ID numbers
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

	public void clear() {
		this.ns = 0;
		this.receiver = RtkServer.RECEIVER_ROVER;
	}

	public void copyTo(RtkServerObservationStatus dst) {
		if (dst == null) throw new IllegalArgumentException();
		dst.ns = ns;
		time.copyTo(dst.time);
		System.arraycopy(sat, 0, dst.sat, 0, ns);
		System.arraycopy(az, 0, dst.az, 0, ns);
		System.arraycopy(el, 0, dst.el, 0, ns);
		System.arraycopy(freq1Snr, 0, dst.freq1Snr, 0, ns);
		System.arraycopy(freq2Snr, 0, dst.freq2Snr, 0, ns);
		System.arraycopy(freq3Snr, 0, dst.freq3Snr, 0, ns);
		System.arraycopy(vsat, 0, dst.vsat, 0, ns);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RtkServerObservationStatus)) {
			return false;
		}

		RtkServerObservationStatus lhs = (RtkServerObservationStatus)o;

		final boolean  res = ((ns == lhs.ns)
				&& time.equals(lhs.time)
				&& Arrays.equals(Arrays.copyOf(az, ns), Arrays.copyOf(lhs.az, lhs.ns))
				&& Arrays.equals(Arrays.copyOf(el, ns), Arrays.copyOf(lhs.el, lhs.ns))
				&& Arrays.equals(Arrays.copyOf(freq1Snr, ns), Arrays.copyOf(lhs.freq1Snr, lhs.ns))
				&& Arrays.equals(Arrays.copyOf(freq2Snr, ns), Arrays.copyOf(lhs.freq2Snr, lhs.ns))
				&& Arrays.equals(Arrays.copyOf(freq3Snr, ns), Arrays.copyOf(lhs.freq3Snr, lhs.ns))
				&& Arrays.equals(Arrays.copyOf(vsat, ns), Arrays.copyOf(lhs.vsat, lhs.ns))
				);
		if (res) Assert.assertTrue(hashCode() == lhs.hashCode());
		return res;
	}

	@Override
	public int hashCode() {
		int result = 0xab6f75;
		result += 31 * result + ns;
		if (ns == 0) return result;

		result += 31 * result + time.hashCode();
		for (int i=0; i<ns; ++i) {
			result += 31 * result + sat[i];
			long doubleFieldBits = Double.doubleToLongBits(az[i]);
			result = 31 * result + (int) (doubleFieldBits ^ (doubleFieldBits >>> 32));
			doubleFieldBits = Double.doubleToLongBits(el[i]);
			result = 31 * result + (int) (doubleFieldBits ^ (doubleFieldBits >>> 32));
			result += 31 * result + freq1Snr[i];
			result += 31 * result + freq2Snr[i];
			result += 31 * result + freq3Snr[i];
			result += 31 * result + vsat[i];
		}

		return result;
	}

	public String getSatId(int pos) {
		if (pos >= this.ns) throw new IllegalArgumentException();
		return RtkCommon.getSatId(sat[pos]);
	}

	public Dops getDops() {
		return getDops(null);
	}

	public Dops getDops(Dops dst) {
		return getDops(dst, 0);
	}

	public Dops getDops(Dops dst, double elmin) {
		int dopsNs;

		if (dst == null) {
			dst = new Dops();
		}

		if (this.ns == 0) {
			return dst;
		}

		final double azel[] = new double[this.ns*2];
		dopsNs = 0;
		for (int i=0; i<ns; ++i) {
			if (this.vsat[i] != 0) {
				azel[2*dopsNs] = this.az[i];
				azel[2*dopsNs+1] = this.el[i];
				dopsNs += 1;
			}
		}
		if (dopsNs == 0) {
			return dst;
		}

		RtkCommon.dops(azel, dopsNs, elmin, dst);

		return dst;
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

		String header = String.format("RtkServerObservationStatus %s week: %d tm: %.3f %d sat-s ",
				receiverName,
				this.time.getGpsWeek(), this.time.getGpsTow(),
				this.ns);
		if (this.ns == 0)
			return header;

		String tmp[] = new String[this.ns];
		DecimalFormat df = new DecimalFormat("###");

		sb = new StringBuffer(600);
		sb.append(header);

		for (int i=0; i<this.ns; ++i) {
			tmp[i] = RtkCommon.getSatId(sat[i]);
		}

		sb.append("\nprn: ");
		sb.append(Arrays.toString(tmp));
		sb.append("\nf1 snr: ");
		sb.append(Arrays.toString(Arrays.copyOf(this.freq1Snr, this.ns)));
		sb.append("\nvalid: ");
		sb.append(Arrays.toString(Arrays.copyOf(this.vsat, this.ns)));

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
