package ru0xdc.rtkgps;

import ru0xdc.rtkgps.settings.SettingsHelper;
import ru0xdc.rtkgps.settings.StreamBluetoothFragment;
import ru0xdc.rtkgps.settings.StreamBluetoothFragment.Value;
import ru0xdc.rtkgps.settings.StreamUsbFragment;
import ru0xdc.rtklib.RtkControlResult;
import ru0xdc.rtklib.RtkServer;
import ru0xdc.rtklib.RtkServerObservationStatus;
import ru0xdc.rtklib.RtkServerSettings;
import ru0xdc.rtklib.RtkServerSettings.TransportSettings;
import ru0xdc.rtklib.RtkServerStreamStatus;
import ru0xdc.rtklib.Solution;
import ru0xdc.rtklib.constants.StreamType;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class RtkNaviService extends Service {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = RtkNaviService.class.getSimpleName();

    public static final String ACTION_START = "ru0xdc.rtkgps.RtkNaviService.START";
    public static final String ACTION_STOP = "ru0xdc.rtkgps.RtkNaviService.STOP";

    private int NOTIFICATION = R.string.local_service_started;

    // Binder given to clients
    private final IBinder mBinder = new RtkNaviServiceBinder();

    private final RtkServer mRtkServer = new RtkServer();

    private PowerManager.WakeLock mCpuLock;

    private BluetoothToRtklib mBtRover, mBtBase;
    private UsbToRtklib mUsbReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mCpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.v(TAG, "RtkNaviService restarted");
            processStart();
        }else {
            final String action = intent.getAction();
            if (action.equals(ACTION_START)) processStart();
            else if(action.equals(ACTION_STOP)) processStop();
            else Log.e(TAG, "onStartCommand(): unknown action " + action);
        }
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mBinder;
    }


    public final RtkServerStreamStatus getStreamStatus(
            RtkServerStreamStatus status) {
        return mRtkServer.getStreamStatus(status);
    }

    public final RtkServerObservationStatus getRoverObservationStatus(
            RtkServerObservationStatus status) {
        return mRtkServer.getRoverObservationStatus(status);
    }

    public final RtkServerObservationStatus getBaseObservationStatus(
            RtkServerObservationStatus status) {
        return mRtkServer.getBaseObservationStatus(status);
    }

    public RtkControlResult getRtkStatus(RtkControlResult dst) {
        return mRtkServer.getRtkStatus(dst);
    }

    public boolean isServiceStarted() {
        return mRtkServer.getStatus() != RtkServerStreamStatus.STATE_CLOSE;
    }

    public int getServerStatus() {
        return mRtkServer.getStatus();
    }

    public final Solution getLastSolution() {
        return mRtkServer.getLastSolution();
    }

    public Solution[] readSolutionBuffer() {
        return mRtkServer.readSolutionBuffer();
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class RtkNaviServiceBinder extends Binder {
        public RtkNaviService getService() {
            // Return this instance of UpdateDbService so clients can call
            // public methods
            return RtkNaviService.this;
        }
    }

    public void processStart() {
        final RtkServerSettings settings;

        if (isServiceStarted()) return;

        settings = SettingsHelper.loadSettings(this);
        mRtkServer.setServerSettings(settings);

        if (!mRtkServer.start()) {
            Log.e(TAG, "rtkSrvStart() error");
            return;
        }

        startBluetoothPipes();
        startUsb();

        mCpuLock.acquire();

        Notification notification = createForegroundNotification();
        startForeground(NOTIFICATION, notification);
    }


    private void processStop() {
        stop();
        stopSelf();
    }

    private void stop() {
        stopForeground(true);
        if (mCpuLock.isHeld()) mCpuLock.release();

        if (isServiceStarted()) {
            mRtkServer.stop();

            stopBluetoothPipes();
            stopUsb();
            // Tell the user we stopped.
            Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
            .show();
        }
    }

    @Override
    public void onDestroy() {
        stop();
    }

    @SuppressWarnings("deprecation")
    private Notification createForegroundNotification() {
        CharSequence text = getText(R.string.local_service_started);

        Notification notification = new Notification(R.drawable.ic_launcher,
                text, System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this
        // notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        notification.setLatestEventInfo(this,
                getText(R.string.local_service_label), text, contentIntent);

        return notification;
    }

    private class BluetoothCallbacks implements BluetoothToRtklib.Callbacks {

        private int mStreamId;
        private final Handler mHandler;

        public BluetoothCallbacks(int streamId) {
            mStreamId = streamId;
            mHandler = new Handler();
        }

        @Override
        public void onConnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.bluetooth_connected,
                            Toast.LENGTH_SHORT).show();
                }
            });

            new Thread() {
                @Override
                public void run() {
                    mRtkServer.sendStartupCommands(mStreamId);
                }
            }.start();
        }

        @Override
        public void onStopped() {
        }

        @Override
        public void onConnectionLost() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.bluetooth_connection_lost,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    private class UsbCallbacks implements UsbToRtklib.Callbacks {

        private int mStreamId;
        private final Handler mHandler;

        public UsbCallbacks(int streamId) {
            mStreamId = streamId;
            mHandler = new Handler();
        }

        @Override
        public void onConnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.usb_connected,
                            Toast.LENGTH_SHORT).show();
                }
            });

            new Thread() {
                @Override
                public void run() {
                    mRtkServer.sendStartupCommands(mStreamId);
                }
            }.run();

        }

        @Override
        public void onStopped() {
        }

        @Override
        public void onConnectionLost() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.usb_connection_lost,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

    }


    private void startBluetoothPipes() {
        final TransportSettings roverSettngs, baseSettings;

        RtkServerSettings settings = mRtkServer.getServerSettings();

        roverSettngs = settings.getInputRover().getTransportSettings();

        if (roverSettngs.getType() == StreamType.BLUETOOTH) {
            StreamBluetoothFragment.Value btSettings = (Value)roverSettngs;
            mBtRover = new BluetoothToRtklib(btSettings.getAddress(), btSettings.getPath());
            mBtRover.setCallbacks(new BluetoothCallbacks(RtkServer.RECEIVER_ROVER));
            mBtRover.start();
        }else {
            mBtRover = null;
        }

        baseSettings = settings.getInputBase().getTransportSettings();
        if (baseSettings.getType() == StreamType.BLUETOOTH) {
            StreamBluetoothFragment.Value btSettings = (Value)baseSettings;
            mBtBase = new BluetoothToRtklib(btSettings.getAddress(), btSettings.getPath());
            mBtBase.setCallbacks(new BluetoothCallbacks(RtkServer.RECEIVER_BASE));
            mBtBase.start();
        }else {
            mBtBase = null;
        }
    }

    private void stopBluetoothPipes() {
        if (mBtRover != null) mBtRover.stop();
        if (mBtBase != null) mBtBase.stop();
        mBtRover = null;
        mBtBase = null;
    }

    private void startUsb() {
        RtkServerSettings settings = mRtkServer.getServerSettings();

        {
            final TransportSettings roverSettngs;
            roverSettngs = settings.getInputRover().getTransportSettings();
            if (roverSettngs.getType() == StreamType.USB) {
                StreamUsbFragment.Value usbSettings = (ru0xdc.rtkgps.settings.StreamUsbFragment.Value)roverSettngs;
                mUsbReceiver = new UsbToRtklib(this, usbSettings.getPath());
                mUsbReceiver.setBaudRate(usbSettings.getBaudrate());
                mUsbReceiver.setCallbacks(new UsbCallbacks(RtkServer.RECEIVER_ROVER));
                mUsbReceiver.start();
                return;
            }
        }

        {
            final TransportSettings baseSettngs;
            baseSettngs = settings.getInputBase().getTransportSettings();
            if (baseSettngs.getType() == StreamType.USB) {
                StreamUsbFragment.Value usbSettings = (ru0xdc.rtkgps.settings.StreamUsbFragment.Value)baseSettngs;
                mUsbReceiver = new UsbToRtklib(this, usbSettings.getPath());
                mUsbReceiver.setBaudRate(usbSettings.getBaudrate());
                mUsbReceiver.setCallbacks(new UsbCallbacks(RtkServer.RECEIVER_BASE));
                mUsbReceiver.start();
                return;
            }
        }
    }

    private void stopUsb() {
        if (mUsbReceiver != null) {
            mUsbReceiver.stop();
            mUsbReceiver = null;
        }
    }
}
