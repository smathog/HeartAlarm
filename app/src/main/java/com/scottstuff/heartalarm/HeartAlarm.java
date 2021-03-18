package com.scottstuff.heartalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class HeartAlarm extends AppCompatActivity {
    //logcat tag
    private static final String TAG = App.APP_TAG + ".HeartAlarm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utility.checkBluetooth(this);
    }

    public void onClickSetID(View view) {
        Log.d(TAG, "onClickSetID()");
        Intent setIDIntent = new Intent(HeartAlarm.this, SetID.class);
        startActivity(setIDIntent);
    }

    public void onClickStartMonitor(View view) {
        Log.d(TAG, "onClickStrtMonitor()");
        if (((App) this.getApplication()).state.isDeviceIDDefined()) {
            Utility.checkBluetooth(this);
            Intent serviceIntent = new Intent(this, MonitorService.class);
            serviceIntent.putExtra(App.HHR_ENABLED, ((App) this.getApplication()).state.isHhrEnabled());
            serviceIntent.putExtra(App.HHR_SETTING, ((App) this.getApplication()).state.getHhrSetting());
            serviceIntent.putExtra(App.LHR_ENABLED, ((App) this.getApplication()).state.isLhrEnabled());
            serviceIntent.putExtra(App.LHR_SETTING, ((App) this.getApplication()).state.getLhrSetting());
            serviceIntent.putExtra(App.ALARM_SOUND_SETTING, ((App) this.getApplication()).state.getAlarmSoundSetting());
            serviceIntent.putExtra(App.ALARM_ON, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(serviceIntent);
            else
                startService(serviceIntent);
        } else {
            Toast.makeText(this, "Error: no device ID to connect with!", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickStopMonitor(View view) {
        Log.d(TAG, "onClickStopMonitor()");
        Intent serviceIntent = new Intent(this, MonitorService.class);
        stopService(serviceIntent);
    }

    public void onClickActivateAlarm(View view) {
        Log.d(TAG, "onClickActivateAlarm()");
        if (((App) this.getApplication()).state.isDeviceIDDefined()) {
            Utility.checkBluetooth(this);
            if (!((App) this.getApplication()).state.isHhrEnabled() && !((App) this.getApplication()).state.isLhrEnabled()) {
                Toast.makeText(this, "Error: neither upper nor lower heart rate alarms set to on!", Toast.LENGTH_LONG).show();
                return;
            }
            Intent serviceUpdateIntent = new Intent(this, MonitorService.class);
            serviceUpdateIntent.putExtra(App.HHR_ENABLED, ((App) this.getApplication()).state.isHhrEnabled());
            serviceUpdateIntent.putExtra(App.HHR_SETTING, ((App) this.getApplication()).state.getHhrSetting());
            serviceUpdateIntent.putExtra(App.LHR_ENABLED, ((App) this.getApplication()).state.isLhrEnabled());
            serviceUpdateIntent.putExtra(App.LHR_SETTING, ((App) this.getApplication()).state.getLhrSetting());
            serviceUpdateIntent.putExtra(App.ALARM_SOUND_SETTING, ((App) this.getApplication()).state.getAlarmSoundSetting());
            serviceUpdateIntent.putExtra(App.ALARM_ON, true);
            startService(serviceUpdateIntent);
        } else {
            Toast.makeText(this, "Error: no device ID to connect with!", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickDeactivateAlarm(View view) {
        Log.d(TAG, "onClickDeactivateAlarm()");
        Intent serviceUpdateIntent = new Intent(this, MonitorService.class);
        serviceUpdateIntent.putExtra(App.HHR_ENABLED, ((App) this.getApplication()).state.isHhrEnabled());
        serviceUpdateIntent.putExtra(App.HHR_SETTING, ((App) this.getApplication()).state.getHhrSetting());
        serviceUpdateIntent.putExtra(App.LHR_ENABLED, ((App) this.getApplication()).state.isLhrEnabled());
        serviceUpdateIntent.putExtra(App.LHR_SETTING, ((App) this.getApplication()).state.getLhrSetting());
        serviceUpdateIntent.putExtra(App.ALARM_SOUND_SETTING, ((App) this.getApplication()).state.getAlarmSoundSetting());
        serviceUpdateIntent.putExtra(App.ALARM_ON, false);
        startService(serviceUpdateIntent);
    }

    public void onClickSettings(View view) {
        Log.d(TAG, "onClickSettings()");
        Intent setAlarmSettingsIntent = new Intent(HeartAlarm.this, AlarmSettings.class);
        startActivity(setAlarmSettingsIntent);
    }

}