package gpsplus.rtkgps.utils;

import org.proj4.CRSRegistry;

public class UTM {
    private double mLat,mLon;
    private String epsgZone;
    private int miZone;
    private char mUTMZoneLetter;
    private String mUTMZone;
    private String CRSString;

    public UTM(double lat, double lon) {
        mLat = lat;
        mLon = lon;
        mUTMZone = getUTMZone();
        epsgZone = getEpsgZone();
    }

    public String getUTMZone()
    {
        int iZone;
        char cLetter;
        iZone= (int) Math.floor(mLon/6+31);
        if (mLat<-72)
            cLetter='C';
        else if (mLat<-64)
            cLetter='D';
        else if (mLat<-56)
            cLetter='E';
        else if (mLat<-48)
            cLetter='F';
        else if (mLat<-40)
            cLetter='G';
        else if (mLat<-32)
            cLetter='H';
        else if (mLat<-24)
            cLetter='J';
        else if (mLat<-16)
            cLetter='K';
        else if (mLat<-8)
            cLetter='L';
        else if (mLat<0)
            cLetter='M';
        else if (mLat<8)
            cLetter='N';
        else if (mLat<16)
            cLetter='P';
        else if (mLat<24)
            cLetter='Q';
        else if (mLat<32)
            cLetter='R';
        else if (mLat<40)
            cLetter='S';
        else if (mLat<48)
            cLetter='T';
        else if (mLat<56)
            cLetter='U';
        else if (mLat<64)
            cLetter='V';
        else if (mLat<72)
            cLetter='W';
        else
            cLetter='X';
        miZone = iZone;
        mUTMZoneLetter = cLetter;
        return (String.valueOf(iZone)+cLetter);
    }

