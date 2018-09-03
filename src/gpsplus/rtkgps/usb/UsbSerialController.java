package gpsplus.rtkgps.usb;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;


@TargetApi(12)
public abstract class UsbSerialController {

	// Debugging
	private static final String TAG = UsbSerialController.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG & true;

    protected UsbManager mUsbManager;
	protected UsbDevice mUsbDevice;
	protected Context parentContext = null;

	public UsbSerialController(UsbManager usbManager,
			UsbDevice usbDevice, Context parentContext) throws UsbControllerException {
		this.mUsbDevice = usbDevice;
		this.mUsbManager = usbManager;
		this.parentContext = parentContext;
	}

	public abstract void attach() throws UsbControllerException;
	public abstract void detach();

	/**
	 * Set serial line configuration
	 */
	public abstract void setSerialLineConfiguration(final SerialLineConfiguration config);

	/**
	 * @return serial line configuration
	 */
	public abstract SerialLineConfiguration getSerialLineConfiguration();


	public abstract InputStream getInputStream();
	public abstract OutputStream getOutputStream();

	protected static boolean isInUsbDeviceFilter(UsbDevice d, Resources appResources) {
		int type;
		XmlResourceParser parser;
		boolean match = false;

		parser = appResources.getXml(R.xml.usb_device_filter);
		try {
			for (type=parser.getEventType();
					!match && (type != XmlResourceParser.END_DOCUMENT);
					type = parser.next()) {
				int count;
				int vendorId = -1;
				int productId = -1;
				int deviceClass = -1;
				int deviceSubclass = -1;
				int deviceProtocol = -1;


				if (type != XmlResourceParser.START_TAG) continue;
				if ("usb-device".equals(parser.getName())) continue;

				count = parser.getAttributeCount();
				for(int i=0; i<count; ++i) {
					String name = parser.getAttributeName(i);
					// All attribute values are ints
					int value = Integer.parseInt(parser.getAttributeValue(i));

					if ("vendor-id".equals(name)) {
						vendorId = value;
					} else if ("product-id".equals(name)) {
						productId = value;
					} else if ("class".equals(name)) {
						deviceClass = value;
					} else if ("subclass".equals(name)) {
						deviceSubclass = value;
					} else if ("protocol".equals(name)) {
						deviceProtocol = value;
					}
				}
				match = ((vendorId < 0)  || (d.getVendorId() == vendorId)
						&& ((productId < 0) || (d.getProductId() == productId) )
						&& ((deviceClass < 0) || (d.getDeviceClass() == deviceClass) )
						&& ((deviceSubclass < 0) || (d.getDeviceSubclass() == deviceSubclass))
						&& ((deviceProtocol < 0) || (d.getDeviceProtocol() == deviceProtocol))
						);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return match;
	}


	public boolean hasPermission() {
		return this.mUsbManager.hasPermission(this.mUsbDevice);
	}

	public void requestPermission(PendingIntent pi) {
		this.mUsbManager.requestPermission(mUsbDevice, pi);
	}

	public static class UsbControllerException extends Exception {
		private static final long serialVersionUID = 1L;
		public UsbControllerException(String msg) { super(msg); }
	}

	public UsbDevice getDevice() {
	    return mUsbDevice;
	}

	protected static class UsbSerialInterruptListener extends Thread {

		private boolean cancelRequested = false;
		private UsbDeviceConnection mUsbConnection;
		private ByteBuffer buffer;
		private UsbRequest request;

		public UsbSerialInterruptListener(UsbDeviceConnection connection, UsbEndpoint endpoint) {
			this.mUsbConnection = connection;
			this.setName("PL2303InterruptListener");
			buffer =  ByteBuffer.allocate(endpoint.getMaxPacketSize());
			request = new UsbRequest();
			request.initialize(connection, endpoint);
		}

		@Override
		public void run() {
			mainloop: while(!cancelRequested()) {
				request.queue(buffer, buffer.capacity());
				if (mUsbConnection.requestWait() == request) {
					if (D) Log.v(TAG, "Interrupt received: " + buffer.toString() +
							Arrays.toString(buffer.array()));
					synchronized(this) {
						try {
							this.wait(100);
						} catch (InterruptedException e) {
							break mainloop;
						}
						if (cancelRequested) break mainloop;
					}
				}else {
					Log.e(TAG, "requestWait failed, exiting");
					break mainloop;
				}
			}
			Log.d(TAG, "Pl2303InterruptListener thread stopped");
		}

		public synchronized void cancel() {
			cancelRequested = true;
			this.notify();
		}

		private synchronized boolean cancelRequested() {
			return this.cancelRequested;
		}

	}

	protected class UsbSerialInputStream extends InputStream {

		private static final int DEFAULT_READ_TIMEOUT_MS = 30000;
		private int mTimeout = DEFAULT_READ_TIMEOUT_MS;

		private UsbDeviceConnection mUsbConnection;
		private UsbEndpoint mUsbEndpoint;
		private byte rcvPkt[] = null;

		public UsbSerialInputStream(UsbDeviceConnection connection,
				UsbEndpoint bulkInEndpoint,
				int writeTmoutMs
				) {
			mUsbConnection = connection;
			mUsbEndpoint = bulkInEndpoint;
			mTimeout = writeTmoutMs;
			rcvPkt = new byte[mUsbEndpoint.getMaxPacketSize()];
		}

		public UsbSerialInputStream(UsbDeviceConnection connection,
				UsbEndpoint bulkOutEndpoint) {
			this(connection, bulkOutEndpoint, DEFAULT_READ_TIMEOUT_MS);
		}

		@Override
		public int read() throws IOException {
			synchronized(this) {
				int rcvd = read(rcvPkt, 0, 1);
				if (rcvd == 0) throw new IOException("timeout");
				return rcvPkt[0] & 0xff;
			}
		}

		@Override
		public int read(byte[] buffer, int offset, int count) throws IOException {
			int rcvd;

			synchronized(this) {
				if (offset == 0) {
					rcvd = mUsbConnection.bulkTransfer(mUsbEndpoint, buffer,
							count, mTimeout);
					if (rcvd < 0) throw new IOException("bulkTransfer() error");
					//if (D) Log.d(TAG, "Received " + rcvd + " bytes aligned");
					return rcvd;
				}else {
					rcvd = mUsbConnection.bulkTransfer(mUsbEndpoint,
							rcvPkt,
							Math.min(count, rcvPkt.length),
							mTimeout);
					if (rcvd < 0) throw new IOException("bulkTransfer() error");
					else if (rcvd > 0) {
						System.arraycopy(rcvPkt, 0, buffer, offset, rcvd);
					}
					if (D) Log.d(TAG, "Received " + rcvd + " bytes");
					return rcvd;
				}
			}
		}
	}

	protected class UsbSerialOutputStream extends OutputStream {

		private static final int DEFAULT_WRITE_TIMEOUT_MS = 2000;
		private int mTimeout = DEFAULT_WRITE_TIMEOUT_MS;

		private UsbDeviceConnection mUsbConnection;
		private UsbEndpoint mUsbEndpoint;
		private byte sndPkt[] = null;

		public UsbSerialOutputStream(UsbDeviceConnection connection,
				UsbEndpoint bulkOutEndpoint,
				int writeTmoutMs
				) {
			mUsbConnection = connection;
			mUsbEndpoint = bulkOutEndpoint;
			mTimeout = writeTmoutMs;
			sndPkt = new byte[mUsbEndpoint.getMaxPacketSize()];
		}

		public UsbSerialOutputStream(UsbDeviceConnection connection,
				UsbEndpoint bulkOutEndpoint) {
			this(connection, bulkOutEndpoint, DEFAULT_WRITE_TIMEOUT_MS);
		}

		@Override
		public void write(int arg0) throws IOException {
			write(new byte[] { (byte) arg0 } );
		}

		@Override
		public void write(byte[] buffer, int offset, int count) throws IOException {
			synchronized(this) {
				while(count>0) {
					/* XXX: timeout */
					int length = count > sndPkt.length ? sndPkt.length : count;
					System.arraycopy(buffer, offset, sndPkt, 0, length);
					int snd = mUsbConnection.bulkTransfer(mUsbEndpoint, sndPkt, length, mTimeout);
					if (snd<0) throw new IOException("bulkTransfer() failed");
					count -= snd;
					offset += snd;
				}
			}
		}
	}

}

