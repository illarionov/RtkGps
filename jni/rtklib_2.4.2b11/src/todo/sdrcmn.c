/*------------------------------------------------------------------------------
* sdrcmn.c: sdr receiver common functions
*
* options : -DSSE2   use sse2/ssse3 instructions
*
*          Copyright (C) 2010 by T.TAKASU, All rights reserved.
*
* version : $Revision:$ $Date:$
* history : 2010/10/31 1.0  new
*-----------------------------------------------------------------------------*/
#include "sdr.h"

#if defined(SSE2)
#include <emmintrin.h>
#include <tmmintrin.h>
#endif

#if defined(MATLAB_MEX_FILE)
#include "mex.h"
#define Printf      mexPrintf
#else
#define Printf      printf
#endif

#define CSCALE      (1.0/32.0)      /* carrier lookup table scale factor */

/* allocate memory -----------------------------------------------------------*/
extern void *sdrmalloc(size_t size)
{
#if !defined(SSE2)
    return malloc(size);
#else
    return _aligned_malloc(size,16);
#endif
}
/* free memory ---------------------------------------------------------------*/
extern void sdrfree(void *p)
{
#if !defined(SSE2)
    free(p);
#else
    _aligned_free(p);
#endif
}
/* complex malloc/free -------------------------------------------------------*/
extern cpx_t *cpxmalloc(int n)
{
#if defined(FFTW)||(MKLFFT)
    return (cpx_t *)fftwf_malloc(sizeof(cpx_t)*n+32);
#else
    return (cpx_t *)malloc(sizeof(cpx_t)*n+32);
#endif
}
extern void cpxfree(cpx_t *cpx)
{
#if defined(FFTW)||(MKLFFT)
    fftwf_free(cpx);
#else
    free(cpx);
#endif
}
/* complex fft ---------------------------------------------------------------*/
extern void cpxfft(cpx_t *cpx, int n)
{
#if defined(FFTW)||(MKLFFT)
    fftwf_plan p=fftwf_plan_dft_1d(n,cpx,cpx,FFTW_FORWARD,FFTW_ESTIMATE);
    fftwf_execute(p);
    fftwf_destroy_plan(p);
    
#elif defined(CUFFT)
    cufftHandle p;
    cpx_t *dev;
    
    if (cudaMalloc((void **)&dev,sizeof(cpx_t)*n)!=CUFFT_SUCCESS) return;
    
    cudaMemcpy(dev,cpx,sizeof(cpx_t)*n,cudaMemcpyHostToDevice);
    
    if (cufftPlan1d(&p,n,CUFFT_C2C,1)!=CUFFT_SUCCESS||
        cufftExecC2C(p,dev,dev,CUFFT_FORWARD)!=CUFFT_SUCCESS) {
        cudaFree(dev);
        return;
    }
    cudaMemcpy(cpx,dev,sizeof(cpx_t)*n,cudaMemcpyDeviceToHost);
    cudaFree(dev);
    cufftDestroy(p);
#endif
}
/* complex ifft --------------------------------------------------------------*/
extern void cpxifft(cpx_t *cpx, int n)
{
#if defined(FFTW)||(MKLFFT)
    fftwf_plan p=fftwf_plan_dft_1d(n,cpx,cpx,FFTW_BACKWARD,FFTW_ESTIMATE);
    fftwf_execute(p);
    fftwf_destroy_plan(p);
    
#elif defined(CUFFT)
    cufftHandle p;
    cpx_t *dev;
    
    if (cudaMalloc((void **)&dev,sizeof(cpx_t)*n)!=CUFFT_SUCCESS) return;
    
    cudaMemcpy(dev,cpx,sizeof(cpx_t)*n,cudaMemcpyHostToDevice);
    
    if (cufftPlan1d(&p,n,CUFFT_C2C,1)!=CUFFT_SUCCESS||
        cufftExecC2C(p,dev,dev,CUFFT_INVERSE)!=CUFFT_SUCCESS) {
        cudaFree(dev);
        return;
    }
    cudaMemcpy(cpx,dev,sizeof(cpx_t)*n,cudaMemcpyDeviceToHost);
    cudaFree(dev);
    cufftDestroy(p);
#endif
}
/* generate complex vector: cpx=comlex(I,Q) ----------------------------------*/
extern void cpxcpx(const short *I, const short *Q, double scale, int n,
                   cpx_t *cpx)
{
    float *p=(float *)cpx;
    int i;
    
    for (i=0;i<n;i++,p+=2) {
        p[0]=  I[i]*(float)scale;
        p[1]=Q?Q[i]*(float)scale:0.0f;
    }
}
/* convolution s=sqrt(abs(ifft(fft(cpxa).*conj(cpxb))).^2) -------------------*/
extern void cpxconv(cpx_t *cpxa, const cpx_t *cpxb, int n, double *conv)
{
    float *p,*q,real,n2=(float)n*n;
    int i;
    
    cpxfft(cpxa,n);
    
    for (i=0,p=(float *)cpxa,q=(float *)cpxb;i<n;i++,p+=2,q+=2) {
        real=-p[0]*q[0]-p[1]*q[1];
        p[1]= p[0]*q[1]-p[1]*q[0];
        p[0]=real;
    }
    cpxifft(cpxa,n);
    
    for (i=0,p=(float *)cpxa;i<n;i++,p+=2) {
        conv[i]=(p[0]*p[0]+p[1]*p[1])/n2;
    }
}
/* print xmm register --------------------------------------------------------*/
#define PRINT_INT8(str,xmm) { \
    char _w[16]; \
    _mm_storeu_si128((void *)_w,xmm); \
    Printf("%s%3d %3d %3d %3d %3d %3d %3d %3d %3d %3d %3d %3d %3d %3d %3d %3d\n", \
           str,_w[0],_w[1],_w[2],_w[3],_w[4],_w[5],_w[6],_w[7], \
           _w[8],_w[9],_w[10],_w[11],_w[12],_w[13],_w[14],_w[15]); \
}
#define PRINT_INT16(str,xmm) { \
    short _w[8]; \
    _mm_storeu_si128((void *)_w,xmm); \
    Printf("%s%6d %6d %6d %6d %6d %6d %6d %6d\n", \
           str,_w[0],_w[1],_w[2],_w[3],_w[4],_w[5],_w[6],_w[7]); \
}
#define PRINT_INT32(str,xmm) { \
    int _w[4]; \
    _mm_storeu_si128((void *)_w,xmm); \
    Printf("%s%11d %11d %11d %11d\n",str,_w[0],_w[1],_w[2],_w[3]); \
}
#define PRINT_DBL(str,xmm) { \
    double _w[2]; \
    _mm_storeu_pd((void *)_w,xmm); \
    Printf("%s%.3f %.3f\n",str,_w[0],_w[1]); \
}
/* multiply and add: xmm{int32}+=src1[8]{int16}.*src2[8]{int16} --------------*/
#define MULADD_INT16(xmm,src1,src2) { \
    __m128i _x1,_x2; \
    _x1=_mm_load_si128 ((__m128i *)(src1)); \
    _x2=_mm_loadu_si128((__m128i *)(src2)); \
    _x2=_mm_madd_epi16(_x2,_x1); \
    xmm=_mm_add_epi32(xmm,_x2); \
}
/* sum: dst{any}=sum(xmm{int32}) ---------------------------------------------*/
#define SUM_INT32(dst,xmm) { \
    int _sum[4]; \
    _mm_storeu_si128((void *)_sum,xmm); \
    dst=_sum[0]+_sum[1]+_sum[2]+_sum[3]; \
}
/* expand int8: (xmm1,xmm2){int16}=xmm3{int8} --------------------------------*/
#define EXPAND_INT8(xmm1,xmm2,xmm3,zero) { \
    xmm1=_mm_unpacklo_epi8(zero,xmm3); \
    xmm2=_mm_unpackhi_epi8(zero,xmm3); \
    xmm1=_mm_srai_epi16(xmm1,8); \
    xmm2=_mm_srai_epi16(xmm2,8); \
}
/* load int8: (xmm1,xmm2){int16}=src[16]{int8} -------------------------------*/
#define LOAD_INT8(xmm1,xmm2,src,zero) { \
    __m128i _x; \
    _x  =_mm_loadu_si128((__m128i *)(src)); \
    EXPAND_INT8(xmm1,xmm2,_x,zero); \
}
/* load int8 complex: (xmm1,xmm2){int16}=src[16]{int8,int8} ------------------*/
#define LOAD_INT8C(xmm1,xmm2,src,zero,mask8) { \
    __m128i _x1,_x2; \
    _x1 =_mm_loadu_si128((__m128i *)(src)); \
    _x2 =_mm_srli_epi16(_x1,8); \
    _x1 =_mm_and_si128(_x1,mask8); \
    _x1 =_mm_packus_epi16(_x1,_x2); \
    EXPAND_INT8(xmm1,xmm2,_x1,zero); \
}
/* multiply int16: dst[16]{int16}=(xmm1,xmm2){int16}.*(xmm3,xmm4){int16} -----*/
#define MUL_INT16(dst,xmm1,xmm2,xmm3,xmm4) { \
    xmm1=_mm_mullo_epi16(xmm1,xmm3); \
    xmm2=_mm_mullo_epi16(xmm2,xmm4); \
    _mm_storeu_si128((void *)(dst)    ,xmm1); \
    _mm_storeu_si128((void *)((dst)+8),xmm2); \
}
/* multiply int8: dst[16]{int16}=src[16]{int8}.*(xmm1,xmm2){int16} -----------*/
#define MUL_INT8(dst,src,xmm1,xmm2,zero) { \
    __m128i _x1,_x2; \
    LOAD_INT8(_x1,_x2,src,zero); \
    MUL_INT16(dst,_x1,_x2,xmm1,xmm2); \
}
/* double to int32: xmm{int32}=(xmm1,xmm2){double} ---------------------------*/
#define DBLTOINT32(xmm,xmm1,xmm2) { \
    __m128i _int1,_int2; \
    _int1=_mm_cvttpd_epi32(xmm1); \
    _int2=_mm_cvttpd_epi32(xmm2); \
    _int2=_mm_slli_si128(_int2,8); \
    xmm=_mm_add_epi32(_int1,_int2); \
}
/* double to int16: xmm{int16}=(xmm1,...,xmm4){double}&mask{int32} -----------*/
#define DBLTOINT16(xmm,xmm1,xmm2,xmm3,xmm4,mask) { \
    __m128i _int3,_int4; \
    DBLTOINT32(_int3,xmm1,xmm2); \
    DBLTOINT32(_int4,xmm3,xmm4); \
    _int3=_mm_and_si128(_int3,mask); \
    _int4=_mm_and_si128(_int4,mask); \
    xmm=_mm_packs_epi32(_int3,_int4); \
}
/* multiply int8 with lut: dst[16]{int16}=(xmm1,xmm2){int8}.*xmm3{int8}[index] */
#define MIX_INT8(dst,xmm1,xmm2,xmm3,index,zero) { \
    __m128i _x,_x1,_x2; \
    _x=_mm_shuffle_epi8(xmm3,index); \
    EXPAND_INT8(_x1,_x2,_x,zero); \
    MUL_INT16(dst,_x1,_x2,xmm1,xmm2); \
}
/* multiply int8: c1=a.*real(b),c2=a.*imag(b) --------------------------------*/
extern void mul_12(const char *a, const char *b, int n, short *c1, short *c2)
{
    const char *p=a,*q=b;
    short *r1=c1,*r2=c2;
    
    for (;p<a+n;p++,q+=2,r1++,r2++) {
        *r1=(*p)*q[0];
        *r2=(*p)*q[1];
    }
}
/* multiply int8: c1=a1.*b,c2=a1.*b ------------------------------------------*/
extern void mul_21(const char *a1, const char *a2, const char *b, int n,
                   short *c1, short *c2)
{
    const char *p1=a1,*p2=a2,*q=b;
    short *r1=c1,*r2=c2;
    
#if !defined(SSE2)
    for (;p1<a1+n;p1++,p2++,q++,r1++,r2++) {
        *r1=(*p1)*(*q);
        *r2=(*p2)*(*q);
    }
#else
    __m128i xmm1,xmm2,zero=_mm_setzero_si128();
    
    for (;p1<a1+n;p1+=16,p2+=16,q+=16,r1+=16,r2+=16) {
        LOAD_INT8(xmm1,xmm2,q,zero);
        MUL_INT8(r1,p1,xmm1,xmm2,zero);
        MUL_INT8(r2,p2,xmm1,xmm2,zero);
    }
#endif
}
/* dot products: d1=dot(a1,b),d2=dot(a2,b) -----------------------------------*/
extern void dot_21(const short *a1, const short *a2, const short *b, int n,
                   double *d1, double *d2)
{
    const short *p1=a1,*p2=a2,*q=b;
    
#if !defined(SSE2)
    d1[0]=d2[0]=0.0;
    
    for (;p1<a1+n;p1++,p2++,q++) {
        d1[0]+=(*p1)*(*q);
        d2[0]+=(*p2)*(*q);
    }
#else
    __m128i xmm1,xmm2;
    
    xmm1=_mm_setzero_si128();
    xmm2=_mm_setzero_si128();
    
    for (;p1<a1+n;p1+=8,p2+=8,q+=8) {
        MULADD_INT16(xmm1,p1,q);
        MULADD_INT16(xmm2,p2,q);
    }
    SUM_INT32(d1[0],xmm1);
    SUM_INT32(d2[0],xmm2);
#endif
}
/* dot products: d1={dot(a1,b1),dot(a1,b2)},d2={dot(a2,b1),dot(a2,b2)} -------*/
extern void dot_22(const short *a1, const short *a2, const short *b1,
                   const short *b2, int n, double *d1, double *d2)
{
    const short *p1=a1,*p2=a2,*q1=b1,*q2=b2;
    
#if !defined(SSE2)
    d1[0]=d1[1]=d2[0]=d2[1]=0.0;
    
    for (;p1<a1+n;p1++,p2++,q1++,q2++) {
        d1[0]+=(*p1)*(*q1);
        d1[1]+=(*p1)*(*q2);
        d2[0]+=(*p2)*(*q1);
        d2[1]+=(*p2)*(*q2);
    }
#else
    __m128i xmm1,xmm2,xmm3,xmm4;
    
    xmm1=_mm_setzero_si128();
    xmm2=_mm_setzero_si128();
    xmm3=_mm_setzero_si128();
    xmm4=_mm_setzero_si128();
    
    for (;p1<a1+n;p1+=8,p2+=8,q1+=8,q2+=8) {
        MULADD_INT16(xmm1,p1,q1);
        MULADD_INT16(xmm2,p1,q2);
        MULADD_INT16(xmm3,p2,q1);
        MULADD_INT16(xmm4,p2,q2);
    }
    SUM_INT32(d1[0],xmm1);
    SUM_INT32(d1[1],xmm2);
    SUM_INT32(d2[0],xmm3);
    SUM_INT32(d2[1],xmm4);
#endif
}
/* dot products: d1={dot(a1,b1),dot(a1,b2),dot(a1,b3)},d2={...} --------------*/
extern void dot_23(const short *a1, const short *a2, const short *b1,
                   const short *b2, const short *b3, int n, double *d1,
                   double *d2)
{
    const short *p1=a1,*p2=a2,*q1=b1,*q2=b2,*q3=b3;
    
#if !defined(SSE2)
    d1[0]=d1[1]=d1[2]=d2[0]=d2[1]=d2[2]=0.0;
    
    for (;p1<a1+n;p1++,p2++,q1++,q2++,q3++) {
        d1[0]+=(*p1)*(*q1);
        d1[1]+=(*p1)*(*q2);
        d1[2]+=(*p1)*(*q3);
        d2[0]+=(*p2)*(*q1);
        d2[1]+=(*p2)*(*q2);
        d2[2]+=(*p2)*(*q3);
    }
#else
    __m128i xmm1,xmm2,xmm3,xmm4,xmm5,xmm6;
    
    xmm1=_mm_setzero_si128();
    xmm2=_mm_setzero_si128();
    xmm3=_mm_setzero_si128();
    xmm4=_mm_setzero_si128();
    xmm5=_mm_setzero_si128();
    xmm6=_mm_setzero_si128();
    
    for (;p1<a1+n;p1+=8,p2+=8,q1+=8,q2+=8,q3+=8) {
        MULADD_INT16(xmm1,p1,q1);
        MULADD_INT16(xmm2,p1,q2);
        MULADD_INT16(xmm3,p1,q3);
        MULADD_INT16(xmm4,p2,q1);
        MULADD_INT16(xmm5,p2,q2);
        MULADD_INT16(xmm6,p2,q3);
    }
    SUM_INT32(d1[0],xmm1);
    SUM_INT32(d1[1],xmm2);
    SUM_INT32(d1[2],xmm3);
    SUM_INT32(d2[0],xmm4);
    SUM_INT32(d2[1],xmm5);
    SUM_INT32(d2[2],xmm6);
#endif
}
/* resample data to 2^bits samples ---------------------------------------------
* resample data to 2^bits samples
* args   : char   *data     I   data
*          int    dtype     I   data type (0:real,1:complex)
*          int    n         I   number of data
*          int    nbit      I   number of bits of result samples
*          char   *rdata    O   resampled data
* return : none
*-----------------------------------------------------------------------------*/
extern void resdata(const char *data, int dtype, int n, int nbit, char *rdata)
{
    char *p;

#if !defined(SSE2)
    double index=0.0;
    int ind,m=1<<nbit;
    
    if (dtype) { /* real */
        for (p=rdata;p<rdata+m;p+=2,index+=n*2) {
            ind=(int)(index/m)*2;
            p[0]=data[ind  ];
            p[1]=data[ind+1];
        }
    }
    else { /* complex */
        for (p=rdata;p<rdata+m;p++,index+=n) {
            ind=(int)(index/m);
            p[0]=data[ind];
        }
    }
#else
    int index[4],m=1<<nbit;
    __m128d x1,x2,xmm1,xmm2,xmm3,xmm4;
    __m128i ind;
    
    xmm1=_mm_set_pd(n,0);
    xmm2=_mm_set_pd(n*3,n*2);
    xmm3=_mm_set1_pd(n*4);
    xmm4=_mm_set1_pd(1.0/m);
    
    if (dtype) {
        for (p=rdata;p<rdata+m;p+=8) {
            x1=_mm_mul_pd(xmm1,xmm4);
            x2=_mm_mul_pd(xmm2,xmm4);
            DBLTOINT32(ind,x1,x2);
            ind=_mm_slli_epi32(ind,1);
            _mm_storeu_si128((void *)index,ind);
            p[0]=data[index[0]];
            p[1]=data[index[0]+1];
            p[2]=data[index[1]];
            p[3]=data[index[1]+1];
            p[4]=data[index[2]];
            p[5]=data[index[2]+1];
            p[6]=data[index[3]];
            p[7]=data[index[3]+1];
            xmm1=_mm_add_pd(xmm1,xmm3);
            xmm2=_mm_add_pd(xmm2,xmm3);
        }
    }
    else {
        for (p=rdata;p<rdata+m;p+=4) {
            x1=_mm_mul_pd(xmm1,xmm4);
            x2=_mm_mul_pd(xmm2,xmm4);
            DBLTOINT32(ind,x1,x2);
            _mm_storeu_si128((void *)index,ind);
            p[0]=data[index[0]];
            p[1]=data[index[1]];
            p[2]=data[index[2]];
            p[3]=data[index[3]];
            xmm1=_mm_add_pd(xmm1,xmm3);
            xmm2=_mm_add_pd(xmm2,xmm3);
        }
    }
#endif
}
/* resample code ---------------------------------------------------------------
* resample code
* args   : char   *code     I   code
*          int    len       I   code length (len < 2^(31-FPBIT))
*          double coff      I   initial code offset (chip)
*          double ci        I   code sampling interval (chip)
*          int    n         I   number of samples
*          short  *rcode    O   resampling code
* return : none
*-----------------------------------------------------------------------------*/
extern void rescode(const char *code, int len, double coff, double ci, int n,
                    short *rcode)
{
    short *p;

#if !defined(SSE2)
    
    coff-=floor(coff/len)*len; /* 0<=coff<len */
    
    for (p=rcode;p<rcode+n;p++,coff+=ci) {
        if (coff>=len) coff-=len;
        *p=code[(int)coff];
    }
#else
    int i,index[4],x[4],nbit,scale;
    __m128i xmm1,xmm2,xmm3,xmm4,xmm5;
    
    coff-=floor(coff/len)*len; /* 0<=coff<len */
    
    for (i=len,nbit=31;i;i>>=1,nbit--) ;
    scale=1<<nbit; /* scale factor */
    
    for (i=0;i<4;i++,coff+=ci) {
        x[i]=(int)(coff*scale+0.5);
    }
    xmm1=_mm_loadu_si128((__m128i *)x);
    xmm2=_mm_set1_epi32(len*scale-1);
    xmm3=_mm_set1_epi32(len*scale);
    xmm4=_mm_set1_epi32((int)(ci*4*scale+0.5));
    
    for (p=rcode;p<rcode+n;p+=4) {
        
        xmm5=_mm_cmpgt_epi32(xmm1,xmm2);
        xmm5=_mm_and_si128(xmm5,xmm3);
        xmm1=_mm_sub_epi32(xmm1,xmm5);
        xmm5=_mm_srai_epi32(xmm1,nbit);
        _mm_storeu_si128((void *)index,xmm5);
        p[0]=code[index[0]];
        p[1]=code[index[1]];
        p[2]=code[index[2]];
        p[3]=code[index[3]];
        xmm1=_mm_add_epi32(xmm1,xmm4);
    }
#endif
}
/* mix local carrier -----------------------------------------------------------
* mix local carrier to data
* args   : char   *data     I   data
*          int    dtype     I   data type (0:real,1:complex)
*          double ti        I   sampling interval (s)
*          int    n         I   number of samples
*          double freq      I   carrier frequency (Hz)
*          double phi0      I   initial phase (rad)
*          short  *I,*Q     O   carrier mixed data I, Q component
*          double *scale    O   scale factor of output data
* return : none
*-----------------------------------------------------------------------------*/
extern void mixcarr(const char *data, int dtype, double ti, int n, double freq,
                    double phi0, short *I, short *Q, double *scale)
{
    const char *p;
    double phi,ps;

#if !defined(SSE2)
    static char cost[32]={0},sint[32]={0};
    int i,index;
    
    if (!cost[0]) {
        for (i=0;i<32;i++) {
            cost[i]=(char)floor((cos(PI/16*i)/CSCALE+0.5));
            sint[i]=(char)floor((sin(PI/16*i)/CSCALE+0.5));
        }
    }
    phi=phi0-floor(phi0/32)*32;
    ps=freq*32*ti;
    
    if (dtype) { /* complex */
        for (p=data;p<data+n*2;p+=2,I++,Q++,phi+=ps) {
            index=((int)phi)&31;
            *I=cost[index]*p[0];
            *Q=cost[index]*p[1];
        }
    }
    else { /* real */
        for (p=data;p<data+n;p++,I++,Q++,phi+=ps) {
            index=((int)phi)&31;
            *I=cost[index]*p[0];
            *Q=sint[index]*p[0];
        }
    }
    *scale=CSCALE;
#else
    static char cost[16]={0},sint[16]={0};
    int i;
    __m128d xmm1,xmm2,xmm3,xmm4,xmm5,xmm6,xmm7,xmm8,xmm9;
    __m128i dat1,dat2,dat3,dat4,ind1,ind2,xcos,xsin;
    __m128i zero=_mm_setzero_si128();
    __m128i mask4=_mm_set1_epi32(15);
    __m128i mask8=_mm_set1_epi16(255);
    
    if (!cost[0]) {
        for (i=0;i<16;i++) {
            cost[i]=(char)floor((cos(PI/8*i)/CSCALE+0.5));
            sint[i]=(char)floor((sin(PI/8*i)/CSCALE+0.5));
        }
    }
    phi=phi0-floor(phi0/16)*16;
    ps=freq*16*ti;
    xmm1=_mm_set_pd(phi+ps,phi); phi+=ps*2;
    xmm2=_mm_set_pd(phi+ps,phi); phi+=ps*2;
    xmm3=_mm_set_pd(phi+ps,phi); phi+=ps*2;
    xmm4=_mm_set_pd(phi+ps,phi); phi+=ps*2;
    xmm5=_mm_set_pd(phi+ps,phi); phi+=ps*2;
    xmm6=_mm_set_pd(phi+ps,phi); phi+=ps*2;
    xmm7=_mm_set_pd(phi+ps,phi); phi+=ps*2;
    xmm8=_mm_set_pd(phi+ps,phi); phi+=ps*2;
    xmm9=_mm_set1_pd(ps*16);
    xcos=_mm_loadu_si128((__m128i *)cost);
    xsin=_mm_loadu_si128((__m128i *)sint);
    
    if (dtype) { /* complex */
        for (p=data;p<data+n*2;p+=32,I+=16,Q+=16) {
            LOAD_INT8C(dat1,dat2,p   ,zero,mask8);
            LOAD_INT8C(dat3,dat4,p+16,zero,mask8);
            
            DBLTOINT16(ind1,xmm1,xmm2,xmm3,xmm4,mask4);
            DBLTOINT16(ind2,xmm5,xmm6,xmm7,xmm8,mask4);
            ind1=_mm_packus_epi16(ind1,ind2);
            MIX_INT8(I,dat1,dat3,xcos,ind1,zero);
            MIX_INT8(Q,dat2,dat4,xcos,ind1,zero);
            xmm1=_mm_add_pd(xmm1,xmm9);
            xmm2=_mm_add_pd(xmm2,xmm9);
            xmm3=_mm_add_pd(xmm3,xmm9);
            xmm4=_mm_add_pd(xmm4,xmm9);
            xmm5=_mm_add_pd(xmm5,xmm9);
            xmm6=_mm_add_pd(xmm6,xmm9);
            xmm7=_mm_add_pd(xmm7,xmm9);
            xmm8=_mm_add_pd(xmm8,xmm9);
        }
    }
    else { /* real */
        for (p=data;p<data+n;p+=16,I+=16,Q+=16) {
            LOAD_INT8(dat1,dat2,p,zero);
            
            DBLTOINT16(ind1,xmm1,xmm2,xmm3,xmm4,mask4);
            DBLTOINT16(ind2,xmm5,xmm6,xmm7,xmm8,mask4);
            ind1=_mm_packus_epi16(ind1,ind2);
            MIX_INT8(I,dat1,dat2,xcos,ind1,zero);
            MIX_INT8(Q,dat1,dat2,xsin,ind1,zero);
            xmm1=_mm_add_pd(xmm1,xmm9);
            xmm2=_mm_add_pd(xmm2,xmm9);
            xmm3=_mm_add_pd(xmm3,xmm9);
            xmm4=_mm_add_pd(xmm4,xmm9);
            xmm5=_mm_add_pd(xmm5,xmm9);
            xmm6=_mm_add_pd(xmm6,xmm9);
            xmm7=_mm_add_pd(xmm7,xmm9);
            xmm8=_mm_add_pd(xmm8,xmm9);
        }
    }
    *scale=CSCALE;
#endif
}
