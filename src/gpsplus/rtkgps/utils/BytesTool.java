package gpsplus.rtkgps.utils;

public class BytesTool {
    /**
     * Gets the subarray from <tt>array</tt> that starts at <tt>offset</tt>.
     */
    public static byte[] get(byte[] array, int offset) {
        return get(array, offset, array.length - offset);
    }

    /**
     * Gets the subarray of length <tt>length</tt> from <tt>array</tt>
     * that starts at <tt>offset</tt>.
     */
    public static byte[] get(byte[] array, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(array, offset, result, 0, length);
        return result;
    }

}
