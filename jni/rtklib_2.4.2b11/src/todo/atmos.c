/*------------------------------------------------------------------------------
* atmos.c : atmospheric function
*
*          Copyright (C) 2010 by T.TAKASU, All rights reserved.
*
* reference :
*     [1] A.E.Niell, Global mapping functions for the atmosphere delay at radio
*         wavelengths, Jounal of geophysical research, 1996 *
*     [2] J. Boehm, R. Heinkelmann, and H. Schuh, Global Pressure and
*         Temperature (GPT): A spherical harmonics expansion of annual pressure
*         and  temperature variations for geodetic applications, Journal of
*         Geodesy, 2006
*     [3] J. Boehm, A.E. Niell, P. Tregoning, H. Schuh, Global Mapping Functions
*         (GMF): A new empirical mapping function based on numerical weather
*         model data, Geoph. Res. Letters, 2006
*     [4] IS-GPS-200D, Navstar GPS Space Segment/Navigation User Interfaces,
*         7 March, 2006
*     
*     (*  original paper has bugs in eq.(4) and (5). the corrected paper is 
*         obtained from: ftp://web.haystack.edu/pub/aen/nmf/NMF_JGR.pdf)
*
* version : $Revision:$ $Date:$
* history : 2010/09/11 1.0  new
*-----------------------------------------------------------------------------*/
#include "rtklib.h"

static const char rcsid[]="$Id:$";

/* constants/macros ----------------------------------------------------------*/

#define MIN(x,y)    ((x)<(y)?(x):(y))

#define NMAX        9                  /* max degree of spherical harmonics */
#define NP          (NMAX+1)*(NMAX+2)/2 /* number of spherical harmonics */

