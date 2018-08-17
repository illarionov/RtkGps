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

import gpsplus.rtkgps.utils.ublox.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import gpsplus.rtkgps.utils.BytesTool;
import gpsplus.rtkgps.utils.HexString;
import gpsplus.rtkgps.settings.StreamMobileMapperFragment.Value;

import static gpsplus.rtkgps.settings.StreamMobileMapperFragment.MOBILEMAPPER_INTERNAL_SENSOR_POGOPIN_PORT;


@SuppressWarnings("ALL")
public class MobileMapperToRtklib implements android.location.GpsStatus.Listener, LocationListener {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = MobileMapperToRtklib.class.getSimpleName();

    public static final int MOBILEMAPPER_RAW_PORT = 46434;
    final LocalSocketThread mLocalSocketThread;
    private static Context mParentContext = null;
    private MobileMapperReceiver mMobileMapperReceiver;
    private Value mMobileMapperSettings;
    LocationManager locationManager = null;
    FileOutputStream autoCaptureFileOutputStream = null;
    File autoCaptureFile=null;
    private int nbSat = 0;
    private boolean isStarting = false;
    private String mSessionCode;

    public MobileMapperToRtklib(Context parentContext, @Nonnull Value mobileMapperSettings, String sessionCode) {
        MobileMapperToRtklib.mParentContext = parentContext;
        mSessionCode = sessionCode;
        this.mMobileMapperSettings = mobileMapperSettings;
        mLocalSocketThread = new LocalSocketThread(mMobileMapperSettings.getPath());
        mLocalSocketThread.setBindpoint(mMobileMapperSettings.getPath());
    }

