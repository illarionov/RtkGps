package gpsplus.rtkgps;


import org.proj4.CRSRegistry;
import org.proj4.PJ;
import org.proj4.PJException;
import org.proj4.ProjCoordinate;

public class Proj4Converter {

    private String mApplicationPath="";
    private PJ sourcePJ;
    private PJ targetPJ;
    private String mCurrentTargetDefinition = null;

    public Proj4Converter() {
        this( MainActivity.getApplicationDirectory() );
    }

    public Proj4Converter(String applicationPath) {
        this.mApplicationPath = applicationPath;
        sourcePJ = new PJ(CRSRegistry.WGS84_LON_LAT, mApplicationPath);
    }

    public ProjCoordinate convert(String proj4DefinitionString, double lat, double lon)
    {
        if ( (mCurrentTargetDefinition == null) || (!proj4DefinitionString.equals(mCurrentTargetDefinition)) ) {
            targetPJ = new PJ(proj4DefinitionString, mApplicationPath);           // (x,y) axis order
            mCurrentTargetDefinition = proj4DefinitionString;
        }

        double[] coordinates = new double[2];
        coordinates[0] = lon;
        coordinates[1] = lat;
        try {
            sourcePJ.transform(targetPJ, 2, coordinates, 0, 1);
        } catch (PJException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new ProjCoordinate(coordinates[0], coordinates[1]);
    }

}
