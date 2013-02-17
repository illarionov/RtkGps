/*------------------------------------------------------------------------------
* sdrcode.c: sdr receiver code generator
*
*          Copyright (C) 2010 by T.TAKASU, All rights reserved.
*
* version : $Revision:$ $Date:$
* history : 2010/10/31 1.0  new
*-----------------------------------------------------------------------------*/
#include "sdr.h"

#define MAXSATNO     210         /* max satellite number */
#define LEN_CODE_CA  1023        /* C/A code length */

/* global variables ----------------------------------------------------------*/
static int codelen[MAXSATNO]={0};   /* local code length */
static char *codes[MAXSATNO]={0};   /* local code table */

/* C/A code (IS-GPS-200D) ----------------------------------------------------*/
static char *gencode_CA(int prn, int *len)
{
    const static short delay[]={ /* G2 delay (chips) */
          5,   6,   7,   8,  17,  18, 139, 140, 141, 251,   /*   1- 10 */
        252, 254, 255, 256, 257, 258, 469, 470, 471, 472,   /*  11- 20 */
        473, 474, 509, 512, 513, 514, 515, 516, 859, 860,   /*  21- 30 */
        861, 862, 863, 950, 947, 948, 950,  67, 103,  91,   /*  31- 40 */
         19, 679, 225, 625, 946, 638, 161,1001, 554, 280,   /*  41- 50 */
        710, 709, 775, 864, 558, 220, 397,  55, 898, 759,   /*  51- 60 */
        367, 299,1018, 729, 695, 780, 801, 788, 732,  34,   /*  61- 70 */
        320, 327, 389, 407, 525, 405, 221, 761, 260, 326,   /*  71- 80 */
        955, 653, 699, 422, 188, 438, 959, 539, 879, 677,   /*  81- 90 */
        586, 153, 792, 814, 446, 264,1015, 278, 536, 819,   /*  91-100 */
        156, 957, 159, 712, 885, 461, 248, 713, 126, 807,   /* 101-110 */
        279, 122, 197, 693, 632, 771, 467, 647, 203, 145,   /* 111-120 */
        175,  52,  21, 237, 235, 886, 657, 634, 762, 355,   /* 121-130 */
       1012, 176, 603, 130, 359, 595,  68, 386, 797, 456,   /* 131-140 */
        499, 883, 307, 127, 211, 121, 118, 163, 628, 853,   /* 141-150 */
        484, 289, 811, 202,1021, 463, 568, 904, 670, 230,   /* 151-160 */
        911, 684, 309, 644, 932,  12, 314, 891, 212, 185,   /* 161-170 */
        675, 503, 150, 395, 345, 846, 798, 992, 357, 995,   /* 171-180 */
        877, 112, 144, 476, 193, 109, 445, 291,  87, 399,   /* 181-190 */
        292, 901, 339, 208, 711, 189, 263, 537, 663, 942,   /* 191-200 */
        173, 900,  30, 500, 935, 556, 373,  85, 652, 310    /* 201-210 */
    };
    char G1[LEN_CODE_CA],G2[LEN_CODE_CA],R1[10],R2[10],C1,C2,*code;
    int i,j;
    
    if (prn<1||210<prn||!(code=(char *)malloc(sizeof(char)*LEN_CODE_CA))) {
        return NULL;
    }
    for (i=0;i<10;i++) R1[i]=R2[i]=-1;
    for (i=0;i<LEN_CODE_CA;i++) {
        G1[i]=R1[9];
        G2[i]=R2[9];
        C1=R1[2]*R1[9];
        C2=R2[1]*R2[2]*R2[5]*R2[7]*R2[8]*R2[9];
        for (j=9;j>=0;j--) {
            R1[j+1]=R1[j];
            R2[j+1]=R2[j];
        }
        R1[0]=C1;
        R2[0]=C2;
    }
    for (i=0,j=LEN_CODE_CA-delay[prn-1];i<LEN_CODE_CA;i++,j++) {
        code[i]=-G1[i]*G2[j%LEN_CODE_CA];
    }
    *len=LEN_CODE_CA;
    
    return code;
}
/* generate code ---------------------------------------------------------------
* generate resampled code
* args   : int    sat       I   satellite number
*          int    ctype     I   code type (not used)
*          double coff      I   initial code offset (chip)
*          double ci        I   code sampling interval (chip)
*          int    n         I   number of samples
*          short  *code     O   sampling code vector (n x 1)
* return : none
*-----------------------------------------------------------------------------*/
extern void gencode(int sat, int ctype, double coff, double ci, int n,
                    short *code)
{
    int i;
    
    if (sat<1||MAXSATNO<sat) {
        for (i=0;i<n;i++) code[i]=0;
        return;
    }
    /* generate code table */
    if (!codes[sat-1]) {
        codes[sat-1]=gencode_CA(sat,codelen+sat-1);
    }
    /* resample code */
    rescode(codes[sat-1],codelen[sat-1],-coff,ci,n,code);
}
