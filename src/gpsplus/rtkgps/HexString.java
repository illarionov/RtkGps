package gpsplus.rtkgps;

public class HexString {
    byte[] mBytes;
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public HexString(byte[] mBytes) {
        super();
        this.mBytes = mBytes;
    }

    public static String bytesToHex(byte[] bytes) {
        return HexString.bytesToHex(bytes, bytes.length);
    }

    public static String bytesToHex(byte[] bytes, int length) {
        int iLength = 0;
        if (length > bytes.length)
        {
            iLength = bytes.length;
        }else{
            iLength = length;
        }
        char[] hexChars = new char[iLength * 2];
        for ( int j = 0; j < iLength; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public String toString() {
        return HexString.bytesToHex(mBytes);
    }
}
