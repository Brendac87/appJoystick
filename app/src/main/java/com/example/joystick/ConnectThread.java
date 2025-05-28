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
        this.handler=handler;
        try {

            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e("Connect", "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        //bluetoothAdapter.cancelDiscovery(); //Detiene cualquier escaneo
        try {
            //Conecta con el disposito a traves del socket
            mmSocket.connect();
            sendToast("Conexión establecida con éxito");
        } catch (IOException connectException) {
            //No se pudo conectar cierra el socket y regresa

            Log.e("Connect", "connectException: " + connectException);
            cancel();
            if (handler != null) {
                Message readFailMsg = handler.obtainMessage(MessageConstants.MESSAGE_READ);
                readFailMsg.obj = "--";
                handler.sendMessage(readFailMsg);
            }
            sendToast("Error al conectar con el dispositivo Bluetooth");
            return;
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
            Log.e("Connect", "Could not close the client socket", e);
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