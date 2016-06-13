package com.mimickme.bluetoothtictac;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE_BT = 2;
    private static final int DISCOVER_DEVICE = 3;

    private BluetoothService btService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btService = new BluetoothService(handler);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(btReceiver,filter);

        if (!btService.supportBluetooth())
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.run_Bluetooth)
        {
            if (!btService.getBluetoothAdapter().isEnabled())
            {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                return true;
            }
            else
            {
                Intent deviceListIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(deviceListIntent, DISCOVER_DEVICE);
                return true;
            }
        }
        else if (id == R.id.make_discoverable)
        {
            if (btService.getBluetoothAdapter().isEnabled())
            {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,140);
                startActivityForResult(enableIntent, REQUEST_DISCOVERABLE_BT);
                return true;
            }
        }
        else if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void joinBluetoothScanner(View view)
    {
    }

    public void hostBluetoothScanner(View view)
    {
    }

    public void makeMove()
    {
        // Should be called when one of the tile buttons are clicked
        // Will check to see if its our turn to make a move
        // Will place the move into the array/or whatever we're storing it in, validate whether any victory or tie conditions have been met
        // If the game is still going, we'll stream the move other to our opponent, so this will call whatever does the streaming

        sendMove("");
    }

    public void receiveMove(String incomingData)
    {
        // Something like this will be called by whatever is handling incoming bluetooth traffic
        // This will check if it is the opponent's turn to make a move, if so, accept the move
        // This will place the move into our array/whatever and validate victory or tie conditions
        // If the game is proceeding, we should flag that is our our player's move
    }

    private void sendMove(String outgoingData)
    {
        byte[] sendMessage = outgoingData.getBytes();
        btService.write(sendMessage);
    }

    private final Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1)
                    {
                        case BluetoothService.STATE_CONNECTED:
                            ((TextView) findViewById(R.id.gameStateText)).setText("Connected");
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            ((TextView) findViewById(R.id.gameStateText)).setText("Connecting");
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            ((TextView) findViewById(R.id.gameStateText)).setText("Nothing");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //This is we sent the message, not sure we need anything here technically
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    receiveMove(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != MainActivity.this)
                    {
                        Toast.makeText(MainActivity.this.getBaseContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != MainActivity.this)
                    {
                        Toast.makeText(MainActivity.this.getBaseContext(), msg.getData().getString(Constants.TOAST),Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED))
            {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,BluetoothAdapter.ERROR);
                if (mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
                {
                    Toast.makeText(context, "Bluetooth is now discoverable", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_ENABLE_BT:
                if (!(resultCode == Activity.RESULT_OK))
                {
                    Toast.makeText(this, R.string.bt_not_enabled,Toast.LENGTH_SHORT).show();
                }
            case REQUEST_DISCOVERABLE_BT:
                if (resultCode == Activity.RESULT_CANCELED)
                {
                    Toast.makeText(this, R.string.bt_not_discoverable,Toast.LENGTH_SHORT).show();
                }
            case DISCOVER_DEVICE:
                if (resultCode == Activity.RESULT_OK)
                {
                    connectDevice(data);
                }
        }
    }

    private void connectDevice(Intent data)
    {
        String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        BluetoothDevice btDevice = btService.addDevice(address);
        btService.connect(btDevice);
    }

    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(btReceiver);
    }
}
