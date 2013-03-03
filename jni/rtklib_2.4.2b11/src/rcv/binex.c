/*------------------------------------------------------------------------------
* binex.c : binex dependent functions
*
*          Copyright (C) 2013 by T.TAKASU, All rights reserved.
*
* reference :
*     [1] UNAVCO, BINEX: Binary exchange format
*         (http://binex.unavco.org/binex.html)
*
* version : $Revision:$ $Date:$
* history : 2013/02/20 1.0 new
*-----------------------------------------------------------------------------*/
#include "rtklib.h"

static const char rcsid[]="$Id:$";

#define BNXSYNC1    0xC2    /* binex sync (little-endian,regular-crc) */
#define BNXSYNC2    0xE2    /* binex sync (big-endian   ,regular-crc) */
#define BNXSYNC3    0xC8    /* binex sync (little-endian,enhanced-crc) */
#define BNXSYNC4    0xE8    /* binex sync (big-endian   ,enhanced-crc) */

#define BNXSYNC1R   0xD2    /* binex sync (little-endian,regular-crc,rev) */
#define BNXSYNC2R   0xF2    /* binex sync (big-endian   ,regular-crc,rev) */
#define BNXSYNC3R   0xD8    /* binex sync (little-endian,enhanced-crc,rev) */
#define BNXSYNC4R   0xF8    /* binex sync (big-endian   ,enhanced-crc,rev) */

#define POLYCRC16   0x1021u /* CRC16 polynomial (x^16+x^12+x^5+1) (ref [1]) */

#define MIN(x,y)    ((x)<(y)?(x):(y))

/* crc 16 table genrated by util/gencrc --------------------------------------*/
static const unsigned short tbl_CRC16[]={
    0x0000,0x1021,0x2042,0x3063,0x4084,0x50A5,0x60C6,0x70E7,
    0x8108,0x9129,0xA14A,0xB16B,0xC18C,0xD1AD,0xE1CE,0xF1EF,
    0x1231,0x0210,0x3273,0x2252,0x52B5,0x4294,0x72F7,0x62D6,
    0x9339,0x8318,0xB37B,0xA35A,0xD3BD,0xC39C,0xF3FF,0xE3DE,
    0x2462,0x3443,0x0420,0x1401,0x64E6,0x74C7,0x44A4,0x5485,
    0xA56A,0xB54B,0x8528,0x9509,0xE5EE,0xF5CF,0xC5AC,0xD58D,
    0x3653,0x2672,0x1611,0x0630,0x76D7,0x66F6,0x5695,0x46B4,
    0xB75B,0xA77A,0x9719,0x8738,0xF7DF,0xE7FE,0xD79D,0xC7BC,
    0x48C4,0x58E5,0x6886,0x78A7,0x0840,0x1861,0x2802,0x3823,
    0xC9CC,0xD9ED,0xE98E,0xF9AF,0x8948,0x9969,0xA90A,0xB92B,
    0x5AF5,0x4AD4,0x7AB7,0x6A96,0x1A71,0x0A50,0x3A33,0x2A12,
    0xDBFD,0xCBDC,0xFBBF,0xEB9E,0x9B79,0x8B58,0xBB3B,0xAB1A,
    0x6CA6,0x7C87,0x4CE4,0x5CC5,0x2C22,0x3C03,0x0C60,0x1C41,
    0xEDAE,0xFD8F,0xCDEC,0xDDCD,0xAD2A,0xBD0B,0x8D68,0x9D49,
    0x7E97,0x6EB6,0x5ED5,0x4EF4,0x3E13,0x2E32,0x1E51,0x0E70,
    0xFF9F,0xEFBE,0xDFDD,0xCFFC,0xBF1B,0xAF3A,0x9F59,0x8F78,
    0x9188,0x81A9,0xB1CA,0xA1EB,0xD10C,0xC12D,0xF14E,0xE16F,
    0x1080,0x00A1,0x30C2,0x20E3,0x5004,0x4025,0x7046,0x6067,
    0x83B9,0x9398,0xA3FB,0xB3DA,0xC33D,0xD31C,0xE37F,0xF35E,
    0x02B1,0x1290,0x22F3,0x32D2,0x4235,0x5214,0x6277,0x7256,
    0xB5EA,0xA5CB,0x95A8,0x8589,0xF56E,0xE54F,0xD52C,0xC50D,
    0x34E2,0x24C3,0x14A0,0x0481,0x7466,0x6447,0x5424,0x4405,
    0xA7DB,0xB7FA,0x8799,0x97B8,0xE75F,0xF77E,0xC71D,0xD73C,
    0x26D3,0x36F2,0x0691,0x16B0,0x6657,0x7676,0x4615,0x5634,
    0xD94C,0xC96D,0xF90E,0xE92F,0x99C8,0x89E9,0xB98A,0xA9AB,
    0x5844,0x4865,0x7806,0x6827,0x18C0,0x08E1,0x3882,0x28A3,
    0xCB7D,0xDB5C,0xEB3F,0xFB1E,0x8BF9,0x9BD8,0xABBB,0xBB9A,
    0x4A75,0x5A54,0x6A37,0x7A16,0x0AF1,0x1AD0,0x2AB3,0x3A92,
    0xFD2E,0xED0F,0xDD6C,0xCD4D,0xBDAA,0xAD8B,0x9DE8,0x8DC9,
    0x7C26,0x6C07,0x5C64,0x4C45,0x3CA2,0x2C83,0x1CE0,0x0CC1,
    0xEF1F,0xFF3E,0xCF5D,0xDF7C,0xAF9B,0xBFBA,0x8FD9,0x9FF8,
    0x6E17,0x7E36,0x4E55,0x5E74,0x2E93,0x3EB2,0x0ED1,0x1EF0
};
/* ura table -----------------------------------------------------------------*/
static const double ura_eph[]={
    2.4,3.4,4.85,6.85,9.65,13.65,24.0,48.0,96.0,192.0,384.0,768.0,1536.0,
    3072.0,6144.0,0.0
};
/* get fields (big-endian) ---------------------------------------------------*/
#define U1(p) (*((unsigned char *)(p)))
#define I1(p) (*((char *)(p)))

