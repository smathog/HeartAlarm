package com.scottstuff.heartalarm.DataDisplay;

import android.app.Activity;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.scottstuff.heartalarm.Activities.ECGGraphView;
import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.DataTypes.ECGData;
import com.scottstuff.heartalarm.DataTypes.HRData;
import com.scottstuff.heartalarm.Service.MonitorService;

import java.util.Date;
import java.util.List;

/**
 * Class to manage data to be displayed by HeartAlarm activity
 */
public class DataDisplay {
    private static final String TAG = App.APP_TAG + ".DataDisplay";

    // Series for graphs
    private final LineGraphSeries<DataPoint> heartRateSeries;
    private final LineGraphSeries<DataPoint> ecgSeries;

    // Link to the activity consuming the data
    private Activity activity;

    /**
     * Default constructor
     */
    public DataDisplay() {
        Log.d(TAG, "DataDisplay()");
        // Initialize graph series
        heartRateSeries = new LineGraphSeries<>();
        ecgSeries = new LineGraphSeries<>();
    }

    /**
     * Specifies the activity that the service (and this DataDisplay) are to bind to
     * @param activity
     */
    public void bindActivity(Activity activity) {
        Log.d(TAG, "bindActivity()");
        this.activity = activity;
    }

    /**
     * Updates the LineGraphSeries for heart rate
     * @param hrData - new datapoint
     */
    public void updateHeartRateSeries(HRData hrData) {
        Log.d(TAG, "updateHeartRateSeries()");
        // If bound to an activity, use UI thread to avoid concurrentModificationException
        if (activity != null) {
            activity.runOnUiThread(() -> heartRateSeries.appendData(new DataPoint(hrData.getTimeStamp(),
                            hrData.getBpm()),
                    true, Integer.MAX_VALUE, false));
        } else { // If not, can just use the current thread and not bother rerendering
            heartRateSeries.appendData(new DataPoint(hrData.getTimeStamp(), hrData.getBpm()),
                    true, Integer.MAX_VALUE, true);
        }
    }

    /**
     * Updates the LineGraphSeries for ECG data
     * @param ecgData - new datapoint
     */
    public void updateECGSeries(ECGData ecgData) {
        Log.d(TAG, "updateECGSeries() " + ecgData);
        // If bound to an activity, use UI thread to avoid concurrentModificationException
        if (activity != null) {
            activity.runOnUiThread(() -> ecgSeries.appendData(new DataPoint(ecgData.getTimeStamp(),
                            ecgData.getVoltage()),
                    true, Integer.MAX_VALUE, false)
            );
        } else { // If not, can just use the current thread and not bother rerendering
            ecgSeries.appendData(new DataPoint(ecgData.getTimeStamp(), ecgData.getVoltage()),
                    true, Integer.MAX_VALUE, true);
        }
    }


    /**
     * Getter for the heartRateSeries
     * @return heartRateSeries
     */
    public LineGraphSeries<DataPoint> getHeartRateSeries() {
        Log.d(TAG, "getHeartRateSeries()");
        return heartRateSeries;
    }

    /**
     * Getter for the ECG data series
     * @return ecgSeries
     */
    public LineGraphSeries<DataPoint> getEcgSeries() {
        Log.d(TAG, "getEcgSeries()");
        return ecgSeries;
    }

    /**
     * Utility function: taking a list of HRData, convert it to a LineGraphSeries for viewing
     * @param series of HRData for conversion
     * @return LineGraphSeries to be attached to a graph
     */
    public static LineGraphSeries<DataPoint> convertHRToGraphSeries(List<HRData> series) {
        LineGraphSeries<DataPoint> outputSeries = new LineGraphSeries<>();
        for (HRData data : series) {
            outputSeries.appendData(new DataPoint(data.getTimeStamp(), data.getBpm()),
                    false, Integer.MAX_VALUE, false);
        }
        return outputSeries;
    }

    /**
     * Utility function: taking a list of ECGData, convert it to a LineGraphSeries for viewing
     * @param series of ECGData for conversion
     * @return LineGraphSeries to be attached to a graph
     */
    public static LineGraphSeries<DataPoint> convertECGToGraphSeries(List<ECGData> series) {
        LineGraphSeries<DataPoint> outputSeries = new LineGraphSeries<>();
        for (ECGData data : series) {
            outputSeries.appendData(new DataPoint(data.getTimeStamp(), data.getVoltage()),
                    false, Integer.MAX_VALUE, false);
        }
        return outputSeries;
    }
}
