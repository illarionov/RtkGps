package gpsplus.rtkgps.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class UsbFTDIController extends UsbSerialController {

    private static final String TAG = UsbFTDIController.class.getSimpleName();
    private static D2xxManager ftD2xx;
    private FT_Device ftDev;
    private int devCount = 0;
    private InputStream inputStream;
    private OutputStream outputStream;
    private SerialLineConfiguration mConfig;

    public UsbFTDIController(UsbManager usbManager, UsbDevice usbDevice, Context context)
            throws UsbControllerException {
        super(usbManager, usbDevice, context);
        try
        {
            ftD2xx = D2xxManager.getInstance(this.parentContext);
        }
        catch (D2xxManager.D2xxException e) {
            Log.e(TAG,"FTDI_HT getInstance fail!!");
            throw new UsbControllerException("under development");
        }
        devCount = 0;
        devCount = ftD2xx.createDeviceInfoList(this.parentContext);

        if (devCount < 1)
        {
            throw new UsbControllerException("NO FTDI Device");
        }

      //
    }

    @Override
    protected void finalize() throws Throwable {
        // TODO Auto-generated method stub
        super.finalize();
        this.detach();
    }

    private void connect()
    {
        if (ftDev == null) {
            if (devCount > 0) //A compatible FTDI has been found (probably only one)
            {
                D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
                ftD2xx.getDeviceInfoList(devCount, deviceList);

                if (ftDev == null) {
                    ftDev = ftD2xx.openByIndex(this.parentContext, 0);
                } else {
                    synchronized (ftDev) {
                        ftDev = ftD2xx.openByIndex(this.parentContext, 0);
                    }
                }

            }
        }
    }
    @Override
    public void attach() throws UsbControllerException {
        if(ftDev != null) {
            inputStream = new FTDIInputStream();
            outputStream = new FTDIOutputStream();
        }

    }

    @Override
    public void detach() {
        if(ftDev != null) {
            ftDev.close();
        }
    }

    @Override
    public void setSerialLineConfiguration(SerialLineConfiguration config) {
        mConfig = config;
        if (ftDev == null){
            connect();
        }
        if (ftDev != null){
            ftDev.setBaudRate(config.getBaudrate());
            byte stopBits =  D2xxManager.FT_STOP_BITS_1;
            switch (config.getStopBits())
            {
                case STOP_BITS_1:
                    stopBits =  D2xxManager.FT_STOP_BITS_1;
                    break;
                case STOP_BITS_2:
                    stopBits = D2xxManager.FT_STOP_BITS_2;
                    break;
                default:
                    stopBits =  D2xxManager.FT_STOP_BITS_1;
                    Log.e(TAG, "Stop bit 1.5 does not exist in FTDI chipsets");
                    break;
            }
            byte parity = D2xxManager.FT_PARITY_NONE;
            switch (config.getParity()){
                case EVEN:
                    parity = D2xxManager.FT_PARITY_EVEN;
                    break;
                case MARK:
                    parity = D2xxManager.FT_PARITY_MARK;
                    break;
                case NONE:
                    parity = D2xxManager.FT_PARITY_NONE;
                    break;
                case ODD:
                    parity = D2xxManager.FT_PARITY_ODD;
                    break;
                case SPACE:
                    parity = D2xxManager.FT_PARITY_SPACE;
                    break;
                default:
                    break;
            }

            ftDev.setDataCharacteristics((byte)config.getDataBits(), stopBits, parity);

        }

    }

    @Override
    public SerialLineConfiguration getSerialLineConfiguration() {
        if (ftDev != null){
            if (mConfig == null){
                SerialLineConfiguration conf = new SerialLineConfiguration();
                conf.setBaudrate(1);
                return conf;
            }else{
                return mConfig;
            }
           }
        return null;
    }

    @Override
    public InputStream getInputStream() {
        if (ftDev != null){
         return inputStream;
        }
        return null;
    }

    @Override
    public OutputStream getOutputStream() {
        if (ftDev != null){
            return outputStream;
           }
        return null;
    }

    private class FTDIInputStream extends InputStream{

        //private final FT_Device ftDev;

        public FTDIInputStream() {
            super();
        }


        @Override
        public int read() throws IOException {
           byte[] buffer = new byte[1];
           return read(buffer);
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            int retSize = 0;
            synchronized(ftDev)
            {
                int readSize = ftDev.getQueueStatus();

                if (readSize > 0)
                {
                    if (readSize <= byteCount)
                    {
                        byte[] lBuffer = new byte[readSize];
                        retSize = ftDev.read(lBuffer, readSize,10);
                        for (int i=0;i<lBuffer.length;i++){
                            buffer[i+byteOffset] = lBuffer[i];
                        }
                    }else{
                        byte[] lBuffer = new byte[byteCount];
                        retSize = ftDev.read(lBuffer,byteCount,10);
                        for (int i=0;i<lBuffer.length;i++){
                            buffer[i+byteOffset] = lBuffer[i];
                        }
                    }
                }
            }
            return retSize;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            return read(buffer,0,buffer.length);
        }

    }

    private class FTDIOutputStream extends OutputStream{

        public FTDIOutputStream() {
            super();
        }

        @Override
        public void write(int oneByte) throws IOException {
            byte[] data = new byte[1];
            write(data);
        }

        @Override
        public void write(byte[] buffer, int offset, int count) throws IOException {
            byte[] data = new byte[count];
            data = Arrays.copyOfRange(buffer, offset, offset+count);
            synchronized(ftDev)
            {
                @SuppressWarnings("unused")
                int retSize = ftDev.write(data, data.length);
            }
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            write(buffer,0,buffer.length);
        }

    }
}
