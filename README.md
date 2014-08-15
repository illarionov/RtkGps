RtkGps+
=======

RTKLIB rtknavi port on android.


### Features

#### [rtklib][rtklib] features:

* Version pre 2.4.2p8 (all except uBlox improvements wich currently break the parsing)
* GPS, GLONASS, Galileo, QZSS, BeiDou and SBAS Navigation systems
* Single, DGPS/DGNSS, Kinematic, Static, Moving-Baseline, Fixed,
  PPP-Kinematic, PPP-Static and PPP-Fixed positioning modes.
* GNSS formats: 
  RINEX 2.10,2.11,2.12 OBS/NAV/GNAV/HNAV/LNAV/QNAV, RINEX 3.00,3.01,3.02
    OBS/NAV,RINEX 3.02 CLK,RTCM ver.2.3,RTCM ver.3.1 (with amendment 1-5),
    RTCM ver.3.2, BINEX, NTRIP 1.0, NMEA 0183, SP3-c, ANTEX 1.4, IONEX 1.0,
    NGS PCV and EMS 2.0.
* Proprietary protocotols: 
  NovAtel: OEM4/V/6,OEM3,OEMStar,Superstar II, Hemisphere: Eclipse,Crescent,
    u-blox: LEA-4T/5T/6T, SkyTraq: S1315F, JAVAD GRIL/GREIS, Furuno
    GW-10-II/III and NVS NV08C BINR.
* TCP/IP, NTRIP, local log file
* Support for multiple geoids (see explanation)

#### Android features:

* Bluetooth communication
* USB OTG communication with speed/parity/stop configuration (ACM, PL2303 chips and alpha for FTDI chips)
* SiRF IV protocol (experimental)
* Show altitude in status view if Height/Geodetic is choosen
* Send mock location to other applications if the checkbox is ticked in the solution option screen. (Not working yet with apps using Google Maps api)
* Upload Log/Solution to Dropbox if Sync to Dropbox is ticked in the FileSettings screen. (the sync occurs after the stop of the server)
* Ability to zip files before uploading to Dropbox for reducing the data volume.
* Generate a GPX track (Output view/GPX Track tab)
* Show UTM coordinates in solution view
* Support for french Geoportail maps (cadastral parcels, roads and satellite) - needs api license key see src/gpsplus/rtkgps/geoportail/License.java.sample 

#### Geoids
* you can select different geoid model in "Solution Option"/Geoid model but except for embedded model (EGM96 1°x1°)  
  you will need to place the corresponding model in the RtkGps storage (probably /storage/sdcard0/RtkGps)  
  the model format is RTKLIB dependent, check jni/RTKLIB/src/geoid.c  
  the model file MUST be one of:  
  ```
    EGM96_M150.geoid  
    EGM2008_M25.geoid  
    EGM2008_M10.geoid  
    GSI2000_M15.geoid
    RAF09_M15x20.geoid  
  ``` 
  
  for example if you want to use EGM2008 2.5'x2.5' model you need:
  ```
    1.  download from National Geospatial Intelligence Agency the model  
  			  on august 2014 file can be found http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm2008/Small_Endian/Und_min2.5x2.5_egm2008_isw=82_WGS84_TideFree_SE.gz   
    2.  extract the Und_min2.5x2.5_egm2008_isw=82_WGS84_TideFree_SE file and rename it EGM2008_M25.geoid (149Mo)   
    3.  place this model in RtkGps storage path (where all the trace and log go), on my tools it is /storage/sdcard0/RtkGps/
  ```

  for example if you want to use IGN RAF09 1.5'x2.5' model you need:
  ```
    1.  download from IGN wich is the french national geographical institute the model  
  			  on august 2014 file can be found http://geodesie.ign.fr/contenu/fichiers/documentation/grilles/metropole/RAF09.mnt   
    2.  rename it RAF09_M15x20.geoid    
    3.  place this model in RtkGps storage path (where all the trace and log go), on my tools it is /storage/sdcard0/RtkGps/
  ```
   
  if you do not have the correct model or if model is inconsistent it will not show an error, rather than it will use the EGM96 1°x1° embedded model or it use ellipsoidal height.
  				
#### Translations
Contributors are welcomed for translating RTKGPS+, the translation can be easily managed on [Crowdin](https://crowdin.net/project/gpsplusrtkgps/invite).   
You can freely create a translator account and with it you will be able request for a new translation.  
I already made this translations:
* English (source language)
* French


### Requirements

* android > 4.0
* Bluetooth or USB OTG GPS receiver supported by rtklib
* Allow mock locations in your device developer settings (optional but required for sending mock locations)
* A Dropbox account for uploading log/solution. (you will be asked for authorizing this app by Dropbox)

###### Download original version from Alexey Illarionov.
[original version](https://github.com/illarionov/RtkGps)

### Download alpha version (1.0apha14) from Google Play.

![qr](https://raw.githubusercontent.com/eltorio/RtkGps/master/qr_googleplay.png)

[Google play](https://play.google.com/store/apps/details?id=gpsplus.rtkgps)


[rtklib]: http://www.rtklib.com/

###### Binary distribution

* the latest apk wich is maybe very instable can be found in bin directory, but prefer the Google Play version