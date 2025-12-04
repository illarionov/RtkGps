************************
* Standard NtripCaster *
************************

Introduction
~~~~~~~~~~~~

The Standard NtripCaster is a software written in C Programming
Language for disseminating GNSS real-time data streams via Internet.
For details of Ntrip (Networked Transport of RTCM via Internet
Protocol) see its documentation available from
http://igs.ifag.de/index_ntrip.htm. You should understand the Ntrip
data dissemination technique when considering an installation of
the software.

The Standard NtripCaster software has been developed within the
framework of the EUREF-IP project,
see http://www.epncb.oma.be/euref_IP. It is derived from the ICECAST
Internet Radio as written for Linux platforms under GNU General
Public License (GPL). Please note that whenever you make software
available that contains or is based on your copy of the Standard
NtripCaster, you must also make your source code available - at
least on request.

The Standard NtripCaster software has been tested so far on various
Suse, Debian, Gentoo, and Redhat (up to Enterprise 5) Linux
distributions. Note that the software may not run today on some
other Linux distributions of recent date. Version 0.1.5 of the
Standard NtripCaster supports a maximum of 50 NtripServers and
100 NtripClients simultaneously,
see http://igs.ifag.de/pdf/NtripImplementation.pdf for technical
details.
 
Ntrip Version 1.0 is an RTCM standard for streaming GNSS data over
the Internet. Offering the Standard NtripCaster Version 0.1.5 is
part of BKG’s policy to help distributing this standard. RTCM may
decide to issue further Ntrip versions as the need arises. Thus,
it might be necessary to modify the Standard NtripCaster
Version 0.1.5 in the future. Ntrip is already part of some GNSS
equipment available today. You may like to ask your vendor about
the Ntrip capability of your GNSS hardware or software.

Although Standard NtripCaster Version 0.1.5 should satisfy most
needs, we continue to work on a high-performance version with
enhanced functionality. This Professional NtripCaster Version is
meant for professional/commercial service providers.

Following your installation, we would appreciate if you could
inform us about the IP address of your Standard NtripCaster. We
intend to keep track of the upcoming global NtripCaster network,
which allows linking them through appropriate entries in the
corresponding configuration files. 

Note that the BKG does not give any warranty regarding the
function of the Standard NtripCaster Version 0.1.5. Moreover,
the BKG disclaims any liability nor responsibility to any person
or entity with respect to any loss or damage caused, or alleged
to be caused, directly or indirectly by the use of Standard
NtripCaster Version 0.1.5.

Please note that due to limited resource we are not able to
give any support concerning installation and maintanance of the
software. 

Installation
~~~~~~~~~~~~
To install the NtripCaster do the following:
- unzip the software in a separate directory
- run "./configure" (if you do not want the server to be installed in
"/usr/local/ntripcaster" specify the desired path with "./configure --prefix=<path>")
- run "make"
- run "make install"

After that, the server files will be in "/usr/local/ntripcaster", binaries will
be in "/usr/local/ntripcaster/bin", configuration files in
"/usr/local/ntripcaster/conf", logs in "/usr/local/ntripcaster/logs" and
templates in "/usr/local/ntripcaster/templates" (or in your desired path
correspondingly).

Go to the configuration directory and rename "sourcetable.dat.dist" and
"ntripcaster.conf.dist" to "sourcetable.dat" and "ntripcaster.conf".
Edit both files according to your needs. For details about "sourcetable.dat"
see file "NtripSourcetable.doc". In the configuration file "ntripcaster.conf"
you have to specify the name of the machine the server is running on
(no IP adress!!) and you can adapt other settings, like the listening ports,
the server limits and the access control.

Now the server is ready to be run with "./ntripcaster" in the binary directory.
The NtripCaster has been tested on various RedHat and Suse Linux distributions.

Whatever the content of your "sourcetable.dat" finally might be, it is recommended
to include the following line in that configuration file:
CAS;rtcm-ntrip.org;2101;NtripInfoCaster;BKG;0;DEU;50.12;8.69;http://www.rtcm-ntrip.org/home


License
~~~~~~~
NtripCaster, a GNSS real-time data server
Copyright (C) 2004-2008 

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA


Contact and webpage
~~~~~~~~~~~~~~~~~~~~
The main webpage for Ntripcaster is "http://igs.bkg.bund.de/index_ntrip.htm".

27 February 2008, "euref-ip@bkg.bund.de".
