package ru0xdc.rtklib;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtklib.constants.Constants;
import ru0xdc.rtklib.constants.EarthTideCorrectionType;
import ru0xdc.rtklib.constants.EphemerisOption;
import ru0xdc.rtklib.constants.IonosphereOption;
import ru0xdc.rtklib.constants.NavigationSystem;
import ru0xdc.rtklib.constants.PositioningMode;
import ru0xdc.rtklib.constants.TroposphereOption;
import android.util.Log;


/**
 * Processing options prcopt_t
 *
 */
public class ProcessingOptions {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    static final String TAG = ProcessingOptions.class.getSimpleName();

    /**
     * SNR mask type snr_t
     *
     */
    public static class SnrMask {

        boolean enableRover;
        boolean enableBase;

        /** mask (dBHz) at 5,10,...85 deg */
        final double maskL1[];
        final double maskL2[];
        final double maskL5[];

        public SnrMask() {
            enableRover = enableBase = false;
            maskL1 = new double[9];
            maskL2 = new double[9];
            maskL5 = new double[9];
        }

        public void setValues(SnrMask src) {
            enableRover = src.enableRover;
            enableBase = src.enableBase;
            System.arraycopy(src.maskL1, 0, maskL1, 0, maskL1.length);
            System.arraycopy(src.maskL2, 0, maskL2, 0, maskL2.length);
            System.arraycopy(src.maskL5, 0, maskL5, 0, maskL5.length);
        }

        public void setSnrMask(int mask) {
            if (mask <= 0) {
                enableRover = enableBase = false;
                mask = 0;
            }else {
                enableRover = enableBase = true;
            }

            Arrays.fill(maskL1, mask);
            Arrays.fill(maskL2, mask);
            Arrays.fill(maskL5, mask);
        }

        public int getSnrMask() {
            if (!enableRover) return 0;
            return ((int)maskL1[0] / 5) * 5;
        }
    }


    /** positioning mode (PMODE_???) */
    int mode;

    /** solution type (0:forward,1:backward,2:combined) */
    int soltype;

    /** number of frequencies (1:L1,2:L1+L2,3:L1+L2+L5) */
    private int nf;

    /** navigation system */
    int navsys;

    /** elevation mask angle (rad) */
    double elmin;

    /** SNR mask */
    final SnrMask snrmask;

    /** satellite ephemeris/clock (EPHOPT_???) */
    int sateph;

    /** AR mode (0:off,1:continuous,2:instantaneous,3:fix and hold) */
    int modear;

    /** GLONASS AR mode (0:off,1:on,2:auto cal,3:ext cal) */
    int glomodear;

    /** obs outage count to reset bias */
    int maxout;

    /** min lock count to fix ambiguity */
    int minlock;

    /** min fix count to hold ambiguity */
    int minfix;

    /** ionosphere option (IONOOPT_???) */
    int ionoopt;

    /** troposphere option (TROPOPT_???) */
    int tropopt;

    /** dynamics model (0:none,1:velociy,2:accel) */
    int dynamics;

    /** earth tide correction (0:off,1:solid,2:solid+otl+pole) */
    int tidecorr;

    /** number of filter iteration */
    int niter;

    /** code smoothing window size (0:none) */
    int codesmooth;

    /** interpolate reference obs (for post mission)  */
    int intpref;

    /** SBAS correction options */
    int sbascorr;

    /** SBAS satellite selection (0:all) */
    int sbassatsel;

    /** rover position for fixed mode */
    int rovpos;

    /**
     * base position for relative mode
     * 	 (0:pos in prcopt,  1:average of single pos,
     *  2:read from file, 3:rinex header, 4:rtcm pos)
     */
    int refpos;

    /** code/phase error ratio */
    double eratioL1;
    double eratioL2;
    double eratioL5;

    /** measurement error factor */
    double errPhaseA;
    double errPhaseB;
    double errPhaseC;
    double errDopplerFreq;

    /** initial-state std */
    double stdBias;
    double stdIono;
    double stdTrop;

    /** process-noise std */
    double prnBias;
    double prnIono;
    double prnTrop;
    double prnAcch;
    double prnAccv;

    /** satellite clock stability (sec/sec) */
    double sclkstab;

    /** AR validation threshold */
    double thresar_0, thresar_1, thresar_2, thresar_3;

    /** elevation mask of AR for rising satellite (deg) */
    double elmaskar;

    /** elevation mask to hold ambiguity (deg) */
    double elmaskhold;

    /** slip threshold of geometry-free phase (m)  */
    double thresslip;

    /** max difference of time (sec) */
    double maxtdiff;

    /** reject threshold of innovation (m) */
    double maxinno;

    /** reject threshold of gdop */
    double maxgdop;

