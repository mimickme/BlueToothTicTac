package com.mimickme.bluetoothtictac;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by mimickme on 6/6/2016.
 */
public class BluetoothService
{
    BluetoothAdapter bluetoothAdapter;

    public void BluetoothHandler()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean supportBluetooth()
    {
        return !(bluetoothAdapter == null);
    }
}
