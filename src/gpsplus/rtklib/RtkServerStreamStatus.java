package gpsplus.rtklib;

import proguard.annotation.Keep;
import android.annotation.SuppressLint;

public class RtkServerStreamStatus {

    public final static int STATE_ERROR = -1;
    public final static int STATE_CLOSE = 0;
    public final static int STATE_WAIT = 1;
    public final static int STATE_CONNECT = 2;
    public final static int STATE_ACTIVE = 3;


    /**
     * Status of streams
     */
    private int mInputRover;

    private int mInputBase;

    private int mInputCorrection;

    private int mOutputSolution1;

    private int mOutputSolution2;

    private int mLogRover;

    private int mLogBase;

    private int mLogCorrection;

    /**
     * status messages
     */
    public String mMsg;

    public RtkServerStreamStatus() {
        clear();
    }

    public void copyTo(RtkServerStreamStatus dst) {
        if (dst == null) throw new IllegalArgumentException();
        dst.setStatus(
                this.mInputRover,
                this.mInputBase,
                this.mInputCorrection,
                this.mOutputSolution1,
                this.mOutputSolution2,
                this.mLogRover,
                this.mLogBase,
                this.mLogCorrection,
                this.mMsg
                );
    }

    public void clear() {
        setStatus(
                STATE_CLOSE,
                STATE_CLOSE,
                STATE_CLOSE,
                STATE_CLOSE,
                STATE_CLOSE,
                STATE_CLOSE,
                STATE_CLOSE,
                STATE_CLOSE,
                ""
                );
    }

    // Used in native code
    @Keep
    void setStatus(int inputStreamRoverStatus,
            int inputStreamBaseStationStatus,
            int inputStreamCorrectionStatus,
            int outputStreamSolution1Status,
            int outputStreamSolution2Status,
            int logStreamRoverStatus,
            int logStreamBaseStationStatus,
            int logStreamCorrectionStatus,
            String msg
            ) {
        this.mInputRover = inputStreamRoverStatus;
        this.mInputBase = inputStreamBaseStationStatus;
        this.mInputCorrection = inputStreamCorrectionStatus;
        this.mOutputSolution1 = outputStreamSolution1Status;
        this.mOutputSolution2 = outputStreamSolution2Status;
        this.mLogRover = logStreamRoverStatus;
        this.mLogBase = logStreamBaseStationStatus;
        this.mLogCorrection = logStreamCorrectionStatus;
        this.mMsg = msg;
    }

    /**
     * @return Input rover status: {@link #STATE_ERROR},
     * {@link STATE_CLOSE}, {@link STATE_WAIT}, {@link STATE_CONNECT},
     * {@link STATE_ACTIVE}
     */
    public int getInputRoverStatus() {
        return mInputRover;
    }

    /**
     * @return Input base status: {@link #STATE_ERROR},
     * {@link STATE_CLOSE}, {@link STATE_WAIT}, {@link STATE_CONNECT},
     * {@link STATE_ACTIVE}
     */
    public int getInputBaseStatus() {
        return mInputBase;
    }

    /**
     * @return Input correction status: {@link #STATE_ERROR},
     * {@link STATE_CLOSE}, {@link STATE_WAIT}, {@link STATE_CONNECT},
     * {@link STATE_ACTIVE}
     */
    public int getInputCorrectionStatus() {
        return mInputCorrection;
    }

    /**
     * @return Output solution 1 status: {@link #STATE_ERROR},
     * {@link STATE_CLOSE}, {@link STATE_WAIT}, {@link STATE_CONNECT},
     * {@link STATE_ACTIVE}
     */
    public int getOutputSolution1Status() {
        return mOutputSolution1;
    }

    /**
     * @return Output solution 2 status: {@link #STATE_ERROR},
     * {@link STATE_CLOSE}, {@link STATE_WAIT}, {@link STATE_CONNECT},
     * {@link STATE_ACTIVE}
     */
    public int getOutputSolution2Status() {
        return mOutputSolution2;
    }

    /**
     * @return Log rover status: {@link #STATE_ERROR},
     * {@link STATE_CLOSE}, {@link STATE_WAIT}, {@link STATE_CONNECT},
     * {@link STATE_ACTIVE}
     */
    public int getLogRoverStatus() {
        return mLogRover;
    }

    /**
     * @return Log base status: {@link #STATE_ERROR},
     * {@link STATE_CLOSE}, {@link STATE_WAIT}, {@link STATE_CONNECT},
     * {@link STATE_ACTIVE}
     */
    public int getLogBaseStatus() {
        return mLogBase;
    }

    /**
     * @return Log correction status: {@link #STATE_ERROR},
     * {@link STATE_CLOSE}, {@link STATE_WAIT}, {@link STATE_CONNECT},
     * {@link STATE_ACTIVE}
     */
    public int getLogCorrectionStatus() {
        return mLogCorrection;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("RtkServerStreamStatus %d%d%d %d%d %d%d%d %s",
                mInputRover,
                mInputBase,
                mInputCorrection,

                mOutputSolution1,
                mOutputSolution2,

                mLogRover,
                mLogBase,
                mLogCorrection,

                mMsg
                );
    }


}
