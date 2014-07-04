package gpsplus.rtklib;

import static junit.framework.Assert.assertNotNull;

import android.text.TextUtils;

import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.constants.PositioningMode;
import gpsplus.rtklib.constants.SolutionFormat;
import gpsplus.rtklib.constants.StationPositionType;
import gpsplus.rtklib.constants.StreamFormat;
import gpsplus.rtklib.constants.StreamType;

import javax.annotation.Nonnull;

public class RtkServerSettings {

    public static class InputStream {

        private TransportSettings mTransport;

        private StreamFormat mFormat;

        private String mCommandsAtStartup;

        private String mCommandsAtShutdown;

        private String mReceiverOption;

        /** nmea request type (0:no,1:base pos,2:single sol) */
        private int mNmeaRequestType;

        /** transmitted nmea position (ecef) X (m) */
        private final Position3d mTransmittedPos;

        /** station position type */
        private StationPositionType mStationPosType;

        /** Station positon  for fixed/relative mode */
        private final Position3d mStationPos;

        public InputStream() {
            mTransport = TRANSPORT_DUMMY;
            mFormat = StreamFormat.RINEX;
            mCommandsAtStartup = "";
            mCommandsAtShutdown = "";
            mReceiverOption = "";
            mNmeaRequestType = 0;
            mTransmittedPos = new Position3d();
            mStationPosType = StationPositionType.RTCM_POS;
            mStationPos = new Position3d();
        }

        public InputStream(@Nonnull InputStream src) {
            this();
            setValue(src);
        }

        public void setValue(@Nonnull InputStream src) {
            mTransport = src.mTransport.copy();
            mFormat = src.mFormat;
            mCommandsAtStartup = src.mCommandsAtStartup;
            mCommandsAtShutdown = src.mCommandsAtShutdown;
            mReceiverOption = src.mReceiverOption;
            mNmeaRequestType = src.mNmeaRequestType;
            mTransmittedPos.setValues(src.mTransmittedPos);
            mStationPosType = src.mStationPosType;
            mStationPos.setValues(src.mStationPos);
        }

        public StreamType getType() {
            return mTransport.getType();
        }

        public StreamFormat getFormat() {
            return mFormat;
        }

        public InputStream setFormat(StreamFormat format) {
            mFormat = format;
            return this;
        }

        @Nonnull
        public String getPath() {
            return mTransport.getPath();
        }

        public InputStream setTransportSettings(TransportSettings transport) {
            mTransport = transport.copy();
            return this;
        }

        public TransportSettings getTransportSettings() {
            return mTransport.copy();
        }

        @Nonnull
        public String getCommandsAtStartup() {
            return mCommandsAtStartup;
        }

        public InputStream setCommandsAtStartup(boolean send, @Nonnull String cmds) {
            if (cmds == null) throw new NullPointerException();
            mCommandsAtStartup = !send || TextUtils.isEmpty(cmds) ? "" : cmds;
            return this;
        }

        @Nonnull
        public String getCommandsAtShutdown() {
            return mCommandsAtShutdown;
        }

        public InputStream setCommandsAtShutdown(boolean send, @Nonnull String cmds) {
            if (cmds == null) throw new NullPointerException();
            mCommandsAtShutdown = !send || TextUtils.isEmpty(cmds) ? "" : cmds;
            return this;
        }

        @Nonnull
        public String getReceiverOption() {
            return mReceiverOption;
        }

        public InputStream setReceiverOption(@Nonnull String option) {
            if (option == null) throw new NullPointerException();
            mReceiverOption = option;
            return this;
        }

        /**
         *
         * @param type nmea request type (0:no,1:base pos,2:single sol)
         * @param pos transmitted nmea position (ecef)
         */
        public InputStream setTransmitNmeaPosition(int type, Position3d pos) {
            mNmeaRequestType = type;
            mTransmittedPos.setValues(pos);
            return this;
        }


        public int getNmeaRequestType() {
            return mNmeaRequestType;
        }

        public Position3d getTransmittedPosition() {
            return new Position3d(mTransmittedPos);
        }

        /**
         *
         * @param type position type
         * @param ecefPos
         * @return
         */
        public InputStream setStationPos(StationPositionType type, Position3d ecefPos) {
            mStationPosType = type;
            mStationPos.setValues(ecefPos);
            return this;
        }

        public StationPositionType getStationPosType() {
            return mStationPosType;
        }

        public Position3d getStationPosition() {
            return new Position3d(mStationPos);
        }

    }

    public static class OutputStream {

        private TransportSettings mTransport;

        private SolutionOptions mOptions;

        public OutputStream() {
            mTransport = TRANSPORT_DUMMY;
            mOptions = new SolutionOptions();
        }

        public void setValue(OutputStream src) {
            mTransport = src.mTransport.copy();
            mOptions.setValues(src.mOptions);
        }

        public OutputStream setTransportSettings(TransportSettings transport) {
            mTransport = transport.copy();
            return this;
        }

        public TransportSettings getTransportSettings() {
            return mTransport.copy();
        }

        public StreamType getType() {
            return mTransport.getType();
        }

