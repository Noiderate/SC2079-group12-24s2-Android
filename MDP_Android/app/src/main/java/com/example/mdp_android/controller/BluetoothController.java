package com.example.mdp_android.controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mdp_android.UI.MainActivity;

import java.io.Serializable;
import java.util.Set;

public class BluetoothController {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Context context;
    private static BluetoothAdapter blAdapter;
    ActivityResultLauncher<Intent> bluetoothLauncher;
    public BluetoothController(Context context, ActivityResultLauncher<Intent> bluetoothLauncher) {
        this.context = context;
        this.bluetoothLauncher = bluetoothLauncher;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
            }
        }
    }

    public BluetoothController(Context context) {
        this.context = context;
        this.bluetoothLauncher = null;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
            }
        }
    }
    // Parcela




    public BluetoothAdapter getBluetoothAdapter() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Bluetooth is not supported on this device, show a toast
            Toast.makeText(context, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return null;
        }
        blAdapter = bluetoothAdapter;
        Log.d("bluetooth1", "bluetooth is supported on this device");
        return bluetoothAdapter;
    }

    public boolean enableBluetooth() {

        if (!blAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothLauncher.launch(enableBtIntent);
            Toast.makeText(context, "Please enable bluetooth and try again", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            return true;
        }
    }

    public Set<BluetoothDevice> getPairedDevices() {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
            }
        }
        Set<BluetoothDevice> pairedDevices = blAdapter.getBondedDevices();

        // Check if there are any paired devices
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                Log.d("bluetooth1", "Device Name: " + deviceName);
                Log.d("bluetooth1", "Device MAC Address: " + deviceHardwareAddress);
            }
        } else {
            Log.d("bluetooth1", "No paired devices found.");
        }

        return pairedDevices;
    }

    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
            }
        }
    }



}