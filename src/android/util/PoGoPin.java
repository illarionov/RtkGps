package android.util;


public class PoGoPin
{
    public static native int closeBeidou();

    public static native int closeUart();

    public static native int exist();

    public static native int ioctlDevice(int paramInt);

    public static native int openBeidou(String devicePath); // /dev/ttyHSL1

    public static native int openUart(String paramString);

    public static native int readDevice(byte[] paramArrayOfByte, int paramInt);

    public static native int readRawData(byte[] paramArrayOfByte, int paramInt);

    public static native int switchDevice(int paramInt);

    public static native int switchRawData(int paramInt);

    public static native int writeDevice(byte[] paramArrayOfByte, int paramInt);
}
