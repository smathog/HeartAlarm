package com.scottstuff.heartalarm.DataTypes;

/**
 * Class to represent a datapoint of a HR time series
 */
public class HRData {
    private final long timeStamp;
    private final int bpm;


    public HRData(long timeStamp, int bpm) {
        this.timeStamp = timeStamp;
        this.bpm = bpm;
    }


    public long getTimeStamp() {
        return timeStamp;
    }

    public int getBpm() {
        return bpm;
    }
}
