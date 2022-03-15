package com.scottstuff.heartalarm.DataSource;

/**
 * Indicates a capability to update a MonitorService with a new ECG reading
 */
public interface ECGDataSource {
    /**
     * Function to be called to update with new ECG data sample, once sample(s) available
     */
    void yieldECGSample(long timeStamp, int ecgData);

    /**
     * Function to call to shut down the ECGDataSource
     */
    void shutdownECGDataSource();
}
