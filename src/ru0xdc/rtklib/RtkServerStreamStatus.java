package ru0xdc.rtklib;

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
    public int inputStreamRoverStatus;

    public int inputStreamBaseStationStatus;

    public int inputStreamCorrectionStatus;

    public int outputStreamSolution1Status;

    public int outputStreamSolution2Status;

    public int logStreamRoverStatus;

    public int logStreamBaseStationStatus;

    public int logStreamCorrectionStatus;

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
                this.inputStreamRoverStatus,
                this.inputStreamBaseStationStatus,
                this.inputStreamCorrectionStatus,
                this.outputStreamSolution1Status,
                this.outputStreamSolution2Status,
                this.logStreamRoverStatus,
                this.logStreamBaseStationStatus,
                this.logStreamCorrectionStatus,
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
        this.inputStreamRoverStatus = inputStreamRoverStatus;
        this.inputStreamBaseStationStatus = inputStreamBaseStationStatus;
        this.inputStreamCorrectionStatus = inputStreamCorrectionStatus;
        this.outputStreamSolution1Status = outputStreamSolution1Status;
        this.outputStreamSolution2Status = outputStreamSolution2Status;
        this.logStreamRoverStatus = logStreamRoverStatus;
        this.logStreamBaseStationStatus = logStreamBaseStationStatus;
        this.logStreamCorrectionStatus = logStreamCorrectionStatus;
        this.mMsg = msg;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("RtkServerStreamStatus %d%d%d %d%d %d%d%d %s",
                inputStreamRoverStatus,
                inputStreamBaseStationStatus,
                inputStreamCorrectionStatus,

                outputStreamSolution1Status,
                outputStreamSolution2Status,

                logStreamRoverStatus,
                logStreamBaseStationStatus,
                logStreamCorrectionStatus,

                mMsg
                );
    }


}
