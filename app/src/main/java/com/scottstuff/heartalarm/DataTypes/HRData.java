package com.scottstuff.heartalarm.DataTypes;

/**
 * Class to represent a datapoint of a HR time series
 */
public class HRData extends DataPoint {
    // Class static constant
    private final static String unit = "BPM";

    // Class instance-specific data
    private final int bpm;

    public HRData(long timeStamp, int bpm) {
        super(timeStamp);
        this.bpm = bpm;
    }

    public int getBpm() {
        return bpm;
    }

    @Override
    public String fieldUnit() {
        return unit;
    }

    @Override
    public String fieldName() {
        return unit;
    }

    @Override
    public String toString() {
        return String.format("HRData: %d BPM %d timeStamp", bpm, getTimeStamp());
    }
}
