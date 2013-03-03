/*------------------------------------------------------------------------------
* rtcm3e.c : rtcm ver.3 message encoder functions
*
*          Copyright (C) 2012 by T.TAKASU, All rights reserved.
*
* references :
*     [1] RTCM Recommended Standards for Differential GNSS (Global Navigation
*         Satellite Systems) Service version 2.3, August 20, 2001
*     [2] RTCM Standard 10403.1 for Differential GNSS (Global Navigation
*         Satellite Systems) Services - Version 3, Octobar 27, 2006
*     [3] RTCM 10403.1-Amendment 3, Amendment 3 to RTCM Standard 10403.1
*     [4] RTCM Paper, April 12, 2010, Proposed SSR Messages for SV Orbit Clock,
*         Code Biases, URA
*     [5] RTCM Paper 012-2009-SC104-528, January 28, 2009 (previous ver of [4])
*     [6] RTCM Paper 012-2009-SC104-582, February 2, 2010 (previous ver of [4])
*     [7] RTCM Standard 10403.1 - Amendment 5, Differential GNSS (Global
*         Navigation Satellite Systems) Services - version 3, July 1, 2011
*     [8] RTCM Paper 019-2012-SC104-689 (draft Galileo ephmeris messages)
*     [9] RTCM 1043 1C draft QZSS ephemeris message add (August 3, 2012)
*     [10] RTCM Paper 059-2011-SC104-635 (draft Galileo and QZSS ssr messages)
*     [11] RTCM Paper 034-2012-SC104-693 (draft multiple signal messages)
*     [12] multiple signal messages for QZSS to RTCM standard 10403.1
*
* version : $Revision:$ $Date:$
* history : 2012/10/05 1.0  new
*-----------------------------------------------------------------------------*/
#include "rtklib.h"

static const char rcsid[]="$Id:$";

/* constants -----------------------------------------------------------------*/

#define PRUNIT_GPS  299792.458  /* rtcm ver.3 unit of gps pseudorange (m) */
#define PRUNIT_GLO  599584.916  /* rtcm ver.3 unit of glonass pseudorange (m) */

#define P2_10       0.0009765625          /* 2^-10 */
#define P2_34       5.820766091346740E-11 /* 2^-34 */
#define P2_46       1.421085471520200E-14 /* 2^-46 */
#define P2_59       1.734723475976810E-18 /* 2^-59 */

/* type definition -----------------------------------------------------------*/

typedef struct {                    /* multi-signal-message header type */
    unsigned char iod;              /* issue of data station */
    unsigned char time_s;           /* cumulative session transmitting time */
    unsigned char clk_str;          /* clock steering indicator */
    unsigned char clk_ext;          /* external clock indicator */
    unsigned char smooth;           /* divergence free smoothing indicator */
    unsigned char tint_s;           /* soothing interval */
    unsigned char nsat,nsig;        /* satellites */
    unsigned char sats[64];         /* satellites */
    unsigned char sigs[32];         /* signals */
    unsigned char cellmask[64];     /* cell mask */
} msm_h_t;

