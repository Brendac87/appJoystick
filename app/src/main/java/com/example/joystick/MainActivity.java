package com.example.joystick;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
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
    private TextView coordinateText;
    private TextView directionText;

    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private JoystickView joystickView;
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


        //referencias a los botones
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);




        //definición del Handler
            uiHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MessageConstants.MESSAGE_TOAST:
                            Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                            break;

                        case MessageConstants.MESSAGE_READ:
                            //String value = (String) msg.obj;

                            break;

                        case MessageConstants.MESSAGE_WRITE:
                            String sentMessage = (String) msg.obj;
                            Log.d("BT_WRITE", "Mensaje enviado: " + sentMessage);
                            break;

                        case MessageConstants.MESSAGE_DISCONNECTED:
                            isConnected = false;
                            cleanSession();

                            break;
                        case MessageConstants.MESSAGE_CONNECTION_SUCCESS:
                            handleConnectionSuccess((BluetoothSocket) msg.obj);
                            break;

                        case MessageConstants.MESSAGE_CONNECTION_FAILED:
                            isConnected = false;
                            handleConnectionFailure();

                            break;

                        case MessageConstants.MESSAGE_CONNECTION_IN_PROGRESS:
                            statusText.setText("Status: Connecting...");
                            break;
                    }
                }
            };


        setupSearchButton();
        setupConnectButton();
        setupOptionButton();

        joystickView.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onMove(float xPercent, float yPercent, String direction) {
                float invertedY=-yPercent;
                coordinateText.setText(String.format("X: %.2f, Y: %.2f", xPercent, invertedY));
                directionText.setText(direction);

                if (!bluetoothAdapter.isEnabled() || !permissionBluetooth()) { //bluetooth está apagado o faltan permisos
                    cleanSession();
                }
                else{
                    //enviar al Arduino
                    if (readWriteThread != null && isConnected && !direction.equals("Idle")) { //se agrega el ; y el eje y invertido
                        String message = String.format("X%.2fY%.2f;\n", xPercent, invertedY); //ejemplo: X0.75Y0.42
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
                            //deviceFound= deviceFound + deviceName + "\n";
                            deviceFound= deviceFound + deviceName + " | "+deviceHardwareAddress+"\n";
                            //deviceFound= deviceFound +deviceHardwareAddress+"\n";
                            if (deviceName.equals("HC-05")) { //esta el nombre del bluetooth por defecto del HC5
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
    private void setupOptionButton() {
        setupCommandButton(button1, "A"); //opciones de los botones
        setupCommandButton(button2, "B");
        setupCommandButton(button3, "C");
        setupCommandButton(button4, "D");
    }

    private void setupCommandButton(Button button, String command){
        if (button != null) {
            button.setOnClickListener(v -> {
                if (readWriteThread != null && isConnected) {
                    readWriteThread.write((command + ";").getBytes(StandardCharsets.UTF_8)); //se envia la opcion + ; | ejemplo: 2; 4; | se puede agregar salto de linea dependiendo de la conf del arduino
                    uiHandler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Enviado: " + command).sendToTarget();

                } else {
                    uiHandler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Sin conexión con el dispositivo").sendToTarget();
                }
            });
        }

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
        isConnected = true;
        updateButtonEnabledState();

    }

    private void handleConnectionFailure() {
        runOnUiThread(() -> {
            statusText.setText("Status: Connection Failed");

        });
        terminateBluetoothSession();
        updateButtonEnabledState();
    }

    private void updateButtonEnabledState()
    {
        boolean enabled = isConnected;
        button1.setEnabled(enabled);
        button2.setEnabled(enabled);
        button3.setEnabled(enabled);
        button4.setEnabled(enabled);
    }


    //Funciones de limpieza de sesion, termina los hilos
    private void cleanSession(){
        deviceText.setText("");
        statusText.setText("Status: Disconnected");
        terminateBluetoothSession();
        //no hay un dispositivo seleccionado
        robotBluetoothDevice = null;
        updateButtonEnabledState();
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
