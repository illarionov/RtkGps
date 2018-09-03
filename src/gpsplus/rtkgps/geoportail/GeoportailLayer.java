package gpsplus.rtkgps.geoportail;

public enum GeoportailLayer {
    CADASTRALPARCELS(0,"CADASTRALPARCELS.PARCELS","image/png",".png",6,20),
    MAPS(1,"GEOGRAPHICALGRIDSYSTEMS.MAPS","image/jpeg",".png",3,18),
    ORTHOIMAGE(2,"ORTHOIMAGERY.ORTHOPHOTOS","image/jpeg",".jpg",3,19);

    private final int mId;
    private final String mLayer;
    private final String mFormat;
    private final String mFilenameEnding;
    private final int mMinimalZoom;
    private final int mMaximalZoom;

    private GeoportailLayer(int id,String layer,String format, String filenameEnding, int minimalZoom, int maximalZoom)
    {
        mId = id;
        mLayer = layer;
        mFormat = format;
        mFilenameEnding = filenameEnding;
        mMinimalZoom = minimalZoom;
        mMaximalZoom = maximalZoom;
    }

    public int getId() {
        return mId;
    }

    public String getLayer() {
        return mLayer;
    }

    public String getFormat() {
        return mFormat;
    }

    public String getFilenameEnding() {
        return mFilenameEnding;
    }

    public int getMinimalZoom() {
        return mMinimalZoom;
    }

    public int getMaximalZoom() {
        return mMaximalZoom;
    }

}

