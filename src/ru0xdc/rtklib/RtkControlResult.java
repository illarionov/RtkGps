package ru0xdc.rtklib;

import ru0xdc.rtklib.RtkCommon.Position3d;

public class RtkControlResult {

    /**
     * RTK solution
     */
    public final Solution sol;

    /**
     * base position/velocity (ecef) (m|m/s)
     */
    public final double rb[];

    /**
     * number of float states
     */
    public int nx;

    /**
     * number of fixed states
     */
    public int na;

    /**
     * time difference between current and previous (s)
     */
    double tt;

    // XXX: *x, *P, *xa, *Pa

    /**
     * number of continuous fixes of ambiguity
     */
    int nfix;

    // XXX:  ambc ssat opt


    /**
     * error message buffer
     */
    String errMsg;

    public RtkControlResult() {
        sol = new Solution();
        rb = new double[6];
    }

    void setStatus1(double rb0, double rb1, double rb2,
            double rb3, double rb4, double rb5,
            int nx, int na, double tt, int nfix,
            String errMsg
            ) {
        this.rb[0] = rb0;
        this.rb[1] = rb1;
        this.rb[2] = rb2;
        this.rb[3] = rb3;
        this.rb[4] = rb4;
        this.rb[5] = rb5;

        this.nx = nx;
        this.na = na;
        this.tt = tt;
        this.nfix = nfix;
        this.errMsg = errMsg;
    }

    public Position3d getBasePosition() {
        return new Position3d(this.rb[0], this.rb[1], this.rb[2]);
    }

}
