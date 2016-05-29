package com.example.d.btsensor;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by d on 09/03/16.
 */
public class BTDriver {
    public static void adapter_init(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null){

        }
    }
}
