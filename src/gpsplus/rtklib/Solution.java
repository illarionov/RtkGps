package gpsplus.rtklib;

import gpsplus.rtklib.RtkCommon.Matrix3x3;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.constants.Constants;
import gpsplus.rtklib.constants.SolutionStatus;
import proguard.annotation.Keep;

import java.util.Arrays;
import java.util.Locale;

public class Solution {

    public static final int TYPE_XYZ_ECEF = 0;

    public static final int TYPE_ENU_BASELINE = 1;

    /**
     * time (GPST)
     */
    private final GTime mTime;

    /**
     * position/velocity (m|m/s)  {x,y,z,vx,vy,vz} or {e,n,u,ve,vn,vu}
     */
    private final double mRr[];

    /**
     * position variance/covariance (m^2)
     * {c_xx,c_yy,c_zz,c_xy,c_yz,c_zx} or
     * {c_ee,c_nn,c_uu,c_en,c_nu,c_ue}
     */
    private final float mQr[];

    /**
     * receiver clock bias to time systems (s)
     */
    private final double mDtr[];

    /**
     * type {@link #TYPE_XYZ_ECEF}, {@link #TYPE_ENU_BASELINE}
     */
    private int mType;

    /**
     * solution status
     */
    private SolutionStatus mStatus;

    /**
     * number of valid satellites
     */
    private int mNs;

    /**
     * age of differential (s)
     */
    private float mAge;

    /**
     * AR ratio factor for valiation
     */
    private float mRatio;

    public Solution() {
        mTime = new GTime();
        mRr = new double[6];
        mQr = new float[6];
        mDtr = new double[6];
        mNs=0;
        mType=TYPE_XYZ_ECEF;
        mStatus = SolutionStatus.NONE;
    }

    public void copyTo(Solution dst) {
        if (dst == null) throw new IllegalArgumentException();

        this.mTime.copyTo(dst.mTime);
        System.arraycopy(this.mRr, 0, dst.mRr, 0, this.mRr.length);
        System.arraycopy(this.mQr, 0, dst.mRr, 0, this.mQr.length);
        System.arraycopy(this.mDtr, 0, dst.mDtr, 0, this.mDtr.length);
        dst.mType = this.mType;
        dst.mStatus = this.mStatus;
        dst.mNs = this.mNs;
        dst.mAge = this.mAge;
        dst.mRatio = this.mRatio;
    }

    public GTime getTime() {
        GTime r = new GTime();
        mTime.copyTo(r);
        return r;
    }

    public Matrix3x3 getQrMatrix() {
        double m[] = new double[9];
        m[0] = mQr[0];
        m[4] = mQr[1];
        m[8] = mQr[2];
        m[1] = m[3] = mQr[3];
        m[5] = m[7] = mQr[4];
        m[2] = m[6] = mQr[5];
        return new Matrix3x3(m);
    }

    public Position3d getPosition() {
        return new Position3d(mRr[0], mRr[1], mRr[2]);
    }

    /**
     * @return position variance/covariance (m^2)
     * {c_xx,c_yy,c_zz,c_xy,c_yz,c_zx} or
     * {c_ee,c_nn,c_uu,c_en,c_nu,c_ue}
     */
    public float[] getPositionVariance() {
        return Arrays.copyOf(mQr, mQr.length);
    }

    /**
     * @return age of differential (s)
     */
    public float getAge() {
        return mAge;
    }

    /**
     * @return AR ratio factor for valiation
     */
    public double getRatio() {
        return mRatio;
    }

    /**
     * @return number of valid satellites
     */
    public int getNs() {
        return mNs;
    }

    /**
     * position/velocity (m|m/s)  {x,y,z,vx,vy,vz} or {e,n,u,ve,vn,vu}
     */
    public double[] getPosVelocity() {
        return mRr;
    }

    // Used in native code
    @Keep
    void setSolution(
            long time_time, double time_sec,
            int type,
            int status,
            int ns,
            float age,
            float ratio,
            double rr0, double rr1, double rr2, double rr3, double rr4, double rr5,
            float qr0, float qr1, float qr2, float qr3, float qr4, float qr5,
            double dtr0, double dtr1, double dtr2, double dtr3, double dtr4, double dtr5
            ) {
        this.mTime.setGTime(time_time, time_sec);
        this.mType = type;
        this.mStatus = SolutionStatus.valueOf(status);
        this.mNs = ns;
        this.mAge = age;
        this.mRatio = ratio;
        this.mRr[0] = rr0;
        this.mRr[1] = rr1;
        this.mRr[2] = rr2;
        this.mRr[3] = rr3;
        this.mRr[4] = rr4;
        this.mRr[5] = rr5;

        this.mQr[0] = qr0;
        this.mQr[1] = qr1;
        this.mQr[2] = qr2;
        this.mQr[3] = qr3;
        this.mQr[4] = qr4;
        this.mQr[5] = qr5;

        this.mDtr[0] = dtr0;
        this.mDtr[1] = dtr1;
        this.mDtr[2] = dtr2;
        this.mDtr[3] = dtr3;
        this.mDtr[4] = dtr4;
        this.mDtr[5] = dtr5;
    }

    /**
     * @return solution status
     */
    public final SolutionStatus getSolutionStatus() {
        return mStatus;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "%d %.2f type: %d status: %s ns: %d age: %.2f ratio: %.2f\n" +
                        "rr: %.3f %.3f %.3f %.3f %.3f %.3f\n" +
                        "qr: %.3f %.3f %.3f %.3f %.3f %.3f\n" +
                        "dtr: %.3f %.3f %.3f %.3f %.3f %.3f\n",
                        mTime.getGpsWeek(), mTime.getGpsTow(),
                        mType, mStatus.name(), mNs, mAge, mRatio,
                        mRr[0], mRr[1], mRr[2], mRr[3], mRr[4], mRr[5],
                        mQr[0], mQr[1], mQr[2], mQr[3], mQr[4], mQr[5],
                        mDtr[0], mDtr[1], mDtr[2], mDtr[3], mDtr[4], mDtr[5]
                );
    }

    public static class SolutionBuffer {

        Solution mBuffer[];

        /**
         * number of solution buffer
         */
        int mNSol;

        public SolutionBuffer() {
            mNSol = 0;
            mBuffer = new Solution[Constants.MAXSOLBUF];
            for (int i=0; i<Constants.MAXSOLBUF; ++i) {
                mBuffer[i] = new Solution();
            }
        }

        public final Solution[] get(){
            return Arrays.copyOf(mBuffer, mNSol);
        }

        public final Solution getLastSolution() {
            if (mNSol == 0) return null;
            return mBuffer[mNSol-1];
        }

        // Used in native code
        @Keep
        void setSolution(
                int idx,
                long time_time, double time_sec,
                int type,
                int status,
                int ns,
                float age,
                float ratio,
                double rr0, double rr1, double rr2, double rr3, double rr4, double rr5,
                float qr0, float qr1, float qr2, float qr3, float qr4, float qr5,
                double dtr0, double dtr1, double dtr2, double dtr3, double dtr4, double dtr5
                ) {
            mBuffer[idx].setSolution(time_time, time_sec, type,
                    status, ns, age, ratio,
                    rr0, rr1, rr2, rr3, rr4, rr5,
                    qr0, qr1, qr2, qr3, qr4, qr5,
                    dtr0, dtr1, dtr2, dtr3, dtr4, dtr5);
        }

    }
}
