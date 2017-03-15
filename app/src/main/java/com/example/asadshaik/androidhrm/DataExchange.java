package com.example.asadshaik.androidhrm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by AsadShaik on 13/02/17.
 */

public class DataExchange {

    private static final String TAG = "DataExchange";


    public static final String NAME_SECURE = "BluetoothChatSecure";
    public static final String NAME_INSECURE = "BluetoothChatInsecure";

    public static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mhandler;
    private AcceptThread secureAcceptThread;
    private AcceptThread insecureAcceptthread;
    private ConnectThread mConnectThread;
    private Connectedthread mConnectedThread;


    private int mstate;
    private int mnewState;



    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device



    public DataExchange(Context context,Handler handler){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mstate=STATE_NONE;
        mnewState = mstate;
        mhandler=handler;
    }


    private synchronized void updateUserInterfaceTitle(){
        mstate = getState();
        Log.d(TAG,"Update user interface title"+mnewState+"->"+mstate);
        mnewState = mstate;
        mhandler.obtainMessage(DataConstants.MESSAGE_STATE_CHANGE,mnewState,-1).sendToTarget();
    }

    public synchronized int getState(){
        return mstate;
    }

    public synchronized void start(){
        Log.d(TAG,"Start");


        if(mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if(secureAcceptThread == null){
            secureAcceptThread = new AcceptThread(true);
            secureAcceptThread.start();
        }

        if (insecureAcceptthread == null){
            secureAcceptThread = new AcceptThread(false);
            secureAcceptThread.start();
        }

        updateUserInterfaceTitle();
    }


    public synchronized void connect(BluetoothDevice device, boolean secure){
        Log.d(TAG,"connect to:"+device);

        if(mstate==STATE_CONNECTING){
            if (mConnectThread!=null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device,secure);
        mConnectThread.start();

        updateUserInterfaceTitle();

    }

    public synchronized void connected(BluetoothSocket socket,BluetoothDevice device,
                                       final String socketType){
        Log.d(TAG,"connected,Socket Type:"+socketType);

        if (mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread=null;
        }

        if(secureAcceptThread!=null){
            secureAcceptThread.cancel();
            secureAcceptThread = null;
        }

        if (insecureAcceptthread!=null){
            insecureAcceptthread.cancel();
            insecureAcceptthread = null;
        }
        mConnectedThread = new Connectedthread(socket,socketType);
        mConnectedThread.start();

        Message msg = mhandler.obtainMessage(DataConstants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DataConstants.device_name,device.getName());
        msg.setData(bundle);
        mhandler.sendMessage(msg);


        updateUserInterfaceTitle();

    }

    public synchronized void stop(){
        Log.d(TAG,"stop");

        if (mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread = null;

        }

        if (mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (secureAcceptThread!=null){
            secureAcceptThread.cancel();
            secureAcceptThread = null;
        }

        if (insecureAcceptthread!=null){
            insecureAcceptthread.cancel();
            insecureAcceptthread =null;
        }
        mstate = STATE_NONE;

        updateUserInterfaceTitle();
    }

    public void write(byte[] out){
        Connectedthread r;

        synchronized (this){
            if(mstate!=STATE_CONNECTED) return;
            r=mConnectedThread;
        }

        r.write(out);


    }

    private void connectionfailed(){
        Message msg = mhandler.obtainMessage(DataConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(DataConstants.toast,"Unable to connect to device");
        msg.setData(bundle);
        mhandler.sendMessage(msg);

        mstate = STATE_NONE;

        updateUserInterfaceTitle();

        DataExchange.this.start();
    }

    private void connectionLost(){
        Message msg = mhandler.obtainMessage(DataConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(DataConstants.toast,"Connection has Lost");
        msg.setData(bundle);
        mhandler.sendMessage(msg);

        mstate=STATE_NONE;

        updateUserInterfaceTitle();

        DataExchange.this.start();
    }


    //AcceptThread Class
    private class AcceptThread extends Thread{

        private final BluetoothServerSocket serverSocket;
        private String socketType;


        public AcceptThread(boolean secure){
            BluetoothServerSocket tmp = null;
            socketType = secure ? "Secure" : "Insecure";


            try{
                if(secure){
                    tmp=bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,MY_UUID_SECURE);

                }
                else{
                    tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE,MY_UUID_INSECURE);

                }
            }catch (IOException e){
                Log.e(TAG,"Socket Type:"+ socketType +"listen() failed",e);
            }
            serverSocket = tmp;
            mstate = STATE_LISTEN;


        }

        public void run(){
            Log.d(TAG, "Socket Type:" +socketType+ "Begin acceptThread"+this);
            setName("Accept Thread" + socketType);

            BluetoothSocket socket = null;

            while(mstate!=STATE_CONNECTING){

                try{


                    socket = serverSocket.accept();

                }catch(IOException e){
                    Log.e(TAG, "Socket Type:" + socketType + "accept() failed",e);
                    break;
                }

                //If connection was accepted
                if(socket!=null){

                    synchronized (DataExchange.this){
                        switch(mstate){
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket,socket.getRemoteDevice(),socketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try{
                                    socket.close();
                                }catch (IOException e){
                                    Log.e(TAG,"Could not close unwanted socket",e);
                                }
                                break;
                        }


                    }


                }


            }
            Log.i(TAG,"End acceptThread,Socket Type:"+socketType);

        }
        public void cancel(){
            Log.d(TAG,"Socket Type" + socketType + this);
            try{
                serverSocket.close();
            }catch (IOException e){
                Log.e(TAG,"Socket Type" + socketType + "close() of server failed", e);
            }
        }


    }



    private class ConnectThread extends Thread{

        private final BluetoothSocket btsocket;
        private final BluetoothDevice btdevice;
        private String socketType;

        public ConnectThread(BluetoothDevice device,boolean secure){
            btdevice = device;
            BluetoothSocket tmp = null;
            socketType = secure ? "Secure" : "Insecure";

            try{
                if(secure){
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                }else{
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            }catch (IOException e){
                Log.e(TAG,"Socket Type:" + socketType + "create() failed",e);
            }
            btsocket = tmp;
            mstate = STATE_CONNECTING;
        }


        public void run(){
            Log.i(TAG,"Begin connectedThread, Socket Type:" + socketType);
            setName("ConnectThread" + socketType);

            bluetoothAdapter.cancelDiscovery();

            try{
                btsocket.connect();

            }catch (IOException e){
                try{
                    btsocket.close();
                }catch (IOException e1){
                    Log.e(TAG,"Unable to close()" + socketType + "socket during connection failure",e1);

                }
                connectionfailed();
                return;
            }

            synchronized (DataExchange.this){
                mConnectThread = null;
            }

            connected(btsocket,btdevice,socketType);
        }
        public void cancel(){
            try{
                btsocket.close();
            }catch (IOException e){
                Log.e(TAG,"close() of connect" + socketType,e);
            }

        }

    }



    private class Connectedthread extends Thread{
        private final BluetoothSocket btsocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public Connectedthread(BluetoothSocket bluetoothSocket,String socketType){
            InputStream tmpIn;
            OutputStream tmpOut;

            btsocket = bluetoothSocket;
            tmpIn = null;
            tmpOut = null;
            try{
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();
            }catch (IOException e){
                Log.e(TAG,"streaming failed,Socket Type:" + socketType,e);
            }

            inStream = tmpIn;
            outStream = tmpOut;
            mstate = STATE_CONNECTED;
        }

        public void run(){
            Log.i(TAG,"Begin connectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (mstate == STATE_CONNECTED){
                try{
                    bytes = inStream.read(buffer);
                    mhandler.obtainMessage(DataConstants.MESSAGE_READ,bytes,-1,buffer)
                            .sendToTarget();
                }catch (IOException e){
                    Log.e(TAG,"Disconnected",e);
                    connectionLost();
                    break;
                }
            }
        }


        public void write(byte[] buffer){
            try{
                outStream.write(buffer);
                mhandler.obtainMessage(DataConstants.MESSAGE_WRITE,-1,-1,buffer)
                        .sendToTarget();
            }catch (IOException e){
                Log.e(TAG,"Exception during write",e);
            }
        }
        public void cancel(){
            try{
                btsocket.close();
            }catch (IOException e){
                Log.e(TAG,"close() socket connect failed",e);
            }

        }




    }

}
