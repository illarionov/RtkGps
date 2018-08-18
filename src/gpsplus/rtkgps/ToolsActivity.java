package gpsplus.rtkgps;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import gpsplus.rtkgps.settings.ProcessingOptions1Fragment;
import gpsplus.rtkgps.utils.HTTPDownloader;
import gpsplus.rtkgps.utils.IDownloaderAccessResponse;
import gpsplus.rtkgps.utils.PreciseEphemerisDownloader;
import gpsplus.rtkgps.utils.PreciseEphemerisProvider;
import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.constants.EphemerisOption;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;




public class ToolsActivity extends Activity implements IDownloaderAccessResponse{

    private final String TAG = ToolsActivity.class.getSimpleName();
    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;

    private final String RAF09URL = "http://geodesie.ign.fr/contenu/fichiers/documentation/grilles/metropole/RAF09.mnt";
    private final String RAF09LOCALFILE = MainActivity.getFileStorageDirectory() + "/RAF09_M15x20.geoid";
    @SuppressWarnings("rawtypes")
    private final Class RAF09_COMPRESS = null;
    private final String RAF09_MD5 = "5f91dcd18f37a5d0d5775ac649ddc715";

    private final String EGM2008_M25_URL = "http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm2008/Small_Endian/Und_min2.5x2.5_egm2008_isw=82_WGS84_TideFree_SE.gz";
    private final String EGM2008_M25_LOCALFILE = MainActivity.getFileStorageDirectory() + "/EGM2008_M25.geoid";
    @SuppressWarnings("rawtypes")
    private final Class EGM2008_M25_COMPRESS = GZIPInputStream.class;
    private final String EGM2008_M25_MD5 = "9cb14097ab717c5ae2eff35906903d47";

    private final String EGM2008_M10_URL = "http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm2008/Small_Endian/Und_min1x1_egm2008_isw=82_WGS84_TideFree_SE.gz";
    private final String EGM2008_M10_LOCALFILE = MainActivity.getFileStorageDirectory() + "/EGM2008_M10.geoid";
    @SuppressWarnings("rawtypes")
    private final Class EGM2008_M10_COMPRESS = GZIPInputStream.class;
    private final String EGM2008_M10_MD5 = "549d9305c9833481cebc51bd713488b5";

    private final String EGM96_M150_URL = "http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/binary/WW15MGH.DAC";
    private final String EGM96_M150_LOCALFILE = MainActivity.getFileStorageDirectory() + "/EGM96_M150.geoid";
    @SuppressWarnings("rawtypes")
    private final Class EGM96_M150_COMPRESS = null;
    private final String EGM96_M150_MD5 = "a22e6e4f04234b080bfffee8e4993335";

    private Button buttonIgu;
    private Button buttonRaf09;
    private Button buttonEgm2008M25;
    private Button buttonEgm2008M10;
    private Button buttonEgm96M150;
    private ProgressBar pBarIgu;
    private ProgressBar pBarRaf09;
    private ProgressBar pBarEgm2008M25;
    private ProgressBar pBarEgm2008M10;
    private ProgressBar pBarEgm96M150;
    PreciseEphemerisProvider mProvider = null;

    public enum DownloaderCaller{
        IGU_ORBIT,
        RAF09,
        EGM2008_M25,
        EGM2008_M10,
        EGM96_M150
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);
        buttonIgu = findViewById(R.id.tools_button_igu);
        buttonIgu.setEnabled(true);
        SharedPreferences processPrefs = this.getBaseContext().getSharedPreferences(ProcessingOptions1Fragment.SHARED_PREFS_NAME, 0);
        String ephemVa = processPrefs.getString(ProcessingOptions1Fragment.KEY_SAT_EPHEM_CLOCK,"");
        EphemerisOption ephemerisOption = EphemerisOption.valueOf(ephemVa);
        mProvider = ephemerisOption.getProvider();

        if (PreciseEphemerisDownloader.isCurrentOrbitsPresent(mProvider)){
            //Inhject only
            buttonIgu.setText(getString(R.string.tools_inject));
        }else{
            ///Download and inject
            buttonIgu.setText(getString(R.string.tools_get_inject));
        }

        buttonRaf09 = findViewById(R.id.tools_button_raf09);
        if (isRAF09Present()) {
            buttonRaf09.setEnabled(false);
        }else{
            buttonRaf09.setEnabled(true);
        }

        buttonEgm2008M25 = findViewById(R.id.tools_button_egm2008_m25);
        if (isEGM2008_M25Present()) {
            buttonEgm2008M25.setEnabled(false);
        }else{
            buttonEgm2008M25.setEnabled(true);
        }

        buttonEgm2008M10 = findViewById(R.id.tools_button_egm2008_m10);
        if (isEGM2008_M10Present()) {
            buttonEgm2008M10.setEnabled(false);
        }else{
            buttonEgm2008M10.setEnabled(true);
        }

        buttonEgm96M150 = findViewById(R.id.tools_button_egm96_m150);
        if (isEGM96_M150Present()) {
            buttonEgm96M150.setEnabled(false);
        }else{
            buttonEgm96M150.setEnabled(true);
        }

        pBarIgu = findViewById(R.id.tools_progressBar_igu);
        pBarRaf09 = findViewById(R.id.tools_progressBar_raf09);
        pBarEgm2008M25 = findViewById(R.id.tools_progressBar_egm2008_m25);
        pBarEgm2008M10 = findViewById(R.id.tools_progressBar_egm2008_m10);
        pBarEgm96M150 = findViewById(R.id.tools_progressBar_egm96_m150);

