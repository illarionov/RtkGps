package gpsplus.rtkgps.utils.ublox;

public class Message {

    private static final int PAYLOAD_POS = 6;
    private static final int SIZE_POS = 4;
    public static final byte SYNC0 = (byte)-75;
    public static final byte SYNC1 = (byte)98;
    private static final byte[] HEADER = new byte[]{SYNC0, SYNC1};
    private byte mMessageClass;
    private byte mMessageId;
    private byte[] mPayload;

    public Message() {
    }

    public static boolean IsChecksumOK(byte[] message, int length) {
        int ck_A = 0;
        int ck_B = 0;
        int i;
        for (i=2; i<length - 2;i++)
        {
            ck_A = ck_A + message[i];
            ck_B = ck_B + ck_A;
        }
        ck_A = ck_A & 0xFF;
        ck_B = ck_B & 0xFF;

        if ( ((message[length-2] & 0xFF)==ck_A) && ((message[length-1] & 0xFF)==ck_B)){
            return true;
        }else{
            return false;
        }

    }
    public static void buildChecksum(byte[] var0, int var1) {
        for(int var2 = 2; var2 < var1 - 2; ++var2) {
            int var3 = var1 - 2;
            var0[var3] += var0[var2];
            var3 = var1 - 1;
            var0[var3] += var0[var1 - 2];
        }

    }

    private static byte[] createMessage(byte var0, byte var1, byte[] var2) {
        byte[] var3 = createMsgBuffer(var0, var1, var2.length);
        System.arraycopy(var2, 0, var3, 6, var2.length);
        buildChecksum(var3, var3.length);
        return var3;
    }

    private static byte[] createMsgBuffer(byte var0, byte var1, int var2) {
        byte[] var3 = new byte[HEADER.length + 2 + 2 + var2 + 2];
        var3[0] = SYNC0;
        var3[1] = SYNC1;
        var3[2] = var0;
        var3[3] = var1;
        var3[4] = (byte)var2;
        return var3;
    }

   /*
    cold start: B5 62 06 04 04 00 FF FF 02 00 0E 61
    warm start: B5 62 06 04 04 00 01 00 02 00 11 6C
    hot start: B5 62 06 04 04 00 00 00 02 00 10 68
    */
    public static byte[] coldStart() {
        return createMessage((byte)6, (byte)4, new byte[]{(byte)0xFF, (byte)0xFF, 2, 0});
    }

    public static byte[] warmStart() {
        return createMessage((byte)6, (byte)4, new byte[]{(byte)1, (byte)0, 2, 0});
    }

    public static byte[] hotStart() {
        return createMessage((byte)6, (byte)4, new byte[]{(byte)0, (byte)0, 2, 0});
    }

    public static byte[] enableSbas() {
        return createMessage((byte)6, (byte)22, new byte[]{1, 1, 3, 0, 0, 0, 0, 0});
    }

    public static byte[] enableSbasCorrection() {
        return createMessage((byte)6, (byte)22, new byte[]{1, 2, 3, 0, 0, 0, 0, 0});
    }

    private static byte[] getEmptyMsg(byte var0, byte var1) {
        byte[] var2 = new byte[]{SYNC0, SYNC1, var0, var1, 0, 0, 0, 0};
        buildChecksum(var2, var2.length);
        return var2;
    }

    public static byte[] getGnssConfig() {
        return getEmptyMsg((byte)6, (byte)62);
    }

    public static byte[] getMeasurementRate() {
        return getEmptyMsg((byte)6, (byte)8);
    }

    public static byte[] getNavEngineConfig() {
        return getEmptyMsg((byte)6, (byte)36);
    }

    public static byte[] getNmeaConfig() {
        return getEmptyMsg((byte)6, (byte)23);
    }

    public static byte[] getPortConfig() {
        return getEmptyMsg((byte)6, (byte)0);
    }

    public static byte[] getSbasConfig() {
        return getEmptyMsg((byte)6, (byte)22);
    }

    public static byte[] getSoftwareVersion() {
        return getEmptyMsg((byte)10, (byte)4);
    }

    public static byte[] pollAidHui() {
        return getEmptyMsg((byte)11, (byte)2);
    }

    public static byte[] setUbloxDynamicMode(Message.DynModel var0) {
        byte[] var1 = new byte[]{-1, -1, 3, 3, 0, 0, 0, 0, 16, 39, 0, 0, 5, 0, -6, 0, -6, 0, 100, 0, 94, 1, 0, 60, 0, 0, 0, 0, -56, 0, 3, 0, 0, 0, 0, 0};
        var1[2] = var0.mVal;
        return createMessage((byte)6, (byte)36, var1);
    }

    public static byte[] setMeasurementRate(int var0) {
        byte[] var1 = new byte[]{SYNC0, SYNC1, 6, 8, 6, 0, 0, 0, 1, 0, 1, 0, 0, 0};
        writeU2(var1, 6, var0);
        buildChecksum(var1, var1.length);
        return var1;
    }

    public static byte[] setMessageRate(byte var0, byte var1, byte var2) {
        byte[] var3 = new byte[]{SYNC0, SYNC1, 6, 1, 3, 0, var0, var1, var2, 0, 0};
        buildChecksum(var3, var3.length);
        return var3;
    }

    public static byte[] setNmeaMessageRate(Message.NmeaMsg var0, byte var1) {
        return setMessageRate((byte)-16, var0.mId, var1);
    }

    private static void writeU2(byte[] var0, int var1, int var2) {
        var0[var1] = (byte)(('\uff00' & var2) >> 8);
        var0[var1 + 1] = (byte)(var2 & 255);
    }

    public static enum DynModel {
        AIRBORANE4g(8),
        AIRBORNE1g(6),
        AIRBORNE2g(7),
        AUTOMOTIVE(4),
        PEDESTRIAN(3),
        STATIONARY(0),
        SEA(5),
        PORTABLE(1);

        private final byte mVal;

        private DynModel(int model) {
            this.mVal = (byte) model;
        }

        public byte getModel() {
            return mVal;
        }

        public static DynModel getModel(int modelCole) {
            switch (modelCole) {
                case 0:
                    return DynModel.PORTABLE;
                case 1:
                    return DynModel.STATIONARY;
                case 3:
                    return DynModel.PEDESTRIAN;
                case 4:
                    return DynModel.AUTOMOTIVE;
                case 6:
                    return DynModel.AIRBORNE1g;
                case 7:
                    return DynModel.AIRBORNE2g;
                case 8:
                    return DynModel.AIRBORANE4g;
            }
            return DynModel.STATIONARY;
        }
    }

    public static enum NmeaMsg {
        DTM(10),
        GBQ(68),
        GBS(9),
        GGA(0),
        GLL(1),
        GLQ(67),
        GNQ(66),
        GNS(13),
        GPQ(64),
        GRS(6),
        GSA(2),
        GST(7),
        GSV(3),
        RMC(4),
        TXT(65),
        VLW(15),
        VTG(5),
        ZDA(8);

        private final byte mId;

        private NmeaMsg(int var3) {
            this.mId = (byte)var3;
        }
    }
}