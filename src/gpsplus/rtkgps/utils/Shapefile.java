package gpsplus.rtkgps.utils;

import android.util.Log;

import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Driver;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

import java.io.File;
import java.util.Vector;

public class Shapefile {
    String mshapefileDirectory = null;
    String mshapefileBaseName = null;
    Driver shapefileDriver = null;
    DataSource ds = null;
    Layer layer = null;
    public static String LAYER_NAME = "RtkGPS_Points";

    public Shapefile(String shapefileDirectory, String shapefileBaseName) {
        mshapefileDirectory = shapefileDirectory;
        mshapefileBaseName = shapefileBaseName;

        if (ogr.GetDriverCount() < 1)
        {
            gdal.AllRegister();
            ogr.RegisterAll();
        }
        File fshapefileDirectory = new File(shapefileDirectory);
        if (!fshapefileDirectory.exists()){
            fshapefileDirectory.mkdirs();
        }
        Vector layerOtions = new Vector();
        layerOtions.add("ENCODING=UTF-8");

        shapefileDriver = ogr.GetDriverByName("ESRI Shapefile");
        ds = shapefileDriver.CreateDataSource(mshapefileDirectory+File.separator+mshapefileBaseName);
        if( ds == null )
        {
            Log.e( "GDAL"," driver failed to create "+ mshapefileDirectory+File.separator+mshapefileBaseName );
            return;
        }
        SpatialReference srs = new SpatialReference(osr.SRS_WKT_WGS84);
        layer = ds.CreateLayer(LAYER_NAME, srs, ogrConstants.wkbPoint25D,layerOtions);
        layer.CreateField(new FieldDefn("name",ogrConstants.OFTString));
        layer.CreateField(new FieldDefn("hdop", ogrConstants.OFTReal));
        layer.CreateField(new FieldDefn("vdop", ogrConstants.OFTReal));
        layer.CreateField(new FieldDefn("gps_week", ogrConstants.OFTInteger64));
        layer.CreateField(new FieldDefn("gps_tow", ogrConstants.OFTReal));
    }

    public boolean addPoint(String name, double lon, double lat, double z, double hdop, double vdop, long gps_week, double gps_tow)
    {
        Geometry point = new Geometry(ogr.wkbPoint);
        point.AddPoint(lon, lat,z);

        if (ds.GetLayerCount() == 0)
            return false;
        FeatureDefn featureDefn = layer.GetLayerDefn();
        Feature feature = new Feature(featureDefn);
        feature.SetField("name",name);
        feature.SetField("hdop", hdop);
        feature.SetField("vdop", vdop);
        feature.SetField("gps_week", gps_week);
        feature.SetField("gps_tow", gps_tow);
        feature.SetGeometry(point);
        int icreateRet = layer.CreateFeature(feature);
        int isyncRet = layer.SyncToDisk();
        if ((icreateRet+isyncRet) == 0)
            return true;
        else
            return false;
    }

    public void close(){
        layer.SyncToDisk();
        if (ds != null)
            ds.delete();
    }
}
