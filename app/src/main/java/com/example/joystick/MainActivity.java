package com.example.joystick;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.Manifest;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;
import android.widget.TextView;

import android.bluetooth.BluetoothAdapter;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import android.os.Looper;
import android.os.Message;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice robotBluetoothDevice = null;
    UUID standardUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //default UUID

    private Button connectButton;
    private Button searchButton;
    private TextView statusText;
    private TextView deviceText;
    private TextView deviceReading;
    private Handler uiHandler;
    private ConnectThread currentConnectThread;
    private MyBluetoothService.ConnectedThread readWriteThread;

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        //Joystick, coordenadas y direcciones
        JoystickView joystickView = findViewById(R.id.joystickView);
        TextView coordinateText = findViewById(R.id.coordinateText);
        TextView directionText = findViewById(R.id.directionText);


        //Connexion Bluetooth, busqueda device, estado
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        searchButton = findViewById(R.id.searchButton);
        connectButton = findViewById(R.id.connectButton);
        statusText = findViewById(R.id.status);
        deviceText = findViewById(R.id.deviceText);
        deviceReading = findViewById(R.id.deviceReading);

        //Button sendButton = findViewById(R.id.sendButton);

        //definición del Handler
            uiHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MessageConstants.MESSAGE_TOAST:
                            Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                            break;

                        case MessageConstants.MESSAGE_READ:
                            String value = (String) msg.obj;
                            deviceReading.setText(value);
                            break;

                        case MessageConstants.MESSAGE_WRITE:
                            String sentMessage = (String) msg.obj;
                            Log.d("BT_WRITE", "Mensaje enviado: " + sentMessage);
                            break;

                        case MessageConstants.MESSAGE_DISCONNECTED:
                            cleanSession();
                            break;
                    }
                }
            };


        setupSearchButton();
        setupConnectButton();

        joystickView.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onMove(float xPercent, float yPercent, String direction) {
                coordinateText.setText(String.format("X: %.2f, Y: %.2f", xPercent, yPercent));
                directionText.setText(direction);

                if (!bluetoothAdapter.isEnabled() || !permissionBluetooth()) { //bluetooth está apagado o faltan permisos
                    cleanSession();
                }
                else{
                    //enviar al Arduino
                    if (readWriteThread != null) {
                        String message = String.format("X%.2fY%.2f\n", xPercent, yPercent); //ejemplo: X0.75Y-0.42
                        readWriteThread.write(message.getBytes(StandardCharsets.UTF_8));
                    }

                }

            }
        });
        /* //prueba para un led
        sendButton.setOnClickListener(v -> {
            String command = "on"; // o "off"

            if (readWriteThread != null) {
                readWriteThread.write(command.getBytes());
            }
        });*/


        //barra de estado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.BLACK);
        }

        //margenes
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @SuppressLint("MissingPermission")
    private void setupSearchButton() {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(permissionBluetooth()) {
                    if (!bluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "Bluetooth deshabilitado");

                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        //cuadro de diálogo, solicitará permiso al usuario para habilitar Bluetooth
                    }
                    //obtener el name y address del dispositivo emparejado
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    String deviceFound="";

                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            String deviceName = device.getName();
                            String deviceHardwareAddress = device.getAddress(); //MAC address
                            deviceFound= deviceFound + deviceName + " / "+deviceHardwareAddress+"\n";
                            //deviceFound= deviceFound +deviceHardwareAddress+"\n";
                            if (deviceName.equals("DESKTOP-2S48VJB")) {
                                Log.d(TAG, "HC-05 bluetooth encontrado");
                                standardUUID = device.getUuids()[0].getUuid();
                                robotBluetoothDevice = device;
                                //conectar al dispositivo
                                connectButton.setEnabled(true);
                                //statusText.setText("Status: Connected");
                            }
                            deviceText.setText(deviceFound);
                        }
                    }
                }
                else {
                        requestBluetoothPermission();
                }
            }
        });
    }


    private void setupConnectButton() {
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled() || !permissionBluetooth()) { //bluetooth está apagado o faltan permisos
                    cleanSession();
                } else {
                    //si no hay dispositivo
                    if (robotBluetoothDevice == null) {
                        Toast.makeText(v.getContext(),
                                "El Bluetooth no detecto el dispositivo",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        //cierra hilo de lectura/escritura anterior si existe
                        if (readWriteThread != null) {
                            readWriteThread.cancel();
                            readWriteThread = null;
                        }  // Libera la referencia
                        //flujo de conexion
                        bluetoothConnectionStream();

                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void bluetoothConnectionStream() {
        //lanza en un hilo de fondo la conexión
        new Thread(() -> {
            bluetoothAdapter.cancelDiscovery();//Cancelar discovery para no ralentizar la conexión
            ConnectThread ct = new ConnectThread( //intentar conectar el socket
                    robotBluetoothDevice,
                    standardUUID,
                    uiHandler        //Handler que actualiza deviceReading
            );
            currentConnectThread = ct;
            ct.start();
            try {
                ct.join();  //bloquea este hilo de fondo hasta que ct.run() termine
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrumpido esperando ConnectThread", e);
                Thread.currentThread().interrupt();
            }

            if (ct.getMmSocket() != null && ct.getMmSocket().isConnected()) {
                runOnUiThread(() -> statusText.setText("Status: Connected"));/***********/
                //conecto, entonces arranca el hilo de lectura
                MyBluetoothService btService = new MyBluetoothService(uiHandler);
                readWriteThread = btService.new ConnectedThread(ct.getMmSocket());
                readWriteThread.start();
            } else {
                //si falla actualiza la ui
                runOnUiThread(() -> statusText.setText("Status: Disconnected"));
            }
        }, "BT-Connect-Thread").start();

    }
    private void cleanSession(){
        deviceText.setText("");
        deviceReading.setText("");
        statusText.setText("Status: Disconnected");
        terminateBluetoothSession();
    }

    private void terminateBluetoothSession() {
        if (readWriteThread != null) {
            readWriteThread.cancel(); //cierra el socket
            readWriteThread = null;   // Libera la referencia
        }

        if (currentConnectThread != null) {
            currentConnectThread.cancel();
            currentConnectThread = null;   // Libera la referencia
        }
        //no hay un dispositivo seleccionado
        robotBluetoothDevice = null;
    }

    //Verifica si hay bluetooth y si tiene los permisos, sino hace el request
    private boolean permissionBluetooth() {
        if (!isBluetoothAvailable()) {
            Toast.makeText(this, "Bluetooth no disponible en este dispositivo", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!bluetoothPermissionGranted(this)) {
            requestBluetoothPermission();
            return false;
        }

        Log.d(TAG, "Bluetooth disponible y permisos concedidos");
        return true;
    }

    public boolean isBluetoothAvailable() { //chequea si el dispositivo tiene bluetooth
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    public boolean bluetoothPermissionGranted(Context context) //chequea si el dispositivo tiene los permisos
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //android 12+: este sí es un permiso “dangerous”
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED;

        } else {
            //<=android 10: BLUETOOTH es normal y ya está concedido
            return true;
        }
    }

    public void requestBluetoothPermission() { //request de los permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //Android 12+
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    PERMISSION_REQUEST_CODE
            );
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, //respuesta del usuario a los permisos
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                Toast.makeText(this, "Permiso de Bluetooth concedido", Toast.LENGTH_SHORT).show();

            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de Bluetooth no habilitado", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
