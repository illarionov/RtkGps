/*------------------------------------------------------------------------------
* sdr.h: sdr constants, types and function prototypes
*
*          Copyright (C) 2010 by T.TAKASU, All rights reserved.
*
* options : -DFFTW   use fftw
*           -DMKLFFT use mkl dft with fftw interface
*           -DCUFFT  use cufft with cuda
*
* version : $Revision:$ $Date:$
* history : 2010/10/31 1.0  new
*-----------------------------------------------------------------------------*/
#ifndef SDR_H
#define SDR_H
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#if defined (FFTW)
#include "fftw3.h"
#elif defined (MKLFFT)
#include "fftw.h"
#include "fftw2_mkl.h"
#elif defined (CUFFT)
#include "cuda.h"
#include "cuda_runtime.h"
#include "cufft.h"
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* constants -----------------------------------------------------------------*/
#if !defined(PI)
#define PI          3.1415926535897932  /* pi */
#endif

/* type definition -----------------------------------------------------------*/

#if defined(FFTW)||(MKLFFT)
typedef fftwf_complex cpx_t; /* complex type for fft */
#elif defined(CUFFT)
typedef cufftComplex cpx_t;
#endif

typedef struct {		/* signal options */
    double f_s;			/* sampling rate (Hz) */
    double f_if;		/* intermediate frequency (Hz) */
    double dtype;   	/* sampling data type (0:real,1:complex) */
	int ctype;			/* code type */
    double crate;   	/* code chip rate (chip/s) */
    double tau;			/* coherent integration time (s) */
    double drange;  	/* doppler search half range (Hz) */
    double athres;  	/* acquisition threshold (dBHz) */
    double lthres;  	/* loss of lock threshold (dBHz) */
    double cspace;  	/* correlator space (chip) */
    double b_carr;  	/* noise bandwidth of PLL (Hz) */
    double b_code;  	/* noise bandwidth of DLL (Hz) */
} sdrsigopt_t;

typedef struct {		/* sdr options */
    int nch;			/* number of channel */
} sdropt_t;

typedef struct {		/* sdr receiver options */
    int trksat[MAXSAT];	/* tracking satellite flag */
} sdr_t;

/* sdr common functions ------------------------------------------------------*/
extern void *sdrmalloc(size_t size);
extern void sdrfree(void *p);
extern void mul_12(const char *a, const char *b, int n, short *c1, short *c2);
extern void mul_21(const char *a1, const char *a2, const char *b, int n,
                   short *c1, short *c2);
extern void dot_21(const short *a1, const short *a2, const short *b, int n,
                   double *d1, double *d2);
extern void dot_22(const short *a1, const short *a2, const short *b1,
                   const short *b2, int n, double *d1, double *d2);
extern void dot_23(const short *a1, const short *a2, const short *b1,
                   const short *b2, const short *b3, int n, double *d1,
                   double *d2);
extern void resdata(const char *data, int dtype, int n, int nbit, char *rdata);
extern void rescode(const char *code, int len, double coff, double ci, int n,
                    short *rcode);
extern void mixcarr(const char *data, int type, double ti, int n, double freq,
                    double phi0, short *I, short *Q, double *scale);

/* fft functions -------------------------------------------------------------*/
extern cpx_t *cpxmalloc(int n);
extern void cpxfree(cpx_t *cpx);
extern void cpxfft(cpx_t *cpx, int n);
extern void cpxifft(cpx_t *cpx, int n);
extern void cpxcpx(const short *I, const short *Q, double scale, int n,
                   cpx_t *cpx);
extern void cpxconv(cpx_t *cpxa, const cpx_t *cpxb, int n, double *conv);

/* code generator ------------------------------------------------------------*/
extern void gencode(int sat, int ctype, double coff, double ci, int n,
                    short *code);

/* correlators ---------------------------------------------------------------*/
extern void correlator(const char *data, int dtype, double ti, int n,
                       double freq, double phi0, int sat, int ctype,
                       double crate, double coff, int s, int m, double *I,
                       double *Q);
extern void pcorrelator(const char *data, int dtype, double ti, int n,
                        double freq, int sat, int ctype, double crate, int nbit,
                        double *P);

/* sdr receiver functions-----------------------------------------------------*/
extern int sdrinit(sdr_t *sdr, const sdropt_t *opt);
extern void sdrfree(sdr_t *sdr);
extern int sdrstart(sdr_t *sdr);
extern int sdrstop(sdr_t *sdr);

#ifdef __cplusplus
}
#endif
#endif /* SDR_H */
