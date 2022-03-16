package com.scottstuff.heartalarm.DataDisplay;

import android.app.Activity;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.scottstuff.heartalarm.Service.MonitorService;

import java.util.Date;

/**
 * Class to manage data to be displayed by HeartAlarm activity
 */
public class DataDisplay {
    // Series for graphs
    private final LineGraphSeries<DataPoint> heartRateSeries;
    private final LineGraphSeries<DataPoint> ecgSeries;

    // Link to the activity consuming the data
    private Activity activity;

    /**
     * Default constructor
     */
    public DataDisplay() {
        // Initialize graph series
        heartRateSeries = new LineGraphSeries<>();
        ecgSeries = new LineGraphSeries<>();
    }

    /**
     * Specifies the activity that the service (and this DataDisplay) are to bind to
     * @param activity
     */
    public void bindActivity(Activity activity) {
        this.activity = activity;
    }

    /**
     * Updates the LineGraphSeries for heart rate
     * @param hr - new datapoint
     */
    public void updateHeartRateSeries(int hr) {
        // If bound to an activity, use UI thread to avoid concurrentModificationException
        if (activity != null) {
            activity.runOnUiThread(() -> heartRateSeries.appendData(new DataPoint(new Date(), hr),
                    true, Integer.MAX_VALUE, false));
        } else { // If not, can just use the current thread and not bother rerendering
            heartRateSeries.appendData(new DataPoint(new Date(), hr),
                    true, Integer.MAX_VALUE, true);
        }
    }

    /**
     * Updates the LineGraphSeries for ECG data
     * @param timeStamp - timestamp for datapoint
     * @param ecgData - new datapoint
     */
    public void updateECGSeries(long timeStamp, int ecgData) {
        // If bound to an activity, use UI thread to avoid concurrentModificationException
        if (activity != null) {
            activity.runOnUiThread(() -> ecgSeries.appendData(new DataPoint(timeStamp, ecgData),
                    true, Integer.MAX_VALUE, false)
            );
        } else { // If not, can just use the current thread and not bother rerendering
            ecgSeries.appendData(new DataPoint(timeStamp, ecgData),
                    true, Integer.MAX_VALUE, true);
        }
    }


    /**
     * Getter for the heartRateSeries
     * @return heartRateSeries
     */
    public LineGraphSeries<DataPoint> getHeartRateSeries() {
        return heartRateSeries;
    }

    /**
     * Getter for the ECG data series
     * @return ecgSeries
     */
    public LineGraphSeries<DataPoint> getEcgSeries() {
        return ecgSeries;
    }
}
