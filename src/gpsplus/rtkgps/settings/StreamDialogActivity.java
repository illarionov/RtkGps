package gpsplus.rtkgps.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import gpsplus.rtkgps.R;
import gpsplus.rtklib.constants.StreamType;

public class StreamDialogActivity extends Activity {

    /** Fragment type: {@link StreamType} .name() */
    public static final String ARG_FRAGMENT_TYPE = "fragment_type";

    /** Fragment arguments {@link Bundle} */
    public static final String ARG_FRAGMENT_ARGUMENTS = "fragment_arguments";

    public static final String ARG_SHARED_PREFS_NAME = "shared_preferences_name";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent;
        String fragmentType;
        final Bundle fragmentArgs;
        final Fragment fragment;
        final int title;

        setContentView(R.layout.activity_stream_settings_dialog);

        intent = getIntent();
        fragmentType = intent.getStringExtra(ARG_FRAGMENT_TYPE);
        fragmentArgs = intent.getBundleExtra(ARG_FRAGMENT_ARGUMENTS);

        if (fragmentType == null) {
            throw new IllegalArgumentException("wrong ARG_FRAGMENT_TYPE");
        }

        if (fragmentArgs == null) {
            throw new IllegalArgumentException("wrong ARG_FRAGMENT_ARGUMENTS");
        }

        switch (StreamType.valueOf(fragmentType)) {
        case NTRIPCLI:
            fragment = new StreamNtripClientFragment();
            title = R.string.ntrip_client_dialog_title;
            break;
        case NTRIPSVR:
            fragment = new StreamNtripServerFragment();
            title = R.string.ntrip_server_dialog_title;
            break;
        case TCPCLI:
            fragment = new StreamTcpClientFragment();
            title = R.string.tcp_client_dialog_title;
            break;
        case UDPCLI:
            fragment = new StreamUdpClientFragment();
            title = R.string.udp_client_dialog_title;
            break;
        case FILE:
            fragment = new StreamFileClientFragment();
            title = R.string.file_dialog_title;
            break;
        case BLUETOOTH:
            fragment = new StreamBluetoothFragment();
            title = R.string.bluetooth_dialog_title;
            break;
        case USB:
            fragment = new StreamUsbFragment();
            title = R.string.usb_dialog_title;
            break;
        case MOBILEMAPPER:
            fragment = new StreamMobileMapperFragment();
            title = R.string.mobilemapper_dialog_title;
            break;
        case TCPSVR:
            // TODO
            fragment = new StreamNtripClientFragment();
            title = R.string.ntrip_client_dialog_title;
            break;
        default:
            throw new IllegalArgumentException("wrong ARG_FRAGMENT_ARGUMENTS");
        }

        fragment.setArguments(fragmentArgs);

        getFragmentManager().beginTransaction()
        .replace(R.id.fragment_container, fragment).commit();

        setTitle(title);
    }

    public void closeButtonClicked(View view) {
        finish();
    }


}
