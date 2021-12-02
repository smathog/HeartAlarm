package com.scottstuff.heartalarm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Optional;

/**
 * The primary class for managing the main/entry page's activity
 */
public class HeartAlarm extends MonitorService.UpdateActivity {
    // logcat tag
    private static final String TAG = App.APP_TAG + ".HeartAlarm";

    // MonitorService instance to bind to, if present; else empty
    private Optional<MonitorService> serviceInstance = Optional.empty();

    // Series for the graph in this activity:
    private LineGraphSeries<DataPoint> heartRateSeries;

    // Callbacks for service binding
    private final ServiceConnection monitorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorService.LocalBinder binder = (MonitorService.LocalBinder) service;
            serviceInstance = Optional.of(binder.getService());

            // Register activity with service
            serviceInstance.get().registerActivity(HeartAlarm.this);

            // Configure HR graph
            GraphView graph = findViewById(R.id.entryHeartRateGraph);
            heartRateSeries = serviceInstance.get().getHeartRateSeries();
            graph.addSeries(heartRateSeries);
            // set date label formatter
            graph.getGridLabelRenderer().setLabelFormatter
                    (new DateAsXAxisLabelFormatter(HeartAlarm.this,
                            new SimpleDateFormat("mm:ss")));
            graph.getGridLabelRenderer().setNumHorizontalLabels(4);
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(heartRateSeries.getLowestValueX());
            graph.getViewport().setMaxX(heartRateSeries.getLowestValueX() + 30000);
            graph.getViewport().setYAxisBoundsManual(true);
            graph.getViewport().setMinY(0);
            graph.getViewport().setMaxY(220);
            graph.getViewport().setScrollable(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Remove server instance
            serviceInstance = Optional.empty();

            // Set the text for the alarm and heart rate to their defaults
            TextView alarmText = findViewById(R.id.mainAlarmStatus);
            TextView heartRateText = findViewById(R.id.mainHRValue);
            alarmText.setText(R.string.mainActivityDefaultSensor);
            heartRateText.setText(R.string.mainActivityDefaultSensor);
        }
    };

    // Update implementation
    @Override
    public void serviceUpdate(MonitorService.DataBundle dataBundle) {
        // Set text for alarm
        TextView alarmText = findViewById(R.id.mainAlarmStatus);
        if (dataBundle.alarmActive) {
            alarmText.setText("Active");
        } else {
            alarmText.setText("Off");
        }

        // Set text for heart rate
        TextView heartRateText = findViewById(R.id.mainHRValue);
        if (dataBundle.heartRate.isPresent()) {
            heartRateText.setText(Integer.toString(dataBundle.heartRate.get()));
        } else {
            heartRateText.setText("Waiting...");
        }
    }

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
     * Binds to the MonitorService if one is active.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Bind MonitorService, if active
        bind();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbind();
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
        Log.d(TAG, "onClickStartMonitor()");
        if (((App) this.getApplication()).state.isDeviceIDDefined()) {
            Utility.checkBluetooth(this);
            if (!serviceInstance.isPresent()) {
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
                bind();
            }
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

        // Unbind the service from the activity
        unbind();

        // Stop the service
        Intent serviceIntent = new Intent(this, MonitorService.class);
        stopService(serviceIntent);
    }

    /**
     * Activates the alarm capacity of the foreground service using saved settings, if possible.
     * Will cause the foreground service to activate if not yet active.
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
            if (serviceInstance.isPresent()) {
                serviceInstance.get().updateFromIntent(serviceUpdateIntent);
                Toast.makeText(this, "Service already running!", Toast.LENGTH_LONG).show();
            } else {
                startService(serviceUpdateIntent);
                bind();
            }
        } else {
            Toast.makeText(this, "Error: no device ID to connect with!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Deactivates the alarm component of the foreground service, if it is active.
     * @param view
     */
    public void onClickDeactivateAlarm(View view) {
        Log.d(TAG, "onClickDeactivateAlarm()");
        if (serviceInstance.isPresent()) {
            Intent serviceUpdateIntent = new Intent(this, MonitorService.class);
            serviceUpdateIntent.putExtra(App.HHR_ENABLED, ((App) this.getApplication()).state.isHhrEnabled());
            serviceUpdateIntent.putExtra(App.HHR_SETTING, ((App) this.getApplication()).state.getHhrSetting());
            serviceUpdateIntent.putExtra(App.LHR_ENABLED, ((App) this.getApplication()).state.isLhrEnabled());
            serviceUpdateIntent.putExtra(App.LHR_SETTING, ((App) this.getApplication()).state.getLhrSetting());
            serviceUpdateIntent.putExtra(App.ALARM_SOUND_SETTING, ((App) this.getApplication()).state.getAlarmSoundSetting());
            serviceUpdateIntent.putExtra(App.ALARM_ON, false);
            serviceInstance.get().updateFromIntent(serviceUpdateIntent);
        }
    }

    public void onClickSettings(View view) {
        Log.d(TAG, "onClickSettings()");
        Intent setAlarmSettingsIntent = new Intent(HeartAlarm.this, AlarmSettings.class);
        startActivity(setAlarmSettingsIntent);
    }

    /**
     * Helper function to bind the MonitorService
     */
    private void bind() {
        Intent serviceIntent = new Intent(this, MonitorService.class);
        bindService(serviceIntent, monitorConnection, 0);
    }


    /**
     * Helper function to unbind the MonitorService
     */
    private void unbind() {
        // De-register activity with service
        serviceInstance.ifPresent(MonitorService::deregisterActivity);

        // Set instance to empty
        serviceInstance = Optional.empty();

        // Unbind service
        unbindService(monitorConnection);

        // Restore default text
        // Set the text for the alarm and heart rate to their defaults
        TextView alarmText = findViewById(R.id.mainAlarmStatus);
        TextView heartRateText = findViewById(R.id.mainHRValue);
        alarmText.setText(R.string.mainActivityDefaultSensor);
        heartRateText.setText(R.string.mainActivityDefaultSensor);
    }
}