        @Nonnull
        public String getPath() {
            return mTransport.getPath();
        }

        public OutputStream setSolutionOptions(SolutionOptions opts) {
            if (opts == null) throw new NullPointerException();
            mOptions.setValues(opts);
            return this;
        }

        public OutputStream setSolutionFormat(SolutionFormat format) {
            mOptions.setSolutionFormat(format);
            return this;
        }

    }

    public static class LogStream {

        private TransportSettings mTransport;

        public LogStream() {
            mTransport = TRANSPORT_DUMMY;
        }

        public void setValue(LogStream src) {
            mTransport = src.mTransport.copy();
        }

        public LogStream setTransportSettings(TransportSettings transport) {
            mTransport = transport.copy();
            return this;
        }

        public TransportSettings getTransportSettings() {
            return mTransport.copy();
        }

        public StreamType getType() {
            return mTransport.getType();
        }

        @Nonnull
        public String getPath() {
            return mTransport.getPath();
        }

    }

    public abstract interface TransportSettings {

        public StreamType getType();

        public String getPath();

        public TransportSettings copy();

    }

    public final static TransportSettings TRANSPORT_DUMMY = new TransportSettings() {

        @Override
        public StreamType getType() {
            return StreamType.NONE;
        }

        @Override
        public String getPath() {
            return "";
        }

        @Override
        public TransportSettings copy() {
            return this;
        }

    };


    private final static int DEFAULT_SERVER_CYCLE_MS = 10;

    private final static int DEFAULT_BUFFER_SIZE = 32768;

    private final static int DEFAULT_NMEA_REQUEST_CYCLE = 1000;


    /** server cycle (ms) */
    private int mServerCycleMs;

    /** input buffer size (bytes) */
    private int mBufferSize;

    /** navigation message select (0:rover,1:base,2:ephem,3:all) */
    private int mNavMessageSelect;

    /** nmea request cycle (ms) (0:no request) */
    private int mNmeaRequestCycle;

    private final ProcessingOptions mProcessingOptions;

    private final InputStream mInputRover;

    private final InputStream mInputBase;

    private final InputStream mInputCorrection;

    private final OutputStream mOutputSolution1;

    private final OutputStream mOutputSolution2;

    private final LogStream mLogRover;

    private final LogStream mLogBase;

    private final LogStream mLogCorrection;

    public RtkServerSettings() {
        mServerCycleMs = DEFAULT_SERVER_CYCLE_MS;
        mBufferSize = DEFAULT_BUFFER_SIZE;
        mNavMessageSelect = 0;
        mNmeaRequestCycle = DEFAULT_NMEA_REQUEST_CYCLE;
        mProcessingOptions = new ProcessingOptions();
        mProcessingOptions.setPositioningMode(PositioningMode.PPP_STATIC);
        mInputRover = new InputStream();
        mInputBase = new InputStream();
        mInputCorrection = new InputStream();
        mOutputSolution1 = new OutputStream();
        mOutputSolution2 = new OutputStream();
        mLogRover = new LogStream();
        mLogBase = new LogStream();
        mLogCorrection = new LogStream();
    }

    public void setValues(RtkServerSettings src) {
        mServerCycleMs = src.mServerCycleMs;
        mBufferSize = src.mBufferSize;
        mNavMessageSelect = src.mNavMessageSelect;
        mNmeaRequestCycle = src.mNmeaRequestCycle;
        mProcessingOptions.setValues(src.mProcessingOptions);
        mInputRover.setValue(src.mInputRover);
        mInputBase.setValue(src.mInputBase);
        mInputCorrection.setValue(src.mInputCorrection);
        mOutputSolution1.setValue(src.mOutputSolution1);
        mOutputSolution2.setValue(src.mOutputSolution2);
        mLogRover.setValue(src.mLogRover);
        mLogBase.setValue(src.mLogBase);
        mLogCorrection.setValue(src.mLogCorrection);
    }

    public int getServerCycleMs() {
        return mServerCycleMs;
    }

    public int getBufferSize() {
        return mBufferSize;
    }

    int[] getStreamTypes() {
        final int[] types = new int[8];
        types[0] = mInputRover.getType().getRtklibId();
        types[1] = mInputBase.getType().getRtklibId();
        types[2] = mInputCorrection.getType().getRtklibId();
        types[3] = mOutputSolution1.getType().getRtklibId();
        types[4] = mOutputSolution2.getType().getRtklibId();
        types[5] = mLogRover.getType().getRtklibId();
        types[6] = mLogBase.getType().getRtklibId();
        types[7] = mLogCorrection.getType().getRtklibId();

        return types;
    }

    @Nonnull
    String[] getStreamPaths() {
        final String[] paths = new String[8];
        paths[0] = mInputRover.getPath();
        paths[1] = mInputBase.getPath();
        paths[2] = mInputCorrection.getPath();
        paths[3] = mOutputSolution1.getPath();
        paths[4] = mOutputSolution2.getPath();
        paths[5] = mLogRover.getPath();
        paths[6] = mLogBase.getPath();
        paths[7] = mLogCorrection.getPath();

        for (int i=0; i<8; ++i) assertNotNull(paths[i]);

        return paths;
    }

