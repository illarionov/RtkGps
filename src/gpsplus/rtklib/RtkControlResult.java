package gpsplus.rtklib;

import gpsplus.rtklib.RtkCommon.Position3d;
import proguard.annotation.Keep;

public class RtkControlResult {

    /**
     * RTK solution
     */
    // Used in native code
    @Keep
    private final Solution sol;

    /**
     * base position/velocity (ecef) (m|m/s)
     */
    private final double mRb[];

    /**
     * number of float states
     */
    private int mNx;

    /**
     * number of fixed states
     */
    private int mNa;

    /**
     * time difference between current and previous (s)
     */
    private double mTt;

    // XXX: *x, *P, *xa, *Pa

    /**
     * number of continuous fixes of ambiguity
     */
    private int mNfix;

    // XXX:  ambc ssat opt


    /**
     * error message buffer
     */
    private String mErrMsg;

    public RtkControlResult() {
        sol = new Solution();
        mRb = new double[6];
    }

    //Used in native code
    @Keep
    void setStatus1(double rb0, double rb1, double rb2,
            double rb3, double rb4, double rb5,
            int nx, int na, double tt, int nfix,
            String errMsg
            ) {
        this.mRb[0] = rb0;
        this.mRb[1] = rb1;
        this.mRb[2] = rb2;
        this.mRb[3] = rb3;
        this.mRb[4] = rb4;
        this.mRb[5] = rb5;

        this.mNx = nx;
        this.mNa = na;
        this.mTt = tt;
        this.mNfix = nfix;
        this.mErrMsg = errMsg;
    }

    public Position3d getBasePosition() {
        return new Position3d(this.mRb[0], this.mRb[1], this.mRb[2]);
    }

    /**
     * @return RTK solution
     */
    public final Solution getSolution() {
        return sol;
    }

    /**
     * @return base position/velocity (ecef) (m|m/s)
     */
    public double[] getRb() {
        return mRb;
    }

    /**
     * @return number of float states
     */
    public int getNx() {
        return mNx;
    }
    /**
     * @return number of fixed states
     */
    public int getNa() {
        return mNa;
    }

    /**
     * @return time difference between current and previous (s)
     */
    public double getTt() {
        return mTt;
    }

    /**
     * @return number of continuous fixes of ambiguity
     */
    public int getNfix() {
        return mNfix;
    }

    /**
     * @return error message buffer
     */
    public String getErrMsg() {
        return mErrMsg;
    }



}
