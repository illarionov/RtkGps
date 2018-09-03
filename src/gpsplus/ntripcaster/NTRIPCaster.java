package gpsplus.ntripcaster;

public class NTRIPCaster {
    public native int start(int port, String file);
    public static native String getVersion();
    private native void setApplicationPath(String path);
    private String mApplicationPath = "";

    public NTRIPCaster(String path) {
        this.mApplicationPath = path;
        setApplicationPath(path);
    }
    public native int stop(int force); //0 cleanly try to close everything, 1 brutal will exit(0)

    public native void reset();

}
