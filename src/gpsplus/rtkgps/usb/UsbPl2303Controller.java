package gpsplus.rtkgps.usb;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.util.Log;

import gpsplus.rtkgps.BuildConfig;

import java.io.InputStream;
import java.io.OutputStream;

@TargetApi(12)
public class UsbPl2303Controller extends UsbSerialController {

	// Debugging
	private static final String TAG = UsbPl2303Controller.class.getSimpleName();
	private static final boolean D = BuildConfig.DEBUG & true;

	private static final int PL2303_INIT_TIMEOUT_MS = 2000;

	private UsbDeviceConnection mUsbConnection;
	private android.hardware.usb.UsbInterface mUsbInterfaces[];
	private UsbEndpoint mInterruptEndpoint = null;
	private UsbEndpoint mBulkInEndpoint = null;
	private UsbEndpoint mBulkOutEndpoint = null;
	private final SerialLineConfiguration mSerialLineConfiguration;
	private boolean isPl2303Hx;

	private UsbSerialInputStream inputStream = null;
	private UsbSerialOutputStream outputStream = null;
	private UsbSerialInterruptListener interruptListener = null;


	public UsbPl2303Controller(UsbManager usbManager, UsbDevice usbDevice, Context parentContext)
			throws UsbControllerException {
		super(usbManager, usbDevice, parentContext);

		int endpointCount;

		if (!UsbPl2303Controller.probe(usbDevice)) {
			throw new UsbControllerException("probe() failed");
		}

		if (usbDevice.getInterfaceCount() != 1) {
			throw new UsbControllerException("getInterfaceCount() != 1");
		}

		mUsbInterfaces = new android.hardware.usb.UsbInterface[1];
		mUsbInterfaces[0] = usbDevice.getInterface(0);
		endpointCount = mUsbInterfaces[0].getEndpointCount();
		if (endpointCount != 3) {
			throw new UsbControllerException("getEndpointCount() != 3 (" + endpointCount + ")");
		}

		for (int i=0; i < endpointCount; i++) {
			UsbEndpoint e = mUsbInterfaces[0].getEndpoint(i);
			switch (e.getType()) {
			case UsbConstants.USB_ENDPOINT_XFER_INT:
				mInterruptEndpoint = e;
				break;
			case UsbConstants.USB_ENDPOINT_XFER_BULK:
				if (e.getDirection() == UsbConstants.USB_DIR_IN)
					mBulkInEndpoint = e;
				else
					mBulkOutEndpoint = e;
				break;
			default:
				throw new UsbControllerException("Unexpected endpoint " + i + "type = " + e.getType());
			}
		}

		if (mInterruptEndpoint == null) {
			throw new UsbControllerException("Interrupt input endpoint not found");
		}else if (mBulkInEndpoint == null) {
			throw new UsbControllerException("Bulk data input endpoint not found");
		}else if (mBulkOutEndpoint == null) {
			throw new UsbControllerException("Bulk data output endpoint not found");
		}

		isPl2303Hx = (usbDevice.getDeviceClass() != 0x02)
				&& (mBulkOutEndpoint.getMaxPacketSize() == 0x40);

		mSerialLineConfiguration = new SerialLineConfiguration();
	}

	public static boolean probe(UsbDevice d) {

		int vid, pid;
		boolean passed = false;

		vid = d.getVendorId();
		pid = d.getProductId();

		/* Keep in sync usb_device_filter.xml */
		switch (vid) {
		case 0x067b: /* Prolific */
			switch (pid) {
			case 0x2303: /* 0x067b 0x2303 PL2303 Serial */
			case 0x1234: /* 0x067b 0x1234 DCU-11 Phone Cable */
				passed = true;
				break;
			}
			break;
		case 0x5372: /* Prolific2 */
			if (pid == 0x2303) passed = true; /* 0x5372 0x2303 Prolific2 PL2303 */
			break;
		}

		if (D) Log.v(TAG, "Probe for " + vid + ":" + pid + " " + (passed ? "passed"  : "failed"));

		return passed;
	}

	public synchronized boolean isAttached() {
		return inputStream != null;
	}

	@Override
    public synchronized void attach() throws UsbControllerException {

		if (isAttached()) return;

		if (!mUsbManager.hasPermission(mUsbDevice)) {
			throw new UsbControllerException("no permission");
		}

		mUsbConnection = mUsbManager.openDevice(mUsbDevice);
		if (mUsbConnection == null) {
			throw new UsbControllerException("openDevice() failed");
		}

		for (int i=0; i< mUsbInterfaces.length; ++i) {
			if (mUsbConnection.claimInterface(mUsbInterfaces[i], true) == false) {
				for (int j=0; j<i; ++j) {
					mUsbConnection.releaseInterface(mUsbInterfaces[j]);
				}
				mUsbConnection.close();
				throw new UsbControllerException("claimInterface() failed");
			}
		}
		if (!pl2303Reset()) {
			detach();
			throw new UsbControllerException("pl2303Reset() failed");
		}
		if (!pl2303Init()) {
			detach();
			throw new UsbControllerException("pl2303Init() failed");
		}

		boolean confValid;
		final SerialLineConfiguration conf = new SerialLineConfiguration(mSerialLineConfiguration);
		confValid = pl2303GetLineCoding(conf);
		if (confValid) {
		    Log.i(TAG, "Serial line configuration: " + conf.toString());
		}

		if (!pl2303SetLineCoding()) {
		    Log.d(TAG, "pl2303SetLineCoding() failed");
		}

		inputStream = new UsbSerialInputStream(mUsbConnection, mBulkInEndpoint);
		outputStream = new UsbSerialOutputStream(mUsbConnection, mBulkOutEndpoint);

		Log.v(TAG, "(PL2303) USB serial: " + mUsbConnection.getSerial());

		//interruptListener = new Pl2303InterruptListener(mUsbConnection, mInterruptEndpoint);
		//interruptListener.start();
	}

