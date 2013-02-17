/*------------------------------------------------------------------------------
* ins.h : ins common functions
*
*          Copyright (C) 2008-2009 by T.TAKASU, All rights reserved.
*
* version : $Revision: 1.1 $ $Date: 2008/09/05 01:32:44 $
* history : 2008/08/25 1.0 new
*
*                     This code is not supported yet.
*
*-----------------------------------------------------------------------------*/
#ifndef INS_H
#define INS_H
#ifdef __cplusplus
extern "C" {
#endif

/* type definition -----------------------------------------------------------*/
typedef struct {            /* imu data record type */
    gtime_t time;           /* time */
    double gyro[3];         /* angular rate measurements in b-frame (rad/s) */
    double accl[3];         /* force measuremnts in b-frame (m/s^2) */
    double temp[3];         /* temperature (C) */
    int stat;               /* sensor status (0:ok) */
} imudata_t;

typedef struct {            /* imu data type */
    imudata_t *data;        /* imu data records */
    int n,nmax;             /* number/max number of imu data record */
} imu_t;

typedef struct {            /* ins states */
    gtime_t time;           /* time */
    double Cbe[9];          /* b-frame to e-frame trans matrix */
    double re[3];           /* position (e-frame) */
    double ve[3];           /* velocity (e-frame) */
    double ae[3];           /* acceleration (e-frame) */
    double ba[3];           /* accelemeter-bias */
    double bg[3];           /* gyro-bias */
    double fb[3];           /* corected specific-force (b-frame) */
    double omgb[3];         /* corected angular rate (b-frame) */
} insstate_t;

/* global variable -----------------------------------------------------------*/
extern const double Omge[9]; /* earth rotation matrix in i/e-frame */

/* function prototypes -------------------------------------------------------*/
extern void matmul3(const char *tr, const double *A, const double *B, double *C);
extern void matmul3v(const char *tr, const double *A, const double *b, double *c);
extern void skewsym3(const double *ang, double *C);
extern double gravity0(const double *pos);
extern void gravity(const double *re, double *ge);
extern void initins(insstate_t *ins, const double *re, double angh,
                    const imudata_t *data, int n);
extern int updateins(insstate_t *ins, const imudata_t *data);
extern int readimu(const char *file, imu_t *imu);
extern void traceins(int level, const insstate_t *ins);

#ifdef __cplusplus
}
#endif
#endif /* INS_H */
