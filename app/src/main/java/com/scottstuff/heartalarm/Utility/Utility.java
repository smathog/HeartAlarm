package com.scottstuff.heartalarm.Utility;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

import com.jjoe64.graphview.series.LineGraphSeries;
import com.scottstuff.heartalarm.App.App;

public class Utility {
    //Logcat tag
    private static final String TAG = App.APP_TAG + ".Utility";

    //Auxiliary method for checking if bluetooth is active
    public static void checkBluetooth(Activity activity) {
        Log.d(TAG, "checkBluetooth()");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(bluetoothIntent, 2);
        }
        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},1);
    }
}
