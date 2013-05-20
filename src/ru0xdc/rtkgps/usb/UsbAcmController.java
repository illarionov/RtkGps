package ru0xdc.rtkgps.usb;

import java.io.InputStream;
import java.io.OutputStream;

import ru0xdc.rtkgps.BuildConfig;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.util.Log;

/* USB CDC ACM (Communication Device Class Abstract Control Model) */
public class UsbAcmController extends UsbSerialController {

    // Debugging
    private static final String TAG = UsbAcmController.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG & true;

    private UsbDeviceConnection mUsbConnection;

    private UsbSerialInputStream inputStream = null;
    private UsbSerialOutputStream outputStream = null;
    private UsbSerialInterruptListener interruptListener = null;

    private int mBaudrate;

    private final AcmConfig mAcmConfig;

    private static class AcmConfig {

        android.hardware.usb.UsbInterface mCommunicationInterface = null;
        android.hardware.usb.UsbInterface mDataInterface = null;

        UsbEndpoint mInterruptEndpoint = null;
        UsbEndpoint mBulkInEndpoint = null;
        UsbEndpoint mBulkOutEndpoint = null;

        public AcmConfig(UsbDevice usbDevice) throws UsbControllerException {

            int ifaceIdx, ifaceCount;
            ifaceCount = usbDevice.getInterfaceCount();
            foreachIface: for (ifaceIdx=0; ifaceIdx<ifaceCount; ++ifaceIdx) {
                int endpointCount;
                int endpointIdx;
                android.hardware.usb.UsbInterface iface;

                iface = usbDevice.getInterface(ifaceIdx);

                /*
                 * Interface class codes:
                 *    0x02 Communication interface Class
                 *    0x0a Data interface Class
                 */
                switch (iface.getInterfaceClass()) {
                case 0x02:

                    if (mCommunicationInterface != null)
                        continue foreachIface;

                    /* Communication Interface Subclass Codes
                     *    0x00 RESERVED
                     *    0x01 Direct Line Control Model
                     *    0x02 Abstract Control Model
                     *    0x03 Telephone Control Model
                     */
                    if (iface.getInterfaceSubclass() != 0x02) {
                        Log.d(TAG, "Interface " + ifaceIdx +
                                "subclass " + iface.getInterfaceSubclass() +
                                "is not ACM subclass.");
                        continue foreachIface;
                    }
                    /* Communication Interface Protocol Codes
                     *    0x00 No class specific protocol required
                     *    0x01 AT commands: V250 etc
                     *    0xff Vendor-specific
                     */
                    switch (iface.getInterfaceProtocol()) {
                    case 0x00:
                    case 0x01:
                        break;
                    default:
                        Log.d(TAG, "Unknown interface " + ifaceIdx +
                                "protocol " + iface.getInterfaceProtocol());
                        continue foreachIface;
                    }

                    /* Search for notification endpoint */
                    endpointCount = iface.getEndpointCount();
                    if (endpointCount < 1) {
                        Log.d(TAG, "No endpoints on Communication Interface" + ifaceIdx);
                        continue foreachIface;
                    }

                    for(endpointIdx=0;  endpointIdx<endpointCount; ++endpointIdx) {
                        UsbEndpoint e = iface.getEndpoint(endpointIdx);
                        if (e.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                            mInterruptEndpoint = e;
                            mCommunicationInterface = iface;
                        }
                    }

                    if (mInterruptEndpoint == null) {
                        Log.d(TAG, "No notification endpoint found on communication interface" + ifaceIdx);
                    }

                    break;
                case 0x0a:

                    if (mDataInterface != null)
                        continue foreachIface;

                    /* Data interface subclass code is unused and should be 0 */
                    if (iface.getInterfaceSubclass() != 0x00) {
                        Log.d(TAG, "Interface " + ifaceIdx +
                                "subclass " + iface.getInterfaceSubclass() +
                                "should be 0");
                        continue foreachIface;
                    }

                    endpointCount = iface.getEndpointCount();
                    if (endpointCount < 2) {
                        Log.d(TAG, "No endpoints on Data Interface" + ifaceIdx);
                        continue foreachIface;
                    }

                    UsbEndpoint eIn = null;
                    UsbEndpoint eOut = null;
                    for(endpointIdx=0;  endpointIdx<endpointCount; ++endpointIdx) {
                        UsbEndpoint e = iface.getEndpoint(endpointIdx);

                        if (e.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (e.getDirection() == UsbConstants.USB_DIR_IN) {
                                eIn = e;
                            }else {
                                eOut = e;
                            }
                        }
                    }

                    if ((eIn != null) && (eOut != null)) {
                        mDataInterface = iface;
                        mBulkInEndpoint = eIn;
                        mBulkOutEndpoint = eOut;
                    }else {
                        Log.d(TAG, "No data endpoints found on communication interface" + ifaceIdx);
                    }
                    break;
                default:
                    break;
                } /* switch (iface.getInterfaceClass()) */
            } /* foreachIface */


            if (mInterruptEndpoint == null) {
                throw new UsbControllerException("Interrupt input endpoint not found");
            }else if (mBulkInEndpoint == null) {
                throw new UsbControllerException("Bulk data input endpoint not found");
            }else if (mBulkOutEndpoint == null) {
                throw new UsbControllerException("Bulk data output endpoint not found");
            }
        }

    }

