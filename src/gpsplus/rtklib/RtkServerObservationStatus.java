package gpsplus.rtklib;

import android.annotation.SuppressLint;

import gpsplus.rtklib.RtkCommon.Dops;
import gpsplus.rtklib.constants.Constants;

import junit.framework.Assert;

import java.text.DecimalFormat;
import java.util.Arrays;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

public class RtkServerObservationStatus {

    public static class SatStatus {

        private int mSatNumber;
        private double mAz;
        private double mEl;
        private int mFreq1Snr;
        private int mFreq2Snr;
        private int mFreq3Snr;
        private boolean mValid;

        public SatStatus() {
        }

        public SatStatus(int satNumber, double az, double el,
                int freq1Snr, int freq2Snr, int freq3Snr, boolean valid) {
            this();
            setValues(satNumber, az, el, freq1Snr, freq2Snr, freq3Snr, valid);
        }

        void setValues(int satNumber, double az, double el,
                int freq1Snr, int freq2Snr, int freq3Snr, boolean valid) {
            mSatNumber = satNumber;
            mAz = az;
            mEl = el;
            mFreq1Snr = freq1Snr;
            mFreq2Snr = freq2Snr;
            mFreq3Snr = freq3Snr;
            mValid = valid;
        }


        /**
         * @return satellite Number
         */
        public int getSatNumber() {
            return mSatNumber;
        }

        /**
         * @return satellite id (Gnn,Rnn,Enn,Jnn,Cnn or nnn)
         */
        public String getSatId() {
            return RtkCommon.getSatId(mSatNumber);
        }

        /**
         * @return valid satellite flag
         */
        public boolean isValid() {
            return mValid;
        }

        /**
         * @return satellite azimuth angles (rad)
         */
        public double getAzimuth() {
            return mAz;
        }

        /**
         * @return satellite elevation angles (rad)
         */
        public double getElevation() {
            return mEl;
        }

        /**
         * @return satellite snr for freq1 (dBHz)
         */
        public int getFreq1Snr() {
            return mFreq1Snr;
        }

        /**
         * @return satellite snr for freq2 (dBHz)
         */
        public int getFreq2Snr() {
            return mFreq2Snr;
        }

        /**
         * @return satellite snr for freq3 (dBHz)
         */
        public int getFreq3Snr() {
            return mFreq3Snr;
        }
    }

    static class Native {
        /**
         * Number of satellites
         */
        private int ns;

        /**
         * time of observation data
         */
        private final GTime time;

        /**
         * satellite ID numbers
         */
        private final int sat[];

        /**
         * satellite azimuth angles (rad)
         */
        private final double az[];

        /**
         * satellite elevation angles (rad)
         */
        private final double el[];

        /**
         * satellite snr for freq1 (dBHz)
         */
        private final int freq1Snr[];

        /**
         * satellite snr for freq2 (dBHz)
         */
        private final int freq2Snr[];

        /**
         * satellite snr for freq3 (dBHz)
         */
        private final int freq3Snr[];

        /**
         * valid satellite flag
         */
        private final int vsat[];

        public Native() {
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

    }

    /**
     * receiver:
     * {@link RtkServer#RECEIVER_ROVER},
     * {@link RtkServer#RECEIVER_BASE},
     * {@link RtkServer#RECEIVER_EPHEM}
     */
    private int receiver;

    private Native mNative;

    public RtkServerObservationStatus() {
        this(RtkServer.RECEIVER_ROVER);
    }

    public RtkServerObservationStatus(int receiver) {
        this.receiver = receiver;
        mNative = new Native();
    }

    public void clear() {
        this.mNative.ns = 0;
        this.receiver = RtkServer.RECEIVER_ROVER;
    }

    public void copyTo(RtkServerObservationStatus dst) {
        if (dst == null) throw new IllegalArgumentException();
        dst.receiver = receiver;
        dst.mNative.ns = mNative.ns;
        mNative.time.copyTo(dst.mNative.time);
        System.arraycopy(mNative.sat, 0, dst.mNative.sat, 0, mNative.ns);
        System.arraycopy(mNative.az, 0, dst.mNative.az, 0, mNative.ns);
        System.arraycopy(mNative.el, 0, dst.mNative.el, 0, mNative.ns);
        System.arraycopy(mNative.freq1Snr, 0, dst.mNative.freq1Snr, 0, mNative.ns);
        System.arraycopy(mNative.freq2Snr, 0, dst.mNative.freq2Snr, 0, mNative.ns);
        System.arraycopy(mNative.freq3Snr, 0, dst.mNative.freq3Snr, 0, mNative.ns);
        System.arraycopy(mNative.vsat, 0, dst.mNative.vsat, 0, mNative.ns);
    }