    /** baseline length constraint {const,sigma} (m) */
    double baselineConst;
    double baselineSigma;

    /** rover position for fixed mode {x,y,z} (ecef) (m) */
    double ruX, ruY, ruZ;

    /** base position for relative mode {x,y,z} (ecef) (m) */
    double baseX, baseY, baseZ;

    /** antenna types {rover,base} */
    String anttypeBase, anttypeRover;

    /** antenna delta {{rov_e,rov_n,rov_u},{ref_e,ref_n,ref_u}} */
    double antdelRovE, antdelRovN, antdelRovU;
    double antdelRefE, antdelRefN, antdelRefU;

    /** XXX: receiver antenna parameters {rov,base} */
    // PcvT pcvrRover,pcvrBase;

    /** excluded satellites */
    final boolean exsats[];

    /** rinex options */
    String rnxoptBase, rnxoptRover;

    /** positioning options */
    final int posopt[];

    /** XXX: extended receiver error model */
    // ExtErr exterr;

    native void _loadDefaults();

    public ProcessingOptions() {
        exsats = new boolean[Constants.MAXSAT];
        posopt = new int[6];
        snrmask = new SnrMask();
        _loadDefaults();
    }


    public void setValues(ProcessingOptions src) {
        mode = src.mode;
        soltype = src.soltype;
        nf = src.nf;
        navsys = src.navsys;
        elmin = src.elmin;
        snrmask.setValues(src.snrmask);
        sateph = src.sateph;
        modear = src.modear;
        glomodear = src.glomodear;
        maxout = src.maxout;
        minlock = src.minlock;
        minfix = src.minfix;
        ionoopt = src.ionoopt;
        tropopt = src.tropopt;
        dynamics = src.dynamics;
        tidecorr = src.tidecorr;
        niter = src.niter;
        codesmooth = src.codesmooth;
        intpref = src.intpref;
        sbascorr = src.sbascorr;
        sbassatsel = src.sbassatsel;
        rovpos = src.rovpos;
        refpos = src.refpos;
        eratioL1 = src.eratioL1;
        eratioL2 = src.eratioL2;
        eratioL5 = src.eratioL5;
        errPhaseA = src.errPhaseA;
        errPhaseB = src.errPhaseB;
        errPhaseC = src.errPhaseC;
        errDopplerFreq = src.errDopplerFreq;
        stdBias = src.stdBias;
        stdIono = src.stdIono;
        stdTrop = src.stdTrop;
        prnBias = src.prnBias;
        prnIono = src.prnIono;
        prnTrop = src.prnTrop;
        prnAcch = src.prnAcch;
        prnAccv = src.prnAccv;
        sclkstab = src.sclkstab;
        thresar_0 = src.thresar_0;
        thresar_1 = src.thresar_1;
        thresar_2 = src.thresar_2;
        thresar_3 = src.thresar_3;
        elmaskar = src.elmaskar;
        elmaskhold = src.elmaskhold;
        thresslip = src.thresslip;
        maxtdiff = src.maxtdiff;
        maxinno = src.maxinno;
        maxgdop = src.maxgdop;
        baselineConst = src.baselineConst;
        baselineSigma = src.baselineSigma;
        ruX = src.ruX;
        ruY = src.ruY;
        ruZ = src.ruZ;
        baseX =src.baseX;
        baseY = src.baseY;
        baseZ = src.baseZ;
        anttypeBase = src.anttypeBase;
        anttypeRover = src.anttypeRover;
        antdelRovE = src.antdelRovE;
        antdelRovN = src.antdelRovN;
        antdelRovU = src.antdelRovU;
        antdelRefE = src.antdelRefE;
        antdelRefN = src.antdelRefN;
        antdelRefU = src.antdelRefU;
        System.arraycopy(src.exsats, 0, exsats, 0, exsats.length);
        rnxoptBase = src.rnxoptBase;
        rnxoptRover = src.rnxoptRover;
        System.arraycopy(src.posopt, 0, posopt, 0, posopt.length);
    }

    public PositioningMode getPositioningMode() {
        return PositioningMode.valueOf(mode);
    }

    public ProcessingOptions setPositioningMode(PositioningMode mode) {
        this.mode = mode.getRtklibId();
        return this;
    }

    /**
     * @return nf (1:L1,2:L1+L2,3:L1+L2+L5)
     */
    public int getNumberOfFrequencies() {
        return nf;
    }

    /**
     * @param nf the nf to set (1:L1,2:L1+L2,3:L1+L2+L5)
     */
    public void setNumberOfFrequencies(int nf) {
        if (nf < 1 || nf > Constants.NFREQ) throw new IllegalArgumentException();
        this.nf = nf;
    }

