package com.example.joystick;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import android.os.Handler;

import android.os.Message;


public class MyBluetoothService {
    private Handler handler;

    public MyBluetoothService(Handler handler) {
        this.handler = handler;
    }

    //Hilo que gestiona la conexión una vez conectado al socket.
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //input,output streams; se usa objetos temp porque streams son finales
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("Connected", "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Connected", "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes=0;

            while (true) {
                try {
                    //Lectura
                    int data = mmInStream.read(); //lee un byte a la vez
                    if (data == -1) throw new IOException("Stream cerrado"); //fin de stream

                    mmBuffer[numBytes++] = (byte) data;

                    // Si se detecta fin de línea, procesar el mensaje
                    if ((byte) data == '\n') {

                        //convertir los bytes leídos a String
                        String readMessage = new String(mmBuffer, 0, numBytes, StandardCharsets.UTF_8).trim();

                        //enviar msj al handler

                        Message readMsg = handler.obtainMessage(
                                com.example.joystick.MessageConstants.MESSAGE_READ, readMessage);
                        readMsg.sendToTarget();
                        numBytes = 0; //reiniciar buffer
                    }
                } catch (IOException e) {
                    Log.d("Connected", "Input stream desconectado", e);
                    handler.obtainMessage(MessageConstants.MESSAGE_DISCONNECTED).sendToTarget();
                    break;
                }
            }
            cancel();
        }

        //funcion para enviar datos, main activity
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                String sentMessage = new String(bytes, StandardCharsets.UTF_8);
                Message writtenMsg = handler.obtainMessage(
                        com.example.joystick.MessageConstants.MESSAGE_WRITE, sentMessage);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e("Connected", "Error al enviar los datos", e);

                Message writeErrorMsg =
                        handler.obtainMessage(com.example.joystick.MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "No se pudo enviar los datos al otro dispositivo");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
            }
        }

        //funcion para cerrar la conexion, usar en main activity
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Connected", "No se pudo cerrar la conexion al socket", e);
            }
        }
    }
}



