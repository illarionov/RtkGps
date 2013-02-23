package ru0xdc.rtklib;

public class RtkCommon {

	/**
	 * convert satellite number to satellite id
	 * @param satNo satellite number
	 * @return satellite id (Gnn,Rnn,Enn,Jnn,Cnn or nnn)
	 */
	public static native String getSatId(int satNo);


	/**
	 * compute DOP (dilution of precision)
	 * @param azel  satellite azimuth/elevation angle (rad)
	 * @param ns    number of satellites
	 * @param dst   DOPs {GDOP,PDOP,HDOP,VDOP}
	 */
	static void dops(final double[] azel, int ns, Dops dst) {
		dops(azel, ns, 0.0, dst);
	}

	/**
	 * compute DOP (dilution of precision)
	 * @param azel  satellite azimuth/elevation angle (rad)
	 * @param ns    number of satellites
	 * @param elmin elevation cutoff angle (rad)
	 * @param dst   DOPs {GDOP,PDOP,HDOP,VDOP}
	 */
	static native void dops(final double[] azel, int ns, double elmin, Dops dst);
}
