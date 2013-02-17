/*------------------------------------------------------------------------------
* ins.c : ins common functions
*
*          Copyright (C) 2008-2009 by T.TAKASU, All rights reserved.
*
* reference :
*    P.D.Groves, Principles of GNSS, Intertial, and Multisensor Integrated
*    Navigation System, Artech House, 2008
*
* version : $Revision: 1.1 $ $Date: 2008/09/05 01:32:44 $
* history : 2008/08/25 1.0 new
*
*                     This code is not supported yet.
*
*-----------------------------------------------------------------------------*/
#include "rtklib.h"
#include "ins.h"

static const char rcsid[]="$Id: ins.c,v 1.1 2008/09/05 01:32:44 TTAKA Exp TTAKA $";

//#define MG      (9.80665/1000.0)    /* constant of mg */
#define MG      (9.8/1000.0)        /* constant of mg */
#define MAXDT   3600.0              /* max interval to update imu (s) */

/* global variable -----------------------------------------------------------*/
const double Omge[9]={0,OMGE,0,-OMGE,0,0,0,0,0}; /* (5.18) */

/* multiply 3d matries -------------------------------------------------------*/
extern void matmul3(const char *tr, const double *A, const double *B, double *C)
{
    matmul(tr,3,3,3,1.0,A,B,0.0,C);
}
/* multiply 3d matrix and vector ---------------------------------------------*/
extern void matmul3v(const char *tr, const double *A, const double *b, double *c)
{
    char t[]="NN";
    t[0]=tr[0];
    matmul(t,3,1,3,1.0,A,b,0.0,c);
}
/* 3d skew symmetric matrix --------------------------------------------------*/
extern void skewsym3(const double *ang, double *C)
{
    C[0]=0.0;     C[3]=-ang[2]; C[6]=ang[1];
    C[1]=ang[2];  C[4]=0.0;     C[7]=-ang[0];
    C[2]=-ang[1]; C[5]=ang[0];  C[8]=0.0;
}
/* gravity at earth surface ----------------------------------------------------
* gravity at earth surface
* args   : double *pos      I   position {lat,lon,height} (rad/m)
* return : gravity acceleration at earth surface (m/s^2)
*-----------------------------------------------------------------------------*/
extern double gravity0(const double *pos)
{
    double sinp,e2=FE*(2.0-FE);
    sinp=sin(pos[0]); /* (2.85) */
    return 9.7803253359*(1.0+0.001931853*sinp*sinp)/sqrt(1-e2*sinp*sinp);
}
/* gravity model ---------------------------------------------------------------
* gravity model in e-frame
* args   : double *re       I   position (ecef) (m)
*          double *ge       O   gravity acceleration (ecef) (m/s^2)
* return : none
*-----------------------------------------------------------------------------*/
extern void gravity(const double *re, double *ge)
{
    double pos[3],gn[3]={0},Cne[9];
    ecef2pos(re,pos);
    gn[2]=gravity0(pos)*(1.0-2.0*pos[2]/RE);
    ned2xyz(pos,Cne);
    matmul3v("N",Cne,gn,ge);
}
/* initialize ins states -------------------------------------------------------
* initialize ins states with stationaly imu measurement data
* args   : insstate_t *ins  IO  ins states
*          double *re       I   initial ins position (ecef) (m)
*          double angh      I   initial heading angle (rad)
*          imudata_t *data  I   imu measurement data
*          int    n         I   number of data (0: no use of data)
* return : none
*-----------------------------------------------------------------------------*/
extern void initins(insstate_t *ins, const double *re, double angh,
                    const imudata_t *data, int n)
{
    gtime_t time={0};
    double rpy[3]={0},ab[3]={0},fb[3]={0},pos[3],Cnb[9],Cne[9],ge[3],gb[3];
    int i,j;
    
    trace(3,"initins: n=%d angh=%.1f\n",n,angh*R2D);
    ecef2pos(re,pos);
    rpy[2]=angh;
    
    for (i=0;i<3;i++) {
        ins->re[i]=re[i];
        ins->ve[i]=ins->ae[i]=ins->ba[i]=ins->bg[i]=ins->fb[i]=0.0;
    }
    if (n>0) { /* gyro and accl bias */
        for (i=0;i<3;i++) {
            for (j=0;j<n;j++) {
                fb[i]+=data[j].accl[i];
                ab[i]+=data[j].gyro[i];
            }
            fb[i]/=n; ab[i]/=n;
        }
        rpy[0]=atan2(-fb[1],-fb[2]); /* (5.89) */
        rpy[1]=atan(fb[0]/norm(fb+1,2));
        time=data[n-1].time;
    }
    rpy2dcm(rpy,Cnb);
    ned2xyz(pos,Cne);
    matmul3("NT",Cne,Cnb,ins->Cbe);
    if (n>0) {
        gravity(re,ge);
        matmul3v("T",ins->Cbe,ge,gb);
        for (i=0;i<3;i++) {
            ins->ba[i]=fb[i]+gb[i];
            ins->bg[i]=ab[i];
        }
    }
    ins->time=time;
}
/* update ins attitude -------------------------------------------------------*/
static void updateatt(double t, double *Cbe, const double *omgb)
{
    double alpha[3],a,a1,a2,Ca[9],Ca2[9],Comg[9],Cbep[9];
    double Cbb[9]={1,0,0,0,1,0,0,0,1};
    int i;
    trace(3,"updateatt:\n");
    for (i=0;i<3;i++) alpha[i]=omgb[i]*t;
    skewsym3(alpha,Ca);
    matmul3("NN",Ca,Ca,Ca2);
    a=norm(alpha,3);
    a1=sin(a)/a;
    a2=(1.0-cos(a))/(a*a);
    for (i=0;i<9;i++) Cbb[i]+=a1*Ca[i]+a2*Ca2[i]; /* (5.63) */
    matmul3("NN",Cbe,Cbb,Cbep);
    matmul3("NN",Omge,Cbe,Comg);
    for (i=0;i<9;i++) Cbe[i]=Cbep[i]-Comg[i]*t; /* (5.20) */
}
/* update ins states -----------------------------------------------------------
* updata ins states with imu measurement data in e-frame
* args   : insstate_t *ins  IO  ins states
*          imudata_t *data  I   imu measurement data
* return : none
*-----------------------------------------------------------------------------*/
extern int updateins(insstate_t *ins, const imudata_t *data)
{
    double t,Cbe[9],fe[3],ge[3],cori[3];
    int i;
    
    trace(3,"updateins:\n");
    
    if ((t=timediff(data->time,ins->time))>MAXDT) {
        fprintf(stderr,"time difference too large : %.0fs\n",t);
        return 0;
    }
    for (i=0;i<3;i++) {
        ins->omgb[i]=data->gyro[i]-ins->bg[i]; /* (4.18) */
        ins->fb[i]=data->accl[i]-ins->ba[i];
    }
    /* update attitude */
    matcpy(Cbe,ins->Cbe,3,3);
    updateatt(t,ins->Cbe,ins->omgb);
    for (i=0;i<9;i++) Cbe[i]=(Cbe[i]+ins->Cbe[i])/2.0; /* (5.21) */
    
    /* specific-force/gravity in e-frame */
    matmul3v("N",Cbe,ins->fb,fe);
    gravity(ins->re,ge);
    
    /* update velocity/position */
    matmul3v("N",Omge,ins->ve,cori);
    for (i=0;i<3;i++) {
        ins->ae[i]=fe[i]+ge[i]-2.0*cori[i]; /* (5.29) */
        ins->ve[i]+=ins->ae[i]*t;
        ins->re[i]+=ins->ve[i]*t-ins->ae[i]/2.0*t*t; /* (5.31) */
    }
    ins->time=data->time;
    trace(5,"ins(+)=\n"); traceins(5,ins);
    return 1;
} 
/* read imu measurement log file -----------------------------------------------
* read imu measurement log file
* args   : char   *file     I   imu measurement log file
*          imu_t  *imu      O   imu measurement data
* return : status (1:ok,0:no data/error)
*-----------------------------------------------------------------------------*/
extern int readimu(const char *file, imu_t *imu)
{
    FILE *fp;
    imudata_t *p;
    gtime_t ts={0};
    double e[6],v[12],toff=0.0;
    size_t siz;
    int i,ch;
    char buff[1024];
    
    trace(3,"readimulog:s=%s\n",file);
    imu->n=imu->nmax=0; imu->data=NULL;
    
    if (!(fp=fopen(file,"r"))) {
        fprintf(stderr,"file open error : %s\n",file);
        return 0;
    }
    while (fgets(buff,sizeof(buff),fp)) {
        if (sscanf(buff,"%d %lf/%lf/%lf %lf:%lf:%lf", /* time */
                   &ch,e,e+1,e+2,e+3,e+4,e+5)<7) {
             continue;
        }
        /* time gyrox gyroy gyroz accx accy accz tempx tempy tempz power stat */
        if (sscanf(buff+26,"%lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                   v,v+1,v+2,v+3,v+4,v+5,v+6,v+7,v+8,v+9,v+10,v+11)<12) {
            continue;
        }
        if (imu->n>=imu->nmax) {
            trace(5,"readimulog:imu->n=%d nmax=%d\n",imu->n,imu->nmax);
            siz=sizeof(imudata_t)*(imu->nmax+=4096);
            if (!(imu->data=(imudata_t *)realloc(imu->data,siz))) {
                fprintf(stderr,"memory allocation error\n");
                break;
            }
        }
        p=imu->data+imu->n++;
#if 1
        if (ts.time==0) {
            ts=utc2gpst(epoch2time(e));
            toff=v[0];
        }
        p->time=timeadd(ts,v[0]-toff);
#else
        p->time=utc2gpst(epoch2time(e));
#endif
        p->gyro[0]= v[1]*D2R; /* rad/s */
        p->gyro[1]=-v[2]*D2R;
        p->gyro[2]=-v[3]*D2R;
        p->accl[0]=-v[4]*MG;  /* m/s^2 */
        p->accl[1]= v[5]*MG;
        p->accl[2]= v[6]*MG;
        for (i=0;i<3;i++) p->temp[i]=v[i+7];
        p->stat=(int)v[11];
    }
    fclose(fp);
    return imu->n<=0?0:1;
}
/* trace ins states ----------------------------------------------------------*/
extern void traceins(int level, const insstate_t *ins)
{
#ifdef TRACE
    double pos[3],Cne[9],Cnb[9],rpy[3],vel[3],acc[3];
    char s[64];
    time2str(ins->time,s,3);
    trace(level,"time  =%s\n",s);
    ecef2pos(ins->re,pos);
    ned2xyz(pos,Cne);
    matmul3("TN",ins->Cbe,Cne,Cnb);
    dcm2rpy(Cnb,rpy);
    trace(level,"attn  =%8.3f %8.3f %8.3f\n",rpy[0]*R2D,rpy[1]*R2D,rpy[2]*R2D);
    
    matmul3v("T",Cne,ins->ve,vel);
    matmul3v("T",Cne,ins->ae,acc);
    trace(level,"veln  =%8.3f %8.3f %8.3f accn =%8.4f %8.4f %8.4f\n",
          vel[0],vel[1],vel[2],acc[0],acc[1],acc[2]); /* n-frame */
    
    matmul3v("T",ins->Cbe,ins->ve,vel);
    matmul3v("T",ins->Cbe,ins->ae,acc);
    trace(level,"velb  =%8.3f %8.3f %8.3f accb =%8.4f %8.4f %8.4f\n",
          vel[0],vel[1],vel[2],acc[0],acc[1],acc[2]); /* b-frame */
    
    trace(level,"gbias =%8.3f %8.3f %8.3f abias=%8.4f %8.4f %8.4f\n",
          ins->bg[0],ins->bg[1],ins->bg[2],ins->ba[0],ins->ba[1],ins->ba[2]);
#endif
}
