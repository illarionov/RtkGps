package ru0xdc.rtklib;

import java.util.Arrays;
import java.util.Locale;

import ru0xdc.rtklib.RtkCommon.Matrix3x3;
import ru0xdc.rtklib.RtkCommon.Position3d;
import ru0xdc.rtklib.constants.Constants;

public class Solution {

    public static final int TYPE_XYZ_ECEF = 0;

    public static final int TYPE_ENU_BASELINE = 1;

    /**
     * Solution status SOLQ_XXX
     */
    public static enum SolutionStatus {

        NONE(0, "None"),
        FIX(1, "Fixed"),
        FLOAT(2, "Float"),
        SBAS(3, "SBAS"),
        DGPS(4, "DGPS/DGNSS"),
        SINGLE(5, "Single"),
        PPP(6, "PPP"),
        DR(7, "Dead reconing")
        ;

        private final int mRtklibId;
        private final String mDescription;

        private SolutionStatus(int solqId, String name) {
            mRtklibId = solqId;
            mDescription = name;
        }

        public static SolutionStatus valueOf(int solqId) {
            for (SolutionStatus v: SolutionStatus.values()) {
                if (v.mRtklibId == solqId) {
                    return v;
                }
            }
            throw new IllegalArgumentException();
        }

        public String getDescription() {
            return mDescription;
        }
    };


    /**
     * time (GPST)
     */
    public final GTime time;

    /**
     * position/velocity (m|m/s)  {x,y,z,vx,vy,vz} or {e,n,u,ve,vn,vu}
     */
    public final double rr[];

    /**
     * position variance/covariance (m^2)
     * {c_xx,c_yy,c_zz,c_xy,c_yz,c_zx} or
     * {c_ee,c_nn,c_uu,c_en,c_nu,c_ue}
     */
    public final float qr[];

    /**
     * receiver clock bias to time systems (s)
     */
    public final double dtr[];

    /**
     * type {@link #TYPE_XYZ_ECEF}, {@link #TYPE_ENU_BASELINE}
     */
    public int type;

    /**
     * solution status
     */
    public SolutionStatus status;

    /**
     * number of valid satellites
     */
    public int ns;

    /**
     * age of differential (s)
     */
    public float age;

    /**
     * AR ratio factor for valiation
     */
    public float ratio;

    public Solution() {
        time = new GTime();
        rr = new double[6];
        qr = new float[6];
        dtr = new double[6];
        ns=0;
        type=TYPE_XYZ_ECEF;
        status = SolutionStatus.NONE;
    }

    public void copyTo(Solution dst) {
        if (dst == null) throw new IllegalArgumentException();

        this.time.copyTo(dst.time);
        System.arraycopy(this.rr, 0, dst.rr, 0, this.rr.length);
        System.arraycopy(this.qr, 0, dst.rr, 0, this.qr.length);
        System.arraycopy(this.dtr, 0, dst.dtr, 0, this.dtr.length);
        dst.type = this.type;
        dst.status = this.status;
        dst.ns = this.ns;
        dst.age = this.age;
        dst.ratio = this.ratio;
    }

    public Matrix3x3 getQrMatrix() {
        double m[] = new double[9];
        m[0] = qr[0];
        m[4] = qr[1];
        m[8] = qr[2];
        m[1] = m[3] = qr[3];
        m[5] = m[7] = qr[4];
        m[2] = m[6] = qr[5];
        return new Matrix3x3(m);
    }

    public Position3d getPosition() {
        return new Position3d(rr[0], rr[1], rr[2]);
    }

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
        this.time.setGTime(time_time, time_sec);
        this.type = type;
        this.status = SolutionStatus.valueOf(status);
        this.ns = ns;
        this.age = age;
        this.ratio = ratio;
        this.rr[0] = rr0;
        this.rr[1] = rr1;
        this.rr[2] = rr2;
        this.rr[3] = rr3;
        this.rr[4] = rr4;
        this.rr[5] = rr5;

        this.qr[0] = qr0;
        this.qr[1] = qr1;
        this.qr[2] = qr2;
        this.qr[3] = qr3;
        this.qr[4] = qr4;
        this.qr[5] = qr5;

        this.dtr[0] = dtr0;
        this.dtr[1] = dtr1;
        this.dtr[2] = dtr2;
        this.dtr[3] = dtr3;
        this.dtr[4] = dtr4;
        this.dtr[5] = dtr5;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "%d %.2f type: %d status: %s ns: %d age: %.2f ratio: %.2f\n" +
                        "rr: %.3f %.3f %.3f %.3f %.3f %.3f\n" +
                        "qr: %.3f %.3f %.3f %.3f %.3f %.3f\n" +
                        "dtr: %.3f %.3f %.3f %.3f %.3f %.3f\n",
                        time.getGpsWeek(), time.getGpsTow(),
                        type, status.mDescription, ns, age, ratio,
                        rr[0], rr[1], rr[2], rr[3], rr[4], rr[5],
                        qr[0], qr[1], qr[2], qr[3], qr[4], qr[5],
                        dtr[0], dtr[1], dtr[2], dtr[3], dtr[4], dtr[5]
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
