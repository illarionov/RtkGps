package org.osmdroid.tileprovider.modules;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.geoportail.License;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpVersion;
import cz.msebera.android.httpclient.auth.AuthScope;
import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.CredentialsProvider;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.conn.ClientConnectionManager;
import cz.msebera.android.httpclient.conn.scheme.PlainSocketFactory;
import cz.msebera.android.httpclient.conn.scheme.Scheme;
import cz.msebera.android.httpclient.conn.scheme.SchemeRegistry;
import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;
import cz.msebera.android.httpclient.impl.client.AbstractHttpClient;
import cz.msebera.android.httpclient.impl.client.BasicCredentialsProvider;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.impl.conn.tsccm.ThreadSafeClientConnManager;
import cz.msebera.android.httpclient.params.BasicHttpParams;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.params.HttpProtocolParams;
import cz.msebera.android.httpclient.protocol.HTTP;
import cz.msebera.android.httpclient.util.EntityUtils;
import org.osmdroid.tileprovider.BitmapPool;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.ReusableBitmapDrawable;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.util.StreamUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This code is mostly copied from MapTileDownloader with changes
 * mainly in WMTSTileLoader.
 *
 * @author Steve Potell -- spotell@t-sciences.com
 *
 */
public class WMTSMapTileDownloader extends MapTileModuleProviderBase {

    // ===========================================================
    // Constants
    // ===========================================================
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = WMTSMapTileDownloader.class.getSimpleName();

    // ===========================================================
    // Fields
    // ===========================================================

    private final IFilesystemCache mFilesystemCache;

    private OnlineTileSourceBase mTileSource;

    private final INetworkAvailablityCheck mNetworkAvailablityCheck;

    // ===========================================================
    // Constructors
    // ===========================================================

    public WMTSMapTileDownloader(final ITileSource pTileSource) {
        this(pTileSource, null, null);
    }

    public WMTSMapTileDownloader(final ITileSource pTileSource, final IFilesystemCache pFilesystemCache) {
        this(pTileSource, pFilesystemCache, null);
    }

