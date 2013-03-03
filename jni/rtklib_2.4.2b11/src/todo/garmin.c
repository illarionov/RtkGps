/*------------------------------------------------------------------------------
* garmin.c : garmin gps 15l receiver dependent functions
*
*          Copyright (C) 2007 by T.TAKASU, All rights reserved.
*
* version : $Revision: 1.2 $ $Date: 2008/07/14 00:05:05 $
* history : 2008/05/21 1.0 new
* notes   :
*           this source code is no longer supported
*-----------------------------------------------------------------------------*/
#include "rtklib.h"

#define PGMNAME     "convgarm v.0.1"
#define RCVTYPE     "Garmin GPS-15"
#define NTOBS       3
#define GARMSYNC    0x10        /* garmin record sync header */
#define GARMMEAS    0x34        /* garmin measurement record id */
#define GARMPOS     0x33        /* garmin position record id */
#define U1(p)       (*((unsigned char *)(p)))
#define U2(p)       (*((unsigned short *)(p)))
#define U4(p)       (*((unsigned int *)(p)))
#define R8(p)       (*((double *)(p)))

static const char rcsid[]="$Id: garmin.c,v 1.2 2008/07/14 00:05:05 TTAKA Exp $";

/* output rinex obs types */
static char tobs[NTOBS][3]={"C1","L1","S1"};

/* application defined functions ---------------------------------------------*/
extern int showmsg(char *format,...);

