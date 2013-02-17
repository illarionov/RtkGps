/*------------------------------------------------------------------------------
* stec.c : slant tec functions
*
*          Copyright (C) 2012 by T.TAKASU, All rights reserved.
*
* version : $Revision:$ $Date:$
* history : 2012/09/15 1.0  new
*-----------------------------------------------------------------------------*/
#include "rtklib.h"

static const char rcsid[]="$Id:$";

#define MAX_STECFILE 2048
#define MAX_NGRID   4           /* number of grids for interpolation */
#define MAX_DIST    100.0       /* max distance to grid (km) */
#define MAX_AGE     300.0       /* max age of difference (s) */
#define VAR_NOTEC   SQR(30.0)   /* variance of no tec */
#define MIN_EL      0.0         /* min elevation angle (rad) */
#define MIN_HGT     -1000.0     /* min user height (m) */

#define SQR(x)      ((x)*(x))
#define MAX(x,y)    ((x)>(y)?(x):(y))
#define MIN(x,y)    ((x)<(y)?(x):(y))

/* add stec grid -------------------------------------------------------------*/
static int add_grid(nav_t *nav, const double *pos)
{
    stec_t *nav_stec;
    int i;
    
    if (nav->nn>=nav->nnmax) {
        nav->nnmax=nav->nnmax<=0?256:nav->nnmax*2;
        
        if (!(nav_stec=(stec_t *)realloc(nav->stec,sizeof(stec_t)*nav->nnmax))) {
            trace(1,"read_stecb malloc error nnmax=%d\n",nav->nnmax);
            free(nav->stec); nav->stec=NULL; nav->nn=nav->nnmax=0;
            return 0;
        }
        nav->stec=nav_stec;
    }
    nav->stec[nav->nn].pos[0]=(float)pos[0];
    nav->stec[nav->nn].pos[1]=(float)pos[1];
    for (i=0;i<MAXSAT;i++) nav->stec[nav->nn].index[i]=0;
    nav->stec[nav->nn].n=nav->stec[nav->nn].nmax=0;
    nav->stec[nav->nn++].data=NULL;
    return 1;
}
/* add stec data -------------------------------------------------------------*/
static int add_data(stec_t *stec, gtime_t time, int sat, int slip, double iono,
                    double rate, double rms)
{
    stecd_t *stec_data;
    
    if (stec->n>=stec->nmax) {
        stec->nmax=stec->nmax<=0?16384:stec->nmax*2;
        
        if (!(stec_data=(stecd_t *)realloc(stec->data,sizeof(stecd_t)*stec->nmax))) {
            trace(1,"add_data malloc error nmax=%d\n",stec->nmax);
            free(stec->data); stec->data=NULL; stec->n=stec->nmax=0;
            return 0;
        }
        stec->data=stec_data;
    }
    stec->data[stec->n].time=time;
    stec->data[stec->n].sat=(unsigned char)sat;
    stec->data[stec->n].slip=(unsigned char)slip;
    stec->data[stec->n].iono=(float)iono;
    stec->data[stec->n].rate=(float)rate;
    stec->data[stec->n++].rms=(float)rms;
    return 1;
}
/* read stec grid data body --------------------------------------------------*/
static void read_stecb(FILE *fp, nav_t *nav)
{
    gtime_t time;
    double tow,pos[2],iono,rate,rms;
    char buff[256],id[64];
    int week,sat,slip;
    
    trace(3,"read_stecb:\n");
    
    while (fgets(buff,sizeof(buff),fp)) {
        
        /* read position */
        if (sscanf(buff,"$SPOS %d %lf %lf %lf",&week,&tow,pos,pos+1)>=4) {
            
            /* add stec grid */
            if (!add_grid(nav,pos)) break;
        }
        /* read ionos */
        if (sscanf(buff,"$STEC %d %lf %s %d %lf %lf %lf",&week,&tow,id,&slip,
                   &iono,&rate,&rms)>=6) {
            
            if (nav->nn<=0||!(sat=satid2no(id))) continue;
            
            time=gpst2time(week,tow);
            
            /* add grid data */
            if (!add_data(nav->stec+nav->nn-1,time,sat,slip,iono,rate,rms)) {
                break;
            }
        }
    }
}
/* compare stec data ---------------------------------------------------------*/
static int comp_data(const void *p1, const void *p2)
{
    stecd_t *q1=(stecd_t *)p1,*q2=(stecd_t *)p2;
    double tt;
    
    if (q1->sat!=q2->sat) return q1->sat-q2->sat;
    if ((tt=timediff(q1->time,q2->time))!=0.0) return tt<0.0?-1:1;
    return 0;
}
/* free stec grid data ---------------------------------------------------------
* free slant tec grid data
* args   : nav_t  *nav        IO  navigation data
*                                 nav->nn, nav->nnmax and nav->stec are modified
* return : none
*-----------------------------------------------------------------------------*/
extern void stec_free(nav_t *nav)
{
    int i;
    
    trace(3,"stec_free\n");
    
    for (i=0;i<nav->nn;i++) {
        free(nav->stec[i].data);
    }
    free(nav->stec);
    nav->stec=NULL;
    nav->nn=nav->nnmax=0;
}
/* read stec grid file ---------------------------------------------------------
* read slant tec grid file
* args   : char   *file       I   slant tec grid file
*                                 (wind-card * is expanded)
*          nav_t  *nav        IO  navigation data
*                                 nav->nn, nav->nnmax and nav->stec are modified
* return : none
*-----------------------------------------------------------------------------*/
extern void stec_read(const char *file, nav_t *nav)
{
    FILE *fp;
    int i,n;
    char *efiles[MAX_STECFILE];
    
    trace(2,"stec_read: file=%s\n",file);
    
    stec_free(nav);
    
    for (i=0;i<MAX_STECFILE;i++) {
        if (!(efiles[i]=(char *)malloc(1024))) {
            for (i--;i>=0;i--) free(efiles[i]);
            return;
        }
    }
    /* expand wild card in file path */
    n=expath(file,efiles,MAX_STECFILE);
    
    for (i=0;i<n;i++) {
        if (!(fp=fopen(efiles[i],"r"))) {
            trace(2,"stec grid file open error %s\n",efiles[i]);
            continue;
        }
        fprintf(stderr,"stec_read: %s\n",efiles[i]);
        
        /* read stec grid file */
        read_stecb(fp,nav);
        
        fclose(fp);
    }
    for (i=0;i<MAX_STECFILE;i++) free(efiles[i]);
    
    /* sort stec data */
    for (i=0;i<nav->nn;i++) {
        if (nav->stec[i].n<=0) continue;
        qsort(nav->stec[i].data,nav->stec[i].n,sizeof(stecd_t),comp_data);
    }
}
/* search stec grid ----------------------------------------------------------*/
extern int stec_grid(const nav_t *nav, const double *pos, int nmax, int *index,
                     double *dist)
{
    double d,dd[2];
    int i,j,k,n=0;
    
    trace(2,"stec_grid: pos=%.2f %.2f n=%d\n",pos[0]*R2D,pos[1]*R2D,nav->nn);
    
    for (i=0;i<nav->nn;i++) {
        
        /* distance to grid (m) */
        dd[0]=RE_WGS84*(pos[0]-nav->stec[i].pos[0]*D2R);
        dd[1]=RE_WGS84*(pos[1]-nav->stec[i].pos[1]*D2R)*cos(pos[0]);
        
        if ((d=MAX(norm(dd,2),1.0))>MAX_DIST*1000.0) continue;
        
        /* sort grids by distance */
        if (n<=0) {
            index[n]=i;
            dist[n++]=d;
        }
        else {
            for (j=0;j<n;j++) if (d<dist[j]) break;
            if (j>=nmax) continue;
            for (k=MIN(n,nmax-1);k>j;k--) {
                index[k]=index[k-1];
                dist [k]=dist [k-1];
            }
            index[j]=i;
            dist [j]=d;
            if (n<nmax) n++;
        }
    }
    for (i=0;i<n;i++) {
        trace(2,"stec_grid: index=%3d dist=%.1f pos=%.2f %.2f\n",index[i],
              dist[i],nav->stec[index[i]].pos[0],nav->stec[index[i]].pos[1]);
    }
    trace(2,"stec_grid: n=%d\n",n);
    return n;
}
/* search stec data ----------------------------------------------------------*/
extern int stec_data(stec_t *stec, gtime_t time, int sat, double *iono,
                     double *rate, double *rms, int *slip)
{
    double tt;
    int i,j,k;
    
    trace(4,"search_data: %s sat=%2d\n",time_str(time,0),sat);
    
    if (stec->n<=0) return 0;
    
    /* use index cache for satellite */
    k=stec->index[sat-1];
    
    /* binary search by satellite */
    if (stec->data[k].sat!=sat) {
        for (i=0,j=stec->n-1;i<j-1;) {
            k=(i+j)/2;
            if (sat==stec->data[k].sat) break;
            if (sat<stec->data[k].sat) j=k; else i=k;
        }
        if (stec->data[k].sat!=sat) {
            trace(2,"search_data: no iono %s sat=%2d\n",time_str(time,0),sat);
            return 0;
        }
    }
    /* serial search by time */
    if (timediff(time,stec->data[k].time)>=0.0) {
        for (;k<stec->n&&stec->data[k].sat==sat;k++) {
            if (timediff(time,stec->data[k].time)<0.0) break;
        }
        k--;
    }
    else {
        for (;k>=0&&stec->data[k].sat==sat;k--) {
            if (timediff(time,stec->data[k].time)>=0.0) break;
        }
        if (k<0||stec->data[k].sat!=sat) {
            trace(2,"search_data: no iono %s sat=%2d\n",time_str(time,0),sat);
            return 0;
        }
    }
    /* save index cache for satellite */
    stec->index[sat-1]=k;
    
    tt=timediff(time,stec->data[k].time);
    if (fabs(tt)>MAX_AGE) {
        trace(2,"search_data: age error %s sat=%2d tt=%.0f\n",time_str(time,0),sat,tt);
        return 0;
    }
    *iono=stec->data[k].iono+stec->data[k].rate*tt;
    *rate=stec->data[k].rate;
    *rms=stec->data[k].rms;
    *slip=stec->data[k].slip;
    
    trace(4,"search_data: pos=%.2f %.2f iono=%6.3f rms=%6.3f slip=%d tt=%3.0f\n",
          stec->pos[0],stec->pos[1],*iono,*rms,*slip,tt);
    
    return 1;
}
/* ionosphere model by stec grid data ------------------------------------------
* compute ionospheric delay by stec grid data
* args   : gtime_t time     I   time (gpst)
*          nav_t  *nav      I   navigation data
*          int    sat       I   satellite number
*          double *pos      I   receiver position {lat,lon,h} (rad,m)
*          double *azel     I   azimuth/elevation angle {az,el} (rad)
*          double *iono     O   ionospheric delay (L1) (m)
*          double *rate     O   ionospheric rate (L1) (m/s)
*          double *var      O   ionospheric dealy (L1) variance (m^2)
*          int    brk       O   break flag
* return : status (1:ok,0:error)
* notes  : non-thread-safe
*-----------------------------------------------------------------------------*/
extern int stec_ion(gtime_t time, const nav_t *nav, int sat, const double *pos,
                    const double *azel, double *iono, double *rate, double *var,
                    int *brk)
{
    static double pos_[2]={0},dist[MAX_NGRID]; /* grid cache */
    static int n=0,index[MAX_NGRID];
    
    double ionos[MAX_NGRID],rates[MAX_NGRID],rms[MAX_NGRID],sum;
    int i,slip;
    
    trace(4,"stec_ion: time=%s sat=%2d pos=%.2f %.2f azel=%.1f %.1f\n",
          time_str(time,0),sat,pos[0]*R2D,pos[1]*R2D,azel[0]*R2D,azel[1]*R2D);
    
    *iono=*rate=*var=0.0;
    *brk=0;
    
    if (azel[1]<MIN_EL||pos[2]<MIN_HGT) return 0;
    
    if (SQR(pos[0]-pos_[0])+SQR(pos[1]-pos_[1])>1E-12) {
        
        /* search stec grid */
        n=stec_grid(nav,pos,MAX_NGRID,index,dist);
        
        pos_[0]=pos[0]; pos_[1]=pos[1];
    }
    if (n<=0) return 0;
    
    for (i=0;i<n;i++) { /* for each grid */
        
        /* search stec data */
        if (!stec_data(nav->stec+index[i],time,sat,ionos+i,rates+i,rms+i,
                       &slip)) {
            return 0;
        }
        if (slip) *brk=1;
    }
    /* inversed distance weighting interpolation */
    sum=*iono=*rate=*var=0.0;
    for (i=0;i<n;i++) sum+=1.0/dist[i];
    for (i=0;i<n;i++) {
        *iono+=ionos[i]/dist[i]/sum;
        *rate+=rates[i]/dist[i]/sum;
        *var+=SQR(rms[i])/dist[i]/sum;
    }
    return 1;
}
