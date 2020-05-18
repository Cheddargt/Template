package android.kaviles.bletutorial;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 52095;
    public static final UUID UUID_HONORBAND5 =
            UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    private HashMap<String, BTLE_Device> mBTDevicesHashMap;
    private ArrayList<BTLE_Device> mBTDevicesArrayList;
    private ListAdapter_BTLE_Devices adapter;
    private Button btn_Scan;
    private Button btn_StartConnection;
    private BroadcastReceiver_BTState mBTStateUpdateReceiver;
    private BroadcastReceiver_BTState mBTStateUpdateReceiver2;
    private Scanner_BTLE mBTLeScanner;
    private Handler mHandler;
    private BTLE_Device mBondedDevice;
    private BluetoothConnectionService mBluetoothConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.toast(getApplicationContext(), "BLE not supported");
            finish();
        }

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        mBTStateUpdateReceiver2 = new BroadcastReceiver_BTState(getApplicationContext());
        mBTLeScanner = new Scanner_BTLE(this, 20500, -100);
        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();
        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);
        mHandler = new Handler();
        mBondedDevice = null;
//        mBluetoothConnection = null;


        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        ((ScrollView) findViewById(R.id.scrollView)).addView(listView);
        btn_Scan = (Button) findViewById(R.id.btn_scan);
        btn_StartConnection = (Button) findViewById(R.id.btn_start_connection);
        findViewById(R.id.btn_scan).setOnClickListener(this);
        findViewById(R.id.btn_start_connection).setOnClickListener(this);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getBaseContext(), "Please allow location", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(mBTStateUpdateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mBTStateUpdateReceiver2, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mBTStateUpdateReceiver);
        unregisterReceiver(mBTStateUpdateReceiver2);
        stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBTStateUpdateReceiver);
        unregisterReceiver(mBTStateUpdateReceiver2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Utils.toast(getApplicationContext(), "Thank you for turning on Bluetooth");
            } else if (resultCode == RESULT_CANCELED) {
                Utils.toast(getApplicationContext(), "Please turn on Bluetooth");
            }
        }
    }

    /**
     * Called when an item in the ListView is clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Context context = view.getContext();
        stopScan();

        final String name = mBTDevicesArrayList.get(position).getName();
        String address = mBTDevicesArrayList.get(position).getAddress();

        Utils.toast(getApplicationContext(), name + " selecionado.");

        //if is bonded
        if (mBTDevicesArrayList.get(position).getBondState() == BluetoothDevice.BOND_BONDED) {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Utils.toast(getApplicationContext(), "Already bonded. Unbonding...");
                }
            }, 1000);

            mBTDevicesArrayList.get(position).removeBond();
            mBondedDevice = null;

        } else {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Utils.toast(getApplicationContext(), "Tentando conectar com " + name);
                }
            }, 1000);

            mBTDevicesArrayList.get(position).createBond();
            mBondedDevice = mBTDevicesArrayList.get(position);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);

        }

    }

    /**
     * Called when the scan button is clicked.
     *
     * @param v The view that was clicked
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btn_scan:
                if (!mBTLeScanner.isScanning()) {
                    startScan();

                } else {
                    stopScan();
                }
                break;

            case R.id.btn_start_connection:
//                if (mBTLeScanner.isScanning()){
//                    stopScan();
//                }
//                if (mBondedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "startConnection(): starting");
                startConnection();
//                }
//
                break;

            default:
                break;
        }
    }

    public void addDevice(BluetoothDevice device, int new_rssi) {

        String address = device.getAddress();
        if (!mBTDevicesHashMap.containsKey(address)) {
            BTLE_Device btleDevice = new BTLE_Device(device);
            btleDevice.setRSSI(new_rssi);

            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);
        } else {
            mBTDevicesHashMap.get(address).setRSSI(new_rssi);
        }
        adapter.notifyDataSetChanged();
    }

    public void startScan() {
        btn_Scan.setText("Scanning...");
        mBTDevicesArrayList.clear();
        mBTDevicesHashMap.clear();
//        adapter.notifyDataSetChanged();
        mBTLeScanner.start();
    }

    public void stopScan() {
        btn_Scan.setText("Scan Again");
        mBTLeScanner.stop();
    }

    public void startConnection () {
        startBTConnection(mBondedDevice.getBluetoothDevice(), UUID_HONORBAND5);
    }

    public void startBTConnection (BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOMM Bluetooth Connection.");

        mBluetoothConnection.startClient(device, uuid);
    }

}
