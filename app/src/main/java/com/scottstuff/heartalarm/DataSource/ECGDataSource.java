package com.scottstuff.heartalarm.DataSource;

import com.scottstuff.heartalarm.DataTypes.ECGData;

import java.util.function.Consumer;

/**
 * Indicates a capability to update a MonitorService with a new ECG reading
 */
public interface ECGDataSource {
    /**
     * Function to be called to set what is to be done with each new instance of ECGData
     */
    void setEcgDataConsumer(Consumer<ECGData> ecgDataConsumer);

    /**
     * Function to call to shut down the ECGDataSource
     */
    void shutdownECGDataSource();
}
