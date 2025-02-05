package com.example.mdp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class Bluetooth extends AppCompatActivity {
    private static final String TAG = "Bluetooth->DEBUG";
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //private static final UUID myUUID = UUID.fromString("86462402-c16a-11ee-a48e-e45f0155a535");
    public static BluetoothDevice myBluetoothDevice;
    public ArrayList<BluetoothDevice> myNewBluetoothDevices;
    public ArrayList<BluetoothDevice> myPairedBTDevices;
    public DeviceListAdapter myNewDeviceListAdapter;
    public DeviceListAdapter myPairedDeviceListAdapter;
    private String connStatus;

    boolean retryConnection = false;
    TextView connStatusTextView;
    ListView otherDevicesListView;
    ListView pairedDevicesListView;
    Button connectBtn;
    ProgressDialog myDialog;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    BluetoothAdapter myBluetoothAdapter;
    BluetoothService mBluetoothConnection;

    Handler reconnectionHandler = new Handler();
    Runnable reconnectionRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!BluetoothService.BluetoothConnectionStatus) {
                    Log.d(TAG, "Reconnecting...");
                    startBTConnection(myBluetoothDevice, myUUID);
                    Toast.makeText(Bluetooth.this, "Reconnection success!", Toast.LENGTH_SHORT).show();
                }
                reconnectionHandler.removeCallbacks(reconnectionRunnable);
                retryConnection = true; //MADE CHANGE 2 FEB 24
            } catch (Exception e) {
                Log.d(TAG, "Reconnecting failed...");
                e.printStackTrace();
                Toast.makeText(Bluetooth.this, "Failed to reconnect, trying again in 5 second", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth);
        Log.d(TAG, "I'm created!");
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        Log.d(TAG, "width: " + width + ", height: " + height);

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Switch bluetoothSwitch = (Switch) findViewById(R.id.bluetoothSwitch);
        if (myBluetoothAdapter.isEnabled()) {
            bluetoothSwitch.setChecked(true);
            bluetoothSwitch.setText("ON");
        }

        otherDevicesListView = (ListView) findViewById(R.id.otherDevicesListView);
        pairedDevicesListView = (ListView) findViewById(R.id.pairedDevicesListView);
        myNewBluetoothDevices = new ArrayList<>();
        myPairedBTDevices = new ArrayList<>();

        connectBtn = (Button) findViewById(R.id.connectBtn);
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(onAndOffWatcher, BTIntent);
        IntentFilter discoverIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(scannerModeWatcher, discoverIntent);
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(scannerWatcher, discoverDevicesIntent);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(pairingWatcher, filter);
        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(connectionWatcher, filter2);
        Button scanButton = (Button) this.findViewById(R.id.scanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                toggleButtonScan(view);
            }
        });

        otherDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                myBluetoothAdapter.cancelDiscovery();
                String deviceName = myNewBluetoothDevices.get(i).getName();
                String deviceAddress = myNewBluetoothDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Log.d(TAG, "onItemClick: Initiating pairing with " + deviceName);
                    boolean success = myNewBluetoothDevices.get(i).createBond();
                    mBluetoothConnection = new BluetoothService(Bluetooth.this);
                    myBluetoothDevice = myNewBluetoothDevices.get(i);
                }
            }
        });

        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                myBluetoothAdapter.cancelDiscovery();
                otherDevicesListView.setAdapter(myNewDeviceListAdapter);

                String deviceName = myPairedBTDevices.get(i).getName();
                String deviceAddress = myPairedBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);
                BluetoothService.myBluetoothDevice = myPairedBTDevices.get(i);
                Toast.makeText(Bluetooth.this, deviceName, Toast.LENGTH_SHORT).show();
                mBluetoothConnection = new BluetoothService(Bluetooth.this);
                myBluetoothDevice = myPairedBTDevices.get(i);
            }
        });

        // on and off bluetooth switch
        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.d(TAG, "onChecked: Switch button toggled. Enabling/Disabling Bluetooth");
                if (isChecked) {
                    compoundButton.setText("ON");
                } else {
                    compoundButton.setText("OFF");
                }

                if (myBluetoothAdapter == null) {
                    Log.d(TAG, "enableDisableBT: Device does not support Bluetooth capabilities!");
                    Toast.makeText(Bluetooth.this, "Device Does Not Support Bluetooth capabilities!", Toast.LENGTH_LONG)
                            .show();
                    compoundButton.setChecked(false);
                } else {
                    if (!myBluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: enabling Bluetooth");
                        Log.d(TAG, "enableDisableBT: Making device discoverable for 600 seconds.");

                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
                        startActivity(discoverableIntent);

                        compoundButton.setChecked(true);

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(onAndOffWatcher, BTIntent);

                        IntentFilter discoverIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                        registerReceiver(scannerModeWatcher, discoverIntent);
                    }
                    if (myBluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: disabling Bluetooth");
                        myBluetoothAdapter.disable();

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(onAndOffWatcher, BTIntent);
                    }
                }
            }
        });

        // connect button
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myBluetoothDevice == null) {
                    Toast.makeText(Bluetooth.this, "Please select a device before connecting.", Toast.LENGTH_LONG)
                            .show();
                } else {
                    startConnection();
                }
            }
        });

        connStatusTextView = (TextView) findViewById(R.id.connStatusTextView);
        connStatus = "Disconnected";
        connStatusTextView.setText(connStatus);

        myDialog = new ProgressDialog(Bluetooth.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    // Search Button
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void toggleButtonScan(View view) {
        Log.d(TAG, "toggleButton: Scanning for unpaired devices.");
        myNewBluetoothDevices.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "BLUETOOTH_SCAN permission not granted.");
                return;
            }
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "Device is currently discovering Bluetooth devices.");
        }
        if (myBluetoothAdapter != null) {
            if (!myBluetoothAdapter.isEnabled()) {
                Toast.makeText(Bluetooth.this, "Please turn on Bluetooth first!", Toast.LENGTH_SHORT).show();
            }
            if (myBluetoothAdapter.isDiscovering()) {
                myBluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "toggleButton: Cancelling Discovery.");

                checkBTPermissions();

                myBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(scannerWatcher, discoverDevicesIntent);
            } else if (!myBluetoothAdapter.isDiscovering()) {
                checkBTPermissions();

                myBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(scannerWatcher, discoverDevicesIntent);
            }
            myPairedBTDevices.clear();
            Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
            Log.d(TAG, "toggleButton: Number of paired devices found: " + pairedDevices.size());
            for (BluetoothDevice d : pairedDevices) {
                Log.d(TAG, "Paired Devices: " + d.getName() + " : " + d.getAddress());
                myPairedBTDevices.add(d);
                myPairedDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view,
                        myPairedBTDevices);
                pairedDevicesListView.setAdapter(myPairedDeviceListAdapter);
            }
        }
    }

    // check bluetooth permission before using it
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
            int scanPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN);
            int connectPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT);

            if (scanPermission != PackageManager.PERMISSION_GRANTED || connectPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                        1001);
            }
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) { // Android 6-11 (API 23-30)
            int fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED || coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        1001);
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    // handles the on and off of bluetooth
    private final BroadcastReceiver onAndOffWatcher = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mBroadcastReceiver1:" + this.getClass().getSimpleName());
            if (action.equals(myBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    // handles the scan mode
    private final BroadcastReceiver scannerModeWatcher = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mBroadcastReceiver2:" + this.getClass().getSimpleName());
            if (action.equals(myBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    // handles new devices discovered
    private BroadcastReceiver scannerWatcher = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");
            Log.d(TAG, "mBroadcastReceiver3:" + this.getClass().getSimpleName());
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                myNewBluetoothDevices.add(device);
                assert device != null;
                Log.d(TAG, "onReceive: " + device.getName() + " : " + device.getAddress());
                myNewDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view,
                        myNewBluetoothDevices);
                otherDevicesListView.setAdapter(myNewDeviceListAdapter);
            }
        }
    };

    // handles pairing of devices
    private BroadcastReceiver pairingWatcher = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "mBroadcastReceiver4:" + this.getClass().getSimpleName());
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice myDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert myDevice != null;
                if (myDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BOND_BONDED.");
                    myPairedBTDevices.clear();
                    Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice d : pairedDevices) {
                        Log.d(TAG, "*********************************************************");
                        Log.d(TAG, "Paired Devices: " + d.getName() + " : " + d.getAddress());
                        myPairedBTDevices.add(d);
                        myPairedDeviceListAdapter = new DeviceListAdapter(Bluetooth.this, R.layout.device_adapter_view,
                                myPairedBTDevices);
                    }
                    pairedDevicesListView.setAdapter(myPairedDeviceListAdapter);
                    Toast.makeText(Bluetooth.this, "Successfully paired with " + myDevice.getName(), Toast.LENGTH_SHORT)
                            .show();
                    myBluetoothDevice = myDevice;
                }
                if (myDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BOND_BONDING.");
                }
                if (myDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BOND_NONE.");
                }
            }
        }
    };

    // handles connection status changes
    private BroadcastReceiver connectionWatcher = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice myDevice = intent.getParcelableExtra("Device");
            Log.d(TAG, "I'm in BLUETOOTH POPUP");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences",
                    Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
            TextView connStatusTextView = findViewById(R.id.connStatusTextView);

            Log.d(TAG, "I'm working");
            Log.d(TAG, status);
            if (status.equals("connected")) {
                try {
                    myDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to " + myDevice.getName());
                Toast.makeText(Bluetooth.this, "Device now connected to " + myDevice.getName(), Toast.LENGTH_SHORT)
                        .show();
                editor.putString("connStatus", "Connected to " + myDevice.getName());
                connStatusTextView.setText("Connected to " + myDevice.getName());
                connStatusTextView.setTextColor(Color.parseColor("#6BDE42"));
                // connStatusTextView.setTextColor(Color.parseColor("#FFA500"));
            } else if (status.equals("disconnected") && retryConnection == false) {
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from " + myDevice.getName());
                Toast.makeText(Bluetooth.this, "Disconnected from " + myDevice.getName(), Toast.LENGTH_SHORT).show();
                mBluetoothConnection = new BluetoothService(Bluetooth.this);
                sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences",
                        Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("connStatus", "Disconnected");

                connStatusTextView.setText("Disconnected");
                connStatusTextView.setTextColor(getResources().getColor(R.color.dark_green));
                editor.commit();

                try {
                    try {
                        if (myDialog != null)
                            myDialog.show();
                    } catch (Exception e) {
                        Log.d(TAG, "Local dialog failure");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Bluetooth: mBroadcastReceiver5 Dialog show failure");
                }
                retryConnection = true;
                reconnectionHandler.postDelayed(reconnectionRunnable, 5000);

            }

            if (status.equals("disconnected"))
                connStatusTextView.setText("Disconnected");
            editor.commit();
        }
    };

    // establish connection wrapper function
    public void startConnection() {
        startBTConnection(myBluetoothDevice, myUUID);
    }

    // establish connection main function
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection");
        mBluetoothConnection.startClientThread(device, uuid);
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra("myBluetoothDevice", myBluetoothDevice);
        data.putExtra("myUUID", myUUID);
        setResult(RESULT_OK, data);
        super.finish();
    }
}
