package ru0xdc.rtklib;

import static junit.framework.Assert.assertNotNull;

import javax.annotation.Nonnull;

import ru0xdc.rtklib.RtkCommon.Position3d;
import ru0xdc.rtklib.constants.PositioningMode;
import ru0xdc.rtklib.constants.SolutionFormat;
import ru0xdc.rtklib.constants.StreamFormat;
import ru0xdc.rtklib.constants.StreamType;
import android.text.TextUtils;

public class RtkServerSettings {

    public static class InputStream {

        private StreamType mType;

        private String mPath;

        private StreamFormat mFormat;

        private String mCommandsAtStartup;

        private String mReceiverOption;

        /** nmea request type (0:no,1:base pos,2:single sol) */
        private int mNmeaRequestType;

        /** transmitted nmea position (ecef) X (m) */
        private final Position3d mTransmittedPos;

        public InputStream() {
            mType = StreamType.NONE;
            mPath = "";
            mFormat = StreamFormat.RINEX;
            mCommandsAtStartup = "";
            mReceiverOption = "";
            mNmeaRequestType = 0;
            mTransmittedPos = new Position3d();
        }

        public InputStream(StreamType type, @Nonnull String path, StreamFormat format) {
            this();
            setType(type).setPath(path).setFormat(format);
        }

        public InputStream(@Nonnull InputStream src) {
            this();
            setValue(src);
        }

        public void setValue(@Nonnull InputStream src) {
            mType = src.mType;
            mPath = src.mPath;
            mFormat = src.mFormat;
            mCommandsAtStartup = src.mCommandsAtStartup;
            mReceiverOption = src.mReceiverOption;
            mNmeaRequestType = src.mNmeaRequestType;
            mTransmittedPos.setValues(src.mTransmittedPos);
        }

        public InputStream setType(StreamType type) {
            mType = type;
            return this;
        }

        public InputStream setFormat(StreamFormat format) {
            mFormat = format;
            return this;
        }

        public InputStream setPath(@Nonnull String path) {
            if (path == null) throw new NullPointerException();
            mPath = path;
            return this;
        }

        public InputStream setCommandsAtStartup(@Nonnull String cmds) {
            if (cmds == null) throw new NullPointerException();
            mCommandsAtStartup = TextUtils.isEmpty(cmds) ? "" : cmds;
            return this;
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

    }

    public static class OutputStream {
        private StreamType mType;

        private String mPath;

        private SolutionOptions mOptions;

        public OutputStream() {
            mType = StreamType.NONE;
            mPath = "";
            mOptions = new SolutionOptions();
        }

        public void setValue(OutputStream src) {
            mType = src.mType;
            mPath = src.mPath;
            mOptions.setValues(src.mOptions);
        }

        public OutputStream setType(StreamType type) {
            mType = type;
            return this;
        }

        public OutputStream setPath(@Nonnull String path) {
            if (path == null) throw new NullPointerException();
            mPath = path;
            return this;
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

        private StreamType mType;

        private String mPath;

        public LogStream() {
            mType = StreamType.NONE;
            mPath = "";
        }

        public void setValue(LogStream src) {
            mType = src.mType;
            mPath = src.mPath;
        }

        public LogStream setType(StreamType type) {
            mType = type;
            return this;
        }

        public LogStream setPath(@Nonnull String path) {
            mPath = path;
            return this;
        }
    }

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
        types[0] = mInputRover.mType.getRtklibId();
        types[1] = mInputBase.mType.getRtklibId();
        types[2] = mInputCorrection.mType.getRtklibId();
        types[3] = mOutputSolution1.mType.getRtklibId();
        types[4] = mOutputSolution2.mType.getRtklibId();
        types[5] = mLogRover.mType.getRtklibId();
        types[6] = mLogBase.mType.getRtklibId();
        types[7] = mLogCorrection.mType.getRtklibId();

        return types;
    }

    @Nonnull
    String[] getStreamPaths() {
        final String[] paths = new String[8];
        paths[0] = mInputRover.mPath;
        paths[1] = mInputBase.mPath;
        paths[2] = mInputCorrection.mPath;
        paths[3] = mOutputSolution1.mPath;
        paths[4] = mOutputSolution2.mPath;
        paths[5] = mLogRover.mPath;
        paths[6] = mLogBase.mPath;
        paths[7] = mLogCorrection.mPath;

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
        commands[0] = TextUtils.isEmpty(mInputRover.mCommandsAtStartup) ? null : mInputRover.mCommandsAtStartup;
        commands[1] = TextUtils.isEmpty(mInputBase.mCommandsAtStartup) ? null : mInputBase.mCommandsAtStartup;
        commands[2] = TextUtils.isEmpty(mInputCorrection.mCommandsAtStartup) ? null : mInputCorrection.mCommandsAtStartup;
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

    public RtkServerSettings setInputRover(InputStream rover) {
        mInputRover.setValue(rover);
        return this;
    }

    public RtkServerSettings setInputBase(InputStream base) {
        mInputBase.setValue(base);
        return this;
    }

    public RtkServerSettings setInputCorrection(InputStream correction) {
        mInputBase.setValue(correction);
        return this;
    }

    public RtkServerSettings setOutputSolution1(OutputStream solution1) {
        mOutputSolution1.setValue(solution1);
        return this;
    }

    public RtkServerSettings setOutputSolution2(OutputStream solution2) {
        mOutputSolution2.setValue(solution2);
        return this;
    }

    public RtkServerSettings setLogRover(LogStream rover) {
        mLogRover.setValue(rover);
        return this;
    }

    public RtkServerSettings setLogBase(LogStream base) {
        mLogBase.setValue(base);
        return this;
    }

    public RtkServerSettings setLogCorrection(LogStream correction) {
        mLogCorrection.setValue(correction);
        return this;
    }

}