    public String getEpsgZone() {
        if(mUTMZoneLetter>'M') {
            epsgZone = "EPSG:326" + String.valueOf(miZone);
            switch (miZone){
                case 1:
                    CRSString = CRSRegistry.EPSG_32601;
                    break;
                case 2:
                    CRSString = CRSRegistry.EPSG_32602;
                    break;
                case 3:
                    CRSString = CRSRegistry.EPSG_32603;
                    break;
                case 4:
                    CRSString = CRSRegistry.EPSG_32604;
                    break;
                case 5:
                    CRSString = CRSRegistry.EPSG_32605;
                    break;
                case 6:
                    CRSString = CRSRegistry.EPSG_32606;
                    break;
                case 7:
                    CRSString = CRSRegistry.EPSG_32607;
                    break;
                case 8:
                    CRSString = CRSRegistry.EPSG_32608;
                    break;
                case 9:
                    CRSString = CRSRegistry.EPSG_32609;
                    break;
                case 10:
                    CRSString = CRSRegistry.EPSG_32610;
                    break;
                case 11:
                    CRSString = CRSRegistry.EPSG_32611;
                    break;
                case 12:
                    CRSString = CRSRegistry.EPSG_32612;
                    break;
                case 13:
                    CRSString = CRSRegistry.EPSG_32613;
                    break;
                case 14:
                    CRSString = CRSRegistry.EPSG_32614;
                    break;
                case 15:
                    CRSString = CRSRegistry.EPSG_32615;
                    break;
                case 16:
                    CRSString = CRSRegistry.EPSG_32616;
                    break;
                case 17:
                    CRSString = CRSRegistry.EPSG_32617;
                    break;
                case 18:
                    CRSString = CRSRegistry.EPSG_32618;
                    break;
                case 19:
                    CRSString = CRSRegistry.EPSG_32619;
                    break;
                case 20:
                    CRSString = CRSRegistry.EPSG_32620;
                    break;
                case 21:
                    CRSString = CRSRegistry.EPSG_32621;
                    break;
                case 22:
                    CRSString = CRSRegistry.EPSG_32622;
                    break;
                case 23:
                    CRSString = CRSRegistry.EPSG_32623;
                    break;
                case 24:
                    CRSString = CRSRegistry.EPSG_32624;
                    break;
                case 25:
                    CRSString = CRSRegistry.EPSG_32625;
                    break;
                case 26:
                    CRSString = CRSRegistry.EPSG_32626;
                    break;
                case 27:
                    CRSString = CRSRegistry.EPSG_32627;
                    break;
                case 28:
                    CRSString = CRSRegistry.EPSG_32628;
                    break;
                case 29:
                    CRSString = CRSRegistry.EPSG_32629;
                    break;
                case 30:
                    CRSString = CRSRegistry.EPSG_32630;
                    break;
                case 31:
                    CRSString = CRSRegistry.EPSG_32631;
                    break;
                case 32:
                    CRSString = CRSRegistry.EPSG_32632;
                    break;
                case 33:
                    CRSString = CRSRegistry.EPSG_32633;
                    break;
                case 34:
                    CRSString = CRSRegistry.EPSG_32634;
                    break;
                case 35:
                    CRSString = CRSRegistry.EPSG_32635;
                    break;
                case 36:
                    CRSString = CRSRegistry.EPSG_32636;
                    break;
                case 37:
                    CRSString = CRSRegistry.EPSG_32637;
                    break;
                case 38:
                    CRSString = CRSRegistry.EPSG_32638;
                    break;
                case 39:
                    CRSString = CRSRegistry.EPSG_32639;
                    break;
                case 40:
                    CRSString = CRSRegistry.EPSG_32640;
                    break;
                case 41:
                    CRSString = CRSRegistry.EPSG_32641;
                    break;
                case 42:
                    CRSString = CRSRegistry.EPSG_32642;
                    break;
                case 43:
                    CRSString = CRSRegistry.EPSG_32643;
                    break;
                case 44:
                    CRSString = CRSRegistry.EPSG_32644;
                    break;
                case 45:
                    CRSString = CRSRegistry.EPSG_32645;
                    break;
                case 46:
                    CRSString = CRSRegistry.EPSG_32646;
                    break;
                case 47:
                    CRSString = CRSRegistry.EPSG_32647;
                    break;
                case 48:
                    CRSString = CRSRegistry.EPSG_32648;
                    break;
                case 49:
                    CRSString = CRSRegistry.EPSG_32649;
                    break;
                case 50:
                    CRSString = CRSRegistry.EPSG_32650;
                    break;
                case 51:
                    CRSString = CRSRegistry.EPSG_32651;
                    break;
                case 52:
                    CRSString = CRSRegistry.EPSG_32652;
                    break;
                case 53:
                    CRSString = CRSRegistry.EPSG_32653;
                    break;
                case 54:
                    CRSString = CRSRegistry.EPSG_32654;
                    break;
                case 55:
                    CRSString = CRSRegistry.EPSG_32655;
                    break;
                case 56:
                    CRSString = CRSRegistry.EPSG_32656;
                    break;
                case 57:
                    CRSString = CRSRegistry.EPSG_32657;
                    break;
                case 58:
                    CRSString = CRSRegistry.EPSG_32658;
                    break;
                case 59:
                    CRSString = CRSRegistry.EPSG_32659;
                    break;
                case 60:
                    CRSString = CRSRegistry.EPSG_32660;
                    break;
            }
        }
        else {
            epsgZone = "EPSG:327" + String.valueOf(miZone);
            switch (miZone){
                case 1:
                    CRSString = CRSRegistry.EPSG_32701;
                    break;
                case 2:
                    CRSString = CRSRegistry.EPSG_32702;
                    break;
                case 3:
                    CRSString = CRSRegistry.EPSG_32703;
                    break;
                case 4:
                    CRSString = CRSRegistry.EPSG_32704;
                    break;
                case 5:
                    CRSString = CRSRegistry.EPSG_32705;
                    break;
                case 6:
                    CRSString = CRSRegistry.EPSG_32706;
                    break;
                case 7:
                    CRSString = CRSRegistry.EPSG_32707;
                    break;
                case 8:
                    CRSString = CRSRegistry.EPSG_32708;
                    break;
                case 9:
                    CRSString = CRSRegistry.EPSG_32709;
                    break;
                case 10:
                    CRSString = CRSRegistry.EPSG_32710;
                    break;
                case 11:
                    CRSString = CRSRegistry.EPSG_32711;
                    break;
                case 12:
                    CRSString = CRSRegistry.EPSG_32712;
                    break;
                case 13:
                    CRSString = CRSRegistry.EPSG_32713;
                    break;
                case 14:
                    CRSString = CRSRegistry.EPSG_32714;
                    break;
                case 15:
                    CRSString = CRSRegistry.EPSG_32715;
                    break;
                case 16:
                    CRSString = CRSRegistry.EPSG_32716;
                    break;
                case 17:
                    CRSString = CRSRegistry.EPSG_32717;
                    break;
                case 18:
                    CRSString = CRSRegistry.EPSG_32718;
                    break;
                case 19:
                    CRSString = CRSRegistry.EPSG_32719;
                    break;
                case 20:
                    CRSString = CRSRegistry.EPSG_32720;
                    break;
                case 21:
                    CRSString = CRSRegistry.EPSG_32721;
                    break;
                case 22:
                    CRSString = CRSRegistry.EPSG_32722;
                    break;
                case 23:
                    CRSString = CRSRegistry.EPSG_32723;
                    break;
                case 24:
                    CRSString = CRSRegistry.EPSG_32724;
                    break;
                case 25:
                    CRSString = CRSRegistry.EPSG_32725;
                    break;
                case 26:
                    CRSString = CRSRegistry.EPSG_32726;
                    break;
                case 27:
                    CRSString = CRSRegistry.EPSG_32727;
                    break;
                case 28:
                    CRSString = CRSRegistry.EPSG_32728;
                    break;
                case 29:
                    CRSString = CRSRegistry.EPSG_32729;
                    break;
                case 30:
                    CRSString = CRSRegistry.EPSG_32730;
                    break;
                case 31:
                    CRSString = CRSRegistry.EPSG_32731;
                    break;
                case 32:
                    CRSString = CRSRegistry.EPSG_32732;
                    break;
                case 33:
                    CRSString = CRSRegistry.EPSG_32733;
                    break;
                case 34:
                    CRSString = CRSRegistry.EPSG_32734;
                    break;
                case 35:
                    CRSString = CRSRegistry.EPSG_32735;
                    break;
                case 36:
                    CRSString = CRSRegistry.EPSG_32736;
                    break;
                case 37:
                    CRSString = CRSRegistry.EPSG_32737;
                    break;
                case 38:
                    CRSString = CRSRegistry.EPSG_32738;
                    break;
                case 39:
                    CRSString = CRSRegistry.EPSG_32739;
                    break;
                case 40:
                    CRSString = CRSRegistry.EPSG_32740;
                    break;
                case 41:
                    CRSString = CRSRegistry.EPSG_32741;
                    break;
                case 42:
                    CRSString = CRSRegistry.EPSG_32742;
                    break;
                case 43:
                    CRSString = CRSRegistry.EPSG_32743;
                    break;
                case 44:
                    CRSString = CRSRegistry.EPSG_32744;
                    break;
                case 45:
                    CRSString = CRSRegistry.EPSG_32745;
                    break;
                case 46:
                    CRSString = CRSRegistry.EPSG_32746;
                    break;
                case 47:
                    CRSString = CRSRegistry.EPSG_32747;
                    break;
                case 48:
                    CRSString = CRSRegistry.EPSG_32748;
                    break;
                case 49:
                    CRSString = CRSRegistry.EPSG_32749;
                    break;
                case 50:
                    CRSString = CRSRegistry.EPSG_32750;
                    break;
                case 51:
                    CRSString = CRSRegistry.EPSG_32751;
                    break;
                case 52:
                    CRSString = CRSRegistry.EPSG_32752;
                    break;
                case 53:
                    CRSString = CRSRegistry.EPSG_32753;
                    break;
                case 54:
                    CRSString = CRSRegistry.EPSG_32754;
                    break;
                case 55:
                    CRSString = CRSRegistry.EPSG_32755;
                    break;
                case 56:
                    CRSString = CRSRegistry.EPSG_32756;
                    break;
                case 57:
                    CRSString = CRSRegistry.EPSG_32757;
                    break;
                case 58:
                    CRSString = CRSRegistry.EPSG_32758;
                    break;
                case 59:
                    CRSString = CRSRegistry.EPSG_32759;
                    break;
                case 60:
                    CRSString = CRSRegistry.EPSG_32760;
                    break;
            }
        }
        return epsgZone;
    }
    public String getCRSString()
    {
        return CRSString;
    }
}
