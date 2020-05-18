package android.kaviles.bletutorial;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";
    private static final String appName = "Grandpa Monitor";
    public static final UUID UUID_HONORBAND5 =
            UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    private BluetoothAdapter mBluetoothAdapter;
    private Context mContext;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    private ProgressDialog mProgressDialog;
    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        //Local server socket
        private BluetoothServerSocket mServerSocket = null;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            //Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Grandpa Monitor", UUID_HONORBAND5);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + UUID_HONORBAND5);

            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }
            mServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;


            try {
                // This is a blocking call and will only return on a
                // sucessful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start...");

                socket = mServerSocket.accept();

                Log.d(TAG, "run: RFCOM server socket accepted connection.");
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }

            if (socket != null) {
                connected(socket,mDevice);
            }

            Log.i(TAG, "END mAcceptThread");

        }

        public void cancel() {
            Log.d(TAG,"cancel: Cancelling AcceptThread.");
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
            }
        }

    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "Connect Thread: started.");
            mDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "Run mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        + UUID_HONORBAND5);
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mSocket = tmp;

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mSocket.connect();

                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                // Close the socket if there's exception
                try {
                    mSocket.close();
                    Log.d(TAG,"run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG,"mConnectThread: run: Unable to close connection in socket " +
                            e1.getMessage());
                }

                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " +
                        UUID_HONORBAND5);
            }

            connected (mSocket, mDevice);

        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mSocket in ConnectThread failed. " +
                        e.getMessage());
            }
        }
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() { // different start from native thread start
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start(); // native thread start
        }
    }

    /**
     * AcceptThread starts and sits waiting for a connection.
     * ConnectThread then starts and attempts to make a connection with other devices AcceptThread
     */
    public void startClient (BluetoothDevice device, UUID uuid) {
        Log.d (TAG, "startClient: Started.");

        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth",
                "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    /**
     * Finally the ConnectedThread which is responsible for maintaing the BTConnecton,
     * Sending the data and receiving incoming data through input/output streams respecitvely.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread (BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the progress dialog when connection is established
            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }


            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run () {
            byte[] buffer = new byte[1024]; // buffer store for the stream

            int bytes; // bytes returned from read ()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG,"InputStream: " + incomingMessage);
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to outputstream." + e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "mSocket close: closed connection " + e.getMessage() );
            }
        }

    }

    private void connected (BluetoothSocket mSocket, BluetoothDevice mDevice) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mSocket);
        mConnectThread.start();
    }

    /**
     * Write to ConnectedThread in an unsyncronized manner
     *
     * @param out the bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;

        //Syncronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        mConnectedThread.write(out);
    }

}
