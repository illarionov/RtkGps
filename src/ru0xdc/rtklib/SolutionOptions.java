package ru0xdc.rtklib;

import ru0xdc.rtklib.constants.GeoidModel;
import ru0xdc.rtklib.constants.SolutionFormat;
import ru0xdc.rtklib.constants.TimeSystem;
import android.text.TextUtils;

/**
 * solution options type solopt_t
 *
 */
public class SolutionOptions {

    /** solution format (SOLF_??? {@link SolutionFormat}) */
    public int posf;

    /** time system (TIMES_??? {@link TimeSystem}) */
    public int times;

    /** time format (0:sssss.s,1:yyyy/mm/dd hh:mm:ss.s) */
    public int timef;

    /** time digits under decimal point */
    int timeu;

    /** latitude/longitude format (0:ddd.ddd,1:ddd mm ss) */
    int degf;

    /** output header (0:no,1:yes) */
    boolean outhead;

    /** output processing options (0:no,1:yes)  */
    boolean outopt;

    /** datum (0:WGS84,1:Tokyo) */
    int datum;

    /** height (0:ellipsoidal,1:geodetic) */
    int height;

    /** geoid model (0:EGM96,1:JGD2000) */
    int geoid;

    /** solution of static mode (0:all,1:single)  */
    int solstatic;

    /** solution statistics level (0:off,1:states,2:residuals) */
    int sstat;

    /** debug trace level (0:off,1-5:debug) */
    int trace;

    /** NMEA GPRMC, GPGGA output interval (s) (<0:no,0:all) */
    double nmeaintv_rmcgga;

    /** NMEA GPGSV output interval (s) (<0:no,0:all) */
    double nmeaintv_gsv;

    /** field separator */
    String sep;

    /** program name */
    String prog;

    native void _loadDefaults();

    public SolutionOptions() {
        _loadDefaults();
    }

    public SolutionOptions(SolutionOptions src) {
        if (src == null) throw new IllegalArgumentException();
        setValues(src);
    }

    public void setValues(SolutionOptions src) {
        if (src == null) throw new IllegalArgumentException();
        setValues(src.posf, src.times,
                src.timef, src.timeu, src.degf, src.outhead, src.outopt,
                src.datum, src.height, src.geoid, src.solstatic, src.sstat,
                src.trace, src.nmeaintv_rmcgga, src.nmeaintv_gsv,
                src.sep, src.prog
                );
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
        this.posf = posf;
        this.times = times;
        this.timef = timef;
        this.timeu = timeu;
        this.degf = degf;
        this.outhead = outhead;
        this.outopt = outopt;
        this.datum = datum;
        this.height = height;
        this.geoid = geoid;
        this.solstatic = solstatic;
        this.sstat = sstat;
        this.trace = trace;
        this.nmeaintv_rmcgga = nmeaintv_rmcgga;
        this.nmeaintv_gsv = nmeaintv_gsv;
        this.sep = sep;
        this.prog = prog;
    }

    public SolutionFormat getSolutionFormat() {
        return SolutionFormat.valueOf(this.posf);
    }

    public SolutionOptions setSolutionFormat(SolutionFormat format) {
        this.posf = format.getRtklibId();
        return this;
    }

    public TimeSystem getTimeSystem() {
        return TimeSystem.valueOf(this.times);
    }

    public SolutionOptions setTimeSystem(TimeSystem timeSystem) {
        this.times = timeSystem.getRtklibId();
        return this;
    }


    public boolean getOutHead() {
        return outhead;
    }

    public SolutionOptions setOutHead(boolean outHead) {
        this.outhead = outHead;
        return this;
    }

    /** latitude/longitude format (0:ddd.ddd,1:ddd mm ss) */
    public int getLatLonFormat() {
        return this.degf;
    }

    /** set latitude/longitude format (0:ddd.ddd,1:ddd mm ss) */
    public SolutionOptions setLatLonFormat(int format) {
        if (format != 0 && format != 1) throw new IllegalArgumentException();
        this.degf = format;
        return this;
    }

    public String getFieldSeparator() {
        return this.sep;
    }

    public SolutionOptions setFieldSeparator(String separator) {
        if (separator == null) throw new NullPointerException();
        if (!TextUtils.equals(this.sep, separator)) this.sep = separator;
        return this;
    }

    public boolean isEllipsoidalHeight() {
        return this.height == 0;
    }

    public SolutionOptions setIsEllipsoidalHeight(boolean v) {
        this.height = v ? 0 : 1;
        return this;
    }

    public GeoidModel getGeoidModel() {
        return GeoidModel.valueOf(this.geoid);
    }

    public SolutionOptions setGeoidModel(GeoidModel model) {
        this.geoid = model.getRtklibId();
        return this;
    }

    public double getNmeaIntervalRmcGga() {
        return this.nmeaintv_rmcgga;
    }

    public SolutionOptions setNmeaIntervalRmcGga(double interval) {
        if (interval < 0) throw new IllegalArgumentException();
        this.nmeaintv_rmcgga = interval;
        return this;
    }

    public double getNmeaIntervalGsv() {
        return this.nmeaintv_gsv;
    }

    public SolutionOptions setNmeaIntervalGsv(double interval) {
        if (interval < 0) throw new IllegalArgumentException();
        this.nmeaintv_gsv = interval;
        return this;
    }

    /** solution statistics level (0:off,1:states,2:residuals) */
    public int getSolutionStatsLevel() {
        return this.sstat;
    }

    /** solution statistics level (0:off,1:states,2:residuals) */
    public SolutionOptions setSolutionStatsLevel(int level) {
        if (level < 0 || level > 2) throw new IllegalArgumentException();
        this.sstat = level;
        return this;
    }

    /** debug trace level (0:off,1-5:debug) */
    public int getDebugTraceLevel() {
        return this.trace;
    }

    /** debug trace level (0:off,1-5:debug) */
    public SolutionOptions setDebugTraceLevel(int level) {
        if (level < 0 || level > 5) throw new IllegalArgumentException();
        this.trace = level;
        return this;
    }


}

