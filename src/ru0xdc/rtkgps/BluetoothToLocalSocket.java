package ru0xdc.rtkgps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.util.Log;

public class BluetoothToLocalSocket {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = BluetoothToLocalSocket.class.getSimpleName();

    // Constants that indicate the current connection state
    public static final int STATE_IDLE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_RECONNECTING = 4;

    //  Standard UUID for the Serial Port Profile
    private static final java.util.UUID UUID_SPP = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    final LocalSocketServiceThread mLocalSocketThread;
    final BluetoothServiceThread mBluetoothThread;

    public static final int RECONNECT_TIMEOUT_MS = 2000;

    public BluetoothToLocalSocket(@Nonnull String bluetoothAddress,
            @Nonnull String localSocketPath) {
        mLocalSocketThread = new LocalSocketServiceThread(localSocketPath);
        mBluetoothThread = new BluetoothServiceThread(bluetoothAddress);
    }

    public void start() {
        mBluetoothThread.start();
        mLocalSocketThread.start();
    }

    public void stop() {
        mBluetoothThread.cancel();
        mLocalSocketThread.cancel();
    }

    private class LocalSocketServiceThread extends Thread {

        private final LocalSocketAddress mSocketPath;

        private int mConnectionState;
        private volatile boolean cancelRequested;
        private LocalSocket mSocket;
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        public LocalSocketServiceThread(String socketPath) {
            mSocketPath = new  LocalSocketAddress(socketPath, Namespace.FILESYSTEM);

            mSocket = new LocalSocket();
            mInputStream = sDummyInputStream;
            mOutputStream = sDummyOutputStream;

        }

        public void cancel() {
            LocalSocket s;
            synchronized(this) {
                cancelRequested = true;
                s = mSocket;
                notifyAll();
            }
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of connect socket failed", e);
                }
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


        private void setState(int state) {
            int oldState = mConnectionState;
            mConnectionState = state;
            if (DBG) Log.d(TAG, "setState() " + oldState + " -> " + state);
        }

        private boolean connectLoop() {
            LocalSocket s;
            synchronized(this) {
                s = mSocket;
            }

            while(!cancelRequested) {
                try {
                    s.connect(mSocketPath);
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
                rcvd =  mInputStream.read(buf, 0, buf.length);
                if (rcvd >= 0) {
                    // TODO: report error to rtksvr?
                    mBluetoothThread.write(buf, 0, rcvd);
                }
            }catch (IOException e) {
                synchronized(this) {
                    try {
                        mSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    mInputStream = sDummyInputStream;
                    mOutputStream = sDummyOutputStream;
                    return cancelRequested;
                }
            }

            return false;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN BluetoothToLocalSocket-Socket");
            setName("BluetoothToLocalSocket-LocalSocket");

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


    private class BluetoothServiceThread extends Thread {

        private final BluetoothAdapter mBtAdapter;
        private final BluetoothDevice mBtDevice;
        private BluetoothSocket mSocket;
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        private int mConnectionState;
        private volatile boolean cancelRequested;

        public BluetoothServiceThread(String bluetoothAddress) {
            mInputStream = sDummyInputStream;
            mOutputStream = sDummyOutputStream;
            mConnectionState = STATE_IDLE;
            cancelRequested = false;
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            mBtDevice = mBtAdapter.getRemoteDevice(bluetoothAddress);
        }

        private void setState(int state) {
            int oldState = mConnectionState;
            mConnectionState = state;
            if (DBG) Log.d(TAG, "setState() " + oldState + " -> " + state);

        }

        public void cancel() {
            BluetoothSocket s;
            synchronized(this) {
                cancelRequested = true;
                s = mSocket;
                notifyAll();
            }
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of connect socket failed", e);
                }
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
                    mInputStream = sDummyInputStream;
                    mOutputStream = sDummyOutputStream;
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
                rcvd =  mInputStream.read(buf, 0, buf.length);
                if (rcvd >= 0) {
                    try {
                        mLocalSocketThread.write(buf, 0, rcvd);
                    }catch (IOException e) {
                        // TODO
                        e.printStackTrace();
                    }
                }
            }catch (IOException e) {
                synchronized(this) {
                    try {
                        mSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    mInputStream = sDummyInputStream;
                    mOutputStream = sDummyOutputStream;
                    return cancelRequested;
                }
            }

            return false;
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

    InputStream sDummyInputStream = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return -1;
        }
    };

    OutputStream sDummyOutputStream = new OutputStream() {

        @Override
        public void write(int arg0) throws IOException {
        }

        @Override
        public void write(byte[] buffer, int offset, int count)
                throws IOException {
        }
    };
}
