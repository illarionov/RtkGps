package ru0xdc.rtkgps.settings;

import ru0xdc.rtkgps.R;
import ru0xdc.rtklib.constants.StreamType;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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
            break;
        case TCPCLI:
            fragment = new StreamTcpClientFragment();
            break;
        case FILE:
            fragment = new StreamFileClientFragment();
            break;
        case TCPSVR:
            // TODO
            fragment = new StreamNtripClientFragment();
            break;
        default:
            throw new IllegalArgumentException("wrong ARG_FRAGMENT_ARGUMENTS");
        }

        fragment.setArguments(fragmentArgs);

        getFragmentManager().beginTransaction()
        .replace(R.id.fragment_container, fragment).commit();

    }

    public void closeButtonClicked(View view) {
        finish();
    }


}
