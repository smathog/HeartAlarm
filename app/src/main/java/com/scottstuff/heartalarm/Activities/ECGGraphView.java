package com.scottstuff.heartalarm.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
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
import com.scottstuff.heartalarm.DataTypes.ECGData;
import com.scottstuff.heartalarm.R;
import com.scottstuff.heartalarm.SQL.ECGSQLiteManager;
import com.scottstuff.heartalarm.Service.MonitorService;

import java.text.SimpleDateFormat;
import java.util.List;

public class ECGGraphView
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
            serviceInstance.registerActivity(ECGGraphView.this);

            // Bind series for ECG graph
            ecgGraphSeries(serviceInstance.getDataDisplay().getEcgSeries());

            ToggleButton recording = findViewById(R.id.ecgToggleButton);
            recording.setChecked(serviceInstance.isRecordingECG());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()");
            // Remove server instance
            serviceInstance = null;

            ToggleButton recording = findViewById(R.id.ecgToggleButton);
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
        setContentView(R.layout.activity_ecggraph_view);

        // Set up ecgGraph
        ecgGraphSetup();

        // Set up source button AlertDialog
        // Inner AlertDialog
        AlertDialog.Builder innerBuilder = new AlertDialog.Builder(this);
        innerBuilder.setTitle("Recordings:");
        String[] innerOptions = ECGSQLiteManager.getInstance(this).getTables().toArray(new String[0]);
        if (innerOptions.length == 0) {
            innerBuilder.setMessage("No recordings available!");
        } else {
            innerBuilder.setItems(innerOptions, (dialog, choice) -> {
                Toast.makeText(this,
                                "You picked " + innerOptions[choice],
                                Toast.LENGTH_LONG)
                        .show();
                List<ECGData> list = ECGSQLiteManager.getInstance(this).getData(innerOptions[choice]);
                ecgGraphSeries(DataDisplay.convertECGToGraphSeries(list));
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
                        ecgGraphSeries(serviceInstance.getDataDisplay().getEcgSeries());
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
        Button sourceButton = findViewById(R.id.ecgSourceSelect);
        sourceButton.setOnClickListener((view) -> builder.create().show());

        // Recording button logic
        ToggleButton recording = findViewById(R.id.ecgToggleButton);
        recording.setOnClickListener((view) -> {
            // Need the negation because clicking the button flips the value!
            if (!recording.isChecked()) {
                // Recording, so stop recording.
                if (serviceInstance != null && serviceInstance.isRecordingECG()) {
                    serviceInstance.stopRecordingECG();
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
                    serviceInstance.startRecordingECG();
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
     * Helper function to initialize ECG graph with series into a readable format
     */
    private void ecgGraphSetup() {
        Log.d(TAG, "ecgGraphSetup()");
        GraphView graph = findViewById(R.id.bigECGGraph);
        graph.setTitle("ECG");
        // set date label formatter
        graph.getGridLabelRenderer().setLabelFormatter
                (new DateAsXAxisLabelFormatter(ECGGraphView.this,
                        new SimpleDateFormat("mm:ss")));
        graph.getGridLabelRenderer().setNumHorizontalLabels(8);
        graph.getGridLabelRenderer().setNumVerticalLabels(10);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(true);
        graph.getGridLabelRenderer().setGridColor(Color.RED);
        graph.getGridLabelRenderer().setHighlightZeroLines(false);
    }

    /**
     * Helper function to bind and configure the ECG series. Note this wipes the graph clean of the
     * preexisting series.
     * @param series - The series to bind to the graph.
     */
    private void ecgGraphSeries(LineGraphSeries<DataPoint> series) {
        Log.d(TAG, "hrGraphSeries()");
        GraphView graph = findViewById(R.id.bigECGGraph);
        // Remove all preexisting series
        graph.removeAllSeries();

        // Viewport settings
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(series.getLowestValueX());
        graph.getViewport().setMaxX(series.getLowestValueX() + 2000);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1000);
        graph.getViewport().setMaxY(1000);
        graph.getViewport().setScrollable(true);

        // Style series
        series.setColor(Color.BLACK);

        // Add this series to the graph
        graph.addSeries(series);
    }
}