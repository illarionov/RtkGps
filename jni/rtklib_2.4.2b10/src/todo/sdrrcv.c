/*------------------------------------------------------------------------------
* sdrrcv.c: sdr receiver common functions
*
*          Copyright (C) 2010 by T.TAKASU, All rights reserved.
*
* version : $Revision:$ $Date:$
* history : 2010/10/31 1.0  new
*-----------------------------------------------------------------------------*/
#include "sdr.h"

function testsdr(montype,file,opt)
%
% software gps receiver prototype
%
% 2010/10/14 0.1 new
%
if nargin<1, montype=3; end
if nargin<2
%    file='../GNSS_signal_records/GPSdata-DiscreteComponents-fs38_192-if9_55.bin';
%    file='../GNSS_signal_records/GPS_and_GIOVE_A-NN-fs16_3676-if4_1304.bin';
    file='../GN3Sv2/Application/gnss.bin';
end
if nargin<3
    %opt.tau    =2e-3;         % pre-integration time (s)
    %opt.f_s    =38.192e6;     % sampling rate (Hz)
    %opt.f_if   =9.548e6;      % intermediate frequency (Hz)
    %opt.tau    =2.5e-3;       % integration time (ms)
    %opt.f_s    =16.3676e6;    % sampling rate (Hz)
    %opt.f_if   =4.1304e6;     % intermediate frequency (Hz)
    %opt.dtype=0;              % sampling data type (0:real,1:complex)
    opt.tau     =5e-3;        % pre-integration time (s)
    opt.f_s    =8.1838e6;     % sampling rate (Hz)
    opt.f_if   =38.4e3;       % intermediate frequency (Hz)
    opt.dtype=1;              % sampling data type (0:real,1:complex)
    opt.crate  =1023/1e-3;    % code chip rate (chip/s)
    opt.drange =5e3;          % doppler search half range (Hz)
    opt.athres =35;           % acquisition threshold (dBHz)
    opt.lthres =25;           % loss of lock threshold (dBHz)
    opt.cspace =1;            % correlator space (chip)
    opt.b_carr =30;           % noise bandwidth of PLL (Hz)
    opt.b_code =0.5;          % noise bandwidth of DLL (Hz)
    opt.nch    =12;           % number of channel
%    opt.sat    =1:32;
    opt.sat    =183:198;
    opt.montype=montype;      % monitor type (1:signal,2:acquisition,3:tracking,4:nav)
    opt.monch  =1;            % monitor channel
    opt.srcint =0.01;         % search interval (s)
    opt.monint =0.05;         % monitor interval (s)
    opt.navint =1;            % navigation interval (s)
    opt.font   ='Consolas';   % font name
end

figure('color','w','renderer','painters');

[sdr,opt]=startsdr(file,opt);

