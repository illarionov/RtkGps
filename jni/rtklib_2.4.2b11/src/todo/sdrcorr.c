/*------------------------------------------------------------------------------
* sdrcorr.c: sdr receiver correlator
*
*          Copyright (C) 2010 by T.TAKASU, All rights reserved.
*
* version : $Revision:$ $Date:$
* history : 2010/10/31 1.0  new
*-----------------------------------------------------------------------------*/
#include "sdr.h"

/* type definition -----------------------------------------------------------*/

typedef struct {                    /* code table type */
    int code_len;                   /* number of code samples */
    cpx_t *code_cpx;                /* sampled code in freq domain */
} codetbl_t;

/* correlator ------------------------------------------------------------------
* multiply sampling data and carrier (I/Q), multiply code (E/P/L), and integrate
* args   : char   *data     I   sampling data vector (n x 1 or 2n x 1)
*          int    dtype     I   sampling data type (0:real,1:complex)
*          double ti        I   sampling interval (s)
*          int    n         I   number of samples
*          double freq      I   carrier frequency (Hz)
*          double phi0      I   carrier initial phase (rad)
*          int    sat       I   satellite number
*          int    ctype     I   code type
*          double crate     I   code chip rate (chip/s)
*          double coff      I   code chip offset (chip)
*          int    s         I   correlator spacing (sample)
*          int    m         I   number of side-correlators
*          double *I,*Q     O   normalized correlation power I,Q
*                                 I={I_P,I_E1,I_L1,I_E2,I_L2,...,I_Em,I_Lm}
*                                 Q={Q_P,Q_E1,Q_L1,Q_E2,Q_L2,...,Q_Em,Q_Lm}
* return : none
* notes  : signal=f(t)+i*g(t)
*            data={f(0),f(ti),f(2*ti),f(3*ti),f(3*ti),...   } : real
*            data={f(0),g(0),f(ti),g(ti),f(2*ti),g(2*ti),...} : complex
*-----------------------------------------------------------------------------*/
extern void correlator(const char *data, int dtype, double ti, int n,
                       double freq, double phi0, int sat, int ctype,
                       double crate, double coff, int s, int m, double *I,
                       double *Q)
{
    double scale;
    short *dataI,*dataQ,*codex,*code;
    int i;
    
    if (!(dataI=(short *)sdrmalloc(sizeof(short)*n+32))||
        !(dataQ=(short *)sdrmalloc(sizeof(short)*n+32))||
        !(codex=(short *)sdrmalloc(sizeof(short)*(n+2*s*m+1)+32))) return;
    
    code=codex+s*m; /* prompt */
    
#if 0
    /* mix local carrier */
    mixcarr(data,dtype,ti,n,freq,phi0,dataI,dataQ,&scale);
#endif
    
#if 0
    /* generate code */
    gencode(sat,ctype,coff+s*m*ti*crate,ti*crate,n+2*s*m+1,codex);
#endif
    
#if 1
    /* multiply code and integrate */
    if (m<=0) {
        dot_21(dataI,dataQ,code,n,I,Q);
    }
    else {
        dot_23(dataI,dataQ,code,code-s,code+s,n,I,Q);
    }
    for (i=1;i<m;i++) {
        dot_22(dataI,dataQ,code-(i+1)*s,code+(i+1)*s,n,I+1+i*2,Q+1+i*2);
    }
    for (i=0;i<1+2*m;i++) {
        I[i]*=scale/n;
        Q[i]*=scale/n;
    }
#endif
    sdrfree(dataI); sdrfree(dataQ); sdrfree(codex);
}
/* parallel correlator ----------------------------------------------------------
* fft based parallel correlator
* args   : char   *data     I   sampling data vector (n x 1 or 2n x 1)
*          int    dtype     I   sampling data type (0:real,1:complex)
*          double ti        I   sampling interval (s)
*          int    n         I   number of samples
*          double freq      I   carrier frequency (Hz)
*          int    sat       I   satellite number
*          int    ctype     I   code type
*          double crate     I   code chip rate (chip/s)
*          int    nbit      I   number of bits for resampling data
*          double *P        O   normalized correlation power vector (2^nbit x 1)
* return : none
* notes  : P=abs(ifft(conj(fft(code)).*fft(data.*e^(2*pi*freq*t*i)))).^2
*-----------------------------------------------------------------------------*/
extern void pcorrelator(const char *data, int dtype, double ti, int n,
                        double freq, int sat, int ctype, double crate, int nbit,
                        double *P)
{
    static codetbl_t codetbl[256]={{0}};
    cpx_t *datax;
    double scale;
    short *dataI,*dataQ,*code;
    char *dataR;
    int m=1<<nbit; /* number of resampling data */
    
    ti=ti*n/m; /* resamling interval */
    
    if (codetbl[sat-1].code_len!=m||!codetbl[sat-1].code_cpx) {
        
        if (codetbl[sat-1].code_cpx) cpxfree(codetbl[sat-1].code_cpx);
        
        if (!(code=(short *)sdrmalloc(sizeof(short)*m+32))||
            !(codetbl[sat-1].code_cpx=cpxmalloc(m))) return;
        
        /* generate code */
        gencode(sat,ctype,0.0,ti*crate,m,code);
        
        /* to frequency domain */
        cpxcpx(code,NULL,1.0,m,codetbl[sat-1].code_cpx);
        cpxfft(codetbl[sat-1].code_cpx,m);
        codetbl[sat-1].code_len=m;
        
        sdrfree(code);
    }
    if (!(dataR=(char  *)sdrmalloc(sizeof(char )*m+32))||
        !(dataI=(short *)sdrmalloc(sizeof(short)*m+32))||
        !(dataQ=(short *)sdrmalloc(sizeof(short)*m+32))||
        !(datax=cpxmalloc(m))) return;
    
    /* resample data */
    resdata(data,dtype,n,nbit,dataR);
    
    /* mix local carrier */
    mixcarr(dataR,dtype,ti,m,freq,0.0,dataI,dataQ,&scale);
    
    /* to complex */
    cpxcpx(dataI,dataQ,scale/m,m,datax);
    
    /* convolution */
    cpxconv(datax,codetbl[sat-1].code_cpx,m,P);
    
    sdrfree(dataR); sdrfree(dataI); sdrfree(dataQ); cpxfree(datax);
}
