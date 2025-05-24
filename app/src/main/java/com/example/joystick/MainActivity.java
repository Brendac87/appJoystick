package com.example.joystick;

import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;

import android.view.View;
import android.view.Window;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;
import android.widget.TextView;

import android.bluetooth.BluetoothAdapter;
import android.widget.Toast;




public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private Button connectButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //Joystick, coordenadas y direcciones
        JoystickView joystickView = findViewById(R.id.joystickview);
        TextView coordinateText = findViewById(R.id.coordinate_text);
        TextView directionText = findViewById(R.id.directionText);

        joystickView.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onMove(float xPercent, float yPercent, String direction) {
                coordinateText.setText(String.format("X: %.2f, Y: %.2f", xPercent, yPercent));
                directionText.setText(direction);

            }
        });

        setupConnectButton();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.BLACK);
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupConnectButton() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectButton = findViewById(R.id.connect_button);
        statusText = findViewById(R.id.status);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "Bluetooth deshabilitado", Toast.LENGTH_SHORT).show();
                    statusText.setText("Status: Disconnected");
                    return;
                }

                boolean conexionExitosa = intentarConexionConRobot();
                if (conexionExitosa) {
                    statusText.setText("Status: Connected");
                    Toast.makeText(MainActivity.this, "Conectado al robot", Toast.LENGTH_SHORT).show();
                } else {
                    statusText.setText("Status: Disconnected");
                    Toast.makeText(MainActivity.this, "No se pudo conectar al robot", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private boolean intentarConexionConRobot() {
        //aca establecer la conexion con el robot
        return Math.random() < 0.8;
    }
}
