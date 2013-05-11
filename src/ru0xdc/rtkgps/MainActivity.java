package ru0xdc.rtkgps;

import java.io.File;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.settings.SettingsActivity;
import ru0xdc.rtkgps.settings.SettingsHelper;
import ru0xdc.rtkgps.settings.StreamSettingsActivity;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class MainActivity extends Activity implements ActionBar.OnNavigationListener {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;

    static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    RtkNaviService mRtkService;
    boolean mRtkServiceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<String>(getActionBar().getThemedContext(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[] { getString(R.string.title_status) }),
                        this);

        if (savedInstanceState == null) {
            SettingsHelper.setDefaultValues(this, false);
            startRtkService();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mRtkServiceBound) {
            final Intent intent = new Intent(this, RtkNaviService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            RtkNaviService.RtkNaviServiceBinder binder = (RtkNaviService.RtkNaviServiceBinder) service;
            mRtkService = binder.getService();
            mRtkServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mRtkServiceBound = false;
            mRtkService = null;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (mRtkServiceBound) {
            unbindService(mConnection);
            mRtkServiceBound = false;
            mRtkService = null;
        }

    }

    private void startRtkService() {
        final Intent intent = new Intent(RtkNaviService.ACTION_START);
        intent.setClass(this, RtkNaviService.class);
        startService(intent);
    }

    private void stopRtkService() {
        final Intent intent = new Intent(RtkNaviService.ACTION_STOP);
        intent.setClass(this, RtkNaviService.class);
        startService(intent);
    }

    public RtkNaviService getRtkService() {
        return mRtkService;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
                .getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean serviceActive = mRtkServiceBound && (mRtkService.isServiceStarted());
        menu.findItem(R.id.menu_start_service).setVisible(!serviceActive);
        menu.findItem(R.id.menu_stop_service).setVisible(serviceActive);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            showSettings();
            break;
        case R.id.menu_input_stream_settings:
            showInputStreamSettings();
            break;
        case R.id.menu_output_stream_settings:
            showOutputStreamSettings();
            break;
        case R.id.menu_log_stream_settings:
            showLogStreamSettings();
            break;
        case R.id.menu_start_service:
            startRtkService();
            break;
        case R.id.menu_stop_service:
            stopRtkService();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showSettings() {
        final Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void showInputStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsActivity.STREAM_INPUT_SETTINGS);
        startActivity(intent);
    }

    private void showOutputStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsActivity.STREAM_OUTPUT_SETTINGS);
        startActivity(intent);
    }

    private void showLogStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsActivity.STREAM_LOG_SETTINGS);
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        Fragment fragment;

        // When the given dropdown item is selected, show its contents in the
        // container view.
        switch (position) {
        case 0:
            fragment = new StatusFragment();
            break;
        default:
            throw new IllegalStateException();
        }

        getFragmentManager().beginTransaction()
        .replace(R.id.container, fragment).commit();

        return true;
    }

    @Nonnull
    public static File getFileStorageDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "RtkGps/");
    }

    @Nonnull
    public static File getLocalSocketPath(Context ctx, String socketName) {
        return ctx.getFileStreamPath(socketName);
    }


    static {
        System.loadLibrary("rtkgps");
    }
}