/* print status --------------------------------------------------------------*/
static void printstat(FILE *fp, const sdr_t *sdr)
{
    sdrch_t *ch;
    char *stats[]={"-","LOCK","SYNC","NAV"};
    int i;
    
    fprintf(fp,"TIME=%04.0f/%08.1f  # OF SAT=%2d                      SEARCH=%3d %7.2fs\n"',
            sdr->week,sdr->tow,length(find(sdr.sats)),opt.sat(sdr.sat),sdr->time);
    fprintf(fp,"POS =%010.1f %010.1f %010.1f VEL =%09.3f %09.3f %09.3f\n",
            sdr->pos,sdr->vel);
    fprintf(fp,"  CH  SAT FREQ  DOPP(Hz)     ADR(cyc) CODOFF(chip)  C/N0  LOCKTIME STATE\n");
    
    for (i=0;i<sdr->nch;i++) {
        ch=sdr->ch+i;
        fprintf(fp,"(%2d) %3d  L%d %10.1f %12.3f %11.3f %6.1f %8.1f %6s\n",
                i+1,ch->sat,ch->freq,ch->dopp,ch->adr+ch.poff/2/PI,ch->coff,
                ch->cn0,ch->lock*opt->tau,stats[ch->stat]);
    }
}
/* compute c/n0 --------------------------------------------------------------*/
static double carriertonoise(double I, double Q, double noise, double tau)
{
    return 10.0*log10((I*I+Q*Q)/noise/tau);
}
/* initialize channel --------------------------------------------------------*/
static void initch(sdrch_t *ch, int sat, int stat)
{
    ch->sat =sat;              % satellite
    ch->stat=stat;             % track status (0:search,1:bit sync,2:frame sync)
    ch->freq=1;                % frquency
    ch->dopp=0;                % doppler frequency (Hz)
    ch->poff=0;                % phase offset (rad)
    ch->phi0=0;                % initial phase (rad)
    ch->adr =0;                % acumulated doppler (cyc)
    ch->coff=0;                % code offset (chip)
    ch->cod0=0;                % initial code (chip)
    ch->cn0 =0;                % c/n0 (dBHz)
    ch->lock=0;                % lock time
    ch->n=30/opt.tau;          % signal buffer size (30s)
    ch->T=repmat(nan,1,ch.n);  % signal buffer time
    ch->I=repmat(nan,1,ch.n);  % I signal buffer
    ch->Q=repmat(nan,1,ch.n);  % Q signal buffer
    ch->P=[nan,nan,nan];
    ch->data=0;                % navigation data
}
/* tracking ------------------------------------------------------------------*/
static void tracking(sdrch_t *ch, const char *buff, int n, sdropt_t *opt)
{
    double freq,phi0,coff,cn0,noise,D,E,L,k1,k2,I[3],Q[3];
    int index;
    
    freq=opt->f_if+ch->dopp;
    phi0=ch->phi0+ch->poff;
    coff=ch->cod0+ch->coff;
    ch->phi0=ch.phi0+2*pi*freq*opt.tau;
    ch->adr=ch.adr+ch.dopp*opt.tau;
    ch->cod0=ch.cod0+opt.tau*opt.crate;

    /* correlator */
    correlator(buff,opt->dtype,opt->ti,freq,phi0,ch->sat,0,opt->crate,coff,
               opt->cs,1,I,Q);
    
    /* compute c/n0 */
    cn0=carriertonoise(ch->I[0],ch->Q[1],noise,opt->tau);
    ch->cn0=ch->cn0*0.8+cn0->0.2;
    
    /* detect loss of lock */
    if (ch->cn0<opt->lthres) {
        fprintf(stderr,'loss of lock: sat=%d\n',ch->sat);
        initch(ch,0,0.0,opt);
        return
    }
    /* track carrier (costas PLL) */
    D=atan(I(1)/Q(1)); /* rad */
    k1=2.4*opt->b_carr*opt->tau;
    k2=2.88*SQR(opt->b_carr*opt->tau);
    ch->poff=ch.poff-k1*D;
    ch->dopp=ch.dopp-k2*D/(2.0*PI*opt->tau);

    /* track code (noncoherent DLL) */
    E=SQR(I[1])+SQR(Q[1]);
    L=SQR(I[2])+SQR(Q[2]);
    D=(E-L)/(E+L);
    k1=4.0*opt->b_code*opt->tau;
    ch->coff=ch->coff+k1*D;
    
    /* record correlation power */
    ch->lock++;
    i=mod(ch.lock-1,ch.n)+1;
    ch->T[i]=time;
    ch->I[i]=I[0];
    ch->Q[i]=Q[1];
    ch->P=sqrt(SQR(I[0])+SQR(Q[0]));
}
/* decode data ---------------------------------------------------------------*/
static void decodedata(sdrch_t *ch, const sdropt_t *opt)
{
}
/* navigation ----------------------------------------------------------------*/
static void navigation(sdr_t *sdr)
{
}
/* receiver main thread ------------------------------------------------------*/
static void *sdrrcvthread(void *arg)
{
    sdr_t *sdr=(sdr_t *)arg;
    int i,n;
    
    /* input sampling data */
    while ((n=inputdata(buff,opt))<0) {
        
        for (i=0;i<sdr->nch;i++) { /* for each channel */
            if (!sdr->ch[i].stat) continue;
            
            /* tracking */
            tracking(sdr->ch+i,sdr->opt,buff,n);
            
            /* decode data */
            decodedata(sdr->ch+i,sdr->opt);
        }
        /* navigation */
        navigation(sdr);
    }
    /* stop receiver */
    stopsdr(sdr);
    
    return NULL;
}

