package gpsplus.rtkgps;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.ConditionVariable;
import android.util.Log;

import gpsplus.rtkgps.usb.SerialLineConfiguration;
import gpsplus.rtkgps.usb.UsbAcmController;
import gpsplus.rtkgps.usb.UsbFTDIController;
import gpsplus.rtkgps.usb.UsbPl2303Controller;
import gpsplus.rtkgps.usb.UsbSerialController;
import gpsplus.rtkgps.usb.UsbSerialController.UsbControllerException;
import gpsplus.rtkgps.utils.HexString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class UsbToRtklib {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = UsbToRtklib.class.getSimpleName();

    // Constants that indicate the current connection state
    public static final int STATE_IDLE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_WAITING = 3;
    public static final int STATE_RECONNECTING = 4;

    public static final String ACTION_USB_DEVICE_ATTACHED = "gpsplus.rtkgps.UsbToRtklib.ACTION_USB_DEVICE_ATTACHED";

    final LocalSocketThread mLocalSocketThread;
    final UsbReceiver mUsbReceiver;

    private Callbacks mCallbacks;

    public static final int RECONNECT_TIMEOUT_MS = 2000;

    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onConnected() {}
        @Override
        public void onStopped() {}
        @Override
        public void onConnectionLost() {}
    };


    public interface Callbacks {

        public void onConnected();

        public void onStopped();

        public void onConnectionLost();

    }


    public UsbToRtklib(Context serviceContext, @Nonnull String localSocketPath) {
        mLocalSocketThread = new LocalSocketThread(localSocketPath);
        mLocalSocketThread.setBindpoint(localSocketPath);

        mUsbReceiver = new UsbReceiver(serviceContext);
        mCallbacks = sDummyCallbacks;
    }

    public void start() {
        mLocalSocketThread.start();
        mUsbReceiver.start();
    }

    public void stop() {
        mUsbReceiver.stop();
        mLocalSocketThread.cancel();
    }

    public void setSerialLineConfiguration(SerialLineConfiguration conf) {
        mUsbReceiver.setSerialLineConfiguration(conf);
    }

    public SerialLineConfiguration getSeriallineConfiguration() {
        return mUsbReceiver.getSerialLineConfiguration();
    }

    public void setCallbacks(Callbacks callbacks) {
        if (callbacks == null) throw new IllegalStateException();
        if (mLocalSocketThread.isAlive()) throw new IllegalStateException();
        mCallbacks = callbacks;
    }


    private final class LocalSocketThread extends RtklibLocalSocketThread {

        public LocalSocketThread(String socketPath) {
            super(socketPath);
        }

        @Override
        protected boolean isDeviceReady() {
            return mUsbReceiver.isDeviceReady();
        }

        @Override
        protected void waitDevice() {
            if (DBG) Log.v(TAG, "waitUsb()");
            mUsbReceiver.waitDevice();
        }

        @Override
        protected boolean onDataReceived(byte[] buffer, int offset, int count) {
            if (count <= 0) return true;

            try {
                mUsbReceiver.write(buffer, 0, count);
            }catch(IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onLocalSocketConnected() {
            mCallbacks.onConnected();
        }
    }


    private class UsbReceiver {

        static final String ACTION_USB_PERMISSION = "gpsplus.rtkgps.usb.UsbReceiver.USB_PERMISSION";

        private final SerialLineConfiguration mSerialLineConfiguration;

        private Context mContext;

        private UsbManager mUsbManager;

        final ConditionVariable mIsUsbDeviceReadyCondvar;

        private UsbServiceThread mServiceThread;

        public UsbReceiver(Context pContext) {

            this.mContext = pContext;
            this.mUsbManager = (UsbManager) pContext.getSystemService(Context.USB_SERVICE);
            mIsUsbDeviceReadyCondvar = new ConditionVariable(false);
            mSerialLineConfiguration = new SerialLineConfiguration();

            if (mUsbManager == null) throw new IllegalStateException("USB not available");
        }

        public synchronized void start() {
            final IntentFilter f;
            f = new IntentFilter();
            f.addAction(ACTION_USB_DEVICE_ATTACHED);
            f.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            f.addAction(ACTION_USB_PERMISSION);


            mContext.registerReceiver(mUsbStateListener, f);

            mServiceThread = new UsbServiceThread();
            mServiceThread.start();

            final UsbDevice d = findSupportedDevice();
            if (d != null) {
                requestPermission(d);
            }
        }

        public synchronized void setSerialLineConfiguration(SerialLineConfiguration conf) {
            this.mSerialLineConfiguration.set(conf);
        }

        public synchronized SerialLineConfiguration getSerialLineConfiguration() {
            return new SerialLineConfiguration(mSerialLineConfiguration);
        }


        public boolean isDeviceReady() {
            return mIsUsbDeviceReadyCondvar.block(1);
        }

        public void waitDevice() {
            mIsUsbDeviceReadyCondvar.block();
        }

        public synchronized void write(byte[] buffer, int offset, int count) throws IOException {
            if (mServiceThread == null) throw new IOException("not connected");
            mServiceThread.write(buffer, offset, count);
        }

        public synchronized void stop() {
            mContext.unregisterReceiver(mUsbStateListener);
            mServiceThread.cancel();
            mServiceThread = null;
            mIsUsbDeviceReadyCondvar.open();
        }

        @CheckForNull
        private UsbDevice findSupportedDevice() {
            final HashMap<String, UsbDevice> deviceList;

            deviceList = mUsbManager.getDeviceList();
            if (DBG) Log.v(TAG, "DeviceList size: " + deviceList.size());

            for (UsbDevice d: deviceList.values()) {
                if (probeDevice(d) != null) {
                    return d;
                }
            }
            return null;
        }

        @CheckForNull
        private UsbSerialController probeDevice(UsbDevice d) {
            if (DBG) Log.d(TAG, "probeDevice() device=" + d.toString());
            try {
                final UsbPl2303Controller c = new UsbPl2303Controller(mUsbManager, d, this.mContext);
                return c;
            }catch(UsbControllerException ignore) { }

            try {
                final UsbAcmController c = new UsbAcmController(mUsbManager, d, this.mContext);
                return c;
            }catch (UsbControllerException ignore) {}

            try {
                final UsbFTDIController c = new UsbFTDIController(mUsbManager, d, this.mContext);
                return c;
            }catch (UsbControllerException ignore) {}

           return null;
        }

        private void requestPermission(UsbDevice d) {
            if (DBG) Log.d(TAG, "requestPermission() device=" + d.toString());
            final PendingIntent premissionIntent = PendingIntent.getBroadcast(mContext,
                    0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(d, premissionIntent);
        }

        void onUsbDeviceAttached(UsbDevice device) {
            if (DBG) Log.d(TAG, "onUsbDeviceAttached() device=" + device.toString());

            if (probeDevice(device) != null) {
                requestPermission(device);
            }
        }

        synchronized void onUsbDeviceDetached(UsbDevice device) {

            if (DBG) Log.d(TAG, "onUsbDeviceDetached() device=" + device.toString());

            UsbSerialController controller = mServiceThread.getController();

            if (controller == null) return;
            if (!device.equals(controller.getDevice())) return;

            mServiceThread.setController(null);
        }

        synchronized void onUsbPermissionGranted(UsbDevice device) {
            if (DBG) Log.d(TAG, "onUsbPermissionGranted() device=" + device.toString());
            UsbSerialController controller = mServiceThread.getController();

            if (controller != null) return;

            controller = probeDevice(device);
            if (controller == null) return;

            controller.setSerialLineConfiguration(mSerialLineConfiguration);

            mServiceThread.setController(controller);

        }

        private final BroadcastReceiver mUsbStateListener = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device;
                String action = intent.getAction();
                Log.v(TAG, "Received intent " + action);

                if (action.equals(ACTION_USB_DEVICE_ATTACHED)) {
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    onUsbDeviceAttached(device);
                }else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    onUsbDeviceDetached(device);
                }else if (action.equals(ACTION_USB_PERMISSION)) {
                    boolean granted;
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    if (granted) {
                        onUsbPermissionGranted(device);
                    }
                }else {
                    Log.e(TAG, "Unknown action " + action);
                }
            }
        };

        private class UsbServiceThread extends Thread {

            private InputStream mInputStream;
            private OutputStream mOutputStream;

            private int mConnectionState;
            private volatile boolean cancelRequested;

            private UsbSerialController mUsbController;

            private final ConditionVariable serialControllerSet;

            public UsbServiceThread() {
                mInputStream = RtklibLocalSocketThread.DummyInputStream.instance;
                mOutputStream = RtklibLocalSocketThread.DummyOutputStream.instance;
                mConnectionState = STATE_IDLE;
                cancelRequested = false;
                mUsbController = null;
                serialControllerSet = new ConditionVariable(false);
            }

            public synchronized void setController(@Nullable UsbSerialController controller) {
                if (mUsbController != null) {
                    serialControllerSet.close();
                    mUsbController.detach();
                }
                mUsbController = controller;
                if (controller != null) serialControllerSet.open();
            }

            @CheckForNull
            public synchronized UsbSerialController getController() {
                return mUsbController;
            }

            private synchronized void setState(int state) {
                int oldState = mConnectionState;
                mConnectionState = state;
                if (DBG) Log.d(TAG, "setState() " + oldState + " -> " + state);

                if (mConnectionState == STATE_CONNECTED)
                    mIsUsbDeviceReadyCondvar.open();
                else {
                    mIsUsbDeviceReadyCondvar.close();
                    mLocalSocketThread.disconnect();
                }
            }

            public synchronized void cancel() {
                cancelRequested = true;
                mCallbacks.onStopped();
                if (mUsbController != null) {
                    mUsbController.detach();
                    mUsbController=null;
                }
            }

            /**
             * Write to the connected OutStream.
             * @param buffer  The bytes to write
             */
            public void write(byte[] buffer, int offset, int count) throws IOException {
                OutputStream os;
                synchronized(this) {
                    if (mConnectionState != STATE_CONNECTED) {
                        Log.e(TAG, "write() error: not connected");
                        return;
                    }
                    os = mOutputStream;
                }
                os.write(buffer, offset, count);
            }

            private synchronized void throwIfCancelRequested() throws CancelRequestedException {
                if (cancelRequested) throw new CancelRequestedException();
            }

            private void connect() throws UsbControllerException, CancelRequestedException {

                serialControllerSet.block();

                synchronized(UsbReceiver.this) {
                    synchronized (this) {
                        throwIfCancelRequested();
                        if (DBG) Log.v(TAG, "attach(). conf: "+ mUsbController.getSerialLineConfiguration().toString());
                        mUsbController.attach();
                        mInputStream = mUsbController.getInputStream();
                        mOutputStream = mUsbController.getOutputStream();
                    }
                }
                return;
            }

            private void connectLoop() throws CancelRequestedException {

                if (DBG) Log.v(TAG, "connectLoop()");

                while(true) {
                    try {
                        connect();
                        return;
                    }catch (UsbControllerException e) {
                        synchronized(this) {
                            throwIfCancelRequested();
                            setState(STATE_RECONNECTING);
                            try {
                                wait(RECONNECT_TIMEOUT_MS);
                            } catch(InterruptedException ie) {
                                throwIfCancelRequested();
                            }
                        }
                    }
                }
            }


            private void transferDataLoop() throws CancelRequestedException {
                int rcvd;
                final byte buf[] = new byte[4096];

                try {
                    while(true) {
                        rcvd =  mInputStream.read(buf, 0, buf.length);
                        if (rcvd >= 0) {
                            try {
                                mLocalSocketThread.write(buf, 0, rcvd);
                                if ( (rcvd > 0) && DBG ){
                                    Log.i(TAG, "READ from inputStream :"+rcvd+" bytes");
                                    Log.i(TAG, HexString.bytesToHex(buf,rcvd));
                                    Log.i(TAG, HexString.bytesToAscii(buf,rcvd));
                                }
                            }catch (IOException e) {
                                // TODO
                                e.printStackTrace();
                            }
                        }
                        if (rcvd < 0)
                            throw new IOException("EOF");
                    }
                }catch (IOException e) {
                    synchronized(this) {
                        if (mUsbController!=null) mUsbController.detach();
                        mInputStream = RtklibLocalSocketThread.DummyInputStream.instance;
                        mOutputStream = RtklibLocalSocketThread.DummyOutputStream.instance;
                        throwIfCancelRequested();
                    }
                }
            }

            @Override
            public void run() {
                Log.i(TAG, "BEGIN UsbToLocalSocket-USB");
                setName("UsbToLocalSocket-USB");
                try {
                    setState(STATE_CONNECTING);
                    while (true) {
                        throwIfCancelRequested();
                        connectLoop();

                        setState(STATE_CONNECTED);
                        transferDataLoop();

                        setState(STATE_RECONNECTING);
                        mCallbacks.onConnectionLost();
                    }
                }catch(CancelRequestedException cre) {}
            }
        }

        private class CancelRequestedException extends Exception {
            private static final long serialVersionUID = 1L;
        }
    }

}
