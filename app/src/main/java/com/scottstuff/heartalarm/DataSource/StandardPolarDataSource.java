package com.scottstuff.heartalarm.DataSource;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jjoe64.graphview.series.DataPoint;
import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.App.State;
import com.scottstuff.heartalarm.DataTypes.ECGData;
import com.scottstuff.heartalarm.DataTypes.HRData;
import com.scottstuff.heartalarm.Service.MonitorService;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.reactivex.rxjava3.functions.Function;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarEcgData;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarSensorSetting;

/**
 * Class which represents a standard Polar H10 connection with MonitorService
 * HR Data - Yielded directly from the H10's R-R calculations
 * ECG Data - Yielded from H10's ECG samples
 */
public class StandardPolarDataSource
        implements ECGDataSource,
        HRDataSource {

    // Fields:
    @NotNull
    MonitorService service;

    @NotNull
    Consumer<ECGData> ecgDataConsumer;

    @NotNull
    Consumer<HRData> hrDataConsumer;

    //Polar API reference
    @NotNull
    private PolarBleApi api;

    // Used to record the previous sample's timestamp (nanoseconds):
    private Long timeStamp;

    //String for logcat tag
    private final static String TAG = App.APP_TAG + ".StandardPolarDataSource";

    public StandardPolarDataSource(@NonNull MonitorService service) {
        // Set instance variables
        this.service = service;
        ecgDataConsumer = this.service::receiveECGUpdate;
        hrDataConsumer = this.service::receiveHRUpdate;

        State state = State.getInstance();

        //Polar API Stuff
        api = PolarBleApiDefaultImpl.defaultImplementation(service.getApplicationContext(),
                PolarBleApi.ALL_FEATURES);
        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                Log.d(TAG,"BLE power: " + powered);
            }

            @Override
            public void deviceConnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"CONNECTED: " + polarDeviceInfo.deviceId);
                String text = "Connected to Device "
                        + state.getPolarDeviceID();
                service.update(text, null);
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"CONNECTING: " + polarDeviceInfo.deviceId);
                String text ="Connecting to Device "
                        + state.getPolarDeviceID();
                service.update(text, null);
            }

            @Override
            public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"DISCONNECTED: " + polarDeviceInfo.deviceId);
                String text = "Disconnected from Device "
                        + state.getPolarDeviceID();
                service.update(text, null);
                try {
                    api.connectToDevice(state.getPolarDeviceID());
                } catch (PolarInvalidArgument pia) {
                    Log.w(TAG, "Error: Device Connection Failed. Message: " + pia.getMessage());
                }
            }

            @Override
            public void ecgFeatureReady(String identifier) {
                Log.d(TAG,"ECG READY: " + identifier);
                // ecg streaming setup
                api.requestEcgSettings(state.getPolarDeviceID())
                        .toFlowable()
                        .flatMap((Function<PolarSensorSetting, Publisher<PolarEcgData>>) polarEcgSettings -> {
                            PolarSensorSetting sensorSetting = polarEcgSettings.maxSettings();
                            return api.startEcgStreaming(
                                            (state.getPolarDeviceID()),
                                    sensorSetting);
                        }).subscribe(
                        polarEcgData -> {
                            // Must be a previous timestamp to execute
                            if (timeStamp != null && !polarEcgData.samples.isEmpty()) {
                                long timeDiff = polarEcgData.timeStamp - timeStamp;
                                Log.d(TAG, "     timeDiff (ns): " + timeDiff);
                                Log.d(TAG, "     # samples: " + polarEcgData.samples.size());
                                long deltaT = timeDiff / polarEcgData.samples.size();
                                long sampleTime = timeStamp;
                                for (int i = 0; i < polarEcgData.samples.size(); ++i) {
                                    sampleTime += deltaT;
                                    Log.d(TAG, "    yV: " + polarEcgData.samples.get(i) + "   time (ns): " + sampleTime);
                                    if (i == 0) {
                                        Log.d(TAG, "startstoptime: " + " start " + sampleTime);
                                    } else if ( i == polarEcgData.samples.size() - 1) {
                                        Log.d(TAG, "startstoptime: " + " stop " + sampleTime);
                                        timeStamp = polarEcgData.timeStamp;
                                        Log.d(TAG, "time-gap: " + (timeStamp - sampleTime) + " deltaT: " + deltaT);
                                    }
                                    ecgDataConsumer.accept(new ECGData
                                            (TimeUnit.MILLISECONDS.convert
                                                    (sampleTime, TimeUnit.NANOSECONDS),
                                            polarEcgData.samples.get(i)));
                                }
                            } else {
                                timeStamp = polarEcgData.timeStamp;
                            }
                        },
                        throwable -> Log.e(TAG, "" + throwable.toString()),
                        () -> Log.d(TAG, "complete")
                );
            }

            @Override
            public void accelerometerFeatureReady(String identifier) {
                Log.d(TAG,"ACC READY: " + identifier);
                // acc streaming can be started now if needed
            }

            @Override
            public void ppgFeatureReady(String identifier) {
                Log.d(TAG,"PPG READY: " + identifier);
                // ohr ppg can be started
            }

            @Override
            public void ppiFeatureReady(String identifier) {
                Log.d(TAG,"PPI READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void biozFeatureReady(String identifier) {
                Log.d(TAG,"BIOZ READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void hrFeatureReady(String identifier) {
                Log.d(TAG,"HR READY: " + identifier);
                // hr notifications are about to start
                String text = "HR Notifications Ready From Device "
                        + state.getPolarDeviceID();
                service.update(text, null);
            }

            @Override
            public void disInformationReceived(String identifier, UUID uuid, String value) {
                Log.d(TAG,"uuid: " + uuid + " value: " + value);

            }

            @Override
            public void batteryLevelReceived(String identifier, int level) {
                Log.d(TAG,"BATTERY LEVEL: " + level);
                String text = "Device "
                        + state.getPolarDeviceID()
                        + " Battery Level: " + level;
                service.update(text, null);

            }

            @Override
            public void hrNotificationReceived(String identifier, PolarHrData data) {
                Log.d(TAG,"HR value: " + data.hr + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + "," + data.contactStatusSupported);
                String text = "HR: " + data.hr + " RR: " + data.rrsMs;
                hrDataConsumer.accept(new HRData(new Date().getTime(), data.hr));
                service.update(text, data.hr);
            }

            @Override
            public void polarFtpFeatureReady(String s) {
                Log.d(TAG,"FTP ready");
            }
        });
        try {
            api.connectToDevice(state.getPolarDeviceID());
        } catch (PolarInvalidArgument pia) {
            Log.w(TAG, "Error: Device Connection Failed. Message: " + pia.getMessage());
        }

    }

    @Override
    public void setEcgDataConsumer(Consumer<ECGData> ecgDataConsumer) {
        Log.d(TAG, "setECGDataConsumer()");
        this.ecgDataConsumer = ecgDataConsumer;
    }

    @Override
    public void shutdownECGDataSource() {
        Log.d(TAG, "shutDownECGDataSource()");
        shutdownHRDataSource();
    }

    @Override
    public void setHrDataConsumer(Consumer<HRData> hrDataConsumer) {
        Log.d(TAG, "setHrDataConsumer()");
        this.hrDataConsumer = hrDataConsumer;
    }

    @Override
    public void shutdownHRDataSource() {
        Log.d(TAG, "shutdownHRDataSource()");
        api.shutDown();
    }
}
