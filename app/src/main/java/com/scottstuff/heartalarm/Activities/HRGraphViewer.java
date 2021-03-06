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
import com.scottstuff.heartalarm.DataDisplay.DataDisplay;
import com.scottstuff.heartalarm.DataTypes.HRData;
import com.scottstuff.heartalarm.R;
import com.scottstuff.heartalarm.SQL.HrSQLiteManager;
import com.scottstuff.heartalarm.Service.MonitorService;

import java.text.SimpleDateFormat;
import java.util.List;

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
            hrGraphSeries(serviceInstance.getDataDisplay().getHeartRateSeries());

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
        // Inner AlertDialog
        AlertDialog.Builder innerBuilder = new AlertDialog.Builder(this);
        innerBuilder.setTitle("Recordings:");
        String[] innerOptions = HrSQLiteManager.getInstance(this).getTables().toArray(new String[0]);
        if (innerOptions.length == 0) {
            innerBuilder.setMessage("No recordings available!");
        } else {
            innerBuilder.setItems(innerOptions, (dialog, choice) -> {
                Toast.makeText(this,
                                "You picked " + innerOptions[choice],
                                Toast.LENGTH_LONG)
                        .show();
                List<HRData> list = HrSQLiteManager.getInstance(this).getData(innerOptions[choice]);
                hrGraphSeries(DataDisplay.convertHRToGraphSeries(list));
            });
        }

        // Outer AlertDialog
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
                        hrGraphSeries(serviceInstance.getDataDisplay().getHeartRateSeries());
                    } else {
                        Toast.makeText(this,
                                "The service must be running to see live view!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                    break;
                case 1:
                    Toast.makeText(this, "Saved stuff!", Toast.LENGTH_LONG).show();
                    innerBuilder.create().show();
                    break;
            }
        });
        Button sourceButton = findViewById(R.id.hrSourceSelect);
        sourceButton.setOnClickListener((view) -> builder.create().show());

        // Recording button logic
        ToggleButton recording = findViewById(R.id.hrToggleButton);
        recording.setOnClickListener((view) -> {
            // Need the negation because clicking the button flips the value!
            if (!recording.isChecked()) {
                // Recording, so stop recording.
                if (serviceInstance != null && serviceInstance.isRecordingHR()) {
                    serviceInstance.stopRecordingHR();
                } else {
                    Toast.makeText(this,
                                    "You weren't recording anyways!",
                                    Toast.LENGTH_LONG)
                            .show();
                }
                recording.setChecked(false);
            } else {
                // Not recording, so start recording
                if (serviceInstance != null && !serviceInstance.isRecordingHR()) {
                    serviceInstance.startRecordingHR();
                    recording.setChecked(true);
                } else if (serviceInstance == null) {
                    Toast.makeText(this,
                                    "You need to start the service to record!",
                                    Toast.LENGTH_LONG)
                            .show();
                    recording.setChecked(false);
                }
            }
        });
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
     * Helper function to bind and configure the HR series. Note this wipes the graph clean of the
     * preexisting series.
     * @param series - The series to bind to the graph.
     */
    private void hrGraphSeries(LineGraphSeries<DataPoint> series) {
        Log.d(TAG, "hrGraphSeries()");
        GraphView graph = findViewById(R.id.bigHeartRateGraph);
        // Remove all preexisting series
        graph.removeAllSeries();

        // Viewport configuration
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(series.getLowestValueX());
        graph.getViewport().setMaxX(series.getLowestValueX() + 60000);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(220);
        graph.getViewport().setScrollable(true);

        // Add this series to the graph
        graph.addSeries(series);
    }
}