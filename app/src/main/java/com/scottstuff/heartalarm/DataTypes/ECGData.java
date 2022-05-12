package com.scottstuff.heartalarm.DataTypes;

public class ECGData extends DataPoint {
    private static final String unit = "uV";
    private static final String fieldName = "Voltage";

    private final long voltage;

    public ECGData(long timeStamp, long voltage) {
        super(timeStamp);
        this.voltage = voltage;
    }

    public long getVoltage() {
        return voltage;
    }


    @Override
    public String fieldUnit() {
        return unit;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    @Override
    public String toString() {
        return String.format("ECGData: %d uV %d timeStamp", voltage, getTimeStamp());
    }
}