    public void addValues(int sat, double az, double el, int freq1Snr, int freq2Snr, int freq3Snr, int vsat){
       //mNative.time.setGTime(time, sec);
        mNative.sat[mNative.ns]=sat;
       mNative.az[mNative.ns] = az;
       mNative.el[mNative.ns] = el;
       mNative.freq1Snr[mNative.ns]=freq1Snr;
       mNative.freq2Snr[mNative.ns]=freq2Snr;
       mNative.freq3Snr[mNative.ns]=freq3Snr;
       mNative.vsat[mNative.ns]=vsat;
       mNative.ns++;
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

        final boolean  res = ((mNative.ns == lhs.mNative.ns)
                && mNative.time.equals(lhs.mNative.time)
                && Arrays.equals(Arrays.copyOf(mNative.az, mNative.ns), Arrays.copyOf(lhs.mNative.az, lhs.mNative.ns))
                && Arrays.equals(Arrays.copyOf(mNative.el, mNative.ns), Arrays.copyOf(lhs.mNative.el, lhs.mNative.ns))
                && Arrays.equals(Arrays.copyOf(mNative.freq1Snr, mNative.ns), Arrays.copyOf(lhs.mNative.freq1Snr, lhs.mNative.ns))
                && Arrays.equals(Arrays.copyOf(mNative.freq2Snr, mNative.ns), Arrays.copyOf(lhs.mNative.freq2Snr, lhs.mNative.ns))
                && Arrays.equals(Arrays.copyOf(mNative.freq3Snr, mNative.ns), Arrays.copyOf(lhs.mNative.freq3Snr, lhs.mNative.ns))
                && Arrays.equals(Arrays.copyOf(mNative.vsat, mNative.ns), Arrays.copyOf(lhs.mNative.vsat, lhs.mNative.ns))
                );
        if (res) Assert.assertTrue(hashCode() == lhs.hashCode());
        return res;
    }

    @Override
    public int hashCode() {
        int result = 0xab6f75;
        result += 31 * result + mNative.ns;
        if (mNative.ns == 0) return result;

        result += 31 * result + mNative.time.hashCode();
        for (int i=0; i<mNative.ns; ++i) {
            result += 31 * result + mNative.sat[i];
            long doubleFieldBits = Double.doubleToLongBits(mNative.az[i]);
            result = 31 * result + (int) (doubleFieldBits ^ (doubleFieldBits >>> 32));
            doubleFieldBits = Double.doubleToLongBits(mNative.el[i]);
            result = 31 * result + (int) (doubleFieldBits ^ (doubleFieldBits >>> 32));
            result += 31 * result + mNative.freq1Snr[i];
            result += 31 * result + mNative.freq2Snr[i];
            result += 31 * result + mNative.freq3Snr[i];
            result += 31 * result + mNative.vsat[i];
        }

        return result;
    }

    Native getNative() {
        return mNative;
    }

    /**
     * @return receiver:
     * {@link RtkServer#RECEIVER_ROVER},
     * {@link RtkServer#RECEIVER_BASE},
     * {@link RtkServer#RECEIVER_EPHEM}
     */
    public int getReceiver() {
        return receiver;
    }

    void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    /**
     * Number of satellites
     */
    @Nonnegative
    public int getNumSatellites() {
        return mNative.ns;
    }

    /**
     * @param number sattelite number
     * @param dst Destination
     * @return Satellite status
     */
    public SatStatus getSatStatus(int number, @Nullable SatStatus dst) {
        if (number < 0 || number >= mNative.ns) {
            throw new IllegalArgumentException();
        }
        if (dst == null) dst = new SatStatus();
        dst.setValues(
                mNative.sat[number],
                mNative.az[number],
                mNative.el[number],
                mNative.freq1Snr[number],
                mNative.freq2Snr[number],
                mNative.freq3Snr[number],
                mNative.vsat[number] != 0
                );
        return dst;
    }

    public GTime getTime() {
        return mNative.time;
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

        if (this.mNative.ns == 0) {
            return dst;
        }

        final double azel[] = new double[this.mNative.ns*2];
        dopsNs = 0;
        for (int i=0; i<mNative.ns; ++i) {
            if (this.mNative.vsat[i] != 0) {
                azel[2*dopsNs] = this.mNative.az[i];
                azel[2*dopsNs+1] = this.mNative.el[i];
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
                this.mNative.time.getGpsWeek(), this.mNative.time.getGpsTow(),
                this.mNative.ns);
        if (this.mNative.ns == 0)
            return header;

        String tmp[] = new String[this.mNative.ns];
        DecimalFormat df = new DecimalFormat("###");

        sb = new StringBuffer(600);
        sb.append(header);

        for (int i=0; i<this.mNative.ns; ++i) {
            tmp[i] = RtkCommon.getSatId(mNative.sat[i]);
        }

        sb.append("\nprn: ");
        sb.append(Arrays.toString(tmp));
        sb.append("\nf1 snr: ");
        sb.append(Arrays.toString(Arrays.copyOf(this.mNative.freq1Snr, this.mNative.ns)));
        sb.append("\nvalid: ");
        sb.append(Arrays.toString(Arrays.copyOf(this.mNative.vsat, this.mNative.ns)));

        for (int i=0; i<this.mNative.ns; ++i) {
            tmp[i] = df.format(Math.toDegrees(this.mNative.az[i]));
        }
        sb.append("\naz: ");
        sb.append(Arrays.toString(tmp));

        for (int i=0; i<this.mNative.ns; ++i) {
            tmp[i] = df.format(Math.toDegrees(this.mNative.el[i]));
        }
        sb.append("\nel: ");
        sb.append(Arrays.toString(tmp));

        return sb.toString();
    }

}
