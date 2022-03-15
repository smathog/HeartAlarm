package com.scottstuff.heartalarm.DataSource;

/**
 * Indicates a capability to update a MonitorService with a new HR reading
 */
public interface HRDataSource {
    /**
     * Function to be called to update with new HR data sample, once sample(s) available
     */
    void yieldHRSample(int hr);

    /**
     * Function to be called to shut down HRDataSource.
     */
    void shutdownHRDataSource();
}
