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
    public static final String IGUURLTemplateDir = "ftp://anonymous:world.com@cddis.gsfc.nasa.gov/gnss/products/%W/";
    public static final String IGUURLTemplateFileDest = "igu%W%D_%hb.sp3";
    public static final String IGUURLTemplateFileCompressExt = ".Z";
    public static final String IGUURLTemplate = IGUURLTemplateDir+IGUURLTemplateFileDest+IGUURLTemplateFileCompressExt;

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final long IGU_INDICATIVE_SIZE = 489000 ; //approximative size for progress bar, noway to predict the size

        private String USER="anonymous";
        private String PASSWORD="rtkgps@world.com";

        private ProgressBar pBar;

        private String iguCurrentFile;

        private String currentOrbit;

        private FTPClient ftp;

        private File destFile;

        private URL url;

        public IDownloaderAccessResponse delegate=null;

        private DownloaderCaller mCaller = DownloaderCaller.IGU_ORBIT;




        public PreciseEphemerisDownloader(String url,String destFile, String username, String password, ProgressBar pBar, DownloaderCaller caller){
            this.USER = username;
            this.PASSWORD = password;
            this.currentOrbit = url;
            this.destFile = new File(destFile);
            this.pBar = pBar;
            this.mCaller = caller;
        }

    public static File getCurrentOrbitFile()
    {
        String currentOrbitFile = RtkCommon.reppath(IGUURLTemplateFileDest,System.currentTimeMillis()/1000-3600*6, "", "");
        File destFile = new File(MainActivity.getFileStorageDirectory() + File.separator + currentOrbitFile);
        return destFile;
    }
    public static boolean isCurrentOrbitsPresent()
    {
        return getCurrentOrbitFile().exists();
    }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            iguCurrentFile = "";
            Log.v(TAG, "Remote orbit is: "+currentOrbit);
            pBar.setProgress(0);
            ftp = new FTPClient();
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

            int progress = 0;
            try {

                if (!destFile.exists()) {
                    ftp.connect(url.getHost(), (url.getPort()==-1)?url.getDefaultPort():url.getPort());
                    ftp.login(USER,PASSWORD);
                    ftp.enterLocalPassiveMode();
                    ftp.setFileType(FTP.BINARY_FILE_TYPE);
                    java.io.InputStream is = ftp.retrieveFileStream(url.getPath());

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
                          progress = (int) (alreadyDownloaded*100/IGU_INDICATIVE_SIZE);
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