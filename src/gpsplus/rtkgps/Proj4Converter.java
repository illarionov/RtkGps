package gpsplus.rtkgps;

import org.osgeo.proj4j.BasicCoordinateTransform;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.ProjCoordinate;

public class Proj4Converter {
    public static final String CRS_WGS84      = "EPSG:4326";      // "+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs"; //EPSG:4326
    public static final String CRS_UTM_31     = "EPSG:32631";    //"+proj=utm +zone=31 +ellps=WGS84 +datum=WGS84 +units=m +no_defs"; //EPSG:32631
    public static final String CRS_NAD83      = "EPSG:4269";     //+proj=longlat +ellps=GRS80 +datum=NAD83 +no_defs
    public static final String CRS_RGF93      = "EPSG:2154";     //+proj=lcc +lat_1=49 +lat_2=44 +lat_0=46.5 +lon_0=3 +x_0=700000 +y_0=6600000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs
    public static final String CRS_RGF93_CC43 = "EPSG:3943";
    public static final String CRS_RGF93_CC44 = "EPSG:3944";
    public static final String CRS_RGF93_CC45 = "EPSG:3945";
    public static final String CRS_RGF93_CC46 = "EPSG:3946"; //+proj=lcc +lat_1=45.25 +lat_2=46.75 +lat_0=46 +lon_0=3 +x_0=1700000 +y_0=5200000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs
    public static final String CRS_RGF93_CC47 = "EPSG:3947";
    public static final String CRS_RGF93_CC48 = "EPSG:3948";
    public static final String CRS_RGF93_CC49 = "EPSG:3949";
    public static final String CRS_RGF93_CC50 = "EPSG:3950";

    private CRSFactory factory;
    private CoordinateTransform coordinateTransform;
    private CoordinateReferenceSystem wgs84;
    private CoordinateReferenceSystem destination;
    private ProjCoordinate targetCoordinate;

    public Proj4Converter() {
        factory = new CRSFactory();
        wgs84 = factory.createFromName(CRS_WGS84);
    }

    public ProjCoordinate convert(String destEPSGName, double lat, double lon)
    {
        if (targetCoordinate == null)
        {
            targetCoordinate = new ProjCoordinate();
        }
        if ( (destination == null) || (!destination.getName().equals(destEPSGName)) )
        {
            destination = factory.createFromName(destEPSGName);
            coordinateTransform = new BasicCoordinateTransform(wgs84, destination);
        }
        ProjCoordinate sourcCoordinate = new ProjCoordinate(lon, lat);
        coordinateTransform.transform(sourcCoordinate, targetCoordinate);
        return targetCoordinate;
    }

}
