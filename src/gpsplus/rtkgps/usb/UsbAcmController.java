package gpsplus.rtkgps.usb;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.util.Log;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.usb.SerialLineConfiguration.Parity;
import gpsplus.rtkgps.usb.SerialLineConfiguration.StopBits;

import java.io.InputStream;
import java.io.OutputStream;

/* USB CDC ACM (Communication Device Class Abstract Control Model) */
public class UsbAcmController extends UsbSerialController {

    // Debugging
    private static final String TAG = UsbAcmController.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG & true;

    public static final int PSTN_SET_LINE_CODING = 0x20;
    public static final int PSTN_GET_LINE_CODING = 0x21;

    private UsbDeviceConnection mUsbConnection;

    private UsbSerialInputStream inputStream = null;
    private UsbSerialOutputStream outputStream = null;
    private UsbSerialInterruptListener interruptListener = null;

    private final SerialLineConfiguration mSerialLineConfiguration;

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

    public UsbAcmController(UsbManager usbmanager, UsbDevice usbDevice, Context parentContext)
                    throws UsbControllerException {
        super(usbmanager, usbDevice, parentContext);

        mAcmConfig = new AcmConfig(usbDevice);
        mSerialLineConfiguration = new SerialLineConfiguration();
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

        boolean confValid;
        final SerialLineConfiguration conf = new SerialLineConfiguration(mSerialLineConfiguration);
        confValid = acmGetLineCoding(conf);
        if (confValid) {
            Log.i(TAG, "Serial line configuration: " + conf.toString());
        }
        if (!acmSetLineCoding()) {
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
    public SerialLineConfiguration getSerialLineConfiguration() {
        return new SerialLineConfiguration(mSerialLineConfiguration);
    }

    @Override
    public void setSerialLineConfiguration(final SerialLineConfiguration config) {
        if (mSerialLineConfiguration.equals(config)) return;
        mSerialLineConfiguration.set(config);
        if (isAttached()) {
            acmSetLineCoding();
        }
    }


    /**
     * Packs serial line configuration into an USB CDC PSTN SetLineCoding request
     * @param conf serial line configuration
     * @return the USB CDC PSTN SetLineCoding request
     */
    protected static byte[] packSetLineCodingRequest(final SerialLineConfiguration conf) {
        byte req[] = new byte[7];

        final int baudrate = conf.getBaudrate();
        /*  dwDTERate */
        req[0] = (byte)(baudrate & 0xff);
        req[1] = (byte)((baudrate >>> 8) & 0xff);
        req[2] = (byte)((baudrate >>> 16) & 0xff);
        req[3] = (byte)((baudrate >>> 24) & 0xff);

        /* bCharFormat */
        req[4] = conf.getStopBits().getPstnCode();

        /* bParityType */
        req[5] = conf.getParity().getPstnCode();

        /* bDataBits */
        req[6] = (byte)conf.getDataBits();

        return req;
    }

    protected static SerialLineConfiguration unpackLineCodingResponse(byte[] response) throws IllegalArgumentException {
        final SerialLineConfiguration conf;

        conf = new SerialLineConfiguration();
        if (response.length < 7) throw new IllegalArgumentException();

        final int baudrate =  0xffffffff & ((response[0] & 0xff)
                | ((response[1] << 8) & 0xff00)
                | ((response[2] << 16) & 0xff0000)
                | ((response[3] << 24) & 0xff000000));
        conf.setBaudrate(baudrate);

        conf.setStopBits(StopBits.valueOfPstnCode(response[4]));
        conf.setParity(Parity.valueOfPstnCode(response[5]));
        if (response[6] == 0) {
            // XXX: pl2303 on first attach
            conf.setDataBits(8);
        }else {
            conf.setDataBits(response[6]);
        }


        return conf;
    }

    private boolean acmSetLineCoding() {
        final byte req[];

        Log.d(TAG, "SetLineCoding " + mSerialLineConfiguration.toString());

        req = packSetLineCodingRequest(mSerialLineConfiguration);

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

    private boolean acmGetLineCoding(SerialLineConfiguration dst) {
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
