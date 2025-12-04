package gpsplus.rtkgps.settings.widget;


import gpsplus.rtkgps.R;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

public class StartupShutdownCommandsPreference extends DialogPreference {

    public StartupShutdownCommandsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StartupShutdownCommandsPreference(Context context,
            AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setPersistent(false);
        setDialogLayoutResource(R.layout.activity_startup_shutdown_commands);
    }

    private String getStartupCommandsKey() {
        return getKey()+"_startup";
    }

    private String getShutdownCommandsKey() {
        return getKey()+"_shutdown";
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            persist();
        }
    }

    private void persist() {
        getSharedPreferences().edit()
            .putString(getStartupCommandsKey(), "1")
            .putString(getShutdownCommandsKey(), "1")
            .commit();
    }


}
