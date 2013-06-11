package ru0xdc.rtkgps;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.os.StrictMode;

@ReportsCrashes(formKey = "",
    mailTo = "bug@0xdc.ru",
    mode = ReportingInteractionMode.TOAST,
    resToastText = R.string.crash_toast_text)
public class RtkGps extends Application {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    @Override
    public void onCreate() {
        if (DBG) {
            StrictMode.enableDefaults();
        }
        super.onCreate();
        //ACRA.init(this);

        System.loadLibrary("rtkgps");

    }
}
