package com.mimickme.bluetoothtictac;

import android.content.Intent;
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
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothService btService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btService = new BluetoothService(handler);

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
        if (id == R.id.find_player)
        {

        }
        else if (id == R.id.make_discoverable)
        {

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
    }

    public void receiveMove()
    {
        // Something like this will be called by whatever is handling incoming bluetooth traffic
        // This will check if it is the opponent's turn to make a move, if so, accept the move
        // This will place the move into our array/whatever and validate victory or tie conditions
        // If the game is proceeding, we should flag that is our our player's move
    }

    private final Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {

        }
    };
}
