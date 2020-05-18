package android.kaviles.bletutorial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/**
 * Created by Kelvin on 4/18/16.
 * Detect when the bluetooth on the device changes state.
 */
public class BroadcastReceiver_BTState extends BroadcastReceiver {

    Context activityContext;
    Handler mHandler;

    public BroadcastReceiver_BTState(Context activityContext) {
        this.activityContext = activityContext;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        mHandler = new Handler();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Utils.toast(activityContext, "Bluetooth is off");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Utils.toast(activityContext, "Bluetooth is turning off...");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Utils.toast(activityContext, "Bluetooth is on");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Utils.toast(activityContext, "Bluetooth is turning on...");
                    break;
            }
        }

        if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
            BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            //3 cases:
            //case1: bonded already
            assert mDevice != null;
            if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        Utils.toast(activityContext, "BroadcastReceiver: BOND_BONDED");
                    }
                }, 2000);

//                Utils.toast(activityContext, "BroadcastReceiver: BOND_BONDED");
            }
            //case2: not bonded, bonding...
            if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Utils.toast(activityContext, "BroadcastReceiver: BOND_BONDING");
                    }
                }, 2000);

//                Utils.toast(activityContext, "BroadcastReceiver: BOND_BONDING");
            }
            //case3: breaking a bond
            if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Utils.toast(activityContext, "BroadcastReceiver: BOND_NONE");
                    }
                }, 2000);

//                Utils.toast(activityContext, "BroadcastReceiver: BOND_NONE");
            }

        }
    }
}
