package ru0xdc.rtkgps;

import static junit.framework.Assert.assertNotNull;

import java.util.Timer;
import java.util.TimerTask;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;

import ru0xdc.rtkgps.view.StreamIndicatorsView;
import ru0xdc.rtklib.RtkCommon;
import ru0xdc.rtklib.RtkCommon.Position3d;
import ru0xdc.rtklib.RtkServerStreamStatus;
import ru0xdc.rtklib.Solution;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

    private static final String MAP_MODE_BING="BingMap";
    private static final String MAP_MODE_BING_AERIAL="Bing aerial";
    private static final String MAP_MODE_BING_ROAD="Bing road";

    private Timer mStreamStatusUpdateTimer;
    private RtkServerStreamStatus mStreamStatus;
    private ResourceProxy mResourceProxy;

    private BingMapTileSource mBingRoadTileSource, mBingAerialTileSource;
    private PathOverlay mPathOverlay;

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
        BingMapTileSource.retrieveBingKey(activity);
        mBingRoadTileSource = new BingMapTileSource(null);
        mBingRoadTileSource.setStyle(BingMapTileSource.IMAGERYSET_ROAD);
        mBingAerialTileSource = new BingMapTileSource(null);
        mBingAerialTileSource.setStyle(BingMapTileSource.IMAGERYSET_AERIAL);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

        mPathOverlay = new PathOverlay(Color.GRAY, inflater.getContext());
        mMapView.getOverlays().add(mPathOverlay);

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_map, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final int checked;

        if (mMapView == null) return;

        final String providerName = getTileSourceName();
        if (MAP_MODE_BING_AERIAL.equals(providerName)) {
            checked = R.id.menu_map_mode_bing_aerial;
        }else if (MAP_MODE_BING_ROAD.equals(providerName)) {
            checked = R.id.menu_map_mode_bing_road;
        }else {
            checked = R.id.menu_map_mode_osm;
        }

        menu.findItem(checked).setChecked(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMapPreferences();
    }

    @Override
    public void onStop() {
        super.onStop();
        mPathOverlay.clearPath();
        mStreamStatusUpdateTimer.cancel();
        mStreamStatusUpdateTimer = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapView = null;
        mPathOverlay = null;
        Views.reset(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final String tileSource;

        switch (item.getItemId()) {
        case R.id.menu_map_mode_osm:
            tileSource = TileSourceFactory.MAPNIK.name();
            break;
        case R.id.menu_map_mode_bing_aerial:
            tileSource = MAP_MODE_BING_AERIAL;
            break;
        case R.id.menu_map_mode_bing_road:
            tileSource = MAP_MODE_BING_ROAD;
            break;
        default:
            return super.onOptionsItemSelected(item);
        }

        setTileSource(tileSource);

        return true;
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
            appendSolutions(rtks.readSolutionBuffer());
        }

        assertNotNull(mStreamStatus.mMsg);

        mStreamIndicatorsView.setStats(mStreamStatus, serverStatus);
    }

    private void saveMapPreferences() {

        getActivity()
            .getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_TITLE_SOURCE, getTileSourceName())
            .putInt(PREFS_SCROLL_X, mMapView.getScrollX())
            .putInt(PREFS_SCROLL_Y, mMapView.getScrollY())
            .putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel())
            .commit();

    }

    private void loadMapPreferences() {
        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        final String tileSourceName = prefs.getString(PREFS_TITLE_SOURCE, TileSourceFactory.DEFAULT_TILE_SOURCE.name());
        setTileSource(tileSourceName);

        mMapView.getController().setZoom(prefs.getInt(PREFS_ZOOM_LEVEL, 1));

        mMapView.scrollTo(
                prefs.getInt(PREFS_SCROLL_X, 0),
                prefs.getInt(PREFS_SCROLL_Y, 0)
                );
    }

    private void setTileSource(String name) {
        ITileSource tileSource;

        if (MAP_MODE_BING_AERIAL.equals(name)) {
            tileSource = mBingAerialTileSource;
        }else if (MAP_MODE_BING_ROAD.equals(name)) {
            tileSource = mBingRoadTileSource;
        }else {
            try {
                tileSource = TileSourceFactory.getTileSource(name);
            }catch(IllegalArgumentException iae) {
                tileSource = TileSourceFactory.MAPNIK;
            }
        }

        if (!tileSource.equals(mMapView.getTileProvider().getTileSource())) {
            mMapView.setTileSource(tileSource);
        }
    }

    private String getTileSourceName() {
        final ITileSource provider = mMapView.getTileProvider().getTileSource();

        if (MAP_MODE_BING.equals(provider.name())) {
            if (BingMapTileSource.IMAGERYSET_ROAD.equals(((BingMapTileSource)provider).getStyle())) {
                return MAP_MODE_BING_ROAD;
            }else {
                return MAP_MODE_BING_AERIAL;
            }
        }else {
            return provider.name();
        }
    }

    private void appendSolutions(Solution solutions[]) {
        for (Solution s: solutions) {
            final Position3d pos = RtkCommon.ecef2pos(s.getPosition());
            mPathOverlay.addPoint((int)(Math.toDegrees(pos.getLat())*1e6),
                    (int)(Math.toDegrees(pos.getLon())*1e6));
        }
    }

}
