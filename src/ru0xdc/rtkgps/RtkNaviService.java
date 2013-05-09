package ru0xdc.rtkgps;

import ru0xdc.rtkgps.settings.SettingsHelper;
import ru0xdc.rtklib.RtkControlResult;
import ru0xdc.rtklib.RtkServer;
import ru0xdc.rtklib.RtkServerObservationStatus;
import ru0xdc.rtklib.RtkServerSettings;
import ru0xdc.rtklib.RtkServerStreamStatus;
import ru0xdc.rtklib.Solution;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class RtkNaviService extends Service {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;

    static final String TAG = RtkNaviService.class.getSimpleName();

    private int NOTIFICATION = R.string.local_service_started;

    // Binder given to clients
    private final IBinder mBinder = new RtkNaviServiceBinder();

    private final RtkServer mRtkServer = new RtkServer();

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

    public RtkControlResult getRtkStatus(RtkControlResult dst) {
        return mRtkServer.getRtkStatus(dst);
    }

    public int getServerStatus() {
        return mRtkServer.getStatus();
    }

    public final Solution getLastSolution() {
        return mRtkServer.getLastSolution();
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

    @Override
    public void onCreate() {
        final RtkServerSettings settings;
        super.onCreate();

        settings = SettingsHelper.loadSettings(this);
        mRtkServer.setServerSettings(settings);

        if (!mRtkServer.start()) {
            Log.e(TAG, "rtkSrvStart() error");
            return;
        }

        Notification notification = createForegroundNotification();
        startForeground(NOTIFICATION, notification);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        mRtkServer.stop();

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
                .show();
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
}
