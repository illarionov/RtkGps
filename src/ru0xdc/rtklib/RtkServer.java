package ru0xdc.rtklib;

public class RtkServer {

	/* mObject is used by native code, do not remove or rename */
	private long mObject;

	public final static int RECEIVER_ROVER = 0;
	public final static int RECEIVER_BASE = 1;
	public final static int RECEIVER_EPHEM = 2;

	/**
	 * Status: {@link RtkServerStreamStatus.STATE_CLOSE},
	 * {@link RtkServerStreamStatus.STATE_WAIT},
	 * {@link RtkServerStreamStatus.STATE_ACTIVE}.
	 */
	private int mStatus;

	private final Solution.SolutionBuffer mSolutionBuffer;

	public RtkServer() {
		_create();
		mStatus = RtkServerStreamStatus.STATE_CLOSE;
		mSolutionBuffer = new Solution.SolutionBuffer();
	}

	public boolean start() {
		final boolean started = _start();
		mStatus = started ? RtkServerStreamStatus.STATE_WAIT : RtkServerStreamStatus.STATE_ERROR;
		return started;
	}

	public void stop() {
		_stop();
		mStatus = RtkServerStreamStatus.STATE_CLOSE;
	}

	public RtkServerStreamStatus getStreamStatus(RtkServerStreamStatus status) {
		if (status == null) status = new RtkServerStreamStatus();
		_getStreamStatus(status);
		// XXX
		if ((mStatus == RtkServerStreamStatus.STATE_WAIT)
				&& (status.inputStreamRoverStatus > RtkServerStreamStatus.STATE_WAIT))
			mStatus = RtkServerStreamStatus.STATE_ACTIVE;
		return status;
	}

	public RtkServerObservationStatus getBaseObservationStatus(RtkServerObservationStatus status) {
		return getObservationStatus(RtkServer.RECEIVER_BASE, status);
	}

	public RtkServerObservationStatus getRoverObservationStatus(RtkServerObservationStatus status) {
		return getObservationStatus(RtkServer.RECEIVER_ROVER, status);
	}

	public RtkServerObservationStatus getEphemObservationStatus(RtkServerObservationStatus status) {
		return getObservationStatus(RtkServer.RECEIVER_EPHEM, status);
	}

	public int getStatus(){
		return mStatus;
	}

	public Solution getLastSolution() {
		_readSolutionBuffer(mSolutionBuffer);
		return mSolutionBuffer.getLastSolution();
	}

	public RtkControlResult getRtkStatus(RtkControlResult dst) {
		if (dst == null) dst = new RtkControlResult();
		_getRtkStatus(dst);
		return dst;
	}

	private RtkServerObservationStatus getObservationStatus(int receiver, RtkServerObservationStatus status) {
		if (status == null) status = new RtkServerObservationStatus();
		_getObservationStatus(receiver, status);
		status.receiver = receiver;
		return status;
	}


	@Override
	protected void finalize() throws Throwable {
		_destroy();
		super.finalize();
	}

	private native boolean _start();

	private native void _stop();

	private native void _create();

	private native void _destroy();

	private native void _getStreamStatus(RtkServerStreamStatus status);

	private native void _getObservationStatus(int receiver, RtkServerObservationStatus status);

	private native void _getRtkStatus(RtkControlResult dst);

	private native void _readSolutionBuffer(Solution.SolutionBuffer dst);

}
