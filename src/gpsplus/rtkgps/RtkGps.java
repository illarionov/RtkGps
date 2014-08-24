package gpsplus.rtkgps;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.proj4.PJ;

@ReportsCrashes(formKey = "",
    mailTo = "bug@sudagri-jatropha.com",
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
        System.loadLibrary("proj");
        Log.v("Proj4","Proj4 version: "+PJ.getVersion());
        System.loadLibrary("rtkgps");

    }
}