/* ssr update intervals ------------------------------------------------------*/
static const double ssrudint[16]={
    1,2,5,10,15,30,60,120,240,300,600,900,1800,3600,7200,10800
};
/* get sign-magnitude bits ---------------------------------------------------*/
static double getbitg(const unsigned char *buff, int pos, int len)
{
    double value=getbitu(buff,pos+1,len-1);
    return getbitu(buff,pos,1)?-value:value;
}
/* adjust weekly rollover of gps time ----------------------------------------*/
static void adjweek(rtcm_t *rtcm, double tow)
{
    double tow_p;
    int week;
    
    /* if no time, get cpu time */
    if (rtcm->time.time==0) rtcm->time=utc2gpst(timeget());
    tow_p=time2gpst(rtcm->time,&week);
    if      (tow<tow_p-302400.0) tow+=604800.0;
    else if (tow>tow_p+302400.0) tow-=604800.0;
    rtcm->time=gpst2time(week,tow);
}
/* adjust daily rollover of glonass time -------------------------------------*/
static void adjday_glot(rtcm_t *rtcm, double tod)
{
    gtime_t time;
    double tow,tod_p;
    int week;
    
    if (rtcm->time.time==0) rtcm->time=utc2gpst(timeget());
    time=timeadd(gpst2utc(rtcm->time),10800.0); /* glonass time */
    tow=time2gpst(time,&week);
    tod_p=fmod(tow,86400.0); tow-=tod_p;
    if      (tod<tod_p-43200.0) tod+=86400.0;
    else if (tod>tod_p+43200.0) tod-=86400.0;
    time=gpst2time(week,tow+tod);
    rtcm->time=utc2gpst(timeadd(time,-10800.0));
}
/* adjust carrier-phase rollover ---------------------------------------------*/
static double adjcp(rtcm_t *rtcm, int sat, int freq, double cp)
{
    if (rtcm->cp[sat-1][freq]==0.0) ;
    else if (cp<rtcm->cp[sat-1][freq]-750.0) cp+=1500.0;
    else if (cp>rtcm->cp[sat-1][freq]+750.0) cp-=1500.0;
    rtcm->cp[sat-1][freq]=cp;
    return cp;
}
/* loss-of-lock indicator ----------------------------------------------------*/
static int lossoflock(rtcm_t *rtcm, int sat, int freq, int lock)
{
    int lli=lock<rtcm->lock[sat-1][freq]-lock;
    rtcm->lock[sat-1][freq]=lock;
    return lli;
}
/* s/n ratio -----------------------------------------------------------------*/
static unsigned char snratio(double snr)
{
    return (unsigned char)(snr<=0.0||255.5<=snr?0.0:snr*4.0+0.5);
}
/* get observation data index ------------------------------------------------*/
static int obsindex(obs_t *obs, gtime_t time, int sat)
{
    int i,j;
    
    for (i=0;i<obs->n;i++) {
        if (obs->data[i].sat==sat) return i; /* field already exists */
    }
    if (i>=MAXOBS) return -1; /* overflow */
    
    /* add new field */
    obs->data[i].time=time;
    obs->data[i].sat=sat;
    for (j=0;j<NFREQ;j++) {
        obs->data[i].L[j]=obs->data[i].P[j]=0.0;
        obs->data[i].D[j]=0.0;
        obs->data[i].SNR[j]=obs->data[i].LLI[j]=obs->data[i].code[j]=0;
    }
    obs->n++;
    return i;
}
/* encode type 1002: extended L1-only gps rtk observables --------------------*/
static int encode_type1002(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1004: extended L1&L2 gps rtk observables ----------------------*/
static int encode_type1004(rtcm_t *rtcm)
{
#if 0
    double pr1,cnr1,cnr2,tt,cp1,cp2;
    int i=24+64,j,index,nsat,sync,prn,sat,code1,code2,pr21,ppr1,ppr2;
    int lock1,lock2,amb,sys,nsat=MIN(rtcm->obs.n,MAXOBS);
    
    for (j=0;j<rtcm->obs.n&&nsat<MAXOBS;j++) {
        
        sys=satsys(rtcm->obs.data[j].sat,&prn);
        if (!(sys&(SYS_GPS|SYS_SBS))) continue;
        nsat++;
    }
    encode_head1001(rtcm,sync,nsat);
    
    for (j=0;j<rtcm->obs.n&&nsat<MAXOBS;j++) {
        
        sys=satsys(rtcm->obs.data[j].sat,&prn);
        if (!(sys&(SYS_GPS|SYS_SBS))) continue;
        
        if (sys==SYS_SBS) prn+=-MINPRNSBS+80;
        
        tt=timediff(rtcm->obs.data[0].time,rtcm->time);
        if (rtcm->obsflag||fabs(tt)>1E-9) {
            rtcm->obs.n=rtcm->obsflag=0;
        }
        pr1=pr1*0.02+amb*PRUNIT_GPS;
        
        if (ppr1!=0xFFF80000) {
            rtcm->obs.data[index].P[0]=pr1;
            cp1=adjcp(rtcm,sat,0,ppr1*0.0005/lam[0]);
            rtcm->obs.data[index].L[0]=pr1/lam[0]+cp1;
        }
        lock1=rtcm->obs.data[j].LLI[0];
        cnr1 =rtcm->obs.data[j].SNR[0]=snratio(cnr1*0.25);
        code1=rtcm->obs.data[j].code[0]==CODE_L1P?1:0;
        
        if (rtcm->obs.data[j].P[1]==0.0) pr21=0xFFFF2000;
        if (ppr2!=0xFFF80000) {
            cp2=adjcp(rtcm,sat,1,ppr2*0.0005/lam[1]);
            rtcm->obs.data[index].L[1]=pr1/lam[1]+cp2;
        }
        rtcm->obs.data[index].LLI[1]=lossoflock(rtcm,sat,1,lock2);
        rtcm->obs.data[index].SNR[1]=snratio(cnr2*0.25);
        rtcm->obs.data[index].code[1]=code2?CODE_L2P:CODE_L2C;
        
        setbitu(rtcm->buff,i, 6,prn  ); i+= 6;
        setbitu(rtcm->buff,i, 1,code1); i+= 1;
        setbitu(rtcm->buff,i,24,pr1  ); i+=24;
        setbits(rtcm->buff,i,20,ppr1 ); i+=20;
        setbitu(rtcm->buff,i, 7,lock1); i+= 7;
        setbitu(rtcm->buff,i, 8,amb  ); i+= 8;
        setbitu(rtcm->buff,i, 8,cnr1 ); i+= 8;
        setbitu(rtcm->buff,i, 2,code2); i+= 2;
        setbits(rtcm->buff,i,14,pr21 ); i+=14;
        setbits(rtcm->buff,i,20,ppr2 ); i+=20;
        setbitu(rtcm->buff,i, 7,lock2); i+= 7;
        setbitu(rtcm->buff,i, 8,cnr2 ); i+= 8;
    }
#endif
    return 1;
}
/* encode type 1005: stationary rtk reference station arp --------------------*/
static int encode_type1005(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1006: stationary rtk reference station arp with height --------*/
static int encode_type1006(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1007: antenna descriptor --------------------------------------*/
static int encode_type1007(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1008: antenna descriptor & serial number ----------------------*/
static int encode_type1008(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1010: extended L1-only glonass rtk observables ----------------*/
static int encode_type1010(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1012: extended L1&L2 glonass rtk observables ------------------*/
static int encode_type1012(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1019: gps ephemerides -----------------------------------------*/
static int encode_type1019(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1020: glonass ephemerides -------------------------------------*/
static int encode_type1020(rtcm_t *rtcm)
{
    return 1;
}
/* encode type 1033: receiver and antenna descriptor -------------------------*/
static int encode_type1033(rtcm_t *rtcm)
{
    return 1;
}
/* encode ssr 1,4 message header ---------------------------------------------*/
static int encode_ssr1_head(rtcm_t *rtcm, int sys, int nsat, int sync, int iod,
                            double udint, int refd, int provid, int solid)
{
    double tod,tow;
    int i=24+12,udi;
    
    trace(4,"encode_ssr1_head: time=%s sys=%d nsat=%d sync=%d iod=%d udint=%.0f\n",
          time_str(rtcm->time,1),sys,nsat,sync,iod,udint);
    
    if (sys==SYS_GLO) {
        tow=time2gpst(gpst2utc(rtcm->time),NULL);
        tod=fmod(tow,86400.0);
        setbitu(rtcm->buff,i,17,tod); i+=17;
    }
    else {
        tow=time2gpst(rtcm->time,NULL);
        setbitu(rtcm->buff,i,20,tow); i+=20;
    }
    for (udi=0;udi<15;udi++) if (ssrudint[udi]>=udint) break;
    
    setbitu(rtcm->buff,i, 4,   udi); i+= 4;
    setbitu(rtcm->buff,i, 1,  sync); i+= 1;
    setbitu(rtcm->buff,i, 1,  refd); i+= 1; /* satellite ref datum */
    setbitu(rtcm->buff,i, 4,   iod); i+= 4; /* iod */
    setbitu(rtcm->buff,i,16,provid); i+=16; /* provider id */
    setbitu(rtcm->buff,i, 4, solid); i+= 4; /* solution id */
    
    if (sys==SYS_QZS) {
        setbitu(rtcm->buff,i,4,nsat); i+=4;
    }
    else {
        setbitu(rtcm->buff,i,6,nsat); i+=6;
    }
    return i;
}
/* encode ssr 2,3,5,6 message header -----------------------------------------*/
static int encode_ssr2_head(rtcm_t *rtcm, int sys, int nsat, int sync, int iod,
                            double udint, int provid, int solid)
{
    double tod,tow;
    int i=24+12,nsat,udi,provid=0,solid=0;
    
    trace(4,"encode_ssr2_head: time=%s sys=%d nsat=%d sync=%d iod=%d udint=%.0f\n",
          time_str(rtcm->time,1),sys,nsat,sync,iod,udint);
    
    if (sys==SYS_GLO) {
        tow=time2gpst(gpst2utc(rtcm->time),NULL);
        tod=fmod(tow,86400.0);
        setbitu(rtcm->buff,i,17,tod); i+=17;
    }
    else {
        tow=time2gpst(rtcm->time,NULL);
        setbitu(rtcm->buff,i,20,tow); i+=20;
    }
    for (udi=0;udi<15;udi++) if (ssrudint[udi]>=udint) break;
    
    setbitu(rtcm->buff,i, 4,   udi); i+= 4;
    setbitu(rtcm->buff,i, 1,  sync); i+= 1;
    setbitu(rtcm->buff,i, 4,   iod); i+= 4; /* iod */
    setbitu(rtcm->buff,i,16,provid); i+=16; /* provider id */
    setbitu(rtcm->buff,i, 4, solid); i+= 4; /* solution id */
    
    if (sys==SYS_QZS) {
        setbitu(rtcm->buff,i,4,nsat); i+=4;
    }
    else {
        setbitu(rtcm->buff,i,6,nsat); i+=6;
    }
    return i;
}
/* encode ssr 1: orbit corrections -------------------------------------------*/
static int encode_ssr1(rtcm_t *rtcm, int sys, int sync)
{
    double udint,deph[3],ddeph[3];
    int i,j,k,type,iod,nsat,prn,iode,refd=0,np,offp;
    
    switch (sys) {
        case SYS_GPS: type=1057; np=6; offp=  0; break;
        case SYS_GLO: type=1063; np=5; offp=  0; break;
        case SYS_GAL: type=1240; np=6; offp=  0; break;
        case SYS_QZS: type=1246; np=4; offp=192; break;
        default: return 0;
    }
    /* number of satellites */
    for (j=nsat=0;j<MAXSAT;j++) {
        if (satsys(j+1,&prn)!=sys||!rtcm->ssr[j].update) continue;
        nsat++;
        rtcm->ssr[j].update=0; /* clear update flag */
        udint=rtcm->ssr[j].udint;
        refd =rtcm->ssr[j].refd;
    }
    /* set message type */
    setbitu(rtcm->buff,24,12,type);
    
    /* encode ssr header */
    i=encode_ssr1_head(rtcm,sys,nsat,sync,iod,udint,refd,provid,solid);
    
    for (j=0;j<MAXSAT;j++) {
        if (satsys(j+1,&prn)!=sys||!rtcm->ssr[j].update) continue;
        
        iode=rtcm->ssr[j].iode;
        
        for (k=0;k<3;k++) {
            deph [k]=rtcm->ssr[j].deph [k];
            ddeph[k]=rtcm->ssr[j].ddeph[k];
        }
        setbitu(rtcm->buff,i,np,prn+offp);      i+=np;
        setbitu(rtcm->buff,i, 8,iode);          i+= 8;
        setbits(rtcm->buff,i,22,deph [0]/1E-4); i+=22;
        setbits(rtcm->buff,i,20,deph [1]/4E-4); i+=20;
        setbits(rtcm->buff,i,20,deph [2]/4E-4); i+=20;
        setbits(rtcm->buff,i,21,ddeph[0]/1E-6); i+=21;
        setbits(rtcm->buff,i,19,ddeph[1]/4E-6); i+=19;
        setbits(rtcm->buff,i,19,ddeph[2]/4E-6); i+=19;
    }
    return i/8;
}
/* encode ssr 2: clock corrections -------------------------------------------*/
static int encode_ssr2(rtcm_t *rtcm, int sys)
{
    double udint,dclk[3];
    int i,j,k,type,sync,iod,nsat,prn,sat,np,offp;
    
    switch (sys) {
        case SYS_GPS: np=6; offp=  0; break;
        case SYS_GLO: np=5; offp=  0; break;
        case SYS_GAL: np=6; offp=  0; break;
        case SYS_QZS: np=4; offp=192; break;
        default: return 0;
    }
    type=getbitu(rtcm->buff,24,12);
    
    i=eecode_ssr2_head(rtcm,sys,nsat,sync,iod,udint);
    
    for (j=0;j<nsat&&i+70+np<rtcm->len*8;j++) {
        
        prn    =getbitu(rtcm->buff,i,np)+offp; i+=np;
        dclk[0]=getbits(rtcm->buff,i,22)*1E-4; i+=22;
        dclk[1]=getbits(rtcm->buff,i,21)*1E-6; i+=21;
        dclk[2]=getbits(rtcm->buff,i,27)*2E-8; i+=27;
        
        if (!(sat=satno(sys,prn))) {
            trace(2,"rtcm3 %d satellite number error: prn=%d\n",type,prn);
            continue;
        }
        rtcm->ssr[sat-1].t0=rtcm->time;
        rtcm->ssr[sat-1].udint=udint;
        
        for (k=0;k<3;k++) {
            rtcm->ssr[sat-1].dclk[k]=dclk[k];
        }
        rtcm->ssr[sat-1].update=1;
    }
    return sync?0:10;
}
/* encode ssr 3: satellite code biases ---------------------------------------*/
static int encode_ssr3(rtcm_t *rtcm, int sys)
{
    const int codes_gps[]={
        CODE_L1C,CODE_L1P,CODE_L1W,CODE_L1Y,CODE_L1M,CODE_L2C,CODE_L2D,CODE_L2S,
        CODE_L2L,CODE_L2X,CODE_L2P,CODE_L2W,CODE_L2Y,CODE_L2M,CODE_L5I,CODE_L5Q,
        CODE_L5X
    };
    const int codes_glo[]={
        CODE_L1C,CODE_L1P,CODE_L2C,CODE_L2P
    };
    const int codes_gal[]={
        CODE_L1A,CODE_L1B,CODE_L1C,CODE_L1X,CODE_L1Z,CODE_L5I,CODE_L5Q,CODE_L5X,
        CODE_L7I,CODE_L7Q,CODE_L7X,CODE_L8I,CODE_L8Q,CODE_L8X,CODE_L6A,CODE_L6B,
        CODE_L6C,CODE_L6X,CODE_L6Z
    };
    const int codes_qzs[]={
        CODE_L1C,CODE_L1S,CODE_L1L,CODE_L2S,CODE_L2L,CODE_L2X,CODE_L5I,CODE_L5Q,
        CODE_L5X
    };
    const int *codes;
    double udint,bias,cbias[MAXCODE];
    int i,j,k,type,mode,sync,iod,nsat,prn,sat,nbias,np,offp,ncode;
    
    type=getbitu(rtcm->buff,24,12);
    
#ifndef SSR_DRAFT
    if ((nsat=decode_ssr2_head(rtcm,sys,&sync,&iod,&udint,&i))<0) {
#else
    int rsv=0;
    if ((nsat=decode_ssr1_head(rtcm,sys,&sync,&iod,&udint,&rsv,&i))<0) {
#endif
        trace(2,"rtcm3 %d length error: len=%d\n",type,rtcm->len);
        return -1;
    }
    switch (sys) {
        case SYS_GPS: np=6; offp=  0; codes=codes_gps; ncode=16; break;
        case SYS_GLO: np=5; offp=  0; codes=codes_glo; ncode= 3; break;
        case SYS_GAL: np=6; offp=  0; codes=codes_gal; ncode=18; break;
        case SYS_QZS: np=4; offp=192; codes=codes_qzs; ncode= 9; break;
        default: return 0;
    }
    for (j=0;j<nsat&&i+5+np<rtcm->len*8;j++) {
        prn  =getbitu(rtcm->buff,i,np)+offp; i+=np;
        nbias=getbitu(rtcm->buff,i, 5);      i+= 5;
        
        for (k=0;k<MAXCODE;k++) cbias[k]=0.0;
        for (k=0;k<nbias&&i+19<rtcm->len*8;k++) {
            mode=getbitu(rtcm->buff,i, 5);      i+= 5;
            bias=getbits(rtcm->buff,i,14)*0.01; i+=14;
            if (mode<=ncode) {
                cbias[codes[mode]-1]=(float)bias;
            }
            else {
                trace(2,"rtcm3 %d not supported mode: mode=%d\n",type,mode);
            }
        }
        if (!(sat=satno(sys,prn))) {
            trace(2,"rtcm3 %d satellite number error: prn=%d\n",type,prn);
            continue;
        }
        rtcm->ssr[sat-1].t0=rtcm->time;
        rtcm->ssr[sat-1].udint=udint;
        for (k=0;k<MAXCODE;k++) {
            rtcm->ssr[sat-1].cbias[k]=(float)cbias[k];
        }
        rtcm->ssr[sat-1].update=1;
    }
    return sync?0:10;
}
/* encode ssr 4: combined orbit and clock corrections ------------------------*/
static int decode_ssr4(rtcm_t *rtcm, int sys)
{
    double udint,deph[3],ddeph[3],dclk[3];
    int i,j,k,type,nsat,sync,iod,prn,sat,iode,refd=0,np,offp;
    
    type=getbitu(rtcm->buff,24,12);
    
    if ((nsat=decode_ssr1_head(rtcm,sys,&sync,&iod,&udint,&refd,&i))<0) {
        trace(2,"rtcm3 %d length error: len=%d\n",type,rtcm->len);
        return -1;
    }
    switch (sys) {
        case SYS_GPS: np=6; offp=  0; break;
        case SYS_GLO: np=5; offp=  0; break;
        case SYS_GAL: np=6; offp=  0; break;
        case SYS_QZS: np=4; offp=192; break;
        default: return 0;
    }
    for (j=0;j<nsat&&i+199+np<rtcm->len*8;j++) {
        prn     =getbitu(rtcm->buff,i,np)+offp; i+=np;
        iode    =getbitu(rtcm->buff,i, 8);      i+= 8;
        deph [0]=getbits(rtcm->buff,i,22)*1E-4; i+=22;
        deph [1]=getbits(rtcm->buff,i,20)*4E-4; i+=20;
        deph [2]=getbits(rtcm->buff,i,20)*4E-4; i+=20;
        ddeph[0]=getbits(rtcm->buff,i,21)*1E-6; i+=21;
        ddeph[1]=getbits(rtcm->buff,i,19)*4E-6; i+=19;
        ddeph[2]=getbits(rtcm->buff,i,19)*4E-6; i+=19;
        
        dclk [0]=getbits(rtcm->buff,i,22)*1E-4; i+=22;
        dclk [1]=getbits(rtcm->buff,i,21)*1E-6; i+=21;
        dclk [2]=getbits(rtcm->buff,i,27)*2E-8; i+=27;
        
        if (!(sat=satno(sys,prn))) {
            trace(2,"rtcm3 %d satellite number error: prn=%d\n",type,prn);
            continue;
        }
        rtcm->ssr[sat-1].t0=rtcm->time;
        rtcm->ssr[sat-1].udint=udint;
        rtcm->ssr[sat-1].iode=iode;
        rtcm->ssr[sat-1].refd=refd;
        
        for (k=0;k<3;k++) {
            rtcm->ssr[sat-1].deph [k]=deph [k];
            rtcm->ssr[sat-1].ddeph[k]=ddeph[k];
            rtcm->ssr[sat-1].dclk [k]=dclk [k];
        }
        rtcm->ssr[sat-1].update=1;
    }
    return sync?0:10;
}
/* encode ssr 5: ura ---------------------------------------------------------*/
static int encode_ssr5(rtcm_t *rtcm, int sys)
{
    double udint;
    int i,j,type,nsat,sync,iod,prn,sat,ura,np,offp;
    
    type=getbitu(rtcm->buff,24,12);
    
    if ((nsat=decode_ssr2_head(rtcm,sys,&sync,&iod,&udint,&i))<0) {
        trace(2,"rtcm3 %d length error: len=%d\n",type,rtcm->len);
        return -1;
    }
    switch (sys) {
        case SYS_GPS: np=6; offp=  0; break;
        case SYS_GLO: np=5; offp=  0; break;
        case SYS_GAL: np=6; offp=  0; break;
        case SYS_QZS: np=4; offp=192; break;
        default: return 0;
    }
    for (j=0;j<nsat&&i+6+np<rtcm->len*8;j++) {
        prn=getbitu(rtcm->buff,i,np)+offp; i+=np;
        ura=getbitu(rtcm->buff,i, 6);      i+= 6;
        
        if (!(sat=satno(sys,prn))) {
            trace(2,"rtcm3 %d satellite number error: prn=%d\n",type,prn);
            continue;
        }
        rtcm->ssr[sat-1].t0=rtcm->time;
        rtcm->ssr[sat-1].udint=udint;
        rtcm->ssr[sat-1].ura=ura;
        rtcm->ssr[sat-1].update=1;
    }
    return sync?0:10;
}
/* encode ssr 6: high rate clock correction ----------------------------------*/
static int encode_ssr6(rtcm_t *rtcm, int sys)
{
    double udint,hrclk;
    int i,j,type,nsat,sync,iod,prn,sat,np,offp;
    
    type=getbitu(rtcm->buff,24,12);
    
    if ((nsat=decode_ssr2_head(rtcm,sys,&sync,&iod,&udint,&i))<0) {
        trace(2,"rtcm3 %d length error: len=%d\n",type,rtcm->len);
        return -1;
    }
    switch (sys) {
        case SYS_GPS: np=6; offp=  0; break;
        case SYS_GLO: np=5; offp=  0; break;
        case SYS_GAL: np=6; offp=  0; break;
        case SYS_QZS: np=4; offp=192; break;
        default: return 0;
    }
    for (j=0;j<nsat&&i+22+np<rtcm->len*8;j++) {
        prn  =getbitu(rtcm->buff,i,np)+offp; i+=np;
        hrclk=getbits(rtcm->buff,i,22)*1E-4; i+=22;
        
        if (!(sat=satno(sys,prn))) {
            trace(2,"rtcm3 %d satellite number error: prn=%d\n",type,prn);
            continue;
        }
        rtcm->ssr[sat-1].t0=rtcm->time;
        rtcm->ssr[sat-1].udint=udint;
        rtcm->ssr[sat-1].hrclk=hrclk;
        rtcm->ssr[sat-1].update=1;
    }
    return sync?0:10;
}
/* encode rtcm ver.3 message -------------------------------------------------*/
extern int encode_rtcm3(rtcm_t *rtcm, int type)
{
    int ret=0;
    
    trace(3,"encode_rtcm3: type=%d\n",type);
    
    switch (type) {
        case 1002: ret=encode_type1002(rtcm); break;
        case 1004: ret=encode_type1004(rtcm); break;
        case 1005: ret=encode_type1005(rtcm); break;
        case 1006: ret=encode_type1006(rtcm); break;
        case 1007: ret=encode_type1007(rtcm); break;
        case 1008: ret=encode_type1008(rtcm); break;
        case 1010: ret=encode_type1010(rtcm); break;
        case 1012: ret=encode_type1012(rtcm); break;
        case 1019: ret=encode_type1019(rtcm); break;
        case 1020: ret=encode_type1020(rtcm); break;
        case 1033: ret=encode_type1033(rtcm); break;
        case 1044: ret=encode_type1044(rtcm); break;
        case 1045: ret=encode_type1045(rtcm); break;
        case 1046: ret=encode_type1046(rtcm); break;
        case 1057: ret=encode_ssr1(rtcm,SYS_GPS); break;
        case 1058: ret=encode_ssr2(rtcm,SYS_GPS); break;
        case 1059: ret=encode_ssr3(rtcm,SYS_GPS); break;
        case 1060: ret=encode_ssr4(rtcm,SYS_GPS); break;
        case 1061: ret=encode_ssr5(rtcm,SYS_GPS); break;
        case 1062: ret=encode_ssr6(rtcm,SYS_GPS); break;
        case 1063: ret=encode_ssr1(rtcm,SYS_GLO); break;
        case 1064: ret=encode_ssr2(rtcm,SYS_GLO); break;
        case 1065: ret=encode_ssr3(rtcm,SYS_GLO); break;
        case 1066: ret=encode_ssr4(rtcm,SYS_GLO); break;
        case 1067: ret=encode_ssr5(rtcm,SYS_GLO); break;
        case 1068: ret=encode_ssr6(rtcm,SYS_GLO); break;
        case 1071: ret=encode_msm0(rtcm,SYS_GPS); break; /* not supported */
        case 1072: ret=encode_msm0(rtcm,SYS_GPS); break; /* not supported */
        case 1073: ret=encode_msm0(rtcm,SYS_GPS); break; /* not supported */
        case 1074: ret=encode_msm4(rtcm,SYS_GPS); break;
        case 1075: ret=encode_msm5(rtcm,SYS_GPS); break;
        case 1076: ret=encode_msm6(rtcm,SYS_GPS); break;
        case 1077: ret=encode_msm7(rtcm,SYS_GPS); break;
        case 1081: ret=encode_msm0(rtcm,SYS_GLO); break; /* not supported */
        case 1082: ret=encode_msm0(rtcm,SYS_GLO); break; /* not supported */
        case 1083: ret=encode_msm0(rtcm,SYS_GLO); break; /* not supported */
        case 1084: ret=encode_msm4(rtcm,SYS_GLO); break;
        case 1085: ret=encode_msm5(rtcm,SYS_GLO); break;
        case 1086: ret=encode_msm6(rtcm,SYS_GLO); break;
        case 1087: ret=encode_msm7(rtcm,SYS_GLO); break;
        case 1091: ret=encode_msm0(rtcm,SYS_GAL); break; /* not supported */
        case 1092: ret=encode_msm0(rtcm,SYS_GAL); break; /* not supported */
        case 1093: ret=encode_msm0(rtcm,SYS_GAL); break; /* not supported */
        case 1094: ret=encode_msm4(rtcm,SYS_GAL); break;
        case 1095: ret=encode_msm5(rtcm,SYS_GAL); break;
        case 1096: ret=encode_msm6(rtcm,SYS_GAL); break;
        case 1097: ret=encode_msm7(rtcm,SYS_GAL); break;
        case 1101: ret=encode_msm0(rtcm,SYS_SBS); break; /* not supported */
        case 1102: ret=encode_msm0(rtcm,SYS_SBS); break; /* not supported */
        case 1103: ret=encode_msm0(rtcm,SYS_SBS); break; /* not supported */
        case 1104: ret=encode_msm4(rtcm,SYS_SBS); break;
        case 1105: ret=encode_msm5(rtcm,SYS_SBS); break;
        case 1106: ret=encode_msm6(rtcm,SYS_SBS); break;
        case 1107: ret=encode_msm7(rtcm,SYS_SBS); break;
        case 1111: ret=encode_msm0(rtcm,SYS_QZS); break; /* not supported */
        case 1112: ret=encode_msm0(rtcm,SYS_QZS); break; /* not supported */
        case 1113: ret=encode_msm0(rtcm,SYS_QZS); break; /* not supported */
        case 1114: ret=encode_msm4(rtcm,SYS_QZS); break;
        case 1115: ret=encode_msm5(rtcm,SYS_QZS); break;
        case 1116: ret=encode_msm6(rtcm,SYS_QZS); break;
        case 1117: ret=encode_msm7(rtcm,SYS_QZS); break;
        case 1121: ret=encode_msm0(rtcm,SYS_CMP); break; /* not supported */
        case 1122: ret=encode_msm0(rtcm,SYS_CMP); break; /* not supported */
        case 1123: ret=encode_msm0(rtcm,SYS_CMP); break; /* not supported */
        case 1124: ret=encode_msm0(rtcm,SYS_CMP); break; /* not supported */
        case 1125: ret=encode_msm0(rtcm,SYS_CMP); break; /* not supported */
        case 1126: ret=encode_msm0(rtcm,SYS_CMP); break; /* not supported */
        case 1127: ret=encode_msm0(rtcm,SYS_CMP); break; /* not supported */
        case 1240: ret=encode_ssr1(rtcm,SYS_GAL); break;
        case 1241: ret=encode_ssr2(rtcm,SYS_GAL); break;
        case 1242: ret=encode_ssr3(rtcm,SYS_GAL); break;
        case 1243: ret=encode_ssr4(rtcm,SYS_GAL); break;
        case 1244: ret=encode_ssr5(rtcm,SYS_GAL); break;
        case 1245: ret=encode_ssr6(rtcm,SYS_GAL); break;
        case 1246: ret=encode_ssr1(rtcm,SYS_QZS); break;
        case 1247: ret=encode_ssr2(rtcm,SYS_QZS); break;
        case 1248: ret=encode_ssr3(rtcm,SYS_QZS); break;
        case 1249: ret=encode_ssr4(rtcm,SYS_QZS); break;
        case 1250: ret=encode_ssr5(rtcm,SYS_QZS); break;
        case 1251: ret=encode_ssr6(rtcm,SYS_QZS); break;
    }
    sprintf(rtcm->msgtype,"RTCM3: type=%d len=%3d",type,rtcm->len);
    if (ret>=0) {
        type-=1000;
        if (1<=type&&type<=299) rtcm->nmsg3[type]++; else rtcm->nmsg3[0]++;
    }
    return ret;
}
/* output rtcm message ---------------------------------------------------------
* output rtcm message
* args   : rtcm_t *rtcm IO   rtcm control struct
*          int    type  I    rtcm message type
*          int    sync  I    sync flag
* return : status (1:ok,0:error)
* notes  : before calling the function, set observation data, navigation data,
*          or station information in rtcm control struct. the function returns
*          with a rtcm message in rtcm->buff and the message size in
*          rtcm->nbyte and rtcm->nbit, if no error occures. for ephemeris
*          messages, set rtcm->ephsat to select a satellite
*
*          supported msgs RTCM ver.3: 1002,1004-1008,1010,1012,1019,1020,1033
*-----------------------------------------------------------------------------*/
extern int output_rtcm3(rtcm_t *rtcm, int type, int sync)
{
    unsigned int crc;
    
    trace(4,"output_rtcm3: type=%d\n",type);
    
    /* preamble and reserved */
    setbitu(rtcm->buff, 0, 8,RTCM3PREAMB);
    setbitu(rtcm->buff, 8, 6,0);
    setbitu(rtcm->buff,14,10,0);
    rtcm->nbit=24;
    rtcm->nbyte=3;
    
    /* encode rtcm ver.3 message */
    if (!encode_rtcm3(rtcm,type,sync)) {
        rtcm->nbit=rtcm->nbyte=0;
        return 0;
    }
    /* message length */
    setbitu(rtcm->buff,14,10,rtcm->nbyte);
    
    /* crc-24q */
    crc=crc24q(rtcm->buff,rtcm->nbyte);
    setbitu(rtcm->buff,rtcm->nbit,24,crc);
    return 1;
}
