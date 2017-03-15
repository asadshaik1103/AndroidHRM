package com.example.asadshaik.androidhrm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DeviceList extends AppCompatActivity {

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    public BluetoothAdapter btAdapter;

    private ArrayAdapter<String> newDevicesArrayAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        Button scan = (Button) findViewById(R.id.scan);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dodiscovery();
                v.setVisibility(View.GONE);
            }
        });

        //initialize array adapters for bot paired and new devices

        ArrayAdapter<String> paireddDevicesArrayadapter = new ArrayAdapter<String>(this,R.layout.device_name);
        newDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.device_name);


        ListView pairedlistView = (ListView) findViewById(R.id.paired_devices);
        pairedlistView.setAdapter(paireddDevicesArrayadapter);
        pairedlistView.setOnItemClickListener(mDeviceClickListener);


        ListView newListView = (ListView) findViewById(R.id.new_devices);
        newListView.setAdapter(newDevicesArrayAdapter);
        newListView.setOnItemClickListener(mDeviceClickListener);


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver,filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver,filter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if(pairedDevices.size() > 0){
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for(BluetoothDevice device : pairedDevices){
                paireddDevicesArrayadapter.add(device.getName() + "\n" + device.getAddress());

            }

        }
        else{
            String noDevices = "No Devices Found".toString();
            paireddDevicesArrayadapter.add(noDevices);
        }







    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(btAdapter!=null){
            btAdapter.cancelDiscovery();
        }

        //Unregister Broadcast Receivers
        this.unregisterReceiver(mReceiver);
    }

    //discovery for bluetooth devices using btadapter
    private void dodiscovery(){


        setSupportProgressBarIndeterminateVisibility(true);
        setTitle("Scanning for devices");
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }

        btAdapter.startDiscovery();

    }


    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            btAdapter.cancelDiscovery();

            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() -17);


            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS,address);

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };




    private final BroadcastReceiver mReceiver
            = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();


            if(BluetoothDevice.ACTION_FOUND.equals(action)){


                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getBondState() != BluetoothDevice.BOND_BONDED){

                    newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());


                }





            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);

                //if no devices are found
                if(newDevicesArrayAdapter.getCount()==0){
                    String noDevices = getResources().getString(R.string.none_found).toString();
                    newDevicesArrayAdapter.add(noDevices);
                }


            }
        }
    };


}
