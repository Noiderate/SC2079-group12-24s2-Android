package com.example.mdp_android.UI;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mdp_android.R;
import com.example.mdp_android.controller.BluetoothController;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> requestBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        requestBluetooth = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(androidx.activity.result.ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            // Bluetooth was granted
                            Toast.makeText(MainActivity.this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                        } else {
                            // Bluetooth was denied
                            Toast.makeText(MainActivity.this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );


        Button bluetoothButton = findViewById(R.id.bluetoothButton);

        bluetoothButton.setOnClickListener(v -> {

            Log.d("bluetooth1", "bluetooth button was clicked, we gonna change to a new screen now");
            // im gonna fking kms do i just suck at coding or what
            BluetoothController bluetoothController = new BluetoothController(this, requestBluetooth);
            bluetoothController.getBluetoothAdapter();
            boolean isBluetoothEnabled = bluetoothController.enableBluetooth();

            if (isBluetoothEnabled) {
                Intent openBluetoothConnectionActivity = new Intent(this, BluetoothConnectionActivity.class);
                //openBluetoothConnectionActivity.putExtra("bluetoothController", bluetoothController);
                startActivity(openBluetoothConnectionActivity);
            }
        });
    }
}