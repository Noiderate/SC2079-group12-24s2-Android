package com.example.mdp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothServ";
    public static boolean BluetoothConnectionStatus = false;
    private static ConnectedThread myConnectedThread;
    private final BluetoothAdapter myBluetoothAdapter;
    public static BluetoothDevice myBluetoothDevice;
    private ConnectThread myConnectThread;
    private BluetoothDevice myDevice;
    private UUID deviceUUID;

    Context myContext;
    ProgressDialog myProgressDialog;
    Intent connectionStatus;

    public BluetoothService(Context context) {
        this.myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.myContext = context;
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mySocket;

        public ConnectThread(BluetoothDevice device, UUID u) {
            myDevice = device;
            deviceUUID = u;
            Log.d("amu", "THE UUID IS: "+ u);
        }

        @SuppressLint("MissingPermission")
        public void run() {
            BluetoothSocket tmp = null;

            try {
                tmp = myDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException ignored) {
            }

            mySocket = tmp;
            myBluetoothAdapter.cancelDiscovery(); // u

            try {
                mySocket.connect();
                connected(mySocket, myDevice);
            } catch (IOException e) {
                try {
                    mySocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    Bluetooth mBluetoothActivity = (Bluetooth) myContext;
                    mBluetoothActivity.runOnUiThread(() -> Toast
                            .makeText(myContext, "Failed to connect to the device.", Toast.LENGTH_SHORT).show());
                } catch (Exception z) {
                    z.printStackTrace();
                }
            }

            try {
                myProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public void startClientThread(BluetoothDevice device, UUID uuid) {
        try {
            myBluetoothDevice = device;
            myProgressDialog = ProgressDialog.show(myContext, "Connecting Bluetooth", "Please Wait...", true);
        } catch (Exception e) {
            Log.d(TAG, "Failed to connect!");
            e.printStackTrace();
        }
        Log.d("amu", "THE UUID IS: "+ deviceUUID);
        myConnectThread = new ConnectThread(device, uuid);
        myConnectThread.start();
    }

    private class ConnectedThread extends Thread {
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", myDevice);
            LocalBroadcastManager.getInstance(myContext).sendBroadcast(connectionStatus);
            BluetoothConnectionStatus = true;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
   
            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    String incomingmessage = new String(buffer, 0, bytes);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("receivedMessage", incomingmessage);

                    LocalBroadcastManager.getInstance(myContext).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status", "disconnected");
                    connectionStatus.putExtra("Device", myDevice);
                    LocalBroadcastManager.getInstance(myContext).sendBroadcast(connectionStatus);
                    BluetoothConnectionStatus = false;
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outStream.write(bytes);
                Log.d(TAG, "sending message");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connected(BluetoothSocket mySocket, BluetoothDevice device) {
        myDevice = device;
        myConnectedThread = new ConnectedThread(mySocket);
        myConnectedThread.start();
    }

    public static void write(byte[] out) {
        myConnectedThread.write(out);
    }
}