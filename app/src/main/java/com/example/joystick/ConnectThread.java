package com.example.joystick;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;


public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private Handler handler;


    @SuppressLint("MissingPermission")
    public ConnectThread(BluetoothDevice device, UUID MY_UUID, Handler handler) {
        //Usa un objeto temporal para luego asignarlo a mmSocket porque es final
        BluetoothSocket tmp = null;
        this.handler = handler;
        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e("Connect", "El metodo create() fallo", e);
        }
        mmSocket = tmp;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        //bluetoothAdapter.cancelDiscovery(); //Detiene cualquier escaneo
        sendToast("a punto de conectar");
        handler.sendEmptyMessage(MessageConstants.MESSAGE_CONNECTION_IN_PROGRESS);
        sendToast("a punto de conectar2");
        try {
            //Conecta con el disposito a traves del socket
            mmSocket.connect();
            sendToast("Conexión establecida con éxito");
            // Notificar éxito con el socket
            Message successMsg = handler.obtainMessage(
                    MessageConstants.MESSAGE_CONNECTION_SUCCESS,
                    mmSocket
            );
            handler.sendMessage(successMsg);
        } catch (IOException connectException) {
            //No se pudo conectar cierra el socket y regresa
            sendToast("Error CONEXION ON CONNECT THREAD");

            Log.e("Connect", "connectException: " + connectException);
            handler.sendEmptyMessage(MessageConstants.MESSAGE_CONNECTION_FAILED);
            cancel();



        }

    }
    public BluetoothSocket getMmSocket(){
        return mmSocket;
    }

    //cierra el socket y finaliza el thread
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e("Connect", "No se pudo cerrar el socket", e);
        }
    }

    private void sendToast(String text) {
        if (handler != null) {
            Message msg = handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            msg.obj = text;
            handler.sendMessage(msg);
        }
    }
}