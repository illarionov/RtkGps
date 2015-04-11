package org.osmdroid.tileprovider.modules;

import android.util.Log;

import gpsplus.rtkgps.BuildConfig;

import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An implementation of {@link IFilesystemCache}. It writes tiles to the file system cache. If the
 * cache exceeds 600 Mb then it will be trimmed to 500 Mb.
 *
 * @author Neil Boyd
 *
 */
public class WMTSTileWriter implements IFilesystemCache, OpenStreetMapTileProviderConstants {

    // ===========================================================
    // Constants
    // ===========================================================

    private static final Logger logger = LoggerFactory.getLogger(WMTSTileWriter.class);
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = WMTSMapTileDownloader.class.getSimpleName();

    private long TILE_MAX_CACHE_SIZE_BYTES = org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.TILE_MAX_CACHE_SIZE_BYTES;
    private long TILE_TRIM_CACHE_SIZE_BYTES = org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.TILE_TRIM_CACHE_SIZE_BYTES ;


    // ===========================================================
    // Fields
    // ===========================================================

    /** amount of disk space used by tile cache **/
    private static long mUsedCacheSpace;

    // ===========================================================
    // Constructors
    // ===========================================================

    public WMTSTileWriter() {
        this(org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.TILE_MAX_CACHE_SIZE_BYTES,org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.TILE_TRIM_CACHE_SIZE_BYTES);
    }

    public WMTSTileWriter(long max_cache_size, long trim_cache_size) {

        this.TILE_MAX_CACHE_SIZE_BYTES = max_cache_size;
        this.TILE_TRIM_CACHE_SIZE_BYTES = trim_cache_size;
        // do this in the background because it takes a long time
        final Thread t = new Thread() {
            @Override
            public void run() {
                mUsedCacheSpace = 0; // because it's static
                calculateDirectorySize(TILE_PATH_BASE);
                if (DBG) {
                    Log.d(TAG,"Osmdroid current cache size is "+mUsedCacheSpace);
                }
                if (mUsedCacheSpace > TILE_MAX_CACHE_SIZE_BYTES) {
                    cutCurrentCache();
                }
                if (DBG) {
                    Log.d(TAG,"Finished init thread");
                }
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    /**
     * Get the amount of disk space used by the tile cache. This will initially be zero since the
     * used space is calculated in the background.
     *
     * @return size in bytes
     */
    public static long getUsedCacheSpace() {
        return mUsedCacheSpace;
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public boolean saveFile(final ITileSource pTileSource, final MapTile pTile,
            final InputStream pStream) {

        final File file = new File(TILE_PATH_BASE, pTileSource.getTileRelativeFilenameString(pTile)
                + TILE_PATH_EXTENSION);

        final File parent = file.getParentFile();
        if (!parent.exists() && !createFolderAndCheckIfExists(parent)) {
            return false;
        }

        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file.getPath()),
                    StreamUtils.IO_BUFFER_SIZE);
            final long length = StreamUtils.copy(pStream, outputStream);

            mUsedCacheSpace += length;
            if (mUsedCacheSpace > TILE_MAX_CACHE_SIZE_BYTES) {
                cutCurrentCache(); // TODO perhaps we should do this in the background
            }
        } catch (final IOException e) {
            return false;
        } finally {
            if (outputStream != null) {
                StreamUtils.closeStream(outputStream);
            }
        }
        return true;
    }

    // ===========================================================
    // Methods
    // ===========================================================

    private boolean createFolderAndCheckIfExists(final File pFile) {
        if (pFile.mkdirs()) {
            return true;
        }
        if (DBG) {
            Log.d(TAG,"Failed to create " + pFile + " - wait and check again");
        }

        // if create failed, wait a bit in case another thread created it
        try {
            Thread.sleep(500);
        } catch (final InterruptedException ignore) {
        }
        // and then check again
        if (pFile.exists()) {
            if (DBG) {
                Log.d(TAG,"Seems like another thread created " + pFile);
            }
            return true;
        } else {
            if (DBG) {
                Log.d(TAG,"File still doesn't exist: " + pFile);
            }
            return false;
        }
    }

    private void calculateDirectorySize(final File pDirectory) {
        final File[] z = pDirectory.listFiles();
        if (z != null) {
            for (final File file : z) {
                if (file.isFile()) {
                    mUsedCacheSpace += file.length();
                }
                if (file.isDirectory() && !isSymbolicDirectoryLink(pDirectory, file)) {
                    calculateDirectorySize(file); // *** recurse ***
                }
            }
        }
    }

    /**
     * Checks to see if it appears that a directory is a symbolic link. It does this by comparing
     * the canonical path of the parent directory and the parent directory of the directory's
     * canonical path. If they are equal, then they come from the same true parent. If not, then
     * pDirectory is a symbolic link. If we get an exception, we err on the side of caution and
     * return "true" expecting the calculateDirectorySize to now skip further processing since
     * something went goofy.
     */
    private boolean isSymbolicDirectoryLink(final File pParentDirectory, final File pDirectory) {
        try {
            final String canonicalParentPath1 = pParentDirectory.getCanonicalPath();
            final String canonicalParentPath2 = pDirectory.getCanonicalFile().getParent();
            return !canonicalParentPath1.equals(canonicalParentPath2);
        } catch (final IOException e) {
            return true;
        } catch (final NoSuchElementException e) {
            // See: http://code.google.com/p/android/issues/detail?id=4961
            // See: http://code.google.com/p/android/issues/detail?id=5807
            return true;
        }

    }

    private List<File> getDirectoryFileList(final File aDirectory) {
        final List<File> files = new ArrayList<File>();

        final File[] z = aDirectory.listFiles();
        if (z != null) {
            for (final File file : z) {
                if (file.isFile()) {
                    files.add(file);
                }
                if (file.isDirectory()) {
                    files.addAll(getDirectoryFileList(file));
                }
            }
        }

        return files;
    }

    /**
     * If the cache size is greater than the max then trim it down to the trim level. This method is
     * synchronized so that only one thread can run it at a time.
     */
    private void cutCurrentCache() {

        synchronized (TILE_PATH_BASE) {

            if (mUsedCacheSpace > TILE_TRIM_CACHE_SIZE_BYTES) {

                logger.info("Trimming tile cache from " + mUsedCacheSpace + " to "
                        + TILE_TRIM_CACHE_SIZE_BYTES);

                final List<File> z = getDirectoryFileList(TILE_PATH_BASE);

                // order list by files day created from old to new
                final File[] files = z.toArray(new File[0]);
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(final File f1, final File f2) {
                        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                });

                for (final File file : files) {
                    if (mUsedCacheSpace <= TILE_TRIM_CACHE_SIZE_BYTES) {
                        break;
                    }

                    final long length = file.length();
                    if (file.delete()) {
                        mUsedCacheSpace -= length;
                    }
                }

                Log.i(TAG,"Finished trimming tile cache");
            }
        }
    }

}