    int[] getInputStreamFormats() {
        final int[] formats = new int[3];
        formats[0] = mInputRover.mFormat.getRtklibId();
        formats[1] = mInputBase.mFormat.getRtklibId();
        formats[2] = mInputCorrection.mFormat.getRtklibId();
        return formats;
    }

    String[] getInputStreamCommandsAtStartup() {
        final String[] commands = new String[3];
        commands[0] = TextUtils.isEmpty(mInputRover.mCommandsAtStartup) ? null :
            mInputRover.mCommandsAtStartup.replaceAll("\r?\\n", "\r\n");
        commands[1] = TextUtils.isEmpty(mInputBase.mCommandsAtStartup) ? null :
            mInputBase.mCommandsAtStartup.replaceAll("\r?\\n", "\r\n");
        commands[2] = TextUtils.isEmpty(mInputCorrection.mCommandsAtStartup) ? null
                : mInputCorrection.mCommandsAtStartup.replaceAll("\r?\\n", "\r\n");

        return commands;
    }

    String[] getInputStreamCommandsAtShutdown() {
        final String[] commands = new String[3];
        commands[0] = TextUtils.isEmpty(mInputRover.mCommandsAtShutdown) ? null :
            mInputRover.mCommandsAtShutdown.replaceAll("\r?\\n", "\r\n");
        commands[1] = TextUtils.isEmpty(mInputBase.mCommandsAtShutdown) ? null :
            mInputBase.mCommandsAtShutdown.replaceAll("\r?\\n", "\r\n");
        commands[2] = TextUtils.isEmpty(mInputCorrection.mCommandsAtShutdown) ? null
                : mInputCorrection.mCommandsAtShutdown.replaceAll("\r?\\n", "\r\n");

        return commands;
    }


    @Nonnull
    String[] getInputStreamReceiverOption() {
        final String[] option = new String[3];
        option[0] = mInputRover.mReceiverOption;
        option[1] = mInputBase.mReceiverOption;
        option[2] = mInputCorrection.mReceiverOption;
        assertNotNull(option[0]);
        assertNotNull(option[1]);
        assertNotNull(option[2]);
        return option;
    }

    int getNavMessageSelect() {
        return mNavMessageSelect;
    }

    int getNmeaRequestCycle() {
        return mNmeaRequestCycle;
    }

    public RtkServerSettings setProcessingOptions(ProcessingOptions opts) {
        mProcessingOptions.setValues(opts);
        return this;
    }

    @Nonnull
    ProcessingOptions getProcessingOptions() {
        return mProcessingOptions;
    }

    SolutionOptions getSolutionOptions1() {
        return mOutputSolution1.mOptions;
    }

    SolutionOptions getSolutionOptions2() {
        return mOutputSolution2.mOptions;
    }

    /** nmea request type (0:no,1:base pos,2:single sol) */
    public int getNmeaRequestType() {
        return mInputBase.mNmeaRequestType;
    }

    public Position3d getTransmittedPos() {
        return mInputBase.mTransmittedPos;
    }

    @Nonnull
    public final InputStream getInputRover() {
        return mInputRover;
    }

    public RtkServerSettings setInputRover(@Nonnull InputStream rover) {
        mInputRover.setValue(rover);
        mProcessingOptions.setRoverPosition(rover.mStationPosType, rover.mStationPos);
        return this;
    }

    @Nonnull
    public final InputStream getInputBase() {
        return mInputBase;
    }

    public RtkServerSettings setInputBase(@Nonnull InputStream base) {
        mInputBase.setValue(base);
        mProcessingOptions.setBasePosition(base.mStationPosType, base.mStationPos);
        return this;
    }

    @Nonnull
    public final InputStream getInputCorrection() {
        return mInputCorrection;
    }

    public RtkServerSettings setInputCorrection(@Nonnull InputStream correction) {
        mInputCorrection.setValue(correction);
        return this;
    }

    @Nonnull
    public final OutputStream getOutputSolution1() {
        return mOutputSolution1;
    }

    public RtkServerSettings setOutputSolution1(@Nonnull OutputStream solution1) {
        mOutputSolution1.setValue(solution1);
        return this;
    }

    @Nonnull
    public final OutputStream getOutputSolution2() {
        return mOutputSolution2;
    }

    public RtkServerSettings setOutputSolution2(@Nonnull OutputStream solution2) {
        mOutputSolution2.setValue(solution2);
        return this;
    }

    @Nonnull
    public final LogStream getLogRover() {
        return mLogRover;
    }

    public RtkServerSettings setLogRover(@Nonnull LogStream rover) {
        mLogRover.setValue(rover);
        return this;
    }

    @Nonnull
    public final LogStream getLogBase() {
        return mLogBase;
    }

    public RtkServerSettings setLogBase(@Nonnull LogStream base) {
        mLogBase.setValue(base);
        return this;
    }

    @Nonnull
    public final LogStream getLogCorrection() {
        return mLogCorrection;
    }

    public RtkServerSettings setLogCorrection(@Nonnull LogStream correction) {
        mLogCorrection.setValue(correction);
        return this;
    }

}
