package ru0xdc.rtkgps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ConditionVariable;
import android.util.Log;

public class BluetoothToRtklib {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = BluetoothToRtklib.class.getSimpleName();

    // Constants that indicate the current connection state
    public static final int STATE_IDLE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_WAITING = 3;
    public static final int STATE_RECONNECTING = 4;

    //  Standard UUID for the Serial Port Profile
    private static final java.util.UUID UUID_SPP = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    final LocalSocketThread mLocalSocketThread;

    final BluetoothServiceThread mBluetoothThread;

    final ConditionVariable mIsBluetoothReadyCondvar;

    public static final int RECONNECT_TIMEOUT_MS = 2000;

    public BluetoothToRtklib(@Nonnull String bluetoothAddress,
            @Nonnull String localSocketPath) {
        mLocalSocketThread = new LocalSocketThread(localSocketPath);

        mBluetoothThread = new BluetoothServiceThread(bluetoothAddress);
        mIsBluetoothReadyCondvar = new ConditionVariable(false);

        mLocalSocketThread.setBindpoint(localSocketPath+"_"+bluetoothAddress);
    }

    public void start() {
        mBluetoothThread.start();
        mLocalSocketThread.start();
    }

    public void stop() {
        mBluetoothThread.cancel();
        mLocalSocketThread.cancel();
        mIsBluetoothReadyCondvar.open();
    }

    private final class LocalSocketThread extends RtklibLocalSocketThread {

        public LocalSocketThread(String socketPath) {
            super(socketPath);
        }

        @Override
        protected boolean isDeviceReady() {
            return mIsBluetoothReadyCondvar.block(1);
        }

        @Override
        protected void waitDevice() {
            if (DBG) Log.v(TAG, "waitBluetooth()");
            mIsBluetoothReadyCondvar.block();
        }

        @Override
        protected boolean onDataReceived(byte[] buffer, int offset, int count) {
            if (count <= 0) return true;

            try {
                mBluetoothThread.write(buffer, 0, count);
            }catch(IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    private class BluetoothServiceThread extends Thread {

        private final BluetoothAdapter mBtAdapter;
        private final BluetoothDevice mBtDevice;
        private BluetoothSocket mSocket;
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        private int mConnectionState;
        private volatile boolean cancelRequested;

        public BluetoothServiceThread(String bluetoothAddress) {
            mInputStream = RtklibLocalSocketThread.DummyInputStream.instance;
            mOutputStream = RtklibLocalSocketThread.DummyOutputStream.instance;
            mConnectionState = STATE_IDLE;
            cancelRequested = false;
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            mBtDevice = mBtAdapter.getRemoteDevice(bluetoothAddress);
        }

        private synchronized void setState(int state) {
            int oldState = mConnectionState;
            mConnectionState = state;
            if (DBG) Log.d(TAG, "BT setState() " + oldState + " -> " + state);

            if (mConnectionState == STATE_CONNECTED)
                mIsBluetoothReadyCondvar.open();
            else {
                mIsBluetoothReadyCondvar.close();
                mLocalSocketThread.disconnect();
            }
        }

        public void cancel() {
            BluetoothSocket s;
            synchronized(this) {
                cancelRequested = true;
                s = mSocket;

                if (s != null) {
                    try {
                        if (DBG) Log.v(TAG, "BT close");
                        s.close();
                    } catch (IOException e) {
                        Log.e(TAG, "close() of connect socket failed", e);
                    }
                }

                notifyAll();
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
            //if (DBG) Log.v(TAG, "BT socket write " + count + " bytes");
            os.write(buffer, offset, count);
        }


        private void connect() throws IOException {
            BluetoothSocket s;

            if (!mBtAdapter.isEnabled())
                throw(new IOException("Bluetooth disabled"));

            s = mBtDevice.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
            try {
                s.connect();
                synchronized(this) {
                    mSocket = s;
                    mInputStream = s.getInputStream();
                    mOutputStream = s.getOutputStream();
                }
            }catch (IOException e) {
                Log.e(TAG, "connect() failed: " +  e.getLocalizedMessage());
                try {
                    s.close();
                    mInputStream = RtklibLocalSocketThread.DummyInputStream.instance;
                    mOutputStream = RtklibLocalSocketThread.DummyOutputStream.instance;
                }catch (IOException e2) {
                    Log.e(TAG, "mSocket.close() failed: " + e2.getLocalizedMessage());
                }
                throw(e);
            }
        }

        private boolean connectLoop() {

            while(!cancelRequested) {
                try {
                    connect();
                    return true;
                }catch (IOException e) {
                    synchronized(this) {
                        if (cancelRequested) {
                            return false;
                        }
                        setState(STATE_RECONNECTING);
                        try {
                            wait(RECONNECT_TIMEOUT_MS);
                        } catch(InterruptedException ie) {
                            return false;
                        }
                    }
                }
            }
            return false;
        }

        private boolean transferDataLoop() {
            int rcvd;
            final byte buf[] = new byte[4096];

            try {
                while(!cancelRequested) {
                    rcvd =  mInputStream.read(buf, 0, buf.length);
                    if (rcvd >= 0) {
                        try {
                            mLocalSocketThread.write(buf, 0, rcvd);
                        }catch (IOException e) {
                            // TODO
                            e.printStackTrace();
                        }
                    }
                    if (rcvd < 0)
                        throw new IOException("EOF");
                }
                return false;
            }catch (IOException e) {
                synchronized(this) {
                    try {
                        mSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    mInputStream = RtklibLocalSocketThread.DummyInputStream.instance;
                    mOutputStream = RtklibLocalSocketThread.DummyOutputStream.instance;
                    return !cancelRequested;
                }
            }
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN BluetoothToLocalSocket-BT");
            setName("BluetoothToLocalSocket-BT");

            setState(STATE_CONNECTING);

            while (!cancelRequested) {

                if (!connectLoop())
                    return;

                setState(STATE_CONNECTED);

                if (!transferDataLoop())
                    return;

                setState(STATE_RECONNECTING);
            }
        }
    }


}
