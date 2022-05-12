package com.scottstuff.heartalarm.DataSource;

import com.scottstuff.heartalarm.DataTypes.HRData;

import java.util.function.Consumer;

/**
 * Indicates a capability to update a MonitorService with a new HR reading
 */
public interface HRDataSource {
    /**
     * Function to be called to set a consumer for new HRData
     */
    void setHrDataConsumer(Consumer<HRData> hrDataConsumer);

    /**
     * Function to be called to shut down HRDataSource.
     */
    void shutdownHRDataSource();
}
