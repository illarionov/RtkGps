package gpsplus.rtkgps.utils;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.MainActivity;
import gpsplus.rtkgps.ToolsActivity.DownloaderCaller;
import gpsplus.rtklib.RtkCommon;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class PreciseEphemerisDownloader extends AsyncTask<Void, Integer, String> {

    final String TAG = PreciseEphemerisDownloader.class.getSimpleName();

    private PreciseEphemerisProvider ephemerisProvider = PreciseEphemerisProvider.ESU_ESA;

    private static final boolean DBG = BuildConfig.DEBUG & true;

        private String USER="anonymous";
        private String PASSWORD="rtkgps@world.com";

        private ProgressBar pBar;

        private String iguCurrentFile;

        private String currentOrbit;

        private File destFile;

        private URL url;

        public IDownloaderAccessResponse delegate=null;

        private DownloaderCaller mCaller = DownloaderCaller.IGU_ORBIT;


    public PreciseEphemerisDownloader(PreciseEphemerisProvider provider, String destFile, String username, String password, ProgressBar pBar, DownloaderCaller caller){
        this.ephemerisProvider = provider;
        this.USER = username;
        this.PASSWORD = password;
        this.currentOrbit = getCurrentOrbit(provider);
        this.destFile = new File(destFile);
        this.pBar = pBar;
        this.mCaller = caller;
    }

        public PreciseEphemerisDownloader(String url,String destFile, String username, String password, ProgressBar pBar, DownloaderCaller caller){
            this.USER = username;
            this.PASSWORD = password;
            this.currentOrbit = url;
            this.destFile = new File(destFile);
            this.pBar = pBar;
            this.mCaller = caller;
        }

    public static File getCurrentOrbitFile(PreciseEphemerisProvider provider)
    {
        String currentOrbitFile = RtkCommon.reppath(provider.getURLTemplateFileDest(),System.currentTimeMillis()/1000-3600*6, "", "");
        File destFile = new File(MainActivity.getFileStorageDirectory() + File.separator + currentOrbitFile);
        return destFile;
    }

    public File getCurrentOrbitFile()
    {
        return getCurrentOrbitFile(ephemerisProvider);
    }

    public  boolean isCurrentOrbitsPresent()
    {
        return getCurrentOrbitFile().exists();
    }

    public static String getCurrentOrbit(PreciseEphemerisProvider provider)
    {
        return RtkCommon.reppath(provider.getURLTemplate(),System.currentTimeMillis()/1000-3600*6, "", "");
    }

    public static boolean isCurrentOrbitsPresent(PreciseEphemerisProvider provider)
    {
        if (provider != null)
            return getCurrentOrbitFile(provider).exists();
        else
            return false;
    }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            iguCurrentFile = "";
            Log.v(TAG, "Remote orbit is: "+currentOrbit);
            pBar.setProgress(0);
            try {
                url = new URL(currentOrbit);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(delegate!=null)
            {
                delegate.postResult(result,mCaller);
            }
            else
            {
                Log.e(TAG,"PreciseEphemerisDownloader cannot delegate the postExecute");
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values){
            super.onProgressUpdate(values[0]);
            if (DBG) Log.v(TAG,"Download " + values[0] + "%");
            pBar.setProgress(values[0]);
        }


        @Override
        protected String doInBackground(Void... arg0) {
            java.io.InputStream is;
            int progress = 0;
            try {

                if (!destFile.exists()) {
                    if (url.getProtocol() == "http")
                    {
                        is = url.openStream();

                    }else if (url.getProtocol() == "ftp") {
                        FTPClient client = new FTPClient();
                        client.connect(url.getHost(), (url.getPort() == -1) ? url.getDefaultPort() : url.getPort());
                        client.login(USER, PASSWORD);
                        client.enterLocalPassiveMode();
                        client.setFileType(FTP.BINARY_FILE_TYPE);
                        is = client.retrieveFileStream(url.getPath());
                    }else{
                        is = url.openStream();
                    }
                    UncompressInputStream zStream = new UncompressInputStream( is );

                    if (!destFile.getParentFile().exists())
                        destFile.getParentFile().mkdirs();
                    int sChunk = 16*1024;
                    byte[] buffer = new byte[sChunk];
                    FileOutputStream out = new FileOutputStream(destFile.getAbsolutePath(), true);
                    int length;
                    long alreadyDownloaded = 0;
                     while ((length = zStream.read(buffer, 0, sChunk)) != -1)
                     {
                          alreadyDownloaded += length;
                          out.write(buffer, 0, length);
                          progress = (int) (alreadyDownloaded*100/ephemerisProvider.getPredictedSize());
                          publishProgress(progress);
                          try {
                             Thread.sleep(5);
                         } catch (InterruptedException e) {
                             // TODO Auto-generated catch block
                             e.printStackTrace();
                         }
                     }
                        out.close();
                        zStream.close();
                        iguCurrentFile = destFile.getAbsolutePath();

                }else{
                    iguCurrentFile = destFile.getAbsolutePath();

                }

                 } catch (IOException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                 }
            Log.v(TAG,"IGU file is here");
            return iguCurrentFile;

        }
}