/* input sampling data --------------------------------------------------------*/
static int inputdata(sdr_t *sdr)
{
    if opt.dtype, ns=opt.ns*2; else ns=opt.ns; end
    buff=fread(sdr.fp,ns,'int8=>int8')';
    if length(buff)~=ns, buff=[]; end
}
/* assign channel -------------------------------------------------------------*/
static int assignch(sdr_t *sdr, int sat, double dopp, double coff, double cn0)
{
    int i;
    
    for (i=0;i<sdr->nch;i++) {
        if (sdr->ch[i].stat>0) continue;
        
        initch(sdr->ch+i,sat,1);
        sdr->ch[i].stat=1;
        sdr->ch[i].dopp=dopp;
        sdr->ch[i].coff=coff;
        sdr->ch[i].cn0=cn0;
        sdr->sats[sat-1]=1;
        return 1;
    }
    return 0;
}
/* acquisition thread ---------------------------------------------------------*/
static void *acqthread(void *arg)
{
    for (;;) {
        /* next doppler bin/satellite */
        sdr.bin=sdr.bin+1;
        if (sdr.bin>length(opt.f_bin)) {
            sdr.bin=1;
            sdr.sat=sdr.sat+1;
            if sdr.sat>length(opt.sat), sdr.sat=1; end
        }
        /* parallel correlation */
        sat=opt.sat(sdr.sat);
        dopp=opt.f_bin(sdr.bin);
        freq=opt.f_if+dopp;
        sdr.corr=pcorrelator(buff,opt.dtype,opt.ti,freq,sat,0,opt.crate);

        /* search correlation peak */
        [cmax,i]=max(sdr.corr);
        sdr.noise=sdr.noise*0.9+mean(sdr.corr)*0.1;
        cn0=10*log10(cmax/sdr.noise/opt.tau);
        
        if sdr.sats(sdr.sat)|cn0<opt.athres, return; end
        coff=(i-1)*opt.ti*opt.crate;

        /* search fine doppler */
        for i=1:length(opt.ff_bin)
            freq=opt.f_if+dopp+opt.ff_bin(i);
            [I,Q]=correlator(buff,opt.dtype,opt.ti,freq,0,sat,0,opt.crate,coff,0,0);
            corrf(i)=I^2+Q^2;
        end
        [cmax,i]=max(corrf);
        cn0=10*log10(cmax/sdr.noise/opt.tau);
        dopp=dopp+opt.ff_bin(i);
    }
    return NULL;
}
/* start receiver -------------------------------------------------------------*/
extern void sdrstartrcv(sdr_t *sdr, const char *file)
{
    opt.ti=1/opt.f_s;        % sample interval (s)
    opt.ns=opt.tau*opt.f_s;  % sample in integration time
    opt.cs=round(opt.cspace/opt.ti/2/opt.crate); % correlator space /2 (sample)
    dstep=1/opt.tau/2;
    opt.f_bin =-opt.drange:dstep:opt.drange; % coast doppler search bin (Hz)
    opt.ff_bin=-dstep*2:50:dstep*2; % fine doppler search bin (Hz)
    sdr.time=0;              % running time (s)
    sdr.week=0;              % gps week
    sdr.tow =0;              % tow
    sdr.pos =[0 0 0];        % position
    sdr.vel =[0 0 0];        % velocity
    sdr.sat =1;              % search satellite
    sdr.bin =1;              % search doppler bin
    sdr.corr=[];             % search result
    sdr.coff=[];             % search result
    sdr.noise=3e-4;          % noise level
    sdr.sats=zeros(1,length(opt.sat));

    % initialize channel
    for i=1:opt.nch, sdr.ch(i)=initch(0,0,opt); end

    /* open sampling data file */
    sdr.fp=fopen(file,'rb');
    if sdr.fp<0, error(['no file: ',file]); end
    
    /* start acquisition thread */
    
    /* start tracing thread */
}
/* stop receiver --------------------------------------------------------------*/
extern void sdrstoprcv(sdr_t *sdr)
{
    fclose(sdr->fp);
}
