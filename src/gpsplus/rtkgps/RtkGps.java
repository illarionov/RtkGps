package gpsplus.rtkgps;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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
    private static String VERSION = "";

    @Override
    public void onCreate() {
        if (DBG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate();
        //ACRA.init(this);
        System.loadLibrary("proj");
        Log.v("Proj4","Proj4 version: "+PJ.getVersion());
        System.loadLibrary("rtkgps");
        //set version
        PackageInfo pi;
        try {
            pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            RtkGps.VERSION = pi.versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static String getVersion() {
        return RtkGps.VERSION;
    }
}
