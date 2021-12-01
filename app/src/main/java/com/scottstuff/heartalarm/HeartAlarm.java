package com.scottstuff.heartalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The primary class for managing the main/entry page's activity
 */
public class HeartAlarm extends AppCompatActivity {
    //logcat tag
    private static final String TAG = App.APP_TAG + ".HeartAlarm";

    /**
     * Standard activity onCreate, but checks to make sure Bluetooth is enabled.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utility.checkBluetooth(this);
    }


    /**
     * Updates the main page when resumed.
     * Primarily updates to indicate whether the alarm is on.
     */
    @Override
    protected void onResume() {
        super.onResume();
        setAlarmText();
    }

    /**
     * Launches the activity to set Polar Device ID
     * @param view
     */
    public void onClickSetID(View view) {
        Log.d(TAG, "onClickSetID()");
        Intent setIDIntent = new Intent(HeartAlarm.this, SetID.class);
        startActivity(setIDIntent);
    }

    /**
     * Activates the heart monitor service, which is capable of running in the background as a
     * foreground service.
     * @param view
     */
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
            setAlarmText();
            ((App) this.getApplication()).state.setServiceRunning(true);
        } else {
            Toast.makeText(this, "Error: no device ID to connect with!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Terminates the heart monitor foreground service.
     * @param view
     */
    public void onClickStopMonitor(View view) {
        Log.d(TAG, "onClickStopMonitor()");
        Intent serviceIntent = new Intent(this, MonitorService.class);
        stopService(serviceIntent);
        setAlarmText();
        ((App) this.getApplication()).state.setServiceRunning(false);
    }

    /**
     * Activates the alarm capacity of the foreground service using saved settings, if possible.
     * Note that if the foreground service is *not* active, this will activate it.
     * @param view
     */
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
            setAlarmText();
            ((App) this.getApplication()).state.setServiceRunning(true);
        } else {
            Toast.makeText(this, "Error: no device ID to connect with!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Deactivates the alarm component of the foreground service.
     * @param view
     */
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
        setAlarmText();
    }


    public void onClickSettings(View view) {
        Log.d(TAG, "onClickSettings()");
        Intent setAlarmSettingsIntent = new Intent(HeartAlarm.this, AlarmSettings.class);
        startActivity(setAlarmSettingsIntent);
    }

    /**
     * If the alarm is on, sets the relevant TextView to "Active"; otherwise restores it to default
     */
    private void setAlarmText() {
        App app = (App) this.getApplication();
        TextView alarmActiveField = findViewById(R.id.mainAlarmStatus);
        if (app.state.isServiceRunning()) {
            if (app.state.isHhrEnabled() || app.state.isLhrEnabled()) {
                alarmActiveField.setText("Active");
            } else {
                alarmActiveField.setText("Off");
            }
        } else {
            // No sensor connected
            alarmActiveField.setText(R.string.mainActivityDefaultSensor);
        }
    }
}