	@Override
    public synchronized void detach() {

		if ( ! isAttached() ) return;

		inputStream = null;
		outputStream = null;

		if (interruptListener != null) {
			interruptListener.cancel();
			interruptListener = null;
		}

		if (mUsbConnection != null) {
			if (mUsbInterfaces != null) {
				for (int i=0; i< mUsbInterfaces.length; ++i) {
					mUsbConnection.releaseInterface(mUsbInterfaces[i]);
				}
				mUsbInterfaces = null;
			}
			mUsbConnection.close();
			mUsbConnection = null;
		}
	}

	@Override
    public synchronized InputStream getInputStream() {
		return inputStream;
	}

	@Override
    public synchronized OutputStream getOutputStream() {
		return outputStream;
	}

    @Override
    public SerialLineConfiguration getSerialLineConfiguration() {
        return new SerialLineConfiguration(mSerialLineConfiguration);
    }

    @Override
    public void setSerialLineConfiguration(final SerialLineConfiguration config) {
        if (mSerialLineConfiguration.equals(config)) return;
        mSerialLineConfiguration.set(config);
        if (isAttached()) {
            pl2303SetLineCoding();
        }
    }

	private boolean pl2303Reset() {
		return mUsbConnection.controlTransfer(
				UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_VENDOR,
				0x01 /* set request */,
				0x0000,
				0x0000,
				null,
				0,
				PL2303_INIT_TIMEOUT_MS) >= 0;
	}

	private boolean pl2303Init() {
		byte buf[] = new byte[4];
		final int read = UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_VENDOR;
		final int write = UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_VENDOR;
		final int tmout = PL2303_INIT_TIMEOUT_MS;

		if ((mUsbConnection.controlTransfer(read, 0x01, 0x8484, 0, buf, 1, tmout) < 0)
			|| (mUsbConnection.controlTransfer(write, 0x01, 0x0404, 0, null, 0, tmout) < 0)
			|| (mUsbConnection.controlTransfer(read, 0x01, 0x8484, 0, buf, 1, tmout) < 0)
			|| (mUsbConnection.controlTransfer(read, 0x01, 0x8383, 0, buf, 1, tmout) < 0)
			|| (mUsbConnection.controlTransfer(read, 0x01, 0x8484, 0, buf, 1, tmout) < 0)
			|| (mUsbConnection.controlTransfer(write, 0x01, 0x0404, 1, null, 0, tmout) < 0)
			|| (mUsbConnection.controlTransfer(read, 0x01, 0x8484, 0, buf, 1, tmout) < 0)
			|| (mUsbConnection.controlTransfer(read, 0x01, 0x8383, 0, buf, 1, tmout) < 0)
			|| (mUsbConnection.controlTransfer(write, 0x01, 0, 1, null, 0, tmout) < 0)
			|| (mUsbConnection.controlTransfer(write, 0x01, 1, 0, null, 0, tmout) < 0)
				) return false;
		if (isPl2303Hx) {
			if (mUsbConnection.controlTransfer(write, 0x01, 2, 0x44, null, 0, tmout) < 0) return false;
		}else {
			if (mUsbConnection.controlTransfer(write, 0x01, 2, 0x24, null, 0, tmout) < 0) return false;
		}

		if ((mUsbConnection.controlTransfer(write, 0x01, 8, 0, null, 0, tmout) < 0)
			|| (mUsbConnection.controlTransfer(write, 0x01, 9, 0, null, 0, tmout) < 0)
			) return false;

		return true;
	}

	private boolean pl2303SetLineCoding() {
		final byte req[];

		Log.d(TAG, "SetLineCoding " + mSerialLineConfiguration.toString());

		req = UsbAcmController.packSetLineCodingRequest(mSerialLineConfiguration);

		if (mUsbConnection.controlTransfer(
				0x21 | UsbConstants.USB_DIR_OUT,
				UsbAcmController.PSTN_SET_LINE_CODING,
				0,
				0, /* bulk data interface number */
				req,
				req.length,
				1000
				) < 0)
			return false;

		/* CRTSCTS=off */
		if (mUsbConnection.controlTransfer(
				UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_VENDOR,
				0x01, 0, 0, null, 0, 1000) < 0)
			return false;

		return true;
	}

	private boolean pl2303GetLineCoding(SerialLineConfiguration dst) {
	    final byte response[];

	    response = new byte[7];
	    if (mUsbConnection.controlTransfer(
	            0x21 | UsbConstants.USB_DIR_IN,
	            UsbAcmController.PSTN_GET_LINE_CODING,
	            0,
	            0, /* bulk data interface number */
	            response,
	            response.length,
	            1000
	            ) < 7)
	        return false;

	    try {
	        dst.set(UsbAcmController.unpackLineCodingResponse(response));
	    }catch (IllegalArgumentException iae) {
	        iae.printStackTrace();
	        return false;
	    }

	    return true;
	}

}
