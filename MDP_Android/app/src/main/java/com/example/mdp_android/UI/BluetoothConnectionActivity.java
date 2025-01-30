package com.example.mdp_android.UI;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Set;

import com.example.mdp_android.R;
import com.example.mdp_android.controller.BluetoothController;

public class BluetoothConnectionActivity extends AppCompatActivity {

    private TextView availableDevicesTitle;

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

        //BluetoothController bluetoothController = (BluetoothController) getIntent().getParcelableExtra("bluetoothController");
        BluetoothController bluetoothController = new BluetoothController(this);


        Set<BluetoothDevice> pairedDevices = bluetoothController.getPairedDevices();
        StringBuilder pairedDevicesText = new StringBuilder();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
            }
        }

        for (BluetoothDevice device : pairedDevices) {
            pairedDevicesText.append(device.getName()).append("\n");
            if (pairedDevices.size() > 0) {
                availableDevicesTitle.setText(pairedDevicesText.toString());
            } else {
                availableDevicesTitle.setText("No paired devices found.");
            }
        }
    }

}