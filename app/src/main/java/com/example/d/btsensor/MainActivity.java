package com.example.d.btsensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.os.ParcelUuid;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    Set<BluetoothDevice> btList;
    int resultCode;
    BluetoothAdapter btAdapter;
    List<String> btNameList = new ArrayList<String>();
    Context context = this;
    GraphView graph;
    LineGraphSeries<DataPoint> dataSeries = new LineGraphSeries<DataPoint>();
    btDatabase btd;
    int timeDelay=5;
    Button timeDelayButton;
    Button resetButton;
    EditText timeDelayText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int result = 0;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btd = new btDatabase(this);
        dataSeries.resetData(btd.getAllData());
        graph = (GraphView) findViewById(R.id.graph);
        graph.addSeries(dataSeries);
        graph.getViewport().setMaxY(125);
        graph.getViewport().setMaxX(60);
        graph.getViewport().setMinY(-55);
        graph.getViewport().setMinX(0);
        graph.getViewport().setXAxisBoundsManual(true);
        //graph.getViewport().setYAxisBoundsManual(true);
        graph.onDataChanged(false, false);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        timeDelayButton = (Button) findViewById(R.id.delayButton);
        resetButton = (Button) findViewById(R.id.resetButton);
        timeDelayText = (EditText) findViewById(R.id.delayBox);

        timeDelayButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    timeDelay = Integer.parseInt(timeDelayText.getText().toString());
                    if(timeDelay < 0 || timeDelay > 600){
                        timeDelay=5;
                    }
                }
                catch (Exception e){
                   Log.i("Error",e.toString());
                }
                Log.i("TimeDelay",timeDelayText.getText().toString());
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener(){
            @Override
        public void onClick(View v){
                btd.clearDatabase();
                dataSeries.resetData(btd.getAllData());
            }
        });

        enableBTDevice();

    }
    /*
        @Override
        public void onPause(){
            super.onPause();
        }

        @Override
        public void onResume(){
            super.onResume();
            graph.addSeries(dataSeries);
        }
    */
    void connectBTDevice() {
        UUID btUUID;
        String btAddress;

        btList = btAdapter.getBondedDevices();
        for (BluetoothDevice bt : btList) {
            btNameList.add(bt.getName());
        }
        Button devSelectButton = (Button) findViewById(R.id.devSelect);
        devSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("BT", "Got click");
                final String[] btDeviceListArray = btNameList.toArray(new String[0]);
                AlertDialog.Builder deviceList = new AlertDialog.Builder(context);
                deviceList.setTitle("Devices");
                AlertDialog.Builder builder = deviceList.setItems(btDeviceListArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("Menu:", btDeviceListArray[which]);
                        for (BluetoothDevice bt : btList) {
                            if (bt.getName().equalsIgnoreCase(btDeviceListArray[which])) {
                                ParcelUuid uuid[] = bt.getUuids();
                                Log.i("Address:", bt.getAddress());
                                Toast.makeText(context, bt.getAddress(), Toast.LENGTH_SHORT).show();
                                Thread mConnectThread = new ConnectThread(bt);
                                mConnectThread.start();
                                /*for (int i = 0; i < uuid.length; i++) {
                                    Log.i("UUID", uuid[i].getUuid().toString());
                                }*/
                            }
                        }
                    }
                });
                AlertDialog alert = deviceList.create();
                alert.show();
            }
        });
    }

    void enableBTDevice() {
        Intent enableBT = null;

        if (btAdapter == null) {
            alert(R.string.bt_nosupport);
        }
        if (!btAdapter.isEnabled()) {
            enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        } else {
            connectBTDevice();
        }

        Log.i("Request Enable BT", "Asked to enable bt");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                alert(R.string.bt_usercancel);
            } else {
                Log.i("BT", "Got OK");
                connectBTDevice();
            }
        }
    }


    private void alert(int text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(text)
                .setTitle(R.string.app_name);
        builder.show();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("Exception", e.toString());
            }
            mmSocket = tmp;
        }

        public void run() {
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Thread mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
            } catch (IOException connectException) {

            }
            return;
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {

            }
            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int counter = 0;
            double temp=0;
            int begin = 0;
            int bytes = 0;
            int entryCounter=0;
            String msg;
            String[] msgArray;
            String logMessage;
            TextView tv = (TextView) findViewById(R.id.temperatureView);
            char c;
            counter = btd.getLatestPoint();
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    temp=0;
                    for (int i = begin; i < bytes; i++) {
                        if (buffer[i] == "\r".getBytes()[0]) {
                            msg = new String(buffer).substring(begin, bytes);
                            msgArray = msg.split("\r");
                            if(msgArray.length > 1){
                                entryCounter = 0;
                                Log.i("Averaging data", String.valueOf(msgArray.length));
                                for (int j = 0; j < msgArray.length; j++) {
                                    try {
                                        temp = temp + Double.parseDouble(msgArray[j]);
                                        entryCounter++;
                                    } catch (Exception e) {
                                        Log.e("Parsing error!", msg, e);
                                    }
                                }
                                temp = temp / entryCounter;
                                if(temp > 30){
                                    i = i;
                                    Log.i(",",",");
                                }
                                entryCounter=0;
                            }
                            else{
                                Log.i("Data", "Single point");
                                try {
                                    temp = Double.parseDouble(msgArray[0]);
                                } catch (Exception e) {
                                    Log.e("Parsing error!", msg, e);
                                }
                            }
                            Log.i("Message Length",String.valueOf(msgArray.length));
                            Log.i("Data", String.valueOf(temp));
                            btd.insertDataPoint(counter, temp, 0);
                            updateUI(temp, tv, counter);
                            counter++;
                            //Log.i("Message", msg);
                            /*
                            msgArray = msg.split("\r");

                            if (msgArray.length > 1) {
                                entryCounter = 0;
                                Log.i("Averaging data", String.valueOf(msgArray.length));
                                for (int j = 0; j < msgArray.length; j++) {
                                    try {
                                        temp = temp + Double.parseDouble(msgArray[j]);
                                        entryCounter++;
                                    } catch (Exception e) {
                                        Log.e("Parsing error!", msg, e);
                                    }
                                }
                                temp = temp / entryCounter;
                            } else {
                                Log.i("Data", "Single point");
                                try {
                                    temp = Double.parseDouble(msgArray[0]);
                                } catch (Exception e) {
                                    Log.e("Parsing error!", msg, e);
                                }
                            }
                            btd.insertDataPoint(counter, temp, 0);
                            updateUI(temp, tv, counter);
                            counter++;
                            */
                            //begin = i + msgArray.length;
                            //if(i >= bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            //}
                        }
                        // break;
                        //     }
                        }
                    }catch(IOException e){

                    }
                try {
                    Thread.sleep(timeDelay*1000);
                }
                catch(Exception e){

                }
            }
        }

        public void updateUI(final double temp, final TextView tv, final int counter){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv.setText(String.valueOf(temp));
                    if(counter > 0) { //TODO: Investigate cludge
                        dataSeries.appendData(new DataPoint(counter, temp), true, counter);
                    }
                    //graph.addSeries(dataSeries);
                }
            });
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

}
