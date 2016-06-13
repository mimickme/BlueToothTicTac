package com.mimickme.bluetoothtictac;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by mimickme on 6/6/2016.
 */
public class BluetoothService
{
    BluetoothAdapter bluetoothAdapter;
    Handler handler;
    ArrayList<BluetoothDevice> deviceList;
    int state;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final String CONNECT_NAME = "BlueToothService-Device";
    private static final UUID APP_UUID = UUID.fromString("79b45612-f330-4e46-bb0f-ffb4444e82a4");

    public BluetoothService(Handler handler)
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
        deviceList = new ArrayList<>();
        state = STATE_NONE;
    }

    public boolean supportBluetooth()
    {
        return !(bluetoothAdapter == null);
    }

    public BluetoothAdapter getBluetoothAdapter()
    {
        return bluetoothAdapter;
    }

    public BluetoothDevice addDevice(String address)
    {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        deviceList.add(device);

        return device;
    }

    public void connect(BluetoothDevice device)
    {
        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING)
        {
            if (connectThread != null)
            {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null)
        {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost()
    {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    private void setState(int state)
    {
        this.state = state;

        // Give the new state to the Handler so the UI Activity can update
        handler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized void start()
    {
        // Cancel any thread attempting to make a connection
        if (connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null)
        {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (acceptThread == null)
        {
            acceptThread = new AcceptThread(true);
            acceptThread.start();
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop()
    {
        if (connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null)
        {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null)
        {
            acceptThread.cancel();
            acceptThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out)
    {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this)
        {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed()
    {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState()
    {
        return state;
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
    {

        // Cancel the thread that completed the connection
        if (connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null)
        {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (acceptThread != null)
        {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread
    {
        // The local server socket
        private final BluetoothServerSocket serverSocket;

        public AcceptThread(boolean secure)
        {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try
            {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(CONNECT_NAME, APP_UUID);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            serverSocket = tmp;
        }

        public void run()
        {
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (state != STATE_CONNECTED)
            {
                try
                {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = serverSocket.accept();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    break;
                }

                // If a connection was accepted
                if (socket != null)
                {
                    synchronized (BluetoothService.this)
                    {
                        switch (state)
                        {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try
                                {
                                    socket.close();
                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel()
        {
            try
            {
                serverSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device)
        {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try
            {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);

            } catch (IOException e)
            {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run()
        {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            }
            catch (IOException e)
            {
                // Close the socket
                try
                {
                    mmSocket.close();
                }
                catch (IOException e2)
                {
                    e.printStackTrace();
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this)
            {
                connectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (state == STATE_CONNECTED)
            {
                try
                {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                }
                catch (IOException e)
                {
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer)
        {
            try
            {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                handler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