static unsigned short U2(unsigned char *p)
{
    unsigned short value;
    unsigned char *q=(unsigned char *)&value+1;
    int i;
    for (i=0;i<2;i++) *q--=*p++;
    return value;
}
static unsigned int U4(unsigned char *p)
{
    unsigned int value;
    unsigned char *q=(unsigned char *)&value+3;
    int i;
    for (i=0;i<4;i++) *q--=*p++;
    return value;
}
static int I4(unsigned char *p)
{
    return (int)U4(p);
}
static float R4(unsigned char *p)
{
    float value;
    unsigned char *q=(unsigned char *)&value+3;
    int i;
    for (i=0;i<4;i++) *q--=*p++;
    return value;
}
static double R8(unsigned char *p)
{
    double value;
    unsigned char *q=(unsigned char *)&value+7;
    int i;
    for (i=0;i<8;i++) *q--=*p++;
    return value;
}
/* get binex 1-4 byte unsigned integer (big endian) --------------------------*/
static int getbnxi(unsigned char *p, unsigned int *val)
{
    int i;
    
    for (*val=0,i=0;i<3;i++) {
        *val=(*val<<7)+(p[i]&0x7F);
        if (!(p[i]&0x80)) return i+1;
    }
    *val=(*val<<8)+p[i];
    return 4;
}
/* checksum 8 parity ---------------------------------------------------------*/
static unsigned char csum8(const unsigned char *buff, int len)
{
    unsigned char cs=0;
    int i;
    
    for (i=0;i<len;i++) {
        cs^=buff[i];
    }
    return cs;
}
/* crc-16 parity ------------------------------------------------------------*/
static unsigned short crc16(const unsigned char *buff, int len)
{
    unsigned short crc=0;
    int i;
    
    for (i=0;i<len;i++) {
        crc=(crc<<8)^tbl_CRC16[(crc>>8)^buff[i]];
    }
    return crc;
}
/* adjust weekly rollover of gps time ----------------------------------------*/
static gtime_t adjweek(gtime_t time, double tow)
{
    double tow_p;
    int week;
    tow_p=time2gpst(time,&week);
    if      (tow<tow_p-302400.0) tow+=604800.0;
    else if (tow>tow_p+302400.0) tow-=604800.0;
    return gpst2time(week,tow);
}
/* adjust daily rollover of time ---------------------------------------------*/
static gtime_t adjday(gtime_t time, double tod)
{
    double ep[6],tod_p;
    time2epoch(time,ep);
    tod_p=ep[3]*3600.0+ep[4]*60.0+ep[5];
    if      (tod<tod_p-43200.0) tod+=86400.0;
    else if (tod>tod_p+43200.0) tod-=86400.0;
    ep[3]=ep[4]=ep[5]=0.0;
    return timeadd(epoch2time(ep),tod);
}
/* ura value (m) to ura index ------------------------------------------------*/
static int uraindex(double value)
{
    int i;
    for (i=0;i<15;i++) if (ura_eph[i]>=value) break;
    return i;
}
/* decode binex mesaage 0x00-00: comment -------------------------------------*/
static int decode_bnx_00_00(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-00: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-01: program or software package -----------------*/
static int decode_bnx_00_01(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-01: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-02: program operator ----------------------------*/
static int decode_bnx_00_02(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-02: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-03: reserved ------------------------------------*/
static int decode_bnx_00_03(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-03: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-04: site name/description -----------------------*/
static int decode_bnx_00_04(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-04: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-05: site number ---------------------------------*/
static int decode_bnx_00_05(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-05: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-06: monumnent name ------------------------------*/
static int decode_bnx_00_06(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-06: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-07: monumnent number ----------------------------*/
static int decode_bnx_00_07(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-07: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-08: marker name ---------------------------------*/
static int decode_bnx_00_08(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-08: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-09: marker number -------------------------------*/
static int decode_bnx_00_09(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-09: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-0a: reference point name ------------------------*/
static int decode_bnx_00_0a(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-0a: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-0b: reference point number ----------------------*/
static int decode_bnx_00_0b(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-0b: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-0c: date esttablished ---------------------------*/
static int decode_bnx_00_0c(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-0c: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-0d: reserved ------------------------------------*/
static int decode_bnx_00_0d(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-0d: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-0e: reserved ------------------------------------*/
static int decode_bnx_00_0e(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-0e: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-0f: 4-character id ------------------------------*/
static int decode_bnx_00_0f(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-0f: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-10: project name --------------------------------*/
static int decode_bnx_00_10(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-10: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-11: principal investigator for this project -----*/
static int decode_bnx_00_11(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-11: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-12: pi's agency/institution ---------------------*/
static int decode_bnx_00_12(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-12: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-13: pi's contact information --------------------*/
static int decode_bnx_00_13(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-13: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-14: site operator -------------------------------*/
static int decode_bnx_00_14(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-14: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-15: site operator's agency/institution ----------*/
static int decode_bnx_00_15(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-15: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-16: site operator's contact information ---------*/
static int decode_bnx_00_16(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-16: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-17: antenna type --------------------------------*/
static int decode_bnx_00_17(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-17: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-18: antenna number ------------------------------*/
static int decode_bnx_00_18(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-18: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-19: receiver type -------------------------------*/
static int decode_bnx_00_19(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-19: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-1a: receiver number -----------------------------*/
static int decode_bnx_00_1a(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-1a: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-1b: receiver firmware version -------------------*/
static int decode_bnx_00_1b(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-1b: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-1c: antenna mount description -------------------*/
static int decode_bnx_00_1c(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-1c: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-1d: antenna xyz position ------------------------*/
static int decode_bnx_00_1d(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-1d: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-1e: antenna geographic position -----------------*/
static int decode_bnx_00_1e(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-1e: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-1f: antenna offset from reference point ---------*/
static int decode_bnx_00_1f(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-1f: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-20: antenna radome type -------------------------*/
static int decode_bnx_00_20(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-20: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-21: antenna radome number -----------------------*/
static int decode_bnx_00_21(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-21: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-22: geocode -------------------------------------*/
static int decode_bnx_00_22(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-22: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00-7f: notes/additional information ----------------*/
static int decode_bnx_00_7f(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x00-7f: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x00: site/monument/marker/ref point/setup metadata --*/
static int decode_bnx_00(raw_t *raw, unsigned char *buff, int len)
{
    const static double gpst0[]={1980,1,6,0,0,0};
    char *msg;
    unsigned char *p=buff;
    unsigned int min,qsec,src,fid;
    int n=6;
    
    min =U4(p); p+=4;
    qsec=U1(p); p+=1;
    src =U1(p); p+=1;
    n+=getbnxi(p,&fid);
    raw->time=timeadd(epoch2time(gpst0),min*60.0+qsec*0.25);
    
    if (raw->outtype) {
        msg=raw->msgtype+strlen(raw->msgtype);
        sprintf(msg," fid=%02X time=%s src=%d",fid,time_str(raw->time,0),src);
    }
    switch (fid) {
        case 0x00: return decode_bnx_00_00(raw,buff+n,len-n);
        case 0x01: return decode_bnx_00_01(raw,buff+n,len-n);
        case 0x02: return decode_bnx_00_02(raw,buff+n,len-n);
        case 0x03: return decode_bnx_00_03(raw,buff+n,len-n);
        case 0x04: return decode_bnx_00_04(raw,buff+n,len-n);
        case 0x05: return decode_bnx_00_05(raw,buff+n,len-n);
        case 0x06: return decode_bnx_00_06(raw,buff+n,len-n);
        case 0x07: return decode_bnx_00_07(raw,buff+n,len-n);
        case 0x08: return decode_bnx_00_08(raw,buff+n,len-n);
        case 0x09: return decode_bnx_00_09(raw,buff+n,len-n);
        case 0x0A: return decode_bnx_00_0a(raw,buff+n,len-n);
        case 0x0B: return decode_bnx_00_0b(raw,buff+n,len-n);
        case 0x0C: return decode_bnx_00_0c(raw,buff+n,len-n);
        case 0x0D: return decode_bnx_00_0d(raw,buff+n,len-n);
        case 0x0E: return decode_bnx_00_0e(raw,buff+n,len-n);
        case 0x0F: return decode_bnx_00_0f(raw,buff+n,len-n);
        case 0x10: return decode_bnx_00_10(raw,buff+n,len-n);
        case 0x11: return decode_bnx_00_11(raw,buff+n,len-n);
        case 0x12: return decode_bnx_00_12(raw,buff+n,len-n);
        case 0x13: return decode_bnx_00_13(raw,buff+n,len-n);
        case 0x14: return decode_bnx_00_14(raw,buff+n,len-n);
        case 0x15: return decode_bnx_00_15(raw,buff+n,len-n);
        case 0x16: return decode_bnx_00_16(raw,buff+n,len-n);
        case 0x17: return decode_bnx_00_17(raw,buff+n,len-n);
        case 0x18: return decode_bnx_00_18(raw,buff+n,len-n);
        case 0x19: return decode_bnx_00_19(raw,buff+n,len-n);
        case 0x1A: return decode_bnx_00_1a(raw,buff+n,len-n);
        case 0x1B: return decode_bnx_00_1b(raw,buff+n,len-n);
        case 0x1C: return decode_bnx_00_1c(raw,buff+n,len-n);
        case 0x1D: return decode_bnx_00_1d(raw,buff+n,len-n);
        case 0x1E: return decode_bnx_00_1e(raw,buff+n,len-n);
        case 0x1F: return decode_bnx_00_1f(raw,buff+n,len-n);
        case 0x20: return decode_bnx_00_20(raw,buff+n,len-n);
        case 0x21: return decode_bnx_00_21(raw,buff+n,len-n);
        case 0x22: return decode_bnx_00_22(raw,buff+n,len-n);
        case 0x7F: return decode_bnx_00_7f(raw,buff+n,len-n);
    }
    return 0;
}
/* decode binex mesaage 0x01-00: coded (raw bytes) gnss ephemeris ------------*/
static int decode_bnx_01_00(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x01-00: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x01-01: decoded gps ephmemeris ----------------------*/
static int decode_bnx_01_01(raw_t *raw, unsigned char *buff, int len)
{
    eph_t eph={0};
    unsigned char *p=buff;
    double tow,ura,sqrtA;
    int prn,flag;
    
    trace(4,"binex 0x01-01: len=%d\n",len);
    
    if (len>=127) {
        prn       =U1(p)+1;      p+=1;
        eph.week  =U2(p);        p+=2;
        tow       =I4(p);        p+=4;
        eph.toes  =I4(p);        p+=4;
        eph.tgd[0]=R4(p);        p+=4;
        eph.iodc  =I4(p);        p+=4;
        eph.f2    =R4(p);        p+=4;
        eph.f1    =R4(p);        p+=4;
        eph.f0    =R4(p);        p+=4;
        eph.iode  =I4(p);        p+=4;
        eph.deln  =R4(p)*SC2RAD; p+=4;
        eph.M0    =R8(p);        p+=8;
        eph.e     =R8(p);        p+=8;
        sqrtA     =R8(p);        p+=8;
        eph.cic   =R4(p);        p+=4;
        eph.crc   =R4(p);        p+=4;
        eph.cis   =R4(p);        p+=4;
        eph.crs   =R4(p);        p+=4;
        eph.cuc   =R4(p);        p+=4;
        eph.cus   =R4(p);        p+=4;
        eph.OMG0  =R8(p);        p+=8;
        eph.omg   =R8(p);        p+=8;
        eph.i0    =R8(p);        p+=8;
        eph.OMGd  =R4(p)*SC2RAD; p+=4;
        eph.idot  =R4(p)*SC2RAD; p+=4;
        ura       =R4(p)*0.1;    p+=4;
        eph.svh   =U2(p);        p+=2;
        flag      =U2(p);
    }
    else {
        trace(2,"binex 0x01-01: length error len=%d\n",len);
        return -1;
    }
    if (!(eph.sat=satno(SYS_GPS,prn))) {
        trace(2,"binex 0x01-01: satellite error prn=%d\n",prn);
        return -1;
    }
    eph.A=sqrtA*sqrtA;
    eph.toe=gpst2time(eph.week,eph.toes);
    eph.toc=gpst2time(eph.week,eph.toes);
    eph.ttr=adjweek(eph.toe,tow);
    eph.fit=flag&0xFF;
    eph.flag=(flag>>8)&0x01;
    eph.code=(flag>>9)&0x03;
    eph.sva=uraindex(ura);
    
    if (!strstr(raw->opt,"-EPHALL")) {
        if (raw->nav.eph[eph.sat-1].iode==eph.iode&&
            raw->nav.eph[eph.sat-1].iodc==eph.iodc) return 0; /* unchanged */
    }
    raw->nav.eph[eph.sat-1]=eph;
    raw->ephsat=eph.sat;
    return 2;
}
/* decode binex mesaage 0x01-02: decoded glonass ephmemeris ------------------*/
static int decode_bnx_01_02(raw_t *raw, unsigned char *buff, int len)
{
    geph_t geph={0};
    unsigned char *p=buff;
    double tod,tof,tau_gps;
    int prn,day,leap;
    
    trace(4,"binex 0x01-02: len=%d\n",len);
    
    if (len>=119) {
        prn        =U1(p)+1;   p+=1;
        day        =U2(p);     p+=2;
        tod        =U4(p);     p+=4;
        geph.taun  =-R8(p);    p+=8;
        geph.gamn  =R8(p);     p+=8;
        tof        =U4(p);     p+=4;
        geph.pos[0]=R8(p)*1E3; p+=8;
        geph.vel[0]=R8(p)*1E3; p+=8;
        geph.acc[0]=R8(p)*1E3; p+=8;
        geph.pos[1]=R8(p)*1E3; p+=8;
        geph.vel[1]=R8(p)*1E3; p+=8;
        geph.acc[1]=R8(p)*1E3; p+=8;
        geph.pos[2]=R8(p)*1E3; p+=8;
        geph.vel[2]=R8(p)*1E3; p+=8;
        geph.acc[2]=R8(p)*1E3; p+=8;
        geph.svh   =U1(p)&0x1; p+=1;
        geph.frq   =I1(p);     p+=1;
        geph.age   =U1(p);     p+=1;
        leap       =U1(p);     p+=1;
        tau_gps    =R8(p);     p+=8;
        geph.dtaun =R8(p);
    }
    else {
        trace(2,"binex 0x01-02: length error len=%d\n",len);
        return -1;
    }
    if (!(geph.sat=satno(SYS_GLO,prn))) {
        trace(2,"binex 0x01-02: satellite error prn=%d\n",prn);
        return -1;
    }
    if (raw->time.time==0) return 0;
    geph.toe=utc2gpst(adjday(raw->time,tod-10800.0));
    geph.tof=utc2gpst(adjday(raw->time,tof-10800.0));
    geph.iode=(int)(fmod(tod+10800.0,86400.0)/900.0+0.5);
    
    if (!strstr(raw->opt,"-EPHALL")) {
        if (fabs(timediff(geph.toe,raw->nav.geph[prn-MINPRNGLO].toe))<1.0&&
            geph.svh==raw->nav.geph[prn-MINPRNGLO].svh) return 0; /* unchanged */
    }
    raw->nav.geph[prn-1]=geph;
    raw->ephsat=geph.sat;
    return 2;
}
/* decode binex mesaage 0x01-03: decoded sbas ephmemeris ---------------------*/
static int decode_bnx_01_03(raw_t *raw, unsigned char *buff, int len)
{
    seph_t seph={0};
    unsigned char *p=buff;
    double tow,tod,tof;
    int prn,week,iodn;
    
    trace(4,"binex 0x01-03: len=%d\n",len);
    
    if (len>=98) {
        prn        =U1(p);     p+=1;
        week       =U2(p);     p+=2;
        tow        =U4(p);     p+=4;
        seph.af0   =R8(p);     p+=8;
        tod        =R4(p);     p+=4;
        tof        =U4(p);     p+=4;
        seph.pos[0]=R8(p)*1E3; p+=8;
        seph.vel[0]=R8(p)*1E3; p+=8;
        seph.acc[0]=R8(p)*1E3; p+=8;
        seph.pos[1]=R8(p)*1E3; p+=8;
        seph.vel[1]=R8(p)*1E3; p+=8;
        seph.acc[1]=R8(p)*1E3; p+=8;
        seph.pos[2]=R8(p)*1E3; p+=8;
        seph.vel[2]=R8(p)*1E3; p+=8;
        seph.acc[2]=R8(p)*1E3; p+=8;
        seph.svh   =U1(p);     p+=1;
        seph.sva   =U1(p);     p+=1;
        iodn       =U1(p);
    }
    else {
        trace(2,"binex 0x01-03 length error: len=%d\n",len);
        return -1;
    }
    if (!(seph.sat=satno(SYS_SBS,prn))) {
        trace(2,"binex 0x01-03 satellite error: prn=%d\n",prn);
        return -1;
    }
    seph.t0=gpst2time(week,tow);
    seph.tof=adjweek(seph.t0,tof);
    
    if (!strstr(raw->opt,"-EPHALL")) {
        if (fabs(timediff(seph.t0,raw->nav.seph[prn-MINPRNSBS].t0))<1.0&&
            seph.sva==raw->nav.seph[prn-MINPRNSBS].sva) return 0; /* unchanged */
    }
    raw->nav.seph[prn-MINPRNSBS]=seph;
    raw->ephsat=seph.sat;
    return 2;
}
/* decode binex mesaage 0x01-04: decoded galileo ephmemeris ------------------*/
static int decode_bnx_01_04(raw_t *raw, unsigned char *buff, int len)
{
    eph_t eph={0};
    unsigned char *p=buff;
    double tow,ura,sqrtA;
    int prn;
    
    trace(4,"binex 0x01-04: len=%d\n",len);
    
    if (len>=127) {
        prn       =U1(p)+1;      p+=1;
        eph.week  =U2(p);        p+=2;
        tow       =I4(p);        p+=4;
        eph.toes  =I4(p);        p+=4;
        eph.tgd[0]=R4(p);        p+=4; /* BGD E5a/E1 */
        eph.tgd[1]=R4(p);        p+=4; /* BGD E5b/E1 */
        eph.iode  =I4(p);        p+=4; /* IODnav */
        eph.f2    =R4(p);        p+=4;
        eph.f1    =R4(p);        p+=4;
        eph.f0    =R4(p);        p+=4;
        eph.deln  =R4(p)*SC2RAD; p+=4;
        eph.M0    =R8(p);        p+=8;
        eph.e     =R8(p);        p+=8;
        sqrtA     =R8(p);        p+=8;
        eph.cic   =R4(p);        p+=4;
        eph.crc   =R4(p);        p+=4;
        eph.cis   =R4(p);        p+=4;
        eph.crs   =R4(p);        p+=4;
        eph.cuc   =R4(p);        p+=4;
        eph.cus   =R4(p);        p+=4;
        eph.OMG0  =R8(p);        p+=8;
        eph.omg   =R8(p);        p+=8;
        eph.i0    =R8(p);        p+=8;
        eph.OMGd  =R4(p)*SC2RAD; p+=4;
        eph.idot  =R4(p)*SC2RAD; p+=4;
        ura       =R4(p)*0.1;    p+=4;
        eph.svh   =U2(p);        p+=2;
        eph.code  =U2(p);              /* data source */
    }
    else {
        trace(2,"binex 0x01-04: length error len=%d\n",len);
        return -1;
    }
    if (!(eph.sat=satno(SYS_GAL,prn))) {
        trace(2,"binex 0x01-04: satellite error prn=%d\n",prn);
        return -1;
    }
    eph.A=sqrtA*sqrtA;
    eph.iode=eph.iodc;
    eph.toe=gpst2time(eph.week,eph.toes);
    eph.toc=gpst2time(eph.week,eph.toes);
    eph.ttr=adjweek(eph.toe,tow);
    eph.sva=uraindex(ura);
    
    if (!strstr(raw->opt,"-EPHALL")) {
        if (raw->nav.eph[eph.sat-1].iode==eph.iode&&
            raw->nav.eph[eph.sat-1].iodc==eph.iodc) return 0; /* unchanged */
    }
    raw->nav.eph[eph.sat-1]=eph;
    raw->ephsat=eph.sat;
    return 2;
}
/* decode binex mesaage 0x01-05: decoded compass ephmemeris ------------------*/
static int decode_bnx_01_05(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x01-05: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x01-06: decoded qzss ephmemeris ---------------------*/
static int decode_bnx_01_06(raw_t *raw, unsigned char *buff, int len)
{
    eph_t eph={0};
    unsigned char *p=buff;
    double tow,ura,sqrtA;
    int prn,flag;
    
    trace(4,"binex 0x01-06: len=%d\n",len);
    
    if (len>=127) {
        prn       =U1(p);        p+=1;
        eph.week  =U2(p);        p+=2;
        tow       =I4(p);        p+=4;
        eph.toes  =I4(p);        p+=4;
        eph.tgd[0]=R4(p);        p+=4;
        eph.iodc  =I4(p);        p+=4;
        eph.f2    =R4(p);        p+=4;
        eph.f1    =R4(p);        p+=4;
        eph.f0    =R4(p);        p+=4;
        eph.iode  =I4(p);        p+=4;
        eph.deln  =R4(p)*SC2RAD; p+=4;
        eph.M0    =R8(p);        p+=8;
        eph.e     =R8(p);        p+=8;
        sqrtA     =R8(p);        p+=8;
        eph.cic   =R4(p);        p+=4;
        eph.crc   =R4(p);        p+=4;
        eph.cis   =R4(p);        p+=4;
        eph.crs   =R4(p);        p+=4;
        eph.cuc   =R4(p);        p+=4;
        eph.cus   =R4(p);        p+=4;
        eph.OMG0  =R8(p);        p+=8;
        eph.omg   =R8(p);        p+=8;
        eph.i0    =R8(p);        p+=8;
        eph.OMGd  =R4(p)*SC2RAD; p+=4;
        eph.idot  =R4(p)*SC2RAD; p+=4;
        ura       =R4(p)*0.1;    p+=4;
        eph.svh   =U2(p);        p+=2;
        flag      =U2(p);
    }
    else {
        trace(2,"binex 0x01-06: length error len=%d\n",len);
        return -1;
    }
    if (!(eph.sat=satno(SYS_QZS,prn))) {
        trace(2,"binex 0x01-06: satellite error prn=%d\n",prn);
        return 0;
    }
    eph.A=sqrtA*sqrtA;
    eph.toe=gpst2time(eph.week,eph.toes);
    eph.toc=gpst2time(eph.week,eph.toes);
    eph.ttr=adjweek(eph.toe,tow);
    eph.fit=flag&0x01;
    eph.sva=uraindex(ura);
    eph.code=2; /* codes on L2 channel */
    
    if (!strstr(raw->opt,"-EPHALL")) {
        if (raw->nav.eph[eph.sat-1].iode==eph.iode&&
            raw->nav.eph[eph.sat-1].iodc==eph.iodc) return 0; /* unchanged */
    }
    raw->nav.eph[eph.sat-1]=eph;
    raw->ephsat=eph.sat;
    return 2;
}
/* decode binex mesaage 0x01: gnss navigaion informtion ----------------------*/
static int decode_bnx_01(raw_t *raw, unsigned char *buff, int len)
{
    char *msg;
    int srec=U1(buff),prn=U1(buff+1);
    
    if (raw->outtype) {
        msg=raw->msgtype+strlen(raw->msgtype);
        prn=srec==0x01||srec==0x02||srec==0x04?prn+1:(srec==0x00?0:prn);
        sprintf(msg," subrec=%02X prn=%d",srec,prn);
    }
    switch (srec) {
        case 0x00: return decode_bnx_01_00(raw,buff+1,len-1);
        case 0x01: return decode_bnx_01_01(raw,buff+1,len-1);
        case 0x02: return decode_bnx_01_02(raw,buff+1,len-1);
        case 0x03: return decode_bnx_01_03(raw,buff+1,len-1);
        case 0x04: return decode_bnx_01_04(raw,buff+1,len-1);
        case 0x05: return decode_bnx_01_05(raw,buff+1,len-1);
        case 0x06: return decode_bnx_01_06(raw,buff+1,len-1);
    }
    return 0;
}
/* decode binex mesaage 0x02: generalized gnss data --------------------------*/
static int decode_bnx_02(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x02: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x03: generalized ancillary site data ----------------*/
static int decode_bnx_03(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x03: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x7d: receiver internal state prototyping ------------*/
static int decode_bnx_7d(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x7d: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x7e: ancillary site data prototyping ----------------*/
static int decode_bnx_7e(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x7e: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x7f-00: jpl fiducial site ---------------------------*/
static int decode_bnx_7f_00(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x7f-00: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x7f-01: ucar cosmic ---------------------------------*/
static int decode_bnx_7f_01(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x7f-01: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x7f-02: trimble 4700 --------------------------------*/
static int decode_bnx_7f_02(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x7f-02: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x7f-03: trimble netrs -------------------------------*/
static int decode_bnx_7f_03(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x7f-03: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x7f-04: trimble netrs -------------------------------*/
static int decode_bnx_7f_04(raw_t *raw, unsigned char *buff, int len)
{
    trace(2,"binex 0x7f-04: not supported message\n");
    return 0;
}
/* decode binex mesaage 0x7f-05: trimble netr8 obs data ----------------------*/
static unsigned char *decode_bnx_7f_05_obs(raw_t *raw, unsigned char *buff,
                                           int sat, int nobs, obsd_t *data)
{
    const unsigned char codes_gps[32]={
        CODE_L1C ,CODE_L1C ,CODE_L1P ,CODE_L1W ,CODE_L1Y ,CODE_L1M , /*  0- 5 */
        CODE_L1X ,CODE_L1N ,CODE_NONE,CODE_NONE,CODE_L2W ,CODE_L2C , /*  6-11 */
        CODE_L2D ,CODE_L2S ,CODE_L2L ,CODE_L2X ,CODE_L2P ,CODE_L2W , /* 12-17 */
        CODE_L2Y ,CODE_L2M ,CODE_L2N ,CODE_NONE,CODE_NONE,CODE_L5X , /* 18-23 */
        CODE_L5I ,CODE_L5Q ,CODE_L5X                                 /* 24-26 */
    };
    const unsigned char codes_glo[32]={
        CODE_L1C ,CODE_L1C ,CODE_L1P ,CODE_NONE,CODE_NONE,CODE_NONE, /*  0- 5 */
        CODE_NONE,CODE_NONE,CODE_NONE,CODE_NONE,CODE_L2C ,CODE_L2C , /*  6-11 */
        CODE_L2P ,CODE_L3X ,CODE_L3I ,CODE_L3Q ,CODE_L3X             /* 12-16 */
    };
    const unsigned char codes_gal[32]={
        CODE_L1C ,CODE_L1A ,CODE_L1B ,CODE_L1C ,CODE_L1X ,CODE_L1Z , /*  0- 5 */
        CODE_L5X ,CODE_L5I ,CODE_L5Q ,CODE_L5X ,CODE_L7X ,CODE_L7I , /*  6-11 */
        CODE_L7Q ,CODE_L7X ,CODE_L8X ,CODE_L8I ,CODE_L8Q ,CODE_L8X , /* 12-17 */
        CODE_L6X ,CODE_L6A ,CODE_L6B ,CODE_L6C ,CODE_L6X ,CODE_L6Z , /* 18-23 */
    };
    const unsigned char codes_sbs[32]={
        CODE_L1C ,CODE_L1C ,CODE_NONE,CODE_NONE,CODE_NONE,CODE_NONE, /*  0- 5 */
        CODE_L5X ,CODE_L5I ,CODE_L5Q ,CODE_L5X                       /*  6- 9 */
    };
    const unsigned char codes_cmp[32]={
        CODE_L2X ,CODE_L2I ,CODE_L2Q ,CODE_L2X ,CODE_L7X ,CODE_L7I , /*  0- 5 */
        CODE_L7Q ,CODE_L7X ,CODE_L6X ,CODE_L6I ,CODE_L6Q ,CODE_L6X , /*  6-11 */
        CODE_L1X ,CODE_L1S ,CODE_L1L ,CODE_L1X                       /* 12-15 */
    };
    const unsigned char codes_qzs[32]={
        CODE_L1C ,CODE_L1C ,CODE_L1S ,CODE_L1L ,CODE_L1X ,CODE_NONE, /*  0- 5 */
        CODE_NONE,CODE_L2X ,CODE_L2S ,CODE_L2L ,CODE_L2X ,CODE_NONE, /*  6-11 */
        CODE_NONE,CODE_L5X ,CODE_L5I ,CODE_L5Q ,CODE_L5X ,CODE_NONE, /* 12-17 */
        CODE_NONE,CODE_L6X ,CODE_L6S ,CODE_L6L ,CODE_L6X ,CODE_NONE, /* 18-23 */
        CODE_NONE,CODE_NONE,CODE_NONE,CODE_NONE,CODE_NONE,CODE_NONE, /* 24-29 */
        CODE_L1Z                                                     /* 30-30 */
    };
    const unsigned char *codes=NULL;
    double range[8],phase[8],cnr[8],dopp[8]={0},acc,wl;
    unsigned char *p=buff;
    unsigned char flag,flag0,flag1,flag2,flag3;
    int i,j,k,sys,fcn=-10,code[8],slip[8],pri[8],freq[8],slipcnt[8]={0},mask[8]={0};
    
    trace(5,"decode_bnx_7f_05_obs: sat=%2d nobs=%2d\n",sat,nobs);
    
    sys=satsys(sat,NULL);
    
    switch (sys) {
        case SYS_GPS: codes=codes_gps; break;
        case SYS_GLO: codes=codes_glo; break;
        case SYS_GAL: codes=codes_gal; break;
        case SYS_QZS: codes=codes_qzs; break;
        case SYS_SBS: codes=codes_sbs; break;
        case SYS_CMP: codes=codes_cmp; break;
    }
    for (i=0;i<nobs;i++) {
        
        flag   =getbitu(p,0,1);
        slip[i]=getbitu(p,2,1);
        code[i]=getbitu(p,3,5); p++;
        
        flag0=flag1=flag2=flag3=0;
        
        if (flag)       flag0=U1(p++);
        if (flag0&0x80) flag1=U1(p++);
        if (flag1&0x80) flag2=U1(p++);
        if (flag2&0x80) flag3=U1(p++);
        if (flag1&0x80) fcn=getbits(&flag2,2,4);
        
        acc=(flag0&0x20)?0.0001:0.00002; /* phase accuracy */
        
        cnr[i]=U1(p++)*0.4;
        
        if (i==0) {
            cnr[i]+=getbits(p,0,2)*0.1;
            range[i]=getbitu(p,2,32)*0.064+getbitu(p,34,6)*0.001; p+=5;
        }
        else if (flag0&0x40) {
            cnr[i]+=getbits(p,0,2)*0.1;
            range[i]=range[0]+getbits(p,4,20)*0.001; p+=3;
        }
        else {
            range[i]=range[0]+getbits(p,0,16)*0.001; p+=2;
        }
        if (flag0&0x40) {
            phase[i]=range[i]+getbits(p,0,24)*acc; p+=3;
        }
        else {
            cnr[i]+=getbits(p,0,2)*0.1;
            phase[i]=range[i]+getbits(p,2,22)*acc; p+=3;
        }
        if (flag0&0x04) {
            dopp[i]=getbits(p,0,24)/256.0; p+=3;
        }
        if (flag0&0x18) {
            slipcnt[i]=U2(p); p+=2;
        }
        else if (flag0&0x08) {
            slipcnt[i]=U1(p); p+=1;
        }
        trace(5,"(%d) CODE=%2d S=%d F=%02X %02X %02X %02X\n",i+1,
              code[i],slip,flag0,flag1,flag2,flag3);
        trace(5,"(%d) P=%13.3f L=%13.3f D=%7.1f SNR=%4.1f SCNT=%2d\n",
              i+1,range[i],phase[i],dopp[i],cnr[i],slipcnt[i]);
    }
    if (!codes) {
        data->sat=0;
        return p;
    }
    data->time=raw->time;
    data->sat=sat;
    
    /* get code priority */
    for (i=0;i<nobs;i++) {
        code2obs(codes[code[i]&0x3F],freq+i);
        pri[i]=getcodepri(sys,codes[code[i]&0x3F],raw->opt);
    }
    for (i=0;i<NFREQ;i++) {
        for (j=0,k=-1;j<nobs;j++) {
            if (freq[j]==i+1&&(k<0||pri[j]>pri[k])) k=j;
        }
        if (k<0) {
            data->P[i]=data->L[i]=0.0;
            data->D[i]=0.0f;
            data->SNR[i]=data->LLI[i]=0;
            data->code[i]=CODE_NONE;
        }
        else {
            wl=satwavelen(sat,i,&raw->nav);
            if (sys==SYS_GLO&&fcn>=-7&&freq[k]<=2) {
                wl=CLIGHT/(freq[k]==1?FREQ1_GLO+DFRQ1_GLO*fcn:
                                      FREQ2_GLO+DFRQ2_GLO*fcn);
            }
            data->P[i]=range[k];
            data->L[i]=wl<=0.0?0.0:phase[k]/wl;
            data->D[i]=dopp[k];
            data->SNR[i]=(unsigned char)(cnr[k]/0.25+0.5);
            data->code[i]=codes[code[k]&0x3F];
            data->LLI[i]=slip[k]?1:0;
            mask[k]=1;
        }
    }
    for (;i<NFREQ+NEXOBS;i++) {
        for (k=0;k<nobs;k++) {
            if (!mask[k]) break;
        }
        if (k>=nobs) {
            data->P[i]=data->L[i]=0.0;
            data->D[i]=0.0f;
            data->SNR[i]=data->LLI[i]=0;
            data->code[i]=CODE_NONE;
        }
        else {
            wl=satwavelen(sat,freq[k]-1,&raw->nav);
            if (sys==SYS_GLO&&fcn>=-7&&freq[k]<=2) {
                wl=CLIGHT/(freq[k]==1?FREQ1_GLO+DFRQ1_GLO*fcn:
                                      FREQ2_GLO+DFRQ2_GLO*fcn);
            }
            data->P[i]=range[k];
            data->L[i]=wl<=0.0?0.0:phase[k]/wl;
            data->D[i]=dopp[k];
            data->SNR[i]=(unsigned char)(cnr[k]/0.25+0.5);
            data->code[i]=codes[code[k]&0x3F];
            data->LLI[i]=slip[k]?1:0;
            mask[k]=1;
        }
    }
    return p;
}
/* decode binex mesaage 0x7f-05: trimble netr8 -------------------------------*/
static int decode_bnx_7f_05(raw_t *raw, unsigned char *buff, int len)
{
    obsd_t data={{0}};
    double clkoff=0.0,toff[16]={0};
    char *msg;
    unsigned char *p=buff;
    unsigned int flag;
    int i,nsat,nobs,prn,sys,sat,clkrst=0,rsys=0,nsys=0,tsys[16]={0};
    
    trace(4,"decode_bnx_7f_05\n");
    
    raw->obs.n=0;
    flag=U1(p++);
    nsat=(int)(flag&0x3F)+1;
    
    if (flag&0x80) { /* rxclkoff */
        clkrst=getbitu(p,0, 2);
        clkoff=getbits(p,2,22)*1E-9; p+=3;
    }
    if (flag&0x40) { /* systime */
        nsys=getbitu(p,0,4);
        rsys=getbitu(p,4,4); p++;
        for (i=0;i<nsys;i++) {
            toff[i]=getbits(p,0,24)*1E-9;
            tsys[i]=getbitu(p,28,4); p+=4;
        }
    }
    for (i=0;i<nsat;i++) {
        prn =U1(p++);
        nobs=getbitu(p,1,3);
        sys =getbitu(p,4,4); p++;
        
        trace(5,"binex 0x7F-05 PRN=%3d SYS=%d NOBS=%d\n",prn,sys,nobs);
        
        switch (sys) {
            case 0: sat=satno(SYS_GPS,prn); break;
            case 1: sat=satno(SYS_GLO,prn); break;
            case 2: sat=satno(SYS_SBS,prn); break;
            case 3: sat=satno(SYS_GAL,prn); break;
            case 4: sat=satno(SYS_CMP,prn); break;
            case 5: sat=satno(SYS_QZS,prn); break;
            default: sat=0; break;
        }
        /* decode binex mesaage 0x7F-05 obs data */
        p=decode_bnx_7f_05_obs(raw,p,sat,nobs,&data);
        
        if ((int)(p-buff)>len) {
            trace(2,"binex 0x7F-05 length error: nsat=%2d len=%d\n",nsat,len);
            return -1;
        }
        /* save obs data to obs buffer */
        if (data.sat&&raw->obs.n<MAXOBS) {
            raw->obs.data[raw->obs.n++]=data;
        }
    }
    if (raw->outtype) {
        msg=raw->msgtype+strlen(raw->msgtype);
        sprintf(msg," nsat=%2d",nsat);
    }
    return raw->obs.n>0?1:0;
}
/* decode binex mesaage 0x7f: gnss data prototyping --------------------------*/
static int decode_bnx_7f(raw_t *raw, unsigned char *buff, int len)
{
    const static double gpst0[]={1980,1,6,0,0,0};
    char *msg;
    unsigned char *p=buff;
    unsigned int srec,min,msec;
    
    srec=U1(p); p+=1; /* subrecord id */
    min =U4(p); p+=4;
    msec=U2(p); p+=2;
    raw->time=timeadd(epoch2time(gpst0),min*60.0+msec*0.001);
    
    if (raw->outtype) {
        msg=raw->msgtype+strlen(raw->msgtype);
        sprintf(msg," subrec=%02X time%s",srec,time_str(raw->time,3));
    }
    switch (srec) {
        case 0x00: return decode_bnx_7f_00(raw,buff+7,len-7);
        case 0x01: return decode_bnx_7f_01(raw,buff+7,len-7);
        case 0x02: return decode_bnx_7f_02(raw,buff+7,len-7);
        case 0x03: return decode_bnx_7f_03(raw,buff+7,len-7);
        case 0x04: return decode_bnx_7f_04(raw,buff+7,len-7);
        case 0x05: return decode_bnx_7f_05(raw,buff+7,len-7);
    }
    return 0;
}
/* decode binex mesaage ------------------------------------------------------*/
static int decode_bnx(raw_t *raw)
{
    unsigned int len,cs1,cs2;
    int rec,len_h;
    
    rec=raw->buff[1]; /* record id */
    
    /* record and header length */
    len_h=getbnxi(raw->buff+2,&len);
    
    trace(5,"decode_bnx: rec=%02x len=%d\n",rec,len);
    
    /* check parity */
    if (raw->len-1<128) {
        cs1=U1(raw->buff+raw->len);
        cs2=csum8(raw->buff+1,raw->len-1);
    }
    else {
        cs1=U2(raw->buff+raw->len);
        cs2=crc16(raw->buff+1,raw->len-1);
    }
    if (cs1!=cs2) {
        trace(2,"binex 0x%02X parity error CS=%X %X\n",rec,cs1,cs2);
        return -1;
    }
    if (raw->outtype) {
        sprintf(raw->msgtype,"BINEX 0x%02X (%4d)",rec,raw->len);
    }
    /* decode binex message record */
    switch (rec) {
        case 0x00: return decode_bnx_00(raw,raw->buff+2+len_h,len);
        case 0x01: return decode_bnx_01(raw,raw->buff+2+len_h,len);
        case 0x02: return decode_bnx_02(raw,raw->buff+2+len_h,len);
        case 0x03: return decode_bnx_03(raw,raw->buff+2+len_h,len);
        case 0x7d: return decode_bnx_7d(raw,raw->buff+2+len_h,len);
        case 0x7e: return decode_bnx_7e(raw,raw->buff+2+len_h,len);
        case 0x7f: return decode_bnx_7f(raw,raw->buff+2+len_h,len);
    }
    return 0;
}
/* synchronize binex message -------------------------------------------------*/
static int sync_bnx(unsigned char *buff, unsigned char data)
{
    buff[0]=buff[1]; buff[1]=data;
    
    return buff[0]==BNXSYNC2&&
           (buff[1]==0x00||buff[1]==0x01||buff[1]==0x02||buff[1]==0x03||
            buff[1]==0x7D||buff[1]==0x7E||buff[1]==0x7F);
}
/* input binex message from stream ---------------------------------------------
* fetch next binex data and input a mesasge from stream
* args   : raw_t *raw   IO     receiver raw data control struct
*          unsigned char data I stream data (1 byte)
* return : status (-1: error message, 0: no message, 1: input observation data,
*                  2: input ephemeris)
* notes  : support only the following message (ref [1])
*
*          - big-endian, regular CRC, forward record (sync=0xE2)
*          - record-subrecord:
*            0x01-01: decoded gps ephemeris
*            0x01-02: decoded glonass ephemeris
*            0x01-03: decoded sbas ephemeris
*            0x01-04: decoded galileo ephemeris
*            0x01-06: decoded qzss ephemeris
*            0x7f-05: gnss data prototyping - trimble netr8
*-----------------------------------------------------------------------------*/
extern int input_bnx(raw_t *raw, unsigned char data)
{
    unsigned int len;
    int len_h,len_c;
    
    trace(5,"input_bnx: data=%02x\n",data);
    
    /* synchronize binex message */
    if (raw->nbyte==0) {
        if (!sync_bnx(raw->buff,data)) return 0;
        raw->nbyte=2;
        return 0;
    }
    raw->buff[raw->nbyte++]=data;
    if (raw->nbyte<4) return 0;
    
    len_h=getbnxi(raw->buff+2,&len);
    
    raw->len=len+len_h+2; /* length without crc */
    
    if (raw->len-1>4096) {
        trace(2,"binex length error: len=%d\n",raw->len-1);
        raw->nbyte=0;
        return -1;
    }
    len_c=raw->len-1<128?1:2;
    
    if (raw->nbyte<(int)(raw->len+len_c)) return 0;
    raw->nbyte=0;
    
    /* decode binex message */
    return decode_bnx(raw);
}
/* input binex message from file -----------------------------------------------
* fetch next binex data and input a message from file
* args   : raw_t  *raw   IO     receiver raw data control struct
*          FILE   *fp    I      file pointer
* return : status(-2: end of file, -1...9: same as above)
*-----------------------------------------------------------------------------*/
extern int input_bnxf(raw_t *raw, FILE *fp)
{
    unsigned int len;
    int i,data,len_h,len_c;
    
    trace(4,"input_bnxf\n");
    
    if (raw->nbyte==0) {
        for (i=0;;i++) {
            if ((data=fgetc(fp))==EOF) return -2;
            if (sync_bnx(raw->buff,(unsigned char)data)) break;
            if (i>=4096) return 0;
        }
    }
    if (fread(raw->buff+2,1,4,fp)<4) return -2;
    
    len_h=getbnxi(raw->buff+2,&len);
    
    raw->len=len+len_h+2;
    
    if (raw->len-1>4096) {
        trace(2,"binex length error: len=%d\n",raw->len-1);
        raw->nbyte=0;
        return -1;
    }
    len_c=raw->len-1<128?1:2;
    
    if (fread(raw->buff+6,1,raw->len+len_c-6,fp)<(size_t)(raw->len+len_c-6)) {
        return -2;
    }
    raw->nbyte=0;
    
    /* decode binex message */
    return decode_bnx(raw);
}