    public UsbAcmController(UsbManager usbmanager, UsbDevice usbDevice)
                    throws UsbControllerException {
        super(usbmanager, usbDevice);

        mAcmConfig = new AcmConfig(usbDevice);
        mBaudrate = DEFAULT_BAUDRATE;
    }

    public static boolean probe(UsbDevice d) {
        int devClass, devSubclass, devProto;
        boolean passed = false;

        devClass = d.getDeviceClass();
        devSubclass = d.getDeviceSubclass();
        devProto = d.getDeviceProtocol();

        if ((devClass == UsbConstants.USB_CLASS_COMM)
                && (devSubclass == 0x00 /* unused */)
                && (devProto == 0x00 /* unused */)
                ) {
            try {
                @SuppressWarnings("unused")
                final AcmConfig c = new AcmConfig(d);
                passed = true;
            }catch(UsbControllerException uce) {
                uce.printStackTrace();
            }
        }

        if (D) Log.v(TAG, "Probe for class: " + devClass +
                " subclass: " + devSubclass +
                " proto: " + devProto +
                (passed ? "passed"  : "failed"));

        return passed;
    }

    public synchronized boolean isAttached() {
        return inputStream != null;
    }

    @Override
    public synchronized void attach() throws UsbControllerException {

        if (!mUsbManager.hasPermission(mUsbDevice)) {
            throw new UsbControllerException("no permission");
        }

        mUsbConnection = mUsbManager.openDevice(mUsbDevice);
        if (mUsbConnection == null) {
            throw new UsbControllerException("openDevice() failed");
        }

        if (mUsbConnection.claimInterface(mAcmConfig.mCommunicationInterface, true) == false) {
            mUsbConnection.close();
            throw new UsbControllerException("claimInterface(mCommunicationInterface) failed");
        }
        if (mUsbConnection.claimInterface(mAcmConfig.mDataInterface, true) == false) {
            mUsbConnection.releaseInterface(mAcmConfig.mCommunicationInterface);
            mUsbConnection.close();
            throw new UsbControllerException("claimInterface(mDataInterface) failed");
        }

        if (setLineCoding() == false) {
            Log.d(TAG, "setLineCoding() failed");
        }

        inputStream = new UsbSerialInputStream(mUsbConnection, mAcmConfig.mBulkInEndpoint);
        outputStream = new UsbSerialOutputStream(mUsbConnection, mAcmConfig.mBulkOutEndpoint);

        Log.v(TAG, "(ACM) USB serial: " + mUsbConnection.getSerial());
        //interruptListener = new Pl2303InterruptListener(mUsbConnection, mInterruptEndpoint);
        //interruptListener.start();
    }

    @Override
    public synchronized void detach() {

        if (!isAttached()) return;

        if (interruptListener != null) {
            interruptListener.cancel();
        }

        mUsbConnection.releaseInterface(mAcmConfig.mCommunicationInterface);
        mUsbConnection.releaseInterface(mAcmConfig.mDataInterface);
        mUsbConnection.close();

        inputStream = null;
        outputStream = null;

        mUsbConnection = null;
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
    public synchronized int getBaudRate() {
        return mBaudrate;
    }

    @Override
    public synchronized void setBaudRate(int baudrate) {
        if (this.mBaudrate == baudrate) return;
        this.mBaudrate = baudrate;
        if (isAttached()) {
            setLineCoding(baudrate);
        }
    }

    private boolean setLineCoding() {
        return setLineCoding(this.mBaudrate);
    }

    private boolean setLineCoding(int baudrate) {
        return setLineCoding(baudrate, 8, 'N', 1);
    }

    private boolean setLineCoding(int baudrate, int dataBits, char parity, int stopBits) {
        byte req[] = new byte[7];

        /*  dwDTERate */
        req[0] = (byte)(baudrate & 0xff);
        req[1] = (byte)((baudrate >>> 8) & 0xff);
        req[2] = (byte)((baudrate >>> 16) & 0xff);
        req[3] = (byte)((baudrate >>> 24) & 0xff);

        /* bCharFormat */
        if (stopBits != 1 && (stopBits != 2)) throw new IllegalArgumentException("Wrong stop bits");
        req[4] = stopBits == 1 ? (byte)0 : (byte)2;

        /* bParityType */
        switch (parity) {
        case 'N': req[5] = 0; break; /* None */
        case 'O': req[5] = 1; break; /* Odd */
        case 'E': req[5] = 2; break; /* Even */
        case 'M': req[5] = 3; break; /* Mark */
        case 'S': req[5] = 4; break; /* Space */
        default: throw new IllegalArgumentException("Wrong parity");
        }

        /* bDataBits */
        switch (dataBits) {
        case 5:
        case 6:
        case 7:
        case 8:
        case 16:
            req[6] = (byte)dataBits;
            break;
        default: throw new IllegalArgumentException("Wrong data bits");
        }

        Log.d(TAG, "SetLineCoding rate=" + baudrate + " " +
                Integer.toString(dataBits) +
                Character.toString(parity) +
                stopBits);

        if (mUsbConnection.controlTransfer(
                0x21,
                0x20, /* SET_LINE_CODING */
                0,
                0, /* bulk data interface number */
                req,
                req.length,
                1000
                ) < 0)
            return false;

        return true;
    }
}
