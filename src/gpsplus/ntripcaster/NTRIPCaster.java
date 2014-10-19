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
    public native void stop();

    public native void reset();

}