/* legendre functions --------------------------------------------------------*/
static void legendre(double t, double P[][NMAX+1])
{
    double sum,fac[2*NMAX+2]={1.0};
    int i,j,ir,k;
    
    for (i=1;i<=2*NMAX+1;i++) {
        fac[i]=fac[i-1]*i;
    }
    for (i=0;i<=NMAX;i++) for (j=0;j<=MIN(i,NMAX);j++) {
        ir=(i-j)/2;
        sum=0.0;
        for (k=0;k<=ir;k++) {
            sum+=(-1)**k*fac[2*i-2*k]/fac[k]/fac[i-k]/fac[i-j-2*k]*t**[i-j-2*k];
        }
        /* Legendre functions moved by 1 */
        P(i + 1,j + 1) = 1.d0/2**i*dsqrt((1 - t**2)**(j))*sum
    }
}
/* spherical harmonics -------------------------------------------------------*/
static void sphharmonic(const double pos, double *aP, double *bP)
{
    double P[NMAX+1][NMAX+1],cosm[NMAX+1],sinm[NMAX+1];
    int i=0,n,m;
    
    legendre(sin(pos[0]),P);
    
    for (m=0;m<=NMAX;m++) {
        cosm[m]=cos(m*pos[1]);
        sinm[m]=sin(m*pos[1]);
    }
    for (n=0;n<=NMAX;n++) for (m=0;m<=n;m++) {
        aP[i  ]=P[n][m]*cosm[m];
        bP[i++]=P[n][m]*sinm[m];
    }
}
/* mapping function ----------------------------------------------------------*/
static double mapf(double el, double a, double b, double c)
{
    double sinel=sin(el);
    return (1.0+a/(1.0+b/(1.0+c)))/(sinel+(a/(sinel+b/(sinel+c))));
}
/* pressure and temperature by standard atmosphere ---------------------------*/
static void atmos_std(gtime_t time, const double *pos, double *pres,
                      double *temp)
{
    double hgt=pos[2]<0.0?0.0:pos[2];
    *pres=1013.25*pow(1.0-2.2557E-5*hgt,5.2568);
    *temp=15.0-6.5E-3*hgt+273.16;
}
/* pressure and temperature by GPT -------------------------------------------*/
static void atmos_gpt(gtime_t time, const double *pos, double *pres,
                      double *temp)
{
    const double a_geoid[]={
        -5.6195E-001,-6.0794E-002,-2.0125E-001,-6.4180E-002,-3.6997E-002,
        +1.0098E+001,+1.6436E+001,+1.4065E+001,+1.9881E+000,+6.4414E-001,
        -4.7482E+000,-3.2290E+000,+5.0652E-001,+3.8279E-001,-2.6646E-002,
        +1.7224E+000,-2.7970E-001,+6.8177E-001,-9.6658E-002,-1.5113E-002,
        +2.9206E-003,-3.4621E+000,-3.8198E-001,+3.2306E-002,+6.9915E-003,
        -2.3068E-003,-1.3548E-003,+4.7324E-006,+2.3527E+000,+1.2985E+000,
        +2.1232E-001,+2.2571E-002,-3.7855E-003,+2.9449E-005,-1.6265E-004,
        +1.1711E-007,+1.6732E+000,+1.9858E-001,+2.3975E-002,-9.0013E-004,
        -2.2475E-003,-3.3095E-005,-1.2040E-005,+2.2010E-006,-1.0083E-006,
        +8.6297E-001,+5.8231E-001,+2.0545E-002,-7.8110E-003,-1.4085E-004,
        -8.8459E-006,+5.7256E-006,-1.5068E-006,+4.0095E-007,-2.4185E-008
    };
    const double b_geoid[]={
        +0.0000E+000,+0.0000E+000,-6.5993E-002,+0.0000E+000,+6.5364E-002,
        -5.8320E+000,+0.0000E+000,+1.6961E+000,-1.3557E+000,+1.2694E+000,
        +0.0000E+000,-2.9310E+000,+9.4805E-001,-7.6243E-002,+4.1076E-002,
        +0.0000E+000,-5.1808E-001,-3.4583E-001,-4.3632E-002,+2.2101E-003,
        -1.0663E-002,+0.0000E+000,+1.0927E-001,-2.9463E-001,+1.4371E-003,
        -1.1452E-002,-2.8156E-003,-3.5330E-004,+0.0000E+000,+4.4049E-001,
        +5.5653E-002,-2.0396E-002,-1.7312E-003,+3.5805E-005,+7.2682E-005,
        +2.2535E-006,+0.0000E+000,+1.9502E-002,+2.7919E-002,-8.1812E-003,
        +4.4540E-004,+8.8663E-005,+5.5596E-005,+2.4826E-006,+1.0279E-006,
        +0.0000E+000,+6.0529E-002,-3.5824E-002,-5.1367E-003,+3.0119E-005,
        -2.9911E-005,+1.9844E-005,-1.2349E-006,-7.6756E-009,+5.0100E-008 
    };
    const double ap_mean[]={
        +1.0108E+003,+8.4886E+000,+1.4799E+000,-1.3897E+001,+3.7516E-003,
        -1.4936E-001,+1.2232E+001,-7.6615E-001,-6.7699E-002,+8.1002E-003,
        -1.5874E+001,+3.6614E-001,-6.7807E-002,-3.6309E-003,+5.9966E-004,
        +4.8163E+000,-3.7363E-001,-7.2071E-002,+1.9998E-003,-6.2385E-004,
        -3.7916E-004,+4.7609E+000,-3.9534E-001,+8.6667E-003,+1.1569E-002,
        +1.1441E-003,-1.4193E-004,-8.5723E-005,+6.5008E-001,-5.0889E-001,
        -1.5754E-002,-2.8305E-003,+5.7458E-004,+3.2577E-005,-9.6052E-006,
        -2.7974E-006,+1.3530E+000,-2.7271E-001,-3.0276E-004,+3.6286E-003,
        -2.0398E-004,+1.5846E-005,-7.7787E-006,+1.1210E-006,+9.9020E-008,
        +5.5046E-001,-2.7312E-001,+3.2532E-003,-2.4277E-003,+1.1596E-004,
        +2.6421E-007,-1.3263E-006,+2.7322E-007,+1.4058E-007,+4.9414E-009 
    };
    const double bp_mean[]={
        +0.0000E+000,+0.0000E+000,-1.2878E+000,+0.0000E+000,+7.0444E-001,
        +3.3222E-001,+0.0000E+000,-2.9636E-001,+7.2248E-003,+7.9655E-003,
        +0.0000E+000,+1.0854E+000,+1.1145E-002,-3.6513E-002,+3.1527E-003,
        +0.0000E+000,-4.8434E-001,+5.2023E-002,-1.3091E-002,+1.8515E-003,
        +1.5422E-004,+0.0000E+000,+6.8298E-001,+2.5261E-003,-9.9703E-004,
        -1.0829E-003,+1.7688E-004,-3.1418E-005,+0.0000E+000,-3.7018E-001,
        +4.3234E-002,+7.2559E-003,+3.1516E-004,+2.0024E-005,-8.0581E-006,
        -2.3653E-006,+0.0000E+000,+1.0298E-001,-1.5086E-002,+5.6186E-003,
        +3.2613E-005,+4.0567E-005,-1.3925E-006,-3.6219E-007,-2.0176E-008,
        +0.0000E+000,-1.8364E-001,+1.8508E-002,+7.5016E-004,-9.6139E-005,
        -3.1995E-006,+1.3868E-007,-1.9486E-007,+3.0165E-010,-6.4376E-010 
    };
    const double ap_amp[]={
        -1.0444E-001,+1.6618E-001,-6.3974E-002,+1.0922E+000,+5.7472E-001,
        -3.0277E-001,-3.5087E+000,+7.1264E-003,-1.4030E-001,+3.7050E-002,
        +4.0208E-001,-3.0431E-001,-1.3292E-001,+4.6746E-003,-1.5902E-004,
        +2.8624E+000,-3.9315E-001,-6.4371E-002,+1.6444E-002,-2.3403E-003,
        +4.2127E-005,+1.9945E+000,-6.0907E-001,-3.5386E-002,-1.0910E-003,
        -1.2799E-004,+4.0970E-005,+2.2131E-005,-5.3292E-001,-2.9765E-001,
        -3.2877E-002,+1.7691E-003,+5.9692E-005,+3.1725E-005,+2.0741E-005,
        -3.7622E-007,+2.6372E+000,-3.1165E-001,+1.6439E-002,+2.1633E-004,
        +1.7485E-004,+2.1587E-005,+6.1064E-006,-1.3755E-008,-7.8748E-008,
        -5.9152E-001,-1.7676E-001,+8.1807E-003,+1.0445E-003,+2.3432E-004,
        +9.3421E-006,+2.8104E-006,-1.5788E-007,-3.0648E-008,+2.6421E-010 
    };
    const double bp_amp[]={
        +0.0000E+000,+0.0000E+000,+9.3340E-001,+0.0000E+000,+8.2346E-001,
        +2.2082E-001,+0.0000E+000,+9.6177E-001,-1.5650E-002,+1.2708E-003,
        +0.0000E+000,-3.9913E-001,+2.8020E-002,+2.8334E-002,+8.5980E-004,
        +0.0000E+000,+3.0545E-001,-2.1691E-002,+6.4067E-004,-3.6528E-005,
        -1.1166E-004,+0.0000E+000,-7.6974E-002,-1.8986E-002,+5.6896E-003,
        -2.4159E-004,-2.3033E-004,-9.6783E-006,+0.0000E+000,-1.0218E-001,
        -1.3916E-002,-4.1025E-003,-5.1340E-005,-7.0114E-005,-3.3152E-007,
        +1.6901E-006,+0.0000E+000,-1.2422E-002,+2.5072E-003,+1.1205E-003,
        -1.3034E-004,-2.3971E-005,-2.6622E-006,+5.7852E-007,+4.5847E-008,
        +0.0000E+000,+4.4777E-002,-3.0421E-003,+2.6062E-005,-7.2421E-005,
        +1.9119E-006,+3.9236E-007,+2.2390E-007,+2.9765E-009,-4.6452E-009 
    };
    const double at_mean[]={
        +1.6257E+001,+2.1224E+000,+9.2569E-001,-2.5974E+001,+1.4510E+000,
        +9.2468E-002,-5.3192E-001,+2.1094E-001,-6.9210E-002,-3.4060E-002,
        -4.6569E+000,+2.6385E-001,-3.6093E-002,+1.0198E-002,-1.8783E-003,
        +7.4983E-001,+1.1741E-001,+3.9940E-002,+5.1348E-003,+5.9111E-003,
        +8.6133E-006,+6.3057E-001,+1.5203E-001,+3.9702E-002,+4.6334E-003,
        +2.4406E-004,+1.5189E-004,+1.9581E-007,+5.4414E-001,+3.5722E-001,
        +5.2763E-002,+4.1147E-003,-2.7239E-004,-5.9957E-005,+1.6394E-006,
        -7.3045E-007,-2.9394E+000,+5.5579E-002,+1.8852E-002,+3.4272E-003,
        -2.3193E-005,-2.9349E-005,+3.6397E-007,+2.0490E-006,-6.4719E-008,
        -5.2225E-001,+2.0799E-001,+1.3477E-003,+3.1613E-004,-2.2285E-004,
        -1.8137E-005,-1.5177E-007,+6.1343E-007,+7.8566E-008,+1.0749E-009 
    };
    const double bt_mean[]={
        +0.0000E+000,+0.0000E+000,+1.0210E+000,+0.0000E+000,+6.0194E-001,
        +1.2292E-001,+0.0000E+000,-4.2184E-001,+1.8230E-001,+4.2329E-002,
        +0.0000E+000,+9.3312E-002,+9.5346E-002,-1.9724E-003,+5.8776E-003,
        +0.0000E+000,-2.0940E-001,+3.4199E-002,-5.7672E-003,-2.1590E-003,
        +5.6815E-004,+0.0000E+000,+2.2858E-001,+1.2283E-002,-9.3679E-003,
        -1.4233E-003,-1.5962E-004,+4.0160E-005,+0.0000E+000,+3.6353E-002,
        -9.4263E-004,-3.6762E-003,+5.8608E-005,-2.6391E-005,+3.2095E-006,
        -1.1605E-006,+0.0000E+000,+1.6306E-001,+1.3293E-002,-1.1395E-003,
        +5.1097E-005,+3.3977E-005,+7.6449E-006,-1.7602E-007,-7.6558E-008,
        +0.0000E+000,-4.5415E-002,-1.8027E-002,+3.6561E-004,-1.1274E-004,
        +1.3047E-005,+2.0001E-006,-1.5152E-007,-2.7807E-008,+7.7491E-009 
    };
    const double at_amp[]={
        -1.8654E+000,-9.0041E+000,-1.2974E-001,-3.6053E+000,+2.0284E-002,
        +2.1872E-001,-1.3015E+000,+4.0355E-001,+2.2216E-001,-4.0605E-003,
        +1.9623E+000,+4.2887E-001,+2.1437E-001,-1.0061E-002,-1.1368E-003,
        -6.9235E-002,+5.6758E-001,+1.1917E-001,-7.0765E-003,+3.0017E-004,
        +3.0601E-004,+1.6559E+000,+2.0722E-001,+6.0013E-002,+1.7023E-004,
        -9.2424E-004,+1.1269E-005,-6.9911E-006,-2.0886E+000,-6.7879E-002,
        -8.5922E-004,-1.6087E-003,-4.5549E-005,+3.3178E-005,-6.1715E-006,
        -1.4446E-006,-3.7210E-001,+1.5775E-001,-1.7827E-003,-4.4396E-004,
        +2.2844E-004,-1.1215E-005,-2.1120E-006,-9.6421E-007,-1.4170E-008,
        +7.8720E-001,-4.4238E-002,-1.5120E-003,-9.4119E-004,+4.0645E-006,
        -4.9253E-006,-1.8656E-006,-4.0736E-007,-4.9594E-008,+1.6134E-009 
    };
    const double bt_amp[]={
        +0.0000E+000,+0.0000E+000,-8.9895E-001,+0.0000E+000,-1.0790E+000,
        -1.2699E-001,+0.0000E+000,-5.9033E-001,+3.4865E-002,-3.2614E-002,
        +0.0000E+000,-2.4310E-002,+1.5607E-002,-2.9833E-002,-5.9048E-003,
        +0.0000E+000,+2.8383E-001,+4.0509E-002,-1.8834E-002,-1.2654E-003,
        -1.3794E-004,+0.0000E+000,+1.3306E-001,+3.4960E-002,-3.6799E-003,
        -3.5626E-004,+1.4814E-004,+3.7932E-006,+0.0000E+000,+2.0801E-001,
        +6.5640E-003,-3.4893E-003,-2.7395E-004,+7.4296E-005,-7.9927E-006,
        -1.0277E-006,+0.0000E+000,+3.6515E-002,-7.4319E-003,-6.2873E-004,
        -8.2461E-005,+3.1095E-005,-5.3860E-007,-1.2055E-007,-1.1517E-007,
        +0.0000E+000,+3.1404E-002,+1.5580E-002,-1.1428E-003,+3.3529E-005,
        +1.0387E-005,-1.9378E-006,-2.7327E-007,+7.5833E-009,-9.2323E-009 
    };
    double t,undu,hort,apm,apa,pres0,atm,ata,temp0,aP[NP],bP[NP];
    int i;
    
    t=time2doy(time)-28.0;
    
    /* spherical harmonics */
    sphharmonic(pos,aP,bP);
    
    /* geoidal height */
    undu=0.0;
    for (i=0;i<NP;i++) {
        undu+=a_geoid[i]*aP[i]+b_geoid[i]*bP[i];
    }
    /* orthometric height */
    hort=hgt-undu;
    
    /* surface pressure on the geoid */
    apm=apa=0.0;
    for (i=0;i<NP;i++) {
        apm+=ap_mean[i]*aP[i]+bp_mean[i]*bP[i];
        apa+=ap_amp [i]*aP[i]+bp_amp [i]*bP[i];
    }
    pres0=apm+apa*cos(t/365.25*2.0*PI);
    
    /* height correction for pressure */
    *pres=pres0*pow(1.0-0.0000226*hort,5.225);
    
    /* surface temperature on the geoid */
    atm=ata=0.0;
    for (i=0;i<NP;i++) {
        atm+=at_mean[i]*aP[i]+bt_mean[i]*bP[i];
        ata+=at_amp [i]*aP[i]+bt_amp [i]*bP[i];
    }
    temp0=atm+ata*cos(t/365.25*2.0*PI);
    
    /* height correction for temperature */
    *temp=temp0-0.0065*hort;
}
/* interpolation of coeffincients --------------------------------------------*/
static double interpc(const double coef[], double lat)
{
    int i=(int)(lat/15.0);
    if (i<1) return coef[0]; else if (i>4) return coef[4];
    return coef[i-1]*(1.0-lat/15.0+i)+coef[i]*(lat/15.0-i);
}
/* troposphere mapping function NMF (ref [1]) --------------------------------*/
static double tropmapf_nmf(gtime_t time, const double *pos, const double *azel,
                           double *mapfw)
{
    /* hydro-ave-a,b,c, hydro-amp-a,b,c, wet-a,b,c at latitude 15,30,45,60,75 */
    const double coef[][5]={
        { 1.2769934E-3, 1.2683230E-3, 1.2465397E-3, 1.2196049E-3, 1.2045996E-3},
        { 2.9153695E-3, 2.9152299E-3, 2.9288445E-3, 2.9022565E-3, 2.9024912E-3},
        { 62.610505E-3, 62.837393E-3, 63.721774E-3, 63.824265E-3, 64.258455E-3},
        
        { 0.0000000E-0, 1.2709626E-5, 2.6523662E-5, 3.4000452E-5, 4.1202191E-5},
        { 0.0000000E-0, 2.1414979E-5, 3.0160779E-5, 7.2562722E-5, 11.723375E-5},
        { 0.0000000E-0, 9.0128400E-5, 4.3497037E-5, 84.795348E-5, 170.37206E-5},
        
        { 5.8021897E-4, 5.6794847E-4, 5.8118019E-4, 5.9727542E-4, 6.1641693E-4},
        { 1.4275268E-3, 1.5138625E-3, 1.4572752E-3, 1.5007428E-3, 1.7599082E-3},
        { 4.3472961E-2, 4.6729510E-2, 4.3908931E-2, 4.4626982E-2, 5.4736038E-2}
    };
    const double aht[]={ 2.53E-5, 5.49E-3, 1.14E-3}; /* height correction */
    double y,cosy,ah[3],aw[3],dm;
    double az=azel[0],el=azel[1],lat=pos[0]*R2D,lon=pos[1]*R2D,hgt=pos[2];
    int i;
    
    trace(3,"tropmapf_nmf: pos=%10.6f %11.6f %6.1f azel=%5.1f %4.1f\n",
          pos[0]*R2D,pos[1]*R2D,pos[2],azel[0]*R2D,azel[1]*R2D);
    
    if (el<=0.0) {
        if (mapfw) *mapfw=0.0;
        return 0.0;
    }
    /* year from doy 28, added half a year for southern latitudes */
    y=(time2doy(time)-28.0)/365.25+(lat<0.0?0.5:0.0);
    
    cosy=cos(2.0*PI*y);
    lat=fabs(lat);
    
    for (i=0;i<3;i++) {
        ah[i]=interpc(coef[i  ],lat)-interpc(coef[i+3],lat)*cosy;
        aw[i]=interpc(coef[i+6],lat);
    }
    /* ellipsoidal height is used instead of height above sea level */
    dm=(1.0/sin(el)-mapf(el,aht[0],aht[1],aht[2]))*hgt/1E3;
    
    if (mapfw) *mapfw=mapf(el,aw[0],aw[1],aw[2]);
    
    return mapf(el,ah[0],ah[1],ah[2])+dm;
}
/* troposphere mapping function GMF (ref [2]) --------------------------------*/
static double tropmapf_gmf(gtime_t time, const double *pos, const double *azel,
                           double *mapfw)
{
    const double ah_mean[]={
       +1.2517E+02, +8.503E-01, +6.936E-02, -6.760E+00, +1.771E-01,
        +1.130E-02, +5.963E-01, +1.808E-02, +2.801E-03, -1.414E-03,
        -1.212E+00, +9.300E-02, +3.683E-03, +1.095E-03, +4.671E-05,
        +3.959E-01, -3.867E-02, +5.413E-03, -5.289E-04, +3.229E-04,
        +2.067E-05, +3.000E-01, +2.031E-02, +5.900E-03, +4.573E-04,
        -7.619E-05, +2.327E-06, +3.845E-06, +1.182E-01, +1.158E-02,
        +5.445E-03, +6.219E-05, +4.204E-06, -2.093E-06, +1.540E-07,
        -4.280E-08, -4.751E-01, -3.490E-02, +1.758E-03, +4.019E-04,
        -2.799E-06, -1.287E-06, +5.468E-07, +7.580E-08, -6.300E-09,
        -1.160E-01, +8.301E-03, +8.771E-04, +9.955E-05, -1.718E-06,
        -2.012E-06, +1.170E-08, +1.790E-08, -1.300E-09, +1.000E-10
    };
    const double bh_mean[]={
        +0.000E+00, +0.000E+00, +3.249E-02, +0.000E+00, +3.324E-02,
        +1.850E-02, +0.000E+00, -1.115E-01, +2.519E-02, +4.923E-03,
        +0.000E+00, +2.737E-02, +1.595E-02, -7.332E-04, +1.933E-04,
        +0.000E+00, -4.796E-02, +6.381E-03, -1.599E-04, -3.685E-04,
        +1.815E-05, +0.000E+00, +7.033E-02, +2.426E-03, -1.111E-03,
        -1.357E-04, -7.828E-06, +2.547E-06, +0.000E+00, +5.779E-03,
        +3.133E-03, -5.312E-04, -2.028E-05, +2.323E-07, -9.100E-08,
        -1.650E-08, +0.000E+00, +3.688E-02, -8.638E-04, -8.514E-05,
        -2.828E-05, +5.403E-07, +4.390E-07, +1.350E-08, +1.800E-09,
        +0.000E+00, -2.736E-02, -2.977E-04, +8.113E-05, +2.329E-07,
        +8.451E-07, +4.490E-08, -8.100E-09, -1.500E-09, +2.000E-10
    };
    const double ah_amp[]={
        -2.738E-01, -2.837E+00, +1.298E-02, -3.588E-01, +2.413E-02,
        +3.427E-02, -7.624E-01, +7.272E-02, +2.160E-02, -3.385E-03,
        +4.424E-01, +3.722E-02, +2.195E-02, -1.503E-03, +2.426E-04,
        +3.013E-01, +5.762E-02, +1.019E-02, -4.476E-04, +6.790E-05,
        +3.227E-05, +3.123E-01, -3.535E-02, +4.840E-03, +3.025E-06,
        -4.363E-05, +2.854E-07, -1.286E-06, -6.725E-01, -3.730E-02,
        +8.964E-04, +1.399E-04, -3.990E-06, +7.431E-06, -2.796E-07,
        -1.601E-07, +4.068E-02, -1.352E-02, +7.282E-04, +9.594E-05,
        +2.070E-06, -9.620E-08, -2.742E-07, -6.370E-08, -6.300E-09,
        +8.625E-02, -5.971E-03, +4.705E-04, +2.335E-05, +4.226E-06,
        +2.475E-07, -8.850E-08, -3.600E-08, -2.900E-09, +0.000E+00
    };
    const double bh_amp[]={
        +0.000E+00, +0.000E+00, -1.136E-01, +0.000E+00, -1.868E-01,
        -1.399E-02, +0.000E+00, -1.043E-01, +1.175E-02, -2.240E-03,
        +0.000E+00, -3.222E-02, +1.333E-02, -2.647E-03, -2.316E-05,
        +0.000E+00, +5.339E-02, +1.107E-02, -3.116E-03, -1.079E-04,
        -1.299E-05, +0.000E+00, +4.861E-03, +8.891E-03, -6.448E-04,
        -1.279E-05, +6.358E-06, -1.417E-07, +0.000E+00, +3.041E-02,
        +1.150E-03, -8.743E-04, -2.781E-05, +6.367E-07, -1.140E-08,
        -4.200E-08, +0.000E+00, -2.982E-02, -3.000E-03, +1.394E-05,
        -3.290E-05, -1.705E-07, +7.440E-08, +2.720E-08, -6.600E-09,
        +0.000E+00, +1.236E-02, -9.981E-04, -3.792E-05, -1.355E-05,
        +1.162E-06, -1.789E-07, +1.470E-08, -2.400E-09, -4.000E-10
    };
    const double aw_mean[]={
        +5.640E+01, +1.555E+00, -1.011E+00, -3.975E+00, +3.171E-02,
        +1.065E-01, +6.175E-01, +1.376E-01, +4.229E-02, +3.028E-03,
        +1.688E+00, -1.692E-01, +5.478E-02, +2.473E-02, +6.059E-04,
        +2.278E+00, +6.614E-03, -3.505E-04, -6.697E-03, +8.402E-04,
        +7.033E-04, -3.236E+00, +2.184E-01, -4.611E-02, -1.613E-02,
        -1.604E-03, +5.420E-05, +7.922E-05, -2.711E-01, -4.406E-01,
        -3.376E-02, -2.801E-03, -4.090E-04, -2.056E-05, +6.894E-06,
        +2.317E-06, +1.941E+00, -2.562E-01, +1.598E-02, +5.449E-03,
        +3.544E-04, +1.148E-05, +7.503E-06, -5.667E-07, -3.660E-08,
        +8.683E-01, -5.931E-02, -1.864E-03, -1.277E-04, +2.029E-04,
        +1.269E-05, +1.629E-06, +9.660E-08, -1.015E-07, -5.000E-10
    };
    const double bw_mean[]={
        +0.000E+00, +0.000E+00, +2.592E-01, +0.000E+00, +2.974E-02,
        -5.471E-01, +0.000E+00, -5.926E-01, -1.030E-01, -1.567E-02,
        +0.000E+00, +1.710E-01, +9.025E-02, +2.689E-02, +2.243E-03,
        +0.000E+00, +3.439E-01, +2.402E-02, +5.410E-03, +1.601E-03,
        +9.669E-05, +0.000E+00, +9.502E-02, -3.063E-02, -1.055E-03,
        -1.067E-04, -1.130E-04, +2.124E-05, +0.000E+00, -3.129E-01,
        +8.463E-03, +2.253E-04, +7.413E-05, -9.376E-05, -1.606E-06,
        +2.060E-06, +0.000E+00, +2.739E-01, +1.167E-03, -2.246E-05,
        -1.287E-04, -2.438E-05, -7.561E-07, +1.158E-06, +4.950E-08,
        +0.000E+00, -1.344E-01, +5.342E-03, +3.775E-04, -6.756E-05,
        -1.686E-06, -1.184E-06, +2.768E-07, +2.730E-08, +5.700E-09
    };
    const double aw_amp[]={
        +1.023E-01, -2.695E+00, +3.417E-01, -1.405E-01, +3.175E-01,
        +2.116E-01, +3.536E+00, -1.505E-01, -1.660E-02, +2.967E-02,
        +3.819E-01, -1.695E-01, -7.444E-02, +7.409E-03, -6.262E-03,
        -1.836E+00, -1.759E-02, -6.256E-02, -2.371E-03, +7.947E-04,
        +1.501E-04, -8.603E-01, -1.360E-01, -3.629E-02, -3.706E-03,
        -2.976E-04, +1.857E-05, +3.021E-05, +2.248E+00, -1.178E-01,
        +1.255E-02, +1.134E-03, -2.161E-04, -5.817E-06, +8.836E-07,
        -1.769E-07, +7.313E-01, -1.188E-01, +1.145E-02, +1.011E-03,
        +1.083E-04, +2.570E-06, -2.140E-06, -5.710E-08, +2.000E-08,
        -1.632E+00, -6.948E-03, -3.893E-03, +8.592E-04, +7.577E-05,
        +4.539E-06, -3.852E-07, -2.213E-07, -1.370E-08, +5.800E-09
    };
    const double bw_amp[]={
        +0.000E+00, +0.000E+00, -8.865E-02, +0.000E+00, -4.309E-01,
        +6.340E-02, +0.000E+00, +1.162E-01, +6.176E-02, -4.234E-03,
        +0.000E+00, +2.530E-01, +4.017E-02, -6.204E-03, +4.977E-03,
        +0.000E+00, -1.737E-01, -5.638E-03, +1.488E-04, +4.857E-04,
        -1.809E-04, +0.000E+00, -1.514E-01, -1.685E-02, +5.333E-03,
        -7.611E-05, +2.394E-05, +8.195E-06, +0.000E+00, +9.326E-02,
        -1.275E-02, -3.071E-04, +5.374E-05, -3.391E-05, -7.436E-06,
        +6.747E-07, +0.000E+00, -8.637E-02, -3.807E-03, -6.833E-04,
        -3.861E-05, -2.268E-05, +1.454E-06, +3.860E-07, -1.068E-07,
        +0.000E+00, -2.658E-02, -1.947E-03, +7.131E-04, -3.506E-05,
        +1.885E-07, +5.792E-07, +3.990E-08, +2.000E-08, -5.700E-09
    };
    double doy,bh,c0h,phh,c11h,c10h,ch,ahm,aha,ah,bw,cw,awm,awa,aw,mapfh;
    double sine,beta,gamma,topcon,a_ht,b_ht,c_ht,hs_km,ht_corr_coef,ht_corr;
    double aP[NP],bP[NP];
    
    doy=time2doy(time);
    
    /* spherical harmonics */
    sphharmonic(pos,aP,bP);
    
    /* hydrostatic mapping function */
    bh=0.0029;
    c0h=0.062;
    
    if (lat<0.0) { /* southern hemisphere */
        phh=PI;
        c11h=0.007;
        c10h=0.002;
    }
    else { /* northern hemisphere */
        phh=0.0;
        c11h=0.005;
        c10h=0.001;
    }
    ch=c0h+((cos(doy/365.25*2.0*PI+phh)+1.0)*c11h/2.0+c10h)*(1.0-cos(pos[0]));
    
    ahm=aha=0.0;
    for (i=0;i<NP;i++) {
        ahm+=(ah_mean[i]*aP[i]+bh_mean[i]*bP[i])*1E-5;
        aha+=(ah_amp [i]*aP[i]+bh_amp [i]*bP[i])*1E-5;
    }
    ah=ahm+aha*cos(doy/365.25*2.0*PI);
    
    mapfh=mapf(azel[1],ah,bh,ch);
    
    /* height correction by NMF */
    a_ht=2.53E-5;
    b_ht=5.49E-3;
    c_ht=1.14E-3;
    hs_km=dhgt/1000.0;
    
    beta =b_ht/(sine+c_ht);
    gamma=a_ht/(sine+beta);
    topcon=(1.0+a_ht/(1.0+b_ht/(1.0+c_ht)));
    ht_corr_coef=1.0/sine-topcon/(sine+gamma);
    ht_corr=ht_corr_coef * hs_km
    
    maph+=ht_corr;
    
    /* wet mapping function */
    if (mapfw) {
        bw=0.00146;
        cw=0.04391;
        awm=awa=0.0;
        for (i=0;i<NP;i++) {
            awm+=(aw_mean[i]*aP[i]+bw_mean[i]*bP[i])*1E-5;
            awa+=(aw_amp [i]*aP[i]+bw_amp [i]*bP[i])*1E-5;
        }
        aw=awm+awa*cos(doy/365.25*2.0*PI);
        
        *mapfw=mapf(azel[1],aw,bw,cw);
    }
    return mapfh;
}
/* troposphere mapping function ------------------------------------------------
* compute tropospheric mapping function by NMF
* args   : gtime_t time     I   time
*          double *pos      I   receiver position {lat,lon,h} (rad,m)
*          double *azel     I   azimuth/elevation angle {az,el} (rad)
*          int    opt       I   option (0:NMF,1:GMF,2:VMF1 (reserved))
*          double *mapfw    IO  wet mapping function (NULL: not output)
* return : dry mapping function
*-----------------------------------------------------------------------------*/
extern double tropmapf(gtime_t time, const double *pos, const double *azel,
                       int opt, double *mapfw)
{
    trace(3,"tropmapf: pos=%10.6f %11.6f %6.1f azel=%5.1f %4.1f opt=%d\n",
          pos[0]*R2D,pos[1]*R2D,pos[2],azel[0]*R2D,azel[1]*R2D,opt);
    
    switch (opt) {
        case TRPMAPF_NMF: return tropmapf_nmf(time,pos,azel,mapfw);
        case TRPMAPF_GMF: return tropmapf_gmf(time,pos,azel,mapfw);
    }
    return 0.0;
}
/* troposphere model -----------------------------------------------------------
* compute tropospheric delay by standard atmosphere and saastamoinen model
* args   : double *pos      I   receiver position {lat,lon,h} (rad,m)
*          double *azel     I   azimuth/elevation angle {az,el} (rad)
*          double humi      I   relative humidity
* return : tropospheric delay (m)
*-----------------------------------------------------------------------------*/
extern double tropmodel(const double *pos, const double *azel, double humi)
{
    double hgt,pres,temp,e,z,trph,trpw;
    
    if (pos[2]<-100.0||1E4<pos[2]||azel[1]<=0) return 0.0;
    
    /* standard atmosphere */
    hgt=pos[2]<0.0?0.0:pos[2];
    pres=1013.25*pow(1.0-2.2557E-5*hgt,5.2568);
    temp=15.0-6.5E-3*hgt+273.16;
    e=6.108*humi*exp((17.15*temp-4684.0)/(temp-38.45));
    
    /* saastamoninen model */
    z=PI/2.0-azel[1];
    trph=0.0022768*pres/(1.0-0.00266*cos(2.0*pos[0])-0.00028*hgt/1E3)/cos(z);
    trpw=0.002277 *(1255.0/temp+0.05)*e/cos(z);
    return trph+trpw;
}
/* ionosphere model ------------------------------------------------------------
* compute ionospheric delay by broadcast ionosphere model (klobuchar model)
* args   : gtime_t t        I   time (gpst)
*          double *ion      I   iono model parameters {a0,a1,a2,a3,b0,b1,b2,b3}
*          double *pos      I   receiver position {lat,lon,h} (rad,m)
*          double *azel     I   azimuth/elevation angle {az,el} (rad)
* return : ionospheric delay (L1) (m)
* notes  : ref [3]
*-----------------------------------------------------------------------------*/
extern double ionmodel(gtime_t t, const double *ion, const double *pos,
                       const double *azel)
{
    const double ion_default[]={ /* 2004/1/1 */
        0.1118E-07,-0.7451E-08,-0.5961E-07, 0.1192E-06,
        0.1167E+06,-0.2294E+06,-0.1311E+06, 0.1049E+07
    };
    double tt,f,psi,phi,lam,amp,per,x;
    int week;
    
    if (pos[2]<-1E3||azel[1]<=0) return 0.0;
    if (norm(ion,8)<=0.0) ion=ion_default;
    
    /* earth centered angle (semi-circle) */
    psi=0.0137/(azel[1]/PI+0.11)-0.022;
    
    /* subionospheric latitude/longitude (semi-circle) */
    phi=pos[0]/PI+psi*cos(azel[0]);
    if      (phi> 0.416) phi= 0.416;
    else if (phi<-0.416) phi=-0.416;
    lam=pos[1]/PI+psi*sin(azel[0])/cos(phi*PI);
    
    /* geomagnetic latitude (semi-circle) */
    phi+=0.064*cos((lam-1.617)*PI);
    
    /* local time (s) */
    tt=43200.0*lam+time2gpst(t,&week);
    tt-=floor(tt/86400.0)*86400.0; /* 0<=tt<86400 */
    
    /* slant factor */
    f=1.0+16.0*pow(0.53-azel[1]/PI,3.0);
    
    /* ionospheric delay */
    amp=ion[0]+phi*(ion[1]+phi*(ion[2]+phi*ion[3]));
    per=ion[4]+phi*(ion[5]+phi*(ion[6]+phi*ion[7]));
    amp=amp<    0.0?    0.0:amp;
    per=per<72000.0?72000.0:per;
    x=2.0*PI*(tt-50400.0)/per;
    
    return CLIGHT*f*(fabs(x)<1.57?5E-9+amp*(1.0+x*x*(-0.5+x*x/24.0)):5E-9);
}
/* ionosphere mapping function -------------------------------------------------
* compute ionospheric delay mapping function by single layer model
* args   : double *pos      I   receiver position {lat,lon,h} (rad,m)
*          double *azel     I   azimuth/elevation angle {az,el} (rad)
* return : ionospheric mapping function
*-----------------------------------------------------------------------------*/
extern double ionmapf(const double *pos, const double *azel)
{
    if (pos[2]>=HION) return 1.0;
    return 1.0/cos(asin((RE_WGS84+pos[2])/(RE_WGS84+HION)*sin(PI/2.0-azel[1])));
}