    /**
     * @return selected navigation system set
     */
    public Set<NavigationSystem> getNavigationSystem() {
        EnumSet<NavigationSystem> res = EnumSet.noneOf(NavigationSystem.class);
        for (NavigationSystem ns: NavigationSystem.values()) {
            if ((navsys & ns.getRtklibId()) != 0) res.add(ns);
        }

        return res;
    }

    /**
     * @param nss Navigation system set
     */
    public void setNavigationSystem(Set<NavigationSystem> nss) {
        int res = 0;
        for (NavigationSystem ns: nss) {
            res |= ns.getRtklibId();
        }
        navsys = res;
        if (DBG) Log.v(TAG, "setNavigationSystem() navsys: %x" + navsys);
    }

    /**
     * @return elevation mask angle (rad)
     */
    public double getElevationMask() {
        return elmin;
    }

    /**
     * @param elevation mask angle (rad)
     */
    public void setElevationMask(double elmin) {
        this.elmin = elmin;
    }

    /**
     * @param SNR mask
     */
    public void setSnrMask(int mask) {
        this.snrmask.setSnrMask(mask);
    }

    /**
     * @return SNR mask
     */
    public int getSnrMask() {
        return this.snrmask.getSnrMask();
    }

    /**
     * @return Rec dynamics
     */
    public boolean getRecDynamics() {
        return this.dynamics != 0;
    }

    /**
     * @param on rec dynamics
     */
    public void setRecDynamics(boolean on) {
        this.dynamics = on ? 1 : 0;
    }

    /**
     * @return Earth tides correction
     */
    public EarthTideCorrectionType getEarthTidersCorrection() {
        return EarthTideCorrectionType.valueOf(this.tidecorr);
    }

    /**
     * @param on Earth tides correction
     */
    public void setEarthTidesCorrection(EarthTideCorrectionType type) {
        this.tidecorr = type.getRtklibId();
    }

    /**
     * @return Ionosphere correction
     */
    public IonosphereOption getIonosphereCorrection() {
        return IonosphereOption.valueOf(ionoopt);
    }

    /**
     * @param Set Ionosphere correction
     */
    public void setIonosphereCorrection(IonosphereOption corr) {
        ionoopt = corr.getRtklibId();
    }

    /**
     * @return Troposphere correction
     */
    public TroposphereOption getTroposphereCorrection() {
        return TroposphereOption.valueOf(tropopt);
    }

    /**
     * @param Set Troposphere correction
     */
    public void setTroposphereCorrection(TroposphereOption corr) {
        tropopt = corr.getRtklibId();
    }

    /**
     * @return Satellite Ephemeris/Clock
     */
    public EphemerisOption getSatEphemerisOption() {
        return EphemerisOption.valueOf(sateph);
    }

    /**
     *
     * @param Set satellite Ephemeris/Clock
     */
    public void setSatEphemerisOption(EphemerisOption opt) {
        this.sateph = opt.getRtklibId();
    }

    /**
     * @return satellite antenna model enabled
     */
    public boolean isSatAntennaPcvEnabled() {
        return posopt[0] != 0;
    }

    /**
     * @param enable satellite antenna model
     */
    public void setSatAntennaPcvEnabled(boolean enable) {
        posopt[0] = enable ? 1 : 0;
    }

    /**
     *
     * @return receiver antenna model enabled
     */
    public boolean isReceiverAntennaPcvEnabled() {
        return posopt[1] != 0;
    }

    /**
     *
     * @param enable receiver antenna model
     */
    public void setReceiverAntennaPcvEnabled(boolean enable) {
        posopt[1] = enable ? 1 : 0;
    }

    /**
     * @return phase windup correction enabled
     */
    public boolean isPhaseWindupCorrectionEnabled() {
        return posopt[2] != 0;
    }

    /**
     * @param enable phase windup correction
     */
    public void setPhaseWindupCorrectionEnabled(boolean enable) {
        posopt[2] = enable ? 1 : 0;
    }

    /**
     * @return measurements of eclipsing satellite excluded
     */
    public boolean isExcludeEclipsingSatMeasurements() {
        return posopt[3] != 0;
    }

    /**
     * @param exclude measurements of eclipsing satellite
     */
    public void setExcludeEclipsingSatMeasurements(boolean exclude) {
        posopt[3] = exclude ? 1 : 0;
    }

    /**
     * @return raim fde (failure detection and exclution) enabled
     */
    public boolean isRaimFdeEnabled() {
        return posopt[4] != 0;
    }

    /**
     *
     * @param enable raim fde (failure detection and exclution)
     */
    public void setRaimFdeEnabled(boolean enable) {
        posopt[4] = enable ? 1 : 0;
    }

}
