package gpsplus.rtkgps.geoportail;

import org.osmdroid.ResourceProxy.string;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;


public class GeoportailWMTSTileSource extends OnlineTileSourceBase {

    private GeoportailLayer mLayer;
   // private static String baseUrl[] = {"https://wxs.ign.fr/"+License.KEY+"/geoportail/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&STYLE=normal&TILEMATRIXSET=PM"  };

    public GeoportailWMTSTileSource(final string aResourceId,
                                GeoportailLayer layer) {
        super(layer.getLayer(), aResourceId, layer.getMinimalZoom(), layer.getMaximalZoom(), 256,
                layer.getFilenameEnding(), GeoportailWMTSTileSource.getUrl());

        mLayer = layer;
    }


    private  static String[] getUrl() {
        return new String[]{"https://wxs.ign.fr/"+License.getKey()+"/geoportail/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&STYLE=normal&TILEMATRIXSET=PM"
        };
    }

    @Override
    public String getTileURLString(MapTile aTile) {
        StringBuffer tileURLString = new StringBuffer();
        tileURLString.append(GeoportailWMTSTileSource.getUrl()[0]);
        tileURLString.append("&LAYER="+mLayer.getLayer());
        tileURLString.append("&FORMAT="+mLayer.getFormat());
        tileURLString.append("&TILEMATRIX="+aTile.getZoomLevel());
        tileURLString.append("&TILECOL="+aTile.getX());
        tileURLString.append("&TILEROW="+aTile.getY());

        return tileURLString.toString();
    }

}