    public void start()
    {
        isStarting = true;
        locationManager = (LocationManager) mParentContext.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);
        locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 0.5f, this);
        mMobileMapperReceiver = new MobileMapperReceiver(mSessionCode);

    }

    public void stop()
    {
        locationManager.removeUpdates(this);
        mMobileMapperReceiver.cancel();
        mLocalSocketThread.cancel();

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
            nbSat = 0;
            while (sat.hasNext()) {
                GpsSatellite satellite = sat.next();
                if (satellite.usedInFix()) {
                    nbSat++;
                    Log.d(TAG, "PRN:" + satellite.getPrn() + ", SNR:" + satellite.getSnr() + ", AZI:" + satellite.getAzimuth() + ", ELE:" + satellite.getElevation());
                }
                if ( (satellite.getPrn()>0) && (satellite.getSnr()>0) && isStarting) // run only if MobileMapper is starting
                    {
                        Log.i(TAG,"Starting MobileMapper PoGoPin");
                        mLocalSocketThread.start();
                        mMobileMapperReceiver.configure();
                        mMobileMapperReceiver.start();
                        isStarting = false;
                    }
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
        private String mSessionCode;

        public MobileMapperReceiver(String sessionCode) {
            mSessionCode = sessionCode;
        }

        public MobileMapperReceiver() {
            mSessionCode = String.valueOf(System.currentTimeMillis());
        }

        private int sendCommand(byte[] byteArray) {
            return PoGoPin.writeDevice(byteArray, byteArray.length);
        }

        private void pollAidHui() {
            PoGoPin.openBeidou(MOBILEMAPPER_INTERNAL_SENSOR_POGOPIN_PORT);
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

        public void enableNMEA() {
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GGA, (byte) 1));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GSV, (byte) 1));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GST, (byte) 1));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GLL, (byte) 1));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.GSA, (byte) 1));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.RMC, (byte) 1));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.VTG, (byte) 1));
            sendCommand(Message.setNmeaMessageRate(Message.NmeaMsg.ZDA, (byte) 1));
        }

        public void hotStart() {
            sendCommand(Message.hotStart());
        }

        public void coldStart() {
            sendCommand(Message.coldStart());
        }

        public void warmStart() {
            sendCommand(Message.warmStart());
        }

        public void configure()
        {
            if (isMobileMapper()) {
                PoGoPin.openBeidou(MOBILEMAPPER_INTERNAL_SENSOR_POGOPIN_PORT);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    //TODO better handling
                    e.printStackTrace();
                }
                sendCommand(Message.enableSbas());
                if (mMobileMapperSettings.isForceColdStart()) {
                    coldStart();
                }
                sendCommand(Message.setUbloxDynamicMode(Message.DynModel.getModel(mMobileMapperSettings.getDynamicModel()) ));
                if (mMobileMapperSettings.isEnableNativeSBAS()) {
                    sendCommand(Message.enableSbasCorrection());
                }
                enableRawData();
                if (mMobileMapperSettings.isDisableNMEA()) {
                    disableNMEA();
                }
                sendCommand(Message.pollAidHui());
                PoGoPin.closeBeidou();
            }
        }

        @Override
        public void run() {
            mbIsRunning = true;
            byte[] buffer = new byte[0x10];
            ByteBuffer longUbloxBuffer = ByteBuffer.allocate(8*1024);

            int count;
            DatagramSocket datagramSocketReceiver = null;
            DatagramPacket packet = null;
            Log.v("Start Read","OK");

    //        PoGoPin.openBeidou(MOBILEMAPPER_INTERNAL_SENSOR_POGOPIN_PORT);
            try {
                datagramSocketReceiver = new DatagramSocket(MOBILEMAPPER_RAW_PORT, InetAddress.getByName("localhost"));
                   packet = new DatagramPacket(buffer, buffer.length);
                if (mMobileMapperSettings.isAutocapture()) {
                    autoCaptureFile = MainActivity.getFileInStorageSessionDirectory(mSessionCode, mSessionCode +".ubw");
                    autoCaptureFileOutputStream = new FileOutputStream(autoCaptureFile);
                    // if file doesnt exists, then create it
                    if (!autoCaptureFile.exists()) {
                        autoCaptureFile.createNewFile();
                    }
                }
                while(mbIsRunning) {
                    datagramSocketReceiver.receive(packet);
                    for (int i=0; i< packet.getLength();i++)
                    {
                        byte curByte = packet.getData()[i];
                        if ( (curByte == Message.SYNC1) && (longUbloxBuffer.position() > 0) )
                        {
                            if (longUbloxBuffer.get(longUbloxBuffer.position()-1) ==  Message.SYNC0)
                            {
                                //U-Blox sequence is starting
                                //Flush to log buffer and clear
                                if ((longUbloxBuffer.position()>1) && (longUbloxBuffer.get(0)==Message.SYNC0) && (longUbloxBuffer.get(1)==Message.SYNC1) ){
                                    Log.v("  U-Blox RAW data", HexString.bytesToHex(longUbloxBuffer.array(), longUbloxBuffer.position()-1)); // last char is the previous SYNC0
                                    if (Message.IsChecksumOK(longUbloxBuffer.array(), longUbloxBuffer.position()-1)) {
                                        Log.v("  U-Blox RAW data","Checksum OK");
                                    }
                                }

                                longUbloxBuffer.clear();
                                longUbloxBuffer.put(Message.SYNC0);
                            }
                        }
                        if(longUbloxBuffer.position() == (longUbloxBuffer.capacity()-1))
                        {
                            longUbloxBuffer.clear();
                            longUbloxBuffer.put(Message.SYNC0);
                        }
                        longUbloxBuffer.put(curByte);
                    }
                    if (mMobileMapperSettings.isAutocapture()) {
                        autoCaptureFileOutputStream.write (buffer,0,packet.getLength());
                        autoCaptureFileOutputStream.flush();
                    }

                    mLocalSocketThread.write(buffer,0,packet.getLength());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mMobileMapperSettings.isDisableNMEA()) {
                enableNMEA();
            }
            if (mMobileMapperSettings.isForceColdStart()) {
                coldStart();
            }
  //          PoGoPin.closeBeidou();
            if (datagramSocketReceiver != null) {
                if (!datagramSocketReceiver.isClosed()){
                    datagramSocketReceiver.close();
                }
                datagramSocketReceiver.disconnect();
                datagramSocketReceiver.close();
            }
            try {
                autoCaptureFileOutputStream.close();
            } catch (IOException e) {
                //TODO autoCaptureFileOutputStream.close() Handling
                e.printStackTrace();
            }
            packet = null;
            mLocalSocketThread.cancel();
            Log.v(TAG,"Stop run MobileMapper PoGoPin in to RtkLib socket thread OK");

        }

        public void cancel()
        {
            mbIsRunning = false;
        }
    }
}