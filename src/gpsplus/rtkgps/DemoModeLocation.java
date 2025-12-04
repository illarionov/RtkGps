package gpsplus.rtkgps;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import gpsplus.rtkgps.settings.InputBaseFragment;
import gpsplus.rtkgps.settings.InputCorrectionFragment;
import gpsplus.rtkgps.settings.InputRoverFragment;
import gpsplus.rtkgps.settings.SolutionOutputSettingsFragment;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.RtkServerObservationStatus;

import java.util.Iterator;

public class DemoModeLocation implements android.location.GpsStatus.Listener, LocationListener{

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = DemoModeLocation.class.getSimpleName();
    private static Context mParentContext = null;
    private static boolean mIsInDemoMode = false;
    private Position3d mPos = null;
    private int nbSat = 0;
    private long lTime = System.currentTimeMillis();
    private float accuracy = Float.MAX_VALUE;

    LocationManager locationManager = null;
    private RtkServerObservationStatus mObs;

    public DemoModeLocation(Context parentContext) {
        super();
        DemoModeLocation.mParentContext = parentContext;
        reset();

    }
    public DemoModeLocation() throws NullPointerException{
        super();
        if (DemoModeLocation.mParentContext == null)
        {
            throw (new NullPointerException("One instance must be initialized with constructor DemoModeLocation(Context parentContext)"));
        }
    }

    public void reset(){
        SharedPreferences prefsInputBase = mParentContext.getSharedPreferences(InputBaseFragment.SHARED_PREFS_NAME, 0);
        SharedPreferences prefsInputRover = mParentContext.getSharedPreferences(InputRoverFragment.SHARED_PREFS_NAME, 0);
        SharedPreferences prefsInputCorrection = mParentContext.getSharedPreferences(InputCorrectionFragment.SHARED_PREFS_NAME, 0);
        SharedPreferences prefsSolution = mParentContext.getSharedPreferences(SolutionOutputSettingsFragment.SHARED_PREFS_NAME, 0);

        boolean bBaseEnabled = prefsInputBase.getBoolean(InputBaseFragment.KEY_ENABLE, true);
        boolean bRoverEnabled = prefsInputRover.getBoolean(InputRoverFragment.KEY_ENABLE, true);
        boolean bCorrectionEnabled = prefsInputCorrection.getBoolean(InputCorrectionFragment.KEY_ENABLE, true);
        boolean bIsTestModeEnabled = prefsSolution.getBoolean(SolutionOutputSettingsFragment.KEY_ENABLE_TEST_MODE, true);

        mIsInDemoMode = !bBaseEnabled && !bRoverEnabled && !bCorrectionEnabled && bIsTestModeEnabled;
        if (mIsInDemoMode){
            locationManager = (LocationManager) mParentContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.addGpsStatusListener(this);
        }
    }

    public void startDemoMode(){
                locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000, 0.5f, this);
    }

    public void stopDemoMode(){
        locationManager.removeUpdates(this);
    }
    public boolean isInDemoMode(){

        return DemoModeLocation.mIsInDemoMode;
    }

    public Position3d getPosition(){ //in radians
        return mPos;
    }

    public RtkServerObservationStatus getObservationStatus(RtkServerObservationStatus status){
        if (mObs != null) {
            mObs.copyTo(status);
        }
        return mObs;
    }


    @Override
    public void onGpsStatusChanged(int arg0) {
        GpsStatus gpsStatus = locationManager.getGpsStatus(null);
        if(DBG) {
            Log.d(TAG, "GPS Status changed");
        }
        if(gpsStatus != null) {
            Iterable<GpsSatellite>satellites = gpsStatus.getSatellites();
            Iterator<GpsSatellite>sat = satellites.iterator();
            mObs = new RtkServerObservationStatus();
            while (sat.hasNext()) {
                GpsSatellite satellite = sat.next();
                nbSat = 0;
                if (satellite.usedInFix()) {
                    nbSat++;
                    Log.d(TAG, "PRN:"+satellite.getPrn() + ", SNR:" + satellite.getSnr() + ", AZI:" + satellite.getAzimuth() + ", ELE:" + satellite.getElevation());
                    mObs.addValues(satellite.getPrn(), Math.toRadians(satellite.getAzimuth()), Math.toRadians(satellite.getElevation()), Math.round(satellite.getSnr()), 0, 0, 1);
                }
              }
          }
    }

    public int getNbSat() {
        return nbSat;
    }

    public float getAge() {
        return ((float)(System.currentTimeMillis()-lTime)/1000);
    }

    public float getNAccuracy() {
        return accuracy;
    }

    public float getEAccuracy() {
        return accuracy;
    }
    public float getVAccuracy() {
        return 2*accuracy;  //test mode
    }
    @Override
    public void onLocationChanged(Location location) {
        accuracy = location.getAccuracy();
        mPos = new Position3d(Math.toRadians(location.getLatitude()),Math.toRadians(location.getLongitude()),location.getAltitude());
        lTime = System.currentTimeMillis();

    }
    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }
    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }
}
