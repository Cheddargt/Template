package android.kaviles.bletutorial;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by Kelvin on 5/8/16.
 * Wrapper class for the bluetooth device objects
 * Needed to store de RSSI values when scanner detects bluetooth devices.
 */
public class BTLE_Device {

    private static final String TAG = BTLE_Device.class.getSimpleName(); ;
    private BluetoothDevice bluetoothDevice;
    private int rssi;


    public BTLE_Device(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    public String getName() {
        return bluetoothDevice.getName();
    }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    public int getRSSI() {
        return rssi;
    }

    public void createBond() {
        this.bluetoothDevice.createBond();
    }

    public void removeBond() {
        try {
            Method m = this.bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(this.bluetoothDevice, (Object[]) null);
        } catch (Exception e) { Log.e(TAG, e.getMessage()); }
    }

    public int getBondState() {
         return bluetoothDevice.getBondState();
    }

    public ParcelUuid[] getUUID () {
        ParcelUuid[] uuid = this.bluetoothDevice.getUuids();
        return uuid;
    }

    public BluetoothDevice getBluetoothDevice () {
        return bluetoothDevice;
    }


}
