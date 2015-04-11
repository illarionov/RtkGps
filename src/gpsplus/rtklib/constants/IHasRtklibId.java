package gpsplus.rtklib.constants;


public interface IHasRtklibId {

    /**
     * @return rtklib.h ID
     */
    public int getRtklibId();

    /**
     * @return Localized name (resource ID)
     */
    public int getNameResId();


}