package ru0xdc.rtkgps;

import static junit.framework.Assert.assertNotNull;

import java.util.Timer;
import java.util.TimerTask;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import ru0xdc.rtkgps.view.StreamIndicatorsView;
import ru0xdc.rtklib.RtkServerStreamStatus;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.InjectView;
import butterknife.Views;

public class MapFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = MapFragment.class.getSimpleName();

    private static final String SHARED_PREFS_NAME = "map";
    private static final String PREFS_TITLE_SOURCE = "title_source";
    private static final String PREFS_SCROLL_X = "scroll_x";
    private static final String PREFS_SCROLL_Y = "scroll_y";
    private static final String PREFS_ZOOM_LEVEL = "zoom_level";

    private Timer mStreamStatusUpdateTimer;
    private RtkServerStreamStatus mStreamStatus;
    private ResourceProxy mResourceProxy;

    @InjectView(R.id.streamIndicatorsView) StreamIndicatorsView mStreamIndicatorsView;
    @InjectView(R.id.map_container) ViewGroup mMapViewContainer;
    private MapView mMapView;


    public MapFragment() {
        mStreamStatus = new RtkServerStreamStatus();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mResourceProxy = new ResourceProxyImpl(activity.getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_map, container, false);
        Views.inject(this, v);

        mResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
        mMapView = new MapView(inflater.getContext(), 256, mResourceProxy);
        mMapView.setUseSafeCanvas(true);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mMapViewContainer.addView(mMapView, 0);

        return v;
    }


    @Override
    public void onStart() {
        super.onStart();

        // XXX
        mStreamStatusUpdateTimer = new Timer();
        mStreamStatusUpdateTimer.scheduleAtFixedRate(
                new TimerTask() {
                    Runnable updateStatusRunnable = new Runnable() {
                        @Override
                        public void run() {
                            MapFragment.this.updateStatus();
                        }
                    };
                    @Override
                    public void run() {
                        Activity a = getActivity();
                        if (a == null) return;
                        a.runOnUiThread(updateStatusRunnable);
                    }
                }, 200, 250);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveMapPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMapPreferences();
    }

    @Override
    public void onStop() {
        super.onStop();
        mStreamStatusUpdateTimer.cancel();
        mStreamStatusUpdateTimer = null;

    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapView = null;
        Views.reset(this);
    }

    void updateStatus() {
        MainActivity ma;
        RtkNaviService rtks;
        int serverStatus;

        // XXX
        ma = (MainActivity)getActivity();

        if (ma == null) return;

        rtks = ma.getRtkService();
        if (rtks == null) {
            serverStatus = RtkServerStreamStatus.STATE_CLOSE;
            mStreamStatus.clear();
        }else {
            rtks.getStreamStatus(mStreamStatus);
            serverStatus = rtks.getServerStatus();
        }

        assertNotNull(mStreamStatus.mMsg);

        mStreamIndicatorsView.setStats(mStreamStatus, serverStatus);
    }

    private void saveMapPreferences() {

        getActivity()
            .getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_TITLE_SOURCE, mMapView.getTileProvider().getTileSource().name())
            .putInt(PREFS_SCROLL_X, mMapView.getScrollX())
            .putInt(PREFS_SCROLL_Y, mMapView.getScrollY())
            .putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel())
            .commit();

    }

    private void loadMapPreferences() {
        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        final String tileSourceName = prefs.getString(PREFS_TITLE_SOURCE, TileSourceFactory.DEFAULT_TILE_SOURCE.name());
        try {
            final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
            mMapView.setTileSource(tileSource);
        } catch (final IllegalArgumentException ignore) {
        }

        mMapView.getController().setZoom(prefs.getInt(PREFS_ZOOM_LEVEL, 1));

        mMapView.scrollTo(
                prefs.getInt(PREFS_SCROLL_X, 0),
                prefs.getInt(PREFS_SCROLL_Y, 0)
                );
    }

}
