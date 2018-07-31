package gpsplus.rtkgps;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.util.PoGoPin;

import com.spectraprecision.ublox.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Iterator;

import javax.annotation.Nonnull;

import gpsplus.rtkgps.utils.BytesTool;
import gpsplus.rtkgps.utils.HexString;


public class MobileMapperToRtklib implements android.location.GpsStatus.Listener, LocationListener {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = MobileMapperToRtklib.class.getSimpleName();

    public static final int MOBILEMAPPER_RAW_PORT = 46434;
    final LocalSocketThread mLocalSocketThread;
    private static Context mParentContext = null;
    private MobileMapperReceiver mMobileMapperReceiver;
    LocationManager locationManager = null;
    private int nbSat = 0;
    private boolean isStarting = false;

    public MobileMapperToRtklib(Context parentContext, @Nonnull String localSocketPath) {
        MobileMapperToRtklib.mParentContext = parentContext;
        mLocalSocketThread = new LocalSocketThread(localSocketPath);
        mLocalSocketThread.setBindpoint(localSocketPath);
    }

    public void start()
    {
        isStarting = true;
        locationManager = (LocationManager) mParentContext.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);
        locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 0.5f, this);
        mMobileMapperReceiver = new MobileMapperReceiver();

    }

    public void stop()
    {
        mMobileMapperReceiver.cancel();
        mLocalSocketThread.cancel();
        locationManager.removeUpdates(this);
    }


    public static boolean isMobileMapper() {
        boolean bool = true;
        try {
            int i = PoGoPin.exist();
            if (i == 1) {
                bool = true;
            } else {
                bool = false;
            }
        } catch (UnsatisfiedLinkError localUnsatisfiedLinkError) {
            return false;
        } catch (NoSuchMethodError localNoSuchMethodError) {
            return false;
        }
        return bool;
    }

    @Override
    public void onGpsStatusChanged(int i) {
        GpsStatus gpsStatus = locationManager.getGpsStatus(null);
        if(DBG) {
            Log.d(TAG, "GPS Status changed");
        }
        if(gpsStatus != null) {

            Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
            Iterator<GpsSatellite> sat = satellites.iterator();
            while (sat.hasNext()) {
                GpsSatellite satellite = sat.next();
                nbSat = 0;
                if (satellite.usedInFix()) {
                    nbSat++;
                    Log.d(TAG, "PRN:" + satellite.getPrn() + ", SNR:" + satellite.getSnr() + ", AZI:" + satellite.getAzimuth() + ", ELE:" + satellite.getElevation());
                }
            }
            if ( (nbSat > 0) && isStarting) // run only if MobileMapper is starting
                {
                    Log.i(TAG,"Starting MobileMapper PoGoPin");
                    mLocalSocketThread.start();
                    mMobileMapperReceiver.configure();
                    mMobileMapperReceiver.start();
                    isStarting = false;
                }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private final class LocalSocketThread extends RtklibLocalSocketThread {

        public LocalSocketThread(String socketPath) {
            super(socketPath);
        }

        @Override
        protected boolean isDeviceReady() {
            return isMobileMapper();
        }

        @Override
        protected void waitDevice() {

        }

        @Override
        protected boolean onDataReceived(byte[] buffer, int offset, int count) {
            if (count <= 0) return true;
                   PoGoPin.writeDevice(BytesTool.get(buffer,offset), count);
            return true;
        }

        @Override
        protected void onLocalSocketConnected() {

        }
    }

    private class MobileMapperReceiver extends Thread
    {
        private boolean mbIsRunning = false;

        private void sendCommand(byte[] byteArray) {
            PoGoPin.writeDevice(byteArray, byteArray.length);
        }

        private void pollAidHui() {
            PoGoPin.openBeidou("/dev/ttyHSL1");
            sendCommand(Message.pollAidHui());
            PoGoPin.closeBeidou();
        }

        public void enableRawData() {
            sendCommand(Message.setMessageRate((byte) 2, (byte) 21, (byte) 1));
            sendCommand(Message.setMessageRate((byte) 2, (byte) 19, (byte) 1));
            sendCommand(Message.setMessageRate((byte) 1, (byte) 7, (byte) 1));
            sendCommand(Message.setMessageRate((byte) 1, (byte) 34, (byte) 1));
        }

        public void disableNMEA() {
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GGA, (byte) 0));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GSV, (byte) 0));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GST, (byte) 0));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GLL, (byte) 0));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GSA, (byte) 0));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.RMC, (byte) 0));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.VTG, (byte) 0));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.ZDA, (byte) 0));
        }

        public void configure()
        {
            if (isMobileMapper()) {
                PoGoPin.openBeidou("/dev/ttyHSL1");
                sendCommand(Message.setDynamicMode(Message.DynModel.PORTABLE));
                sendCommand(Message.enableSbasCorrection());
                enableRawData();
                sendCommand(Message.pollAidHui());
            }
        }

        @Override
        public void run() {
            mbIsRunning = true;
            byte[] buffer = new byte[0x20];
            int count;
            DatagramSocket datagramSocketReceiver;
            DatagramPacket packet;
            Log.v("Start Read","OK");

            try {
                datagramSocketReceiver = new DatagramSocket(MOBILEMAPPER_RAW_PORT, InetAddress.getByName("localhost"));
                   packet = new DatagramPacket(buffer, buffer.length);
                while(mbIsRunning) {
                    datagramSocketReceiver.receive(packet);
                    Log.v("Packet:", "" + packet.getLength());
                    Log.v("  content:", HexString.bytesToHex(buffer,packet.getLength()));
                    mLocalSocketThread.write(buffer,0,packet.getLength());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            PoGoPin.closeBeidou();
            Log.v(TAG,"Stop run MobileMapper PoGoPin in to RtkLib socket thread OK");

        }

        public void cancel()
        {
            mbIsRunning = false;
        }
    }
}