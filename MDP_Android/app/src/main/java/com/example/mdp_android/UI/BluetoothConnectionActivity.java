package com.example.mdp_android.UI;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.mdp_android.R;
import com.example.mdp_android.controller.BluetoothController;

public class BluetoothConnectionActivity extends AppCompatActivity {

    private TextView availableDevicesTitle;
    private TextView connectedDevicesTitle;
    private ListView availableDeviceList;
    private ListView connectedDeviceList;

    private RecyclerView connectedDeviceRecyclerView;
    private RecyclerView availableDeviceRecyclerView;

    BluetoothController bluetoothController;

    private List<String> pairedDeviceNames = new ArrayList<>();
    private List<String> availableDeviceNames = new ArrayList<>();


    private RecyclerView.Adapter connectedDevicesAdapter;
    private RecyclerView.Adapter availableDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bluetooth_connection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        availableDevicesTitle = findViewById(R.id.availableDevicesTitle);
        connectedDevicesTitle = findViewById(R.id.connectedDevicesTitle);
        availableDeviceList = findViewById(R.id.availableDeviceList);
        connectedDeviceList = findViewById(R.id.connectedDeviceList);
        connectedDeviceRecyclerView= findViewById(R.id.connectedDevicesRecyclerView);
        availableDeviceRecyclerView= findViewById(R.id.availableDevicesRecyclerView);

        bluetoothController = new BluetoothController(this);

        bluetoothController.checkPermission();

        connectedDeviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        availableDeviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        displayConnectedDevices();

        displayAvailableDevices();

    }


    private void displayConnectedDevices() {
        //Connected devices
        Set<BluetoothDevice> pairedDevices = bluetoothController.getPairedDevices();
        Log.d("bluetooth1", "pairedDevices size: " + pairedDevices.size());
        pairedDeviceNames.clear();
        bluetoothController.checkPermission();

        for (BluetoothDevice device : pairedDevices) {
            String deviceInfo = device.getName() + "\n" + device.getAddress();  // Name and address
            pairedDeviceNames.add(deviceInfo);
            Log.d("bluetooth1", device.getName());
        }
        Log.d("bluetooth1", "deviceNames size: " + pairedDeviceNames.size());


        if (connectedDevicesAdapter == null) {
            connectedDevicesAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                    return new RecyclerView.ViewHolder(view) {};
                }

                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                    TextView text1 = holder.itemView.findViewById(android.R.id.text1);
                    TextView text2 = holder.itemView.findViewById(android.R.id.text2);
                    String deviceInfo = pairedDeviceNames.get(position);
                    String[] deviceParts = deviceInfo.split("\n");  // Split name and address
                    text1.setText(deviceParts[0]);
                    text2.setText(deviceParts[1]);
                }

                @Override
                public int getItemCount() {
                    return pairedDeviceNames.size();
                }
            };
            connectedDeviceRecyclerView.setAdapter(connectedDevicesAdapter);
            Log.d("bluetooth1","Adapter updated");
            connectedDevicesAdapter.notifyDataSetChanged();
        }
    }

    private void displayAvailableDevices() {
        // Clear previous device list
        availableDeviceNames.clear();
        bluetoothController.checkPermission();
        bluetoothController.startDiscovery1();

        // Initialize the RecyclerView and its Adapter
        availableDeviceRecyclerView = findViewById(R.id.availableDevicesRecyclerView);

        // Set the adapter immediately, even if the list is empty for now
        if (availableDevicesAdapter == null) {
            availableDevicesAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    // Inflate the item layout (e.g., simple list item layout)
                    View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                    return new RecyclerView.ViewHolder(view) {};
                }

                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                    // Bind device name and address to the views
                    TextView text1 = holder.itemView.findViewById(android.R.id.text1);
                    TextView text2 = holder.itemView.findViewById(android.R.id.text2);
                    String deviceInfo = availableDeviceNames.get(position);
                    String[] deviceParts = deviceInfo.split("\n");  // Split name and address
                    text1.setText(deviceParts[0]);
                    text2.setText(deviceParts[1]);
                }

                @Override
                public int getItemCount() {
                    return availableDeviceNames.size();
                }
            };

            availableDeviceRecyclerView.setAdapter(availableDevicesAdapter);
        }

        // Use a Handler to call getAvailableDevices() after 30 seconds (30000 milliseconds)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothController.checkPermission();
                // Get available devices and store them in the list
                Set<BluetoothDevice> availableDevices = bluetoothController.getAvailableDevices();
                for (BluetoothDevice device : availableDevices) {
                    String deviceInfo = device.getName() + "\n" + device.getAddress();  // Name and address
                    availableDeviceNames.add(deviceInfo);  // Add to list
                }

                // Notify the adapter that data has changed
                availableDevicesAdapter.notifyDataSetChanged();
                Log.d("bluetooth1","taking device names");
            }
        }, 30000);  // 30 seconds delay
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        //unregisterReceiver(deviceFoundReceiver);
        bluetoothController.stopDiscovery();
    }
}

