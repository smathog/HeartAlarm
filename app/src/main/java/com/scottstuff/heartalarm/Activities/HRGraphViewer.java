package com.scottstuff.heartalarm.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.R;
import com.scottstuff.heartalarm.Service.MonitorService;

import java.text.SimpleDateFormat;

public class HRGraphViewer
        extends AppCompatActivity
        implements MonitorService.UpdateFromService {
    // Logcat tag
    private static final String TAG = App.APP_TAG + ".HRGraphViewer";

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
            serviceInstance.registerActivity(HRGraphViewer.this);

            // Bind series for HR graph
            hrGraphSeries();

            ToggleButton recording = findViewById(R.id.hrToggleButton);
            recording.setChecked(serviceInstance.isRecordingHR());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()");
            // Remove server instance
            serviceInstance = null;

            ToggleButton recording = findViewById(R.id.hrToggleButton);
            recording.setChecked(false);
        }
    };

    // Helper functions to handle service binding
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
    }

    // Android lifecycle functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrgraph_viewer);

        // Set up hrGraph
        hrGraphSetup();

        // Set up source button AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a graph source:");
        String[] options = {"Live View", "Saved Recording"};
        builder.setItems(options, (dialog, choice) -> {
            switch(choice) {
                case 0:
                    if (serviceInstance != null) {
                        Toast.makeText(this,
                                "Displaying live view!",
                                Toast.LENGTH_LONG)
                                .show();
                    } else {
                        Toast.makeText(this,
                                "The service must be running to see live view!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                    break;
                case 1:
                    Toast.makeText(this, "Saved stuff!", Toast.LENGTH_LONG).show();
                    break;
            }
        });
        Button sourceButton = findViewById(R.id.hrSourceSelect);
        sourceButton.setOnClickListener((view) -> builder.create().show());
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

    // Interface implementation
    /**
     * Won't be called, just used to give HRGraphViewer compatibility w/MonitorService
     * @param dataBundle
     */
    @Override
    public void serviceUpdate(MonitorService.DataBundle dataBundle) {

    }

    // Graph helper functions
    /**
     * Helper function to initialize HR graph with series into a readable format
     */
    private void hrGraphSetup() {
        Log.d(TAG, "hrGraphSetup()");
        GraphView graph = findViewById(R.id.bigHeartRateGraph);
        graph.setTitle("Heart Rate");
        // set date label formatter
        graph.getGridLabelRenderer().setLabelFormatter
                (new DateAsXAxisLabelFormatter(HRGraphViewer.this,
                        new SimpleDateFormat("mm:ss")));
        graph.getGridLabelRenderer().setNumHorizontalLabels(8);
    }

    /**
     * Helper function to bind and configure the HR series
     */
    private void hrGraphSeries() {
        Log.d(TAG, "hrGraphSeries()");
        GraphView graph = findViewById(R.id.bigHeartRateGraph);
        LineGraphSeries<DataPoint> series = serviceInstance.getDataDisplay().getHeartRateSeries();
        graph.addSeries(series);

        // Viewport configuration
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(series.getLowestValueX());
        graph.getViewport().setMaxX(series.getLowestValueX() + 60000);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(220);
        graph.getViewport().setScrollable(true);
    }
}