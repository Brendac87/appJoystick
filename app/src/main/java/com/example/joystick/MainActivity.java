package com.example.joystick;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
    private TextView coordinateText; // Declarada aquí
    private TextView directionText;  // Declarada aquí
    private JoystickView joystickView; // Declarada aquí
    private Handler uiHandler;
    private ConnectThread currentConnectThread;
    private MyBluetoothService.ConnectedThread readWriteThread;

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean isConnected;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);





        //Joystick, coordenadas y direcciones
        joystickView = findViewById(R.id.joystickView);
        coordinateText = findViewById(R.id.coordinateText);
        directionText = findViewById(R.id.directionText);


        //Connexion Bluetooth, busqueda device, estado
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        searchButton = findViewById(R.id.searchButton);
        connectButton = findViewById(R.id.connectButton);
        statusText = findViewById(R.id.status);
        deviceText = findViewById(R.id.deviceText);
        deviceReading = findViewById(R.id.deviceReading);




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
                            isConnected = false;
                            break;
                        case MessageConstants.MESSAGE_CONNECTION_SUCCESS:
                            handleConnectionSuccess((BluetoothSocket) msg.obj);
                            break;

                        case MessageConstants.MESSAGE_CONNECTION_FAILED:
                            handleConnectionFailure();
                            isConnected = false;
                            break;

                        case MessageConstants.MESSAGE_CONNECTION_IN_PROGRESS:
                            statusText.setText("Status: Connecting...");
                            //connectButton.setEnabled(false);
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
                    if (readWriteThread != null && isConnected) {
                        String message = String.format("X%.2fY%.2f\n", xPercent, yPercent); //ejemplo: X0.75Y-0.42
                        readWriteThread.write(message.getBytes(StandardCharsets.UTF_8));
                    }

                }

            }
        });




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
                                //connectButton.setEnabled(true);
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
                } else if (robotBluetoothDevice == null) {
                    Toast.makeText(v.getContext(),
                            "No se detecto el dispositivo", //si no hay dispositivo
                            Toast.LENGTH_SHORT).show();
                } else if (!isConnected) {
                    terminateBluetoothSession();
                    bluetoothConnectionStream();
                }
            }
        });


    }

    @SuppressLint("MissingPermission")
    private void bluetoothConnectionStream() {
        //lanza en un hilo de fondo la conexión

        new Thread(() -> {
            bluetoothAdapter.cancelDiscovery();//Cancelar discovery para no ralentizar la conexión

            currentConnectThread = new ConnectThread( //intentar conectar el socket
                    robotBluetoothDevice,
                    standardUUID,
                    uiHandler     //Handler que actualiza deviceReading o el estado de la conexion
            );

            currentConnectThread.start();
            try {
                currentConnectThread.join();  //espera este hilo de fondo hasta que ct.run() termine
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrumpido esperando ConnectThread", e);
                Thread.currentThread().interrupt();
            }
        }, "BT-Connect-Thread").start();

    }

    //Funciones de manejo en caso de Conexion exitosa o fallida
    private void handleConnectionSuccess(BluetoothSocket socket) {
        runOnUiThread(() -> statusText.setText("Status: Connected"));

        // Iniciar hilo de lectura/escritura
        MyBluetoothService btService = new MyBluetoothService(uiHandler);
        readWriteThread = btService.new ConnectedThread(socket);
        readWriteThread.start();

        connectButton.setEnabled(true);
        isConnected = true;
    }

    private void handleConnectionFailure() {
        runOnUiThread(() -> {
            statusText.setText("Status: Connection Failed");
            connectButton.setEnabled(true);
        });
        terminateBluetoothSession();
    }


    //Funciones de limpieza de sesion, termina los hilos

    private void cleanSession(){
        deviceText.setText("");
        deviceReading.setText("");
        statusText.setText("Status: Disconnected");
        terminateBluetoothSession();
        //no hay un dispositivo seleccionado
        robotBluetoothDevice = null;
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
    }

    //Funciones de Bluetooth, Verifica si hay bluetooth, los permisos, el request y el resultado
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
            // Android 12+ (API 31+)
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R || Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android 10 y 11
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 9 o menor: permisos normales, ya están concedidos
            return true;
        }
    }

    public void requestBluetoothPermission() { //request de los permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    },
                    PERMISSION_REQUEST_CODE
            );
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R || Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android 10 y 11
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE
            );
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, //respuesta del usuario a los permisos
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (!granted) {
                Toast.makeText(this, "Permisos de Bluetooth denegados", Toast.LENGTH_SHORT).show();
            } 
        }

    }






}
