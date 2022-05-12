package com.scottstuff.heartalarm.DataTypes;

/**
 * Abstract class to serve as parent for time-series data
 */
public abstract class DataPoint {
    private final long timeStamp;

    public DataPoint(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    abstract public String fieldUnit();
    abstract public String fieldName();
}
