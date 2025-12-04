package gpsplus.rtklib;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import proguard.annotation.Keep;

import android.text.TextUtils;

import gpsplus.rtklib.constants.GeoidModel;
import gpsplus.rtklib.constants.SolutionFormat;
import gpsplus.rtklib.constants.TimeSystem;

/**
 * solution options type solopt_t
 *
 */
public class SolutionOptions {

    private final Native mNative;


    @Keep
    static class Native {

        /** solution format (SOLF_??? {@link SolutionFormat}) */
        private int posf;

        /** time system (TIMES_??? {@link TimeSystem}) */
        private int times;

        /** time format (0:sssss.s,1:yyyy/mm/dd hh:mm:ss.s) */
        private int timef;

        /** time digits under decimal point */
        private int timeu;

        /** latitude/longitude format (0:ddd.ddd,1:ddd mm ss) */
        private int degf;

        /** output header (0:no,1:yes) */
        private boolean outhead;

        /** output processing options (0:no,1:yes)  */
        private boolean outopt;

        /** datum (0:WGS84,1:Tokyo) */
        private int datum;

        /** height (0:ellipsoidal,1:geodetic) */
        private int height;

        /** geoid model (0:EGM96,1:JGD2000) */
        private int geoid;

        /** solution of static mode (0:all,1:single)  */
        private int solstatic;

        /** solution statistics level (0:off,1:states,2:residuals) */
        private int sstat;

        /** debug trace level (0:off,1-5:debug) */
        private int trace;

        /** NMEA GPRMC, GPGGA output interval (s) (<0:no,0:all) */
        private double nmeaintv_rmcgga;

        /** NMEA GPGSV output interval (s) (<0:no,0:all) */
        private double nmeaintv_gsv;

        /** field separator */
        private String sep;

        /** program name */
        private String prog;

        native void _loadDefaults();

        public Native() {
            _loadDefaults();
        }
    }

    public SolutionOptions() {
        mNative = new Native();
    }

    public SolutionOptions(SolutionOptions src) {
        this();
        if (src == null) throw new IllegalArgumentException();
        setValues(src);
    }

    void setValues(
            int posf,
            int times,
            int timef,
            int timeu,
            int degf,
            boolean outhead,
            boolean outopt,
            int datum,
            int height,
            int geoid,
            int solstatic,
            int sstat,
            int trace,
            double nmeaintv_rmcgga,
            double nmeaintv_gsv,
            String sep,
            String prog
            ) {
        mNative.posf = posf;
        mNative.times = times;
        mNative.timef = timef;
        mNative.timeu = timeu;
        mNative.degf = degf;
        mNative.outhead = outhead;
        mNative.outopt = outopt;
        mNative.datum = datum;
        mNative.height = height;
        mNative.geoid = geoid;
        mNative.solstatic = solstatic;
        mNative.sstat = sstat;
        mNative.trace = trace;
        mNative.nmeaintv_rmcgga = nmeaintv_rmcgga;
        mNative.nmeaintv_gsv = nmeaintv_gsv;
        mNative.sep = sep;
        mNative.prog = prog;
    }

    public void setValues(SolutionOptions src) {
        if (src == null) throw new IllegalArgumentException();
        setValues(src.mNative.posf, src.mNative.times,
                src.mNative.timef, src.mNative.timeu, src.mNative.degf,
                src.mNative.outhead, src.mNative.outopt,
                src.mNative.datum, src.mNative.height, src.mNative.geoid,
                src.mNative.solstatic, src.mNative.sstat,
                src.mNative.trace, src.mNative.nmeaintv_rmcgga,
                src.mNative.nmeaintv_gsv,
                src.mNative.sep, src.mNative.prog
                );
    }

    Native getNative() {
        return mNative;
    }

    public SolutionFormat getSolutionFormat() {
        return SolutionFormat.valueOf(this.mNative.posf);
    }

    public SolutionOptions setSolutionFormat(SolutionFormat format) {
        this.mNative.posf = format.getRtklibId();
        return this;
    }

    public TimeSystem getTimeSystem() {
        return TimeSystem.valueOf(this.mNative.times);
    }

    public SolutionOptions setTimeSystem(TimeSystem timeSystem) {
        this.mNative.times = timeSystem.getRtklibId();
        return this;
    }


    public boolean getOutHead() {
        return mNative.outhead;
    }

    public SolutionOptions setOutHead(boolean outHead) {
        this.mNative.outhead = outHead;
        return this;
    }

    /** latitude/longitude format (0:ddd.ddd,1:ddd mm ss) */
    public int getLatLonFormat() {
        return this.mNative.degf;
    }

    /** set latitude/longitude format (0:ddd.ddd,1:ddd mm ss) */
    public SolutionOptions setLatLonFormat(int format) {
        if (format != 0 && format != 1) throw new IllegalArgumentException();
        this.mNative.degf = format;
        return this;
    }

    public String getFieldSeparator() {
        return this.mNative.sep;
    }

    public SolutionOptions setFieldSeparator(@Nonnull String separator) {
        if (separator == null) throw new NullPointerException();
        if (!TextUtils.equals(this.mNative.sep, separator)) this.mNative.sep = separator;
        return this;
    }

    public boolean isEllipsoidalHeight() {
        return this.mNative.height == 0;
    }

    public SolutionOptions setIsEllipsoidalHeight(boolean v) {
        this.mNative.height = v ? 0 : 1;
        return this;
    }

    public GeoidModel getGeoidModel() {
        return GeoidModel.valueOf(this.mNative.geoid);
    }

    public SolutionOptions setGeoidModel(GeoidModel model) {
        this.mNative.geoid = model.getRtklibId();
        return this;
    }

    @Nonnegative
    public double getNmeaIntervalRmcGga() {
        return this.mNative.nmeaintv_rmcgga;
    }

    public SolutionOptions setNmeaIntervalRmcGga(@Nonnegative double interval) {
        if (interval < 0) throw new IllegalArgumentException();
        this.mNative.nmeaintv_rmcgga = interval;
        return this;
    }

    @Nonnegative
    public double getNmeaIntervalGsv() {
        return this.mNative.nmeaintv_gsv;
    }

    public SolutionOptions setNmeaIntervalGsv(@Nonnegative double interval) {
        if (interval < 0) throw new IllegalArgumentException();
        this.mNative.nmeaintv_gsv = interval;
        return this;
    }

    /** solution statistics level (0:off,1:states,2:residuals) */
    public int getSolutionStatsLevel() {
        return this.mNative.sstat;
    }

    /** solution statistics level (0:off,1:states,2:residuals) */
    public SolutionOptions setSolutionStatsLevel(int level) {
        if (level < 0 || level > 2) throw new IllegalArgumentException();
        this.mNative.sstat = level;
        return this;
    }

    /** debug trace level (0:off,1-5:debug) */
    public int getDebugTraceLevel() {
        return this.mNative.trace;
    }

    /** debug trace level (0:off,1-5:debug) */
    public SolutionOptions setDebugTraceLevel(int level) {
        if (level < 0 || level > 5) throw new IllegalArgumentException();
        this.mNative.trace = level;
        return this;
    }


}

