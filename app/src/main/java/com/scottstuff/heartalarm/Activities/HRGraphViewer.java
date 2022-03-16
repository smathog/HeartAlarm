package com.scottstuff.heartalarm.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

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
    private static final String TAG = App.APP_TAG + ".HRGraphViewer";

    // MonitorService instance to bind to, if present; else null
    private MonitorService serviceInstance;

    // Callbacks for service binding
    private final ServiceConnection monitorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorService.LocalBinder binder = (MonitorService.LocalBinder) service;
            serviceInstance = binder.getService();

            // Register activity with service
            serviceInstance.registerActivity(HRGraphViewer.this);

            // Bind series for HR graph
            hrGraphSeries();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Remove server instance
            serviceInstance = null;
        }
    };

    // Helper functions to handle service binding
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrgraph_viewer);
        hrGraphSetup();
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

    @Override
    protected void onStop() {
        super.onStop();
        unbind();
    }

    // Interface implementation
    /**
     * Won't be called, just used to give HRGraphViewer compabitility w/MonitorService
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