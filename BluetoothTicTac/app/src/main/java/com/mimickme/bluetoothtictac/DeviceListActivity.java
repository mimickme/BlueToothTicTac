package com.mimickme.bluetoothtictac;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity
{
    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    /**
     * Member fields
     */
    private BluetoothAdapter btAdapter;

    /**
     * Newly discovered devices
     */
    private ArrayAdapter<String> newDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list_activity);

        // In the case the user backs out before choosing anything
        setResult(Activity.RESULT_CANCELED);

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_item);
        newDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_item);

        ListView pairedList = (ListView) findViewById(R.id.pairedDeviceList);
        pairedList.setAdapter(pairedDevicesArrayAdapter);
        pairedList.setOnItemClickListener(listClickListener);

        ListView discoveredList = (ListView) findViewById(R.id.newDeviceList);
        discoveredList.setAdapter(newDevicesArrayAdapter);
        discoveredList.setOnItemClickListener(listClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.INVISIBLE);
            }
        });

        // Get the local Bluetooth adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0)
        {
            findViewById(R.id.pairedDeviceText).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices)
            {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else
        {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    private void doDiscovery()
    {
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        newDevicesArrayAdapter.clear();

        // Turn on sub-title for new devices
        findViewById(R.id.newDeviceText).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (btAdapter.isDiscovering())
        {
            btAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        btAdapter.startDiscovery();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                // When discovery is finished, change the Activity title
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                Button scanButton = (Button) findViewById(R.id.scanButton);
                scanButton.setVisibility(View.VISIBLE);
                if (newDevicesArrayAdapter.getCount() == 0)
                {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    newDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            btAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

}
