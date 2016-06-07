package com.mimickme.bluetoothtictac;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;

/**
 * Created by mimickme on 6/6/2016.
 */
public class BluetoothService
{
    BluetoothAdapter bluetoothAdapter;
    Handler handler;

    public BluetoothService(Handler handler)
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
    }

    public boolean supportBluetooth()
    {
        return !(bluetoothAdapter == null);
    }
}
