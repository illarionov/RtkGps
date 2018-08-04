package gpsplus.rtklib.constants;


public class Constants {

    /** number of carrier frequencies */
    public final static int NFREQ = 3;

    /** min satellite PRN number of GPS */
    public final static int MINPRNGPS = 1;
    /** max satellite PRN number of GPS */
    public final static int MAXPRNGPS = 32;
    /** number of GPS satellites */
    public final static int NSATGPS = MAXPRNGPS-MINPRNGPS+1;

    /** min satellite slot number of GLONASS */
    public final static int MINPRNGLO = 1;
    /** max satellite slot number of GLONASS */
    public final static int MAXPRNGLO = 27;
    /** number of GLONASS satellites */
    public final static int NSATGLO = MAXPRNGLO-MINPRNGLO+1;

    /** min satellite PRN number of Galileo */
    public final static int MINPRNGAL = 1;
    /** max satellite PRN number of Galileo */
    public final static int MAXPRNGAL = 36;
    /** number of Galileo satellites */
    public final static int NSATGAL = MAXPRNGAL-MINPRNGAL+1;

    /** min satellite PRN number of QZSS */
    public final static int MINPRNQZS = 193;
    /** max satellite PRN number of QZSS */
    public final static int MAXPRNQZS = 195;
    /** min satellite PRN number of QZSS SAIF */
    public final static int MINPRNQZS_S = 183;
    /** max satellite PRN number of QZSS SAIF */
    public final static int MAXPRNQZS_S = 185;
    /** number of QZSS satellites */
    public final static int NSATQZS = MAXPRNQZS-MINPRNQZS+1;

    /** min satellite sat number of BeiDou */
    public final static int MINPRNCMP = 1;
    /** max satellite sat number of BeiDou */
    public final static int MAXPRNCMP = 35;
    /** number of Compass satellites */
    public final static int NSATCMP = MAXPRNCMP-MINPRNCMP+1;

    /** min satellite PRN number of SBAS */
    public final static int MINPRNSBS = 120;
    /** max satellite PRN number of SBAS */
    public final static int MAXPRNSBS = 142;
    /** number of SBAS satellites */
    public final static int NSATSBS = MAXPRNSBS-MINPRNSBS+1;

    public final static int MAXSAT = NSATGPS+NSATGLO+NSATGAL+NSATQZS+NSATCMP+NSATSBS;

    /** max number of solution buffer */
    public final static int MAXSOLBUF = 256;

}