        buttonIgu.setOnClickListener(eventHandler);
        buttonRaf09.setOnClickListener(eventHandler);
        buttonEgm2008M25.setOnClickListener(eventHandler);
        buttonEgm2008M10.setOnClickListener(eventHandler);
        buttonEgm96M150.setOnClickListener(eventHandler);


    }

    View.OnClickListener eventHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.setEnabled(false);
            switch(v.getId()) {
                case R.id.tools_button_igu:
                    getIGUEphemeris();
                    break;
                case R.id.tools_button_raf09:
                    getRAF09Model();
                    break;
                case R.id.tools_button_egm2008_m25:
                    getEGM2008_M25Model();
                    break;
                case R.id.tools_button_egm2008_m10:
                    getEGM2008_M10Model();
                    break;
                case R.id.tools_button_egm96_m150:
                    getEGM96_M150Model();
                    break;
            }
        }
      };

      private String getUncompressedFileName(String file) {
          return file.substring(0, file.lastIndexOf(".Z"));
      }


    private void getIGUEphemeris(){

        final String USER="anonymous";
        final String PASSWORD="rtkgps@world.com";

        if (mProvider == null ){return;}


        URL url;
        try {
            url = new URL(PreciseEphemerisDownloader.getCurrentOrbit(mProvider));
            String uncompressedFilename = getUncompressedFileName( (new File(url.getFile()).getName() ));
            File destFile = new File(MainActivity.getFileStorageDirectory() + File.separator + uncompressedFilename);
            String iguCurrentFile = destFile.getAbsolutePath();

            PreciseEphemerisDownloader iguDownloader = new PreciseEphemerisDownloader(mProvider, iguCurrentFile,  USER, PASSWORD, pBarIgu, DownloaderCaller.IGU_ORBIT);
            iguDownloader.delegate = this;
                iguDownloader.execute();

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private boolean isRAF09Present(){
        return (new File(RAF09LOCALFILE)).exists();
    }

    private void getRAF09Model(){
        @SuppressWarnings("unchecked")
        HTTPDownloader raf09Downloader = new HTTPDownloader(RAF09URL, RAF09LOCALFILE,RAF09_MD5, null, null, pBarRaf09, DownloaderCaller.RAF09, RAF09_COMPRESS);
        raf09Downloader.delegate = this;
        raf09Downloader.execute();
    }

    private boolean isEGM2008_M25Present(){
        return (new File(EGM2008_M25_LOCALFILE)).exists();
    }

    private void getEGM2008_M25Model(){
        @SuppressWarnings("unchecked")
        HTTPDownloader egm2008Downloader = new HTTPDownloader(EGM2008_M25_URL, EGM2008_M25_LOCALFILE, EGM2008_M25_MD5, null, null, pBarEgm2008M25, DownloaderCaller.EGM2008_M25, EGM2008_M25_COMPRESS);
        egm2008Downloader.delegate = this;
        egm2008Downloader.execute();
    }

    private boolean isEGM2008_M10Present(){
        return (new File(EGM2008_M10_LOCALFILE)).exists();
    }

    private void getEGM2008_M10Model(){
        @SuppressWarnings("unchecked")
        HTTPDownloader egm2008Downloader = new HTTPDownloader(EGM2008_M10_URL, EGM2008_M10_LOCALFILE, EGM2008_M10_MD5, null, null, pBarEgm2008M10, DownloaderCaller.EGM2008_M10, EGM2008_M10_COMPRESS);
        egm2008Downloader.delegate = this;
        egm2008Downloader.execute();
    }

    private boolean isEGM96_M150Present(){
        return (new File(EGM96_M150_LOCALFILE)).exists();
    }

    private void getEGM96_M150Model(){
        @SuppressWarnings("unchecked")
        HTTPDownloader egm96Downloader = new HTTPDownloader(EGM96_M150_URL, EGM96_M150_LOCALFILE, EGM96_M150_MD5, null, null, pBarEgm96M150, DownloaderCaller.EGM96_M150, EGM96_M150_COMPRESS);
        egm96Downloader.delegate = this;
        egm96Downloader.execute();
    }

   @Override
    public void postResult(String path, DownloaderCaller caller) {
       switch (caller){
           case IGU_ORBIT:
               Log.i(TAG,"SP3 loaded");
               RtkNaviService.loadSP3(path);
               buttonIgu.setEnabled(false);
               break;
           case RAF09:
               Log.i(TAG,"RAF09 loaded: " + RAF09LOCALFILE);
               buttonRaf09.setEnabled(false);
               break;
           case EGM2008_M25:
               Log.i(TAG,"EGM2008_M25 loaded: " + EGM2008_M25_LOCALFILE);
               buttonEgm2008M25.setEnabled(false);
               break;
           case EGM2008_M10:
               Log.i(TAG,"EGM2008_M10 loaded: " + EGM2008_M10_LOCALFILE);
               buttonEgm2008M10.setEnabled(false);
               break;
           case EGM96_M150:
               Log.i(TAG,"EGM96_M150 loaded: " + EGM96_M150_LOCALFILE);
               buttonEgm96M150.setEnabled(false);
               break;
           default:

       }


    }

}
