package ru0xdc.rtklib;

public class RtkServer {

	/* mObject is used by native code, do not remove or rename */
	private long mObject;

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


	@Override
	protected void finalize() throws Throwable {
		_destroy();
		super.finalize();
	}

	private native void _create();

	private native void _destroy();

	private native void _getStreamStatus(RtkServerStreamStatus status);

}
