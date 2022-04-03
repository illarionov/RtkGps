package gpsplus.rtkgps.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import butterknife.ButterKnife;
import butterknife.BindView;
import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class StartupShutdownSettingsActivity extends Activity {

    private static final boolean DBG = BuildConfig.DEBUG & false;
    static final String TAG = StartupShutdownSettingsActivity.class.getSimpleName();

    public static final String ARG_SHARED_PREFS_NAME = "shared_prefs_name";

    public static final String SHARED_PREFS_KEY_SEND_COMMANDS_AT_STARTUP = "send_commands_at_startup";

    public static final String SHARED_PREFS_KEY_COMMANDS_AT_STARTUP = "commands_at_startup";

    public static final String SHARED_PREFS_KEY_SEND_COMMANDS_AT_SHUTDOWN = "send_commands_at_shutdown";

    public static final String SHARED_PREFS_KEY_COMMANDS_AT_SHUTDOWN = "commands_at_shutdown";

    static final String ASSETS_PATH_COMMANDS = "commands";

    private String mSharedPrefsName;

    @BindView(R.id.send_commands_at_startup) CheckBox mSendCommandsAtStartupView;
    @BindView(R.id.send_commands_at_shutdown) CheckBox mSendCommandsAtShutdownView;
    @BindView(R.id.commands_at_startup) EditText mCommandsAtStartupView;
    @BindView(R.id.commands_at_shutdown) EditText mCommandsAtShutdownView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_startup_shutdown_commands);

        final Intent intent = getIntent();
        mSharedPrefsName = intent.getStringExtra(ARG_SHARED_PREFS_NAME);
        if (mSharedPrefsName == null) {
            throw new IllegalArgumentException("ARG_SHARED_PREFS_NAME not defined");
        }

        ButterKnife.bind(this);

        loadSettings();
    }


    public void onSendCommandsAtStartupClicked(View v) {
        mCommandsAtStartupView.setEnabled(((CheckBox)v).isChecked());
    }

    public void onSendCommandsAtShutdownClicked(View v) {
        mCommandsAtShutdownView.setEnabled(((CheckBox)v).isChecked());
    }

    public void onCancelButtonClicked(View v) {
        finish();
    }

    public void onLoadButtonClicked(View v) {
        showLoadCommandsDialog();
    }

    public void onOkButtonClicked(View v) {
        saveSettings();
        finish();
    }


    private void loadSettings() {
        final SharedPreferences prefs;
        boolean sendStartup, sendShutdown;
        String startup, shutdown;

        prefs = getSharedPreferences(mSharedPrefsName, MODE_PRIVATE);
        startup = prefs.getString(SHARED_PREFS_KEY_COMMANDS_AT_STARTUP, "");
        shutdown = prefs.getString(SHARED_PREFS_KEY_COMMANDS_AT_SHUTDOWN, "");
        sendStartup = prefs.getBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_STARTUP, false);
        sendShutdown = prefs.getBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_SHUTDOWN, false);

        mSendCommandsAtStartupView.setChecked(sendStartup);
        mCommandsAtStartupView.setEnabled(sendStartup);
        mCommandsAtStartupView.setText(startup);

        mSendCommandsAtShutdownView.setChecked(sendShutdown);
        mCommandsAtShutdownView.setEnabled(sendShutdown);
        mCommandsAtShutdownView.setText(shutdown);

    }

    private void saveSettings() {
        getSharedPreferences(mSharedPrefsName, MODE_PRIVATE)
            .edit()
            .putBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_STARTUP, mSendCommandsAtStartupView.isChecked())
            .putString(SHARED_PREFS_KEY_COMMANDS_AT_STARTUP, mCommandsAtStartupView.getText().toString())
            .putBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_SHUTDOWN, mSendCommandsAtShutdownView.isChecked())
            .putString(SHARED_PREFS_KEY_COMMANDS_AT_SHUTDOWN, mCommandsAtShutdownView.getText().toString())
            .commit();
    }

    private void showLoadCommandsDialog() {
        SelectCommandsFileDialog dialog;
        dialog = new SelectCommandsFileDialog();

        dialog.show(getFragmentManager(), "commandsSelector");

    }

    void onLoadFileSelected(String path) {
        StringBuilder startup, shutdown;
        boolean isStartup;
        BufferedReader reader;
        if (DBG) Log.v(TAG, "download file " + path);

        startup = new StringBuilder();
        shutdown = new StringBuilder();
        isStartup = true;
        reader = null;
        try {
            String s;
            reader = new BufferedReader(new InputStreamReader(getAssets().open(path)));
            while ((s=reader.readLine()) != null) {
                if (s.equals("@")) {
                    isStartup=false;
                    continue;
                }
                if (isStartup) {
                    startup.append(s).append('\n');
                }else {
                    shutdown.append(s).append('\n');
                }
            }
        }catch (IOException ioe) {
            ioe.printStackTrace();
        }finally{
            try {
            if (reader != null) reader.close();
            }catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        mSendCommandsAtStartupView.setChecked(startup.length()>0);
        mCommandsAtStartupView.setEnabled(mSendCommandsAtStartupView.isChecked());
        mCommandsAtStartupView.setText(startup);

        mSendCommandsAtShutdownView.setChecked(shutdown.length()>0);
        mCommandsAtShutdownView.setEnabled(mSendCommandsAtShutdownView.isChecked());
        mCommandsAtShutdownView.setText(shutdown);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

        prefs.edit()
        .putBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_STARTUP, false)
        .putString(SHARED_PREFS_KEY_COMMANDS_AT_STARTUP, "")
        .putBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_SHUTDOWN, false)
        .putString(SHARED_PREFS_KEY_COMMANDS_AT_SHUTDOWN, "")
        .commit();

    }

    public static class SelectCommandsFileDialog extends DialogFragment {

        private CharSequence mFiles[];

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            mFiles = getCommandsList();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                .setItems(mFiles, mOnClickListener)
                .setTitle(R.string.load_startup_shutdown_dialog_title)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       dismiss();
                   }
                })
                ;
            return builder.create();
        }

        private CharSequence[] getCommandsList() {
            final String[] files;

            try {
                files = getResources().getAssets().list(ASSETS_PATH_COMMANDS);
                Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
                return files;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new String[0];
        }

        private final DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((StartupShutdownSettingsActivity)getActivity())
                    .onLoadFileSelected(ASSETS_PATH_COMMANDS+"/"+mFiles[which]);
                dismiss();
            }
        };

    }
}