    public WMTSMapTileDownloader(final ITileSource pTileSource,
            final IFilesystemCache pFilesystemCache,
            final INetworkAvailablityCheck pNetworkAvailablityCheck) {
        super(NUMBER_OF_TILE_DOWNLOAD_THREADS, TILE_DOWNLOAD_MAXIMUM_QUEUE_SIZE);

        mFilesystemCache = pFilesystemCache;
        mNetworkAvailablityCheck = pNetworkAvailablityCheck;
        setTileSource(pTileSource);
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public ITileSource getTileSource() {
        return mTileSource;
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected String getName() {
        return "WMTS Online Tile Download Provider";
    }

    @Override
    protected String getThreadGroupName() {
        return "downloader";
    }

    @Override
    protected Runnable getTileLoader() {
        return new WMSTileLoader();
    }

    @Override
    public boolean getUsesDataConnection() {
        return true;
    }

    @Override
    public int getMinimumZoomLevel() {
        return (mTileSource != null ? mTileSource.getMinimumZoomLevel() : MINIMUM_ZOOMLEVEL);
    }

    @Override
    public int getMaximumZoomLevel() {
        return (mTileSource != null ? mTileSource.getMaximumZoomLevel() : MAXIMUM_ZOOMLEVEL);
    }

    @Override
    public void setTileSource(ITileSource tileSource) {
        // We are only interested in OnlineTileSourceBase tile sources
        if (tileSource instanceof OnlineTileSourceBase) {
            mTileSource = (OnlineTileSourceBase) tileSource;
        } else {
            // Otherwise shut down the tile downloader
            mTileSource = null;
        }
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private class WMSTileLoader extends MapTileModuleProviderBase.TileLoader {

        @Override
        public Drawable loadTile(final MapTileRequestState aState) throws CantContinueException {

            if (mTileSource == null) {
                return null;
            }

            InputStream in = null;
            OutputStream out = null;
            final MapTile tile = aState.getMapTile();

            try {

                if (mNetworkAvailablityCheck != null
                        && !mNetworkAvailablityCheck.getNetworkAvailable()) {
                    if (DBG) {
                        Log.d(TAG,"WMTSMapTileDownloader -- Skipping " + getName() + " due to NetworkAvailabliltyCheck.");
                    }
                    return null;
                }

                final String tileURLString = mTileSource.getTileURLString(tile);

                if (DBG) {
                    Log.d(TAG,"WMTSMapTileDownloader -- Downloading Maptile from url: " + tileURLString);
                }

                if (TextUtils.isEmpty(tileURLString)) {
                    return null;
                }

                // The main changes are here dealing with credentials.
                // create the credentials with username and password
                CredentialsProvider credProvider = new BasicCredentialsProvider();
                credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials("", ""));

                // get the client
                HttpClient httpClient = getNewHttpClient();
                ((AbstractHttpClient) httpClient).setCredentialsProvider(credProvider);

                Log.d(TAG,"WMTSMapTileDownloader -- tileURLString ------------------------- " + tileURLString);

                final HttpUriRequest head = new HttpGet(tileURLString);
                //Geoportail has a mandatory user-agent
                head.setHeader("User-Agent", License.getUserAgent()); // TODO have tileSource dependent User-Agent
                final HttpResponse response = httpClient.execute(head);

                // Check to see if we got success
                final cz.msebera.android.httpclient.StatusLine line = response.getStatusLine();
                if (line.getStatusCode() != 200) {
                    Log.w(TAG,"WMTSMapTileDownloader -- Problem downloading MapTile: " + tile + " HTTP response: " + line);
                    if (DBG)
                    {
                        String responseAsString = EntityUtils.toString(response.getEntity());
                        Log.d(TAG,responseAsString);
                    }
                    return null;
                }

                final HttpEntity entity = response.getEntity();
                if (entity == null) {
                    Log.w(TAG,"WMTSMapTileDownloader -- No content downloading MapTile: " + tile);
                    return null;
                }
                in = entity.getContent();

                final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
                StreamUtils.copy(in, out);
                out.flush();
                final byte[] data = dataStream.toByteArray();
                final ByteArrayInputStream byteStream = new ByteArrayInputStream(data);

                // Save the data to the filesystem cache
                if (mFilesystemCache != null) {
                    mFilesystemCache.saveFile(mTileSource, tile, byteStream);
                    byteStream.reset();
                }
                final Drawable result = mTileSource.getDrawable(byteStream);

                return result;
            } catch (final UnknownHostException e) {
                // no network connection so empty the queue
                Log.w(TAG,"WMSMapTileDownloader -- UnknownHostException downloading MapTile: " + tile + " : " + e);
                throw new CantContinueException(e);
            } catch (final LowMemoryException e) {
                // low memory so empty the queue
                Log.w(TAG,"WMSMapTileDownloader -- LowMemoryException downloading MapTile: " + tile + " : " + e);
                throw new CantContinueException(e);
            } catch (final FileNotFoundException e) {
                Log.w(TAG,"WMSMapTileDownloader -- Tile not found: " + tile + " : " + e);
            } catch (final IOException e) {
                Log.w(TAG,"WMSMapTileDownloader -- IOException downloading MapTile: " + tile + " : " + e);
            } catch (final Throwable e) {
                Log.e(TAG,"WMSMapTileDownloader -- Error downloading MapTile: " + tile, e);
            } finally {
                StreamUtils.closeStream(in);
                StreamUtils.closeStream(out);
            }

            return null;
        }

        @Override
        protected void tileLoaded(final MapTileRequestState pState, final Drawable pDrawable) {
            removeTileFromQueues(pState.getMapTile());
            // don't return the tile because we'll wait for the fs provider to ask for it
            // this prevent flickering when a load of delayed downloads complete for tiles
            // that we might not even be interested in any more
            pState.getCallback().mapTileRequestCompleted(pState, null);
            // We want to return the Bitmap to the BitmapPool if applicable
            if (pDrawable instanceof ReusableBitmapDrawable)
                BitmapPool.getInstance().returnDrawableToPool((ReusableBitmapDrawable) pDrawable);
        }

    } // end WMSTileLoader

    // TODO: fix the cert to prompt and store.

    /**
     * This class creates the HttpClient that we will use.
     *
     * @return the HttpClient that will trust all certs.
     */
    public HttpClient getNewHttpClient() {

        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }

    } // end getNewHttpClient

    /**
     * Socket factory for trusting all certs.
     */
    public class MySSLSocketFactory extends SSLSocketFactory {

        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }

    } // end MySSLSocketFactory

} // end WMTSMapTileDownloader
