package com.scottstuff.heartalarm.Activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.Service.MonitorService;
import com.scottstuff.heartalarm.R;
import com.scottstuff.heartalarm.Utility.Utility;

import java.text.SimpleDateFormat;

/**
 * The primary class for managing the main/entry page's activity
 */
public class HeartAlarm
        extends AppCompatActivity
        implements MonitorService.UpdateFromService {
    // logcat tag
    private static final String TAG = App.APP_TAG + ".HeartAlarm";

    // MonitorService instance to bind to, if present; else null
    private MonitorService serviceInstance;

    // Callbacks for service binding
    private final ServiceConnection monitorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            MonitorService.LocalBinder binder = (MonitorService.LocalBinder) service;
            serviceInstance = binder.getService();

            // Register activity with service
            serviceInstance.registerActivity(HeartAlarm.this);

            // Bind series for HR graph
            hrGraphSeries();

            // Bind series for ECG graph
            ecgGraphSeries();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()");
            // Remove server instance
            serviceInstance = null;

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
        Log.d(TAG, "serviceUpdate()");
        // Set text for alarm
        TextView alarmText = findViewById(R.id.mainAlarmStatus);
        if (dataBundle.alarmActive) {
            alarmText.setText("Active");
        } else {
            alarmText.setText("Off");
        }

        // Set text for heart rate
        TextView heartRateText = findViewById(R.id.mainHRValue);
        if (dataBundle.heartRate != null) {
            heartRateText.setText(Integer.toString(dataBundle.heartRate));
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
        hrGraphSetup();
        ecgGraphSetup();
    }


    /**
     * Updates the main page when resumed.
     * Binds to the MonitorService if one is active.
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        // Bind MonitorService, if active
        bind();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        unbind();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
        unbind();
    }

    public void onClickHRGraph(View view) {
        Log.d(TAG, "onClickHRGraph");
        Intent hrGraphIntent = new Intent(this, HRGraphViewer.class);
        startActivity(hrGraphIntent);
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
            if (serviceInstance == null) {
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
            if (serviceInstance != null) {
                serviceInstance.updateFromIntent(serviceUpdateIntent);
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
        if (serviceInstance != null) {
            Intent serviceUpdateIntent = new Intent(this, MonitorService.class);
            serviceUpdateIntent.putExtra(App.HHR_ENABLED, ((App) this.getApplication()).state.isHhrEnabled());
            serviceUpdateIntent.putExtra(App.HHR_SETTING, ((App) this.getApplication()).state.getHhrSetting());
            serviceUpdateIntent.putExtra(App.LHR_ENABLED, ((App) this.getApplication()).state.isLhrEnabled());
            serviceUpdateIntent.putExtra(App.LHR_SETTING, ((App) this.getApplication()).state.getLhrSetting());
            serviceUpdateIntent.putExtra(App.ALARM_SOUND_SETTING, ((App) this.getApplication()).state.getAlarmSoundSetting());
            serviceUpdateIntent.putExtra(App.ALARM_ON, false);
            serviceInstance.updateFromIntent(serviceUpdateIntent);
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
        Log.d(TAG, "bind()");
        Intent serviceIntent = new Intent(this, MonitorService.class);
        bindService(serviceIntent, monitorConnection, 0);
    }


    /**
     * Helper function to unbind the MonitorService
     */
    private void unbind() {
        Log.d(TAG, "unbind()");
        // De-register activity with service
        if (serviceInstance != null) {
            serviceInstance.deregisterActivity();
            // Unbind service
            unbindService(monitorConnection);
        }

        // Set instance to empty
        serviceInstance = null;

        // Restore default text
        // Set the text for the alarm and heart rate to their defaults
        TextView alarmText = findViewById(R.id.mainAlarmStatus);
        TextView heartRateText = findViewById(R.id.mainHRValue);
        alarmText.setText(R.string.mainActivityDefaultSensor);
        heartRateText.setText(R.string.mainActivityDefaultSensor);
    }

    /**
     * Helper function to initialize HR graph into a readable format
     */
    private void hrGraphSetup() {
        Log.d(TAG, "hrGraphSetup()");
        GraphView graph = findViewById(R.id.entryHeartRateGraph);
        graph.setTitle("Heart Rate");
        // set date label formatter
        graph.getGridLabelRenderer().setLabelFormatter
                (new DateAsXAxisLabelFormatter(HeartAlarm.this,
                        new SimpleDateFormat("mm:ss")));
        graph.getGridLabelRenderer().setNumHorizontalLabels(4);
    }

    /**
     * Helper function to bind and configure the HR series
     */
    private void hrGraphSeries() {
        Log.d(TAG, "hrGraphSeries()");
        GraphView graph = findViewById(R.id.entryHeartRateGraph);
        LineGraphSeries<DataPoint> series = serviceInstance.getDataDisplay().getHeartRateSeries();
        graph.addSeries(series);

        // Viewport configuration
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(series.getLowestValueX());
        graph.getViewport().setMaxX(series.getLowestValueX() + 30000);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(220);
        graph.getViewport().setScrollable(true);
    }

    /**
     * Helper function to build ECG graph
     */
    private void ecgGraphSetup() {
        Log.d(TAG, "ecgGraphSetup()");
        GraphView graph = findViewById(R.id.entryECGGraph);
        graph.setTitle("ECG");
        // Style grid
        graph.getGridLabelRenderer().setLabelFormatter
                (new DateAsXAxisLabelFormatter(HeartAlarm.this,
                        new SimpleDateFormat("ss:SS")));
        graph.getGridLabelRenderer().setNumHorizontalLabels(5);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(true);
        graph.getGridLabelRenderer().setNumVerticalLabels(10);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(true);
        graph.getGridLabelRenderer().setGridColor(Color.RED);
        graph.getGridLabelRenderer().setHighlightZeroLines(false);
    }

    /**
     * Helper function to set up graph once DataDisplay is ready
     */
    private void ecgGraphSeries() {
        Log.d(TAG, "ecgGraphSeries()");
        GraphView graph = findViewById(R.id.entryECGGraph);

        LineGraphSeries<DataPoint> series = serviceInstance.getDataDisplay().getEcgSeries();

        // Style series
        series.setColor(Color.BLACK);
        graph.addSeries(series);

        // Viewport settings
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(series.getLowestValueX());
        graph.getViewport().setMaxX(series.getLowestValueX() + 2000);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1000);
        graph.getViewport().setMaxY(1000);
    }
}