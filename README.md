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

#### Android features:

* Bluetooth communication
* USB OTG communication with speed/parity/stop configuration
* SiRF IV protocol (experimental)
* Show altitude in status view if Height/Geodetic is choosen
* Send mock location to other applications if the checkbox is ticked in the solution option screen. (Not working yet with apps using Google Maps api)
* Upload Log/Solution to Dropbox if Sync to Dropbox is ticked in the FileSettings screen. (the sync occurs after the stop of the server)
* Generate a GPX track (Output view/GPX Track tab)

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

### Download alpha version (1.0apha7) from Google Play.

![qr](https://raw.githubusercontent.com/eltorio/RtkGps/master/qr_googleplay.png)

[Google play](https://play.google.com/store/apps/details?id=gpsplus.rtkgps)


[rtklib]: http://www.rtklib.com/
