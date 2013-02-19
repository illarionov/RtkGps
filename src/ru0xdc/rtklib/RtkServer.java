package ru0xdc.rtklib;

public class RtkServer {

	/* mObject is used by native code, do not remove or rename */
	private long mObject;

	public final static int RECEIVER_ROVER = 0;
	public final static int RECEIVER_BASE = 1;
	public final static int RECEIVER_EPHEM = 2;

	public RtkServer() {
		_create();
	}

	public native boolean start();

	public native void stop();


	public RtkServerStreamStatus getStreamStatus(RtkServerStreamStatus status) {
		if (status == null) status = new RtkServerStreamStatus();
		_getStreamStatus(status);
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

	private native void _create();

	private native void _destroy();

	private native void _getStreamStatus(RtkServerStreamStatus status);

	private native void _getObservationStatus(int receiver, RtkServerObservationStatus status);

}
