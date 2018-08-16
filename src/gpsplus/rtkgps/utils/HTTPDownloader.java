package gpsplus.rtkgps.utils;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.ProgressBar;

import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.ToolsActivity.DownloaderCaller;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class HTTPDownloader extends AsyncTask<Void, Integer, String> {

    private static final int HTTP_CHUNK_SIZE = 64*1024;

    final String TAG = HTTPDownloader.class.getSimpleName();
    private static final boolean DBG = BuildConfig.DEBUG & true;

    public IDownloaderAccessResponse delegate=null;
    private ProgressBar pBar;

    private String USER=null;
    private String PASSWORD=null;
    private String remoteFile = "";
    private File localFile;
    private String localFileMD5 = null;
    private DownloaderCaller mCaller;
    private Class<FilterInputStream> mCompressionClass;

    public HTTPDownloader(String url,String destFile, String destFileMD5, String username, String password, ProgressBar pBar, DownloaderCaller caller, Class<FilterInputStream> compressionClass){
        this.USER = username;
        this.PASSWORD = password;
        this.remoteFile = url;
        this.localFile = new File(destFile);
        this.pBar = pBar;
        this.mCaller = caller;
        this.mCompressionClass = compressionClass;
        this.localFileMD5 = destFileMD5;
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
            Log.e(TAG,"HTTPDownloader cannot delegate the postExecute");
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
        String downloadedFile = null;
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpUriRequest req = new HttpGet(remoteFile);
        if ((USER != null) && (PASSWORD != null)) {
            String credentials = USER + ":" + PASSWORD;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            req.addHeader("Authorization", "Basic " + base64EncodedCredentials);
        }
        try {
            HttpResponse response = httpClient.execute(req);
            HttpEntity entity = response.getEntity();

            if (entity == null) {
                Log.e(TAG,"Cannot get response for url "+req.getURI().toString());
                return null;
            }

            if (!localFile.getParentFile().exists())
                localFile.getParentFile().mkdirs();
            int sChunk = HTTP_CHUNK_SIZE;
            byte[] buffer = new byte[sChunk];
            FileOutputStream out = new FileOutputStream(localFile.getAbsolutePath(), true);

            FilterInputStream fs;

            final InputStream in = entity.getContent();

            CountingInputStream cos = new CountingInputStream(in);

            if (mCompressionClass != null){
                fs = mCompressionClass.getDeclaredConstructor(InputStream.class).newInstance(cos);
            }else{
                fs = new BufferedInputStream( cos );
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
              DigestInputStream dis = new DigestInputStream(fs, md);

            int length;

            long remoteSize = entity.getContentLength();
            Log.i(TAG,"Remote file size is (bytes) = " + remoteSize);
            int lastProgress = 0;
            while ((length = dis.read(buffer, 0, sChunk)) != -1)
            {
                 out.write(buffer, 0, length);
                 progress = (int) (cos.getCount()*100/remoteSize);
                 if (progress > lastProgress) {
                     lastProgress = progress;
                     publishProgress(progress);
                 }

                 try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            String md5Downloaded = HexString.bytesToHex(md.digest());
            Log.v(TAG,"MD5 = "+ md5Downloaded );

               downloadedFile = localFile.getAbsolutePath();
               if  (localFileMD5 != null) {
                   if (localFileMD5.toUpperCase(Locale.US).equals(md5Downloaded) ) {
                       downloadedFile = localFile.getAbsolutePath();
                   }else{
                       Log.e(TAG,"MD5 ARE DIFFERENT "+localFileMD5.toUpperCase(Locale.US)+"/"+md5Downloaded+", delete the downloaded file" );
                       localFile.delete();
                       downloadedFile = "";
                   }
               }
               out.close();
               dis.close();
               fs.close();

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return downloadedFile;
    }

}