/* check checksum ------------------------------------------------------------*/
static int chksum(const unsigned char *buff, int len)
{
    int i;
    unsigned char sum=0;
    for (i=0;i<len;i++) sum+=buff[i];
    return !sum;
}
/* decode receiver measurements ----------------------------------------------*/
static int decoderawobs(const unsigned char *buff, int len, gtime_t ts,
                        gtime_t te, double tint, int opt, obsd_t *obs)
{
    int i,n,sat,week;
    double tow,off;
    const unsigned char *q;
    gtime_t time,t0={0};
    
    tow=R8(buff);
    off=floor(tow+0.5)-tow;
    week=U2(buff+8)+OFFWEEK;
    time=gpst2time(week,tow+off);
    if (!screent(time,ts,t0,tint)) return 0;
    for (i=n=0,q=buff+10;i<12&&n<MAXOBS;i++,q+=18) {
        if (!U1(q+17)) continue;
        if ((sat=U1(q+16)+1)>32) continue;
        obs[n].time=time;
        obs[n].sat=sat;
        obs[n].L[0]=U4(q)+(double)U2(q+12)/2048.0+FREQ1*off;
        obs[n].P[0]=R8(q+4)+CLIGHT*off;
        obs[n].D[0]=0.0;
        obs[n].SNR[0]=(unsigned char)((U1(q+16)+30)*4.0+0.5);
        obs[n++].LLI[0]=U1(q+14)?1:0;
    }
    return n;
}
/* decode garmin bin message ---------------------------------------------------
* decode and input a garmin bin message.
* args   : unsigned char *buff I message buffer
*          int   len     I      buffer length (bytes)
*          gtime_t ts    I      time start
*          gtime_t te    I      time end
*          double tint   I      time interval
*          int   opt     I      decode option
*          obs_t *obs    O      observation data
* return : input status (0:no valid data, 1:obs data, 2:ephemeris,
*                        3:sbas message, 9:ion/utc parameters, -1:error)
*-----------------------------------------------------------------------------*/
extern int decodegarm(const unsigned char *buff, int len, gtime_t ts, gtime_t te,
                      double tint, int opt, obs_t *obs)
{
    if (!chksum(buff,len)) return -1;
    switch (U1(buff)) {
    case GARMMEAS:
        if ((obs->n=decoderawobs(buff+2,len-2,ts,te,tint,opt,obs->data))>0)
            return 1;
        break;
    case GARMPOS:
        break;
    }
    return 0;
}
/* convert garmin log file ----------------------------------------------------
* convert garmin log file to rinex obs/nav and sbas message file.
* args   : FILE  *fp     I      input log file pointer
*          FILE  *ofp    I      output rinex obs file pointer (NULL:no output)
*          FILE  *nfp    I      output rinex nav file pointer (NULL:no output)
*          FILE  *sfp    I      output sbas message file pointer (NULL:no output)
*          gtime_t ts    I      time start
*          gtime_t te    I      time end
*          double tint   I      time interval
*          rnxopt_t *rnxopt I   rinex options
*          int   opt     I      conversion options
* return : none
*-----------------------------------------------------------------------------*/
extern void convgarm(FILE *fp, FILE *ofp, FILE *nfp, FILE *sfp, gtime_t ts,
                     gtime_t te, double tint, const rnxopt_t *rnxopt, int opt)
{
    obsd_t data[MAXOBS]={{{0}}};
    obs_t obs={0};
    int c,type,nmsg[4]={0};
    char s[128]="",s1[64],s2[64];
    unsigned short len;
    unsigned char buff[1024];
    gtime_t time={0},tstart={0};
    obs.data=data;
    
    while ((c=fgetc(fp))!=EOF) {
        if (c!=GARMSYNC) continue;
        if (fread(buff,1,1,fp)<1) break;
        if (buff[0]!=GARMMEAS&&buff[0]!=GARMPOS) continue;
        if (fread(buff+1,1,1,fp)<1) break;
        if ((len=buff[1])>sizeof(buff)-4) continue;
        if (fread(buff+2,len+3,1,fp)<1) break;
        type=decodegarm(buff,len+3,ts,te,tint,opt,&obs);
        
        if (type==1&&ofp&&screent(obs.data[0].time,ts,te,tint)) {
            if (nmsg[0]==0) {
                outrnxobsh(ofp,rnxopt,obs.data[0].time);
            }
            outrnxobsb(ofp,rnxopt,obs.data,obs.n,0);
            nmsg[0]++;
            time=obs.data[0].time;
        }
        else if (type==-1) nmsg[3]++;
        if (te.time!=0&&timediff(obs.data[0].time,te)>DTTOL+1.0) break;
        if (time.time!=0) {
            time2str(time,s1,0); sprintf(s,"%s: ",s1);
            if (tstart.time==0) tstart=time;
        }
        if (showmsg("%sObs=%d Nav=%d Sbs=%d Err=%d",s,nmsg[0],nmsg[1],
                    nmsg[2],nmsg[3])) break;
    }
    if (tstart.time!=0) {
        time2str(tstart,s1,0); time2str(time,s2,0);
        sprintf(s,"%s-%s: ",s1,s2+5);
    }
    showmsg("%sObs=%d Nav=%d Sbs=%d Err=%d",s,nmsg[0],nmsg[1],nmsg[2],nmsg[3]);
}
/* read garmin log file --------------------------------------------------------
* convert garmin log file to rinex obs/nav and sbas message file.
* args   : FILE  *fp     I      input file pointer
*          gtime_t ts,te I      time start/end
*          double tint   I      time interval
*          int   opt     I      read option
*          obs_t *obs    IO     observation data    (NULL: no output)
*          int   *nmax   I      max number of messages 
* return : none
*-----------------------------------------------------------------------------*/
extern void readgarm(FILE *fp, gtime_t ts, gtime_t te, double tint, int opt,
                     obs_t *obs, int *nmax)
{
    obsd_t data[MAXOBS]={{{0}}};
    obs_t obsp={0};
    int c,type;
    unsigned short len;
    unsigned char buff[1024];
    obsp.data=data;
    
    while ((c=fgetc(fp))!=EOF) {
        if (c!=GARMSYNC) continue;
        if (fread(buff,1,1,fp)<1) break;
        if (buff[0]!=GARMMEAS&&buff[0]!=GARMPOS) continue;
        if (fread(buff+2,1,1,fp)<1) break;
        if ((len=buff[1])>sizeof(buff)-4) continue;
        if (fread(buff+3,len+4,1,fp)<1) break;
        
        type=decodegarm(buff,len+2,ts,te,tint,opt,&obsp);
        
        if (type==1&&screent(obsp.data[0].time,ts,te,tint)) {
            addobs(obs,&obsp,nmax);
        }
        if (te.time!=0&&timediff(obsp.data[0].time,te)>DTTOL) break;
    }
}
