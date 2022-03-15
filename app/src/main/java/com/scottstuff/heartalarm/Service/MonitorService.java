package com.scottstuff.heartalarm.Service;

//My Imports
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

// Imports for Polar API Stuff
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

// Graph imports
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.scottstuff.heartalarm.Activities.AlarmSettings;
import com.scottstuff.heartalarm.Activities.HeartAlarm;
import com.scottstuff.heartalarm.Alarm.AlarmManager;
import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.R;

import org.reactivestreams.Publisher;

import java.util.Date;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


import io.reactivex.rxjava3.functions.Function;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarEcgData;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarSensorSetting;


public class MonitorService extends Service {
    //String for logcat tag
    private final static String TAG = App.APP_TAG + ".MonitorService";

    //Notification ID for updates
    private final static int NOTIFICATION_ID = 1;
    //Notification builder (API >= 26)
    Notification.Builder builder;
    //NotificationCompat builder (API < 26)
    NotificationCompat.Builder compatBuilder;
    //Notification manager
    NotificationManager notificationManager;

    //Polar API reference
    private PolarBleApi api;

    //AlarmManager for alarm sounds
    private AlarmManager am;

    //SoundPool for connection noises
    private SoundPool sp;
    private int soundConnectID;
    private int soundConnectStreamID = -1;
    private int soundDisconnectID;
    private int soundDisconnectStreamID = -1;

    // Binder to return to activities
    private final IBinder binder = new LocalBinder();

    // Activity binding
    private Optional<UpdateActivity> boundActivity = Optional.empty();

    // Series for graphs
    private LineGraphSeries<DataPoint> heartRateSeries;
    private LineGraphSeries<DataPoint> ecgSeries;

    // Used to record the previous sample's timestamp:
    private OptionalLong timeStamp = OptionalLong.empty();

    // Android boilerplate management
    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "onCreate()");

        // Initialize graph series
        heartRateSeries = new LineGraphSeries<>();
        ecgSeries = new LineGraphSeries<>();

        //SoundPool setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build();
            sp = new SoundPool
                    .Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            sp = new SoundPool(2, AudioManager.STREAM_ALARM, 0);
        }
        soundConnectID = sp.load(this, R.raw.sheep, 1);
        soundDisconnectID = sp.load(this, R.raw.beat, 1);


        //Setup AlarmManager
        int mode = ((App) this.getApplication()).state.getAlarmSoundSetting();
        AlarmManager.AlarmMode aMode;
        switch (mode) {
            case AlarmSettings.IMMEDIATE_STOP:
                aMode = AlarmManager.AlarmMode.INSTANT_STOP;
                break;
            case AlarmSettings.TIMED_STOP:
                aMode = AlarmManager.AlarmMode.DELAYED_STOP;
                break;
            case AlarmSettings.NONSTOP:
            default:
                aMode = AlarmManager.AlarmMode.NONSTOP;
                break;
        }
        //Start with alarms off; must be updated through onStartCommand() intent.
        am = new AlarmManager(this,
                ((App) this.getApplication()).state.getLhrSetting(),
                false,
                ((App) this.getApplication()).state.getHhrSetting(),
                false,
                aMode,
                false);

        //Foreground service boilerplate
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, HeartAlarm.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, App.CHANNEL_ID)
                    .setContentTitle("HeartAlarm Monitor (Polar H10)")
                    .setContentText("The monitor is running.")
                    .setSmallIcon(R.drawable.eye)
                    .setContentIntent(pendingIntent)
                    .setOnlyAlertOnce(true);
            notification = builder.build();
        } else {
            compatBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle("HeartAlarm Monitor (Polar H10)")
                    .setContentText("The monitor is running.")
                    .setSmallIcon(R.drawable.eye)
                    .setContentIntent(pendingIntent)
                    .setOnlyAlertOnce(true);
            notification = compatBuilder.build();
        }
        startForeground(NOTIFICATION_ID, notification);

        // Initial update
        update(Optional.empty(), Optional.empty());

        //Polar API Stuff
        api = PolarBleApiDefaultImpl.defaultImplementation(this, PolarBleApi.ALL_FEATURES);
        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                Log.d(TAG,"BLE power: " + powered);
            }

            @Override
            public void deviceConnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"CONNECTED: " + polarDeviceInfo.deviceId);
                String text = "Connected to Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID();
                update(Optional.of(text), Optional.empty());
                if (soundConnectStreamID != -1)
                    sp.stop(soundConnectStreamID);
                if (soundDisconnectStreamID != -1)
                    sp.stop(soundDisconnectStreamID);
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"CONNECTING: " + polarDeviceInfo.deviceId);
                String text ="Connecting to Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID();
                update(Optional.of(text), Optional.empty());
                soundConnectStreamID = sp.play(soundConnectID, 1, 1, 1, -1, 1);
            }

            @Override
            public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"DISCONNECTED: " + polarDeviceInfo.deviceId);
                String text = "Disconnected from Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID();
                update(Optional.of(text), Optional.empty());
                soundDisconnectStreamID = sp.play(soundDisconnectID, 1,  1, 1, -1, 1);
                try {
                    api.connectToDevice(((App) MonitorService.this.getApplication()).state.getPolarDeviceID());
                } catch (PolarInvalidArgument pia) {
                    Log.w(TAG, "Error: Device Connection Failed. Message: " + pia.getMessage());
                }
            }

            @Override
            public void ecgFeatureReady(String identifier) {
                Log.d(TAG,"ECG READY: " + identifier);
                // ecg streaming setup
                api.requestEcgSettings(((App) MonitorService.this.getApplication()).state.getPolarDeviceID())
                        .toFlowable()
                        .flatMap((Function<PolarSensorSetting, Publisher<PolarEcgData>>) polarEcgSettings -> {
                            PolarSensorSetting sensorSetting = polarEcgSettings.maxSettings();
                            return api.startEcgStreaming((
                                    ((App) MonitorService.this.getApplication()).state.getPolarDeviceID()),
                                    sensorSetting);
                        }).subscribe(
                        polarEcgData -> {
                            // Must be a previous timestamp to execute
                            if (timeStamp.isPresent() && !polarEcgData.samples.isEmpty()) {
                                long timeDiff = (TimeUnit.MILLISECONDS.convert(polarEcgData.timeStamp, TimeUnit.NANOSECONDS)
                                        - timeStamp.getAsLong());
                                Log.d(TAG, "     timeDiff: " + timeDiff);
                                long deltaT = timeDiff / polarEcgData.samples.size();
                                long sampleTime = timeStamp.getAsLong();
                                for (int i = 0; i < polarEcgData.samples.size(); ++i) {
                                    sampleTime += deltaT;
                                    Log.d(TAG, "    yV: " + polarEcgData.samples.get(i) + "   time: " + sampleTime);
                                    if (i == 0) {
                                        Log.d(TAG, "startstoptime: " + " start " + sampleTime);
                                    } else if ( i == polarEcgData.samples.size() - 1) {
                                        Log.d(TAG, "startstoptime: " + " stop " + sampleTime);
                                        timeStamp = OptionalLong.of(TimeUnit.MILLISECONDS.convert(polarEcgData.timeStamp, TimeUnit.NANOSECONDS));
                                    }
                                    ecgSeries.appendData(new DataPoint(sampleTime, polarEcgData.samples.get(i)),
                                            true, Integer.MAX_VALUE, i != polarEcgData.samples.size() - 1);

                                }
                            } else {
                                timeStamp = OptionalLong.of(TimeUnit.MILLISECONDS.convert(polarEcgData.timeStamp, TimeUnit.NANOSECONDS));
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
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID();
                update(Optional.of(text), Optional.empty());
            }

            @Override
            public void disInformationReceived(String identifier, UUID uuid, String value) {
                Log.d(TAG,"uuid: " + uuid + " value: " + value);

            }

            @Override
            public void batteryLevelReceived(String identifier, int level) {
                Log.d(TAG,"BATTERY LEVEL: " + level);
                String text = "Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID()
                        + " Battery Level: " + level;
                update(Optional.of(text), Optional.empty());

            }

            @Override
            public void hrNotificationReceived(String identifier, PolarHrData data) {
                Log.d(TAG,"HR value: " + data.hr + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + "," + data.contactStatusSupported);
                String text = "HR: " + data.hr + " RR: " + data.rrsMs;
                heartRateSeries.appendData(new DataPoint(new Date(), data.hr),
                        true, Integer.MAX_VALUE, false);
                update(Optional.of(text), Optional.of(data.hr));
                am.sendUpdate(data.hr);
            }

            @Override
            public void polarFtpFeatureReady(String s) {
                Log.d(TAG,"FTP ready");
            }
        });
        try {
            api.connectToDevice(((App) this.getApplication()).state.getPolarDeviceID());
        } catch (PolarInvalidArgument pia) {
            Log.w(TAG, "Error: Device Connection Failed. Message: " + pia.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        api.shutDown();
        am.shutDown();
        if (soundDisconnectStreamID != -1)
            sp.stop(soundDisconnectStreamID);
        if (soundConnectStreamID != -1)
            sp.stop(soundConnectStreamID);
        Log.w(TAG, "onDestroy()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Log.w(TAG, "onStartCommand()");
        super.onStartCommand(intent, flags, startID);
        //Grab alarm stuff from intent, use to update alarm
        updateFromIntent(intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public MonitorService getService() {
            // Returns instance of MonitorService
            return MonitorService.this;
        }
    }

    // Public interface

    /**
     * Attempts to register an update-capable activity as bound to this service
     * @param activity to be registered
     * @return true if activity was registered, false if unable
     */
    public boolean registerActivity(UpdateActivity activity) {
        if (boundActivity.isPresent()) {
            return false;
        } else {
            boundActivity = Optional.of(activity);
            return true;
        }
    }

    /**
     * De-registers the activity bound to this service.
     */
    public void deregisterActivity() {
        boundActivity = Optional.empty();
    }

    /**
     * Data class for passing information to activities via update method
     */
    public static class DataBundle {
        // Optionals may or may not be present at the time of the request
        public Optional<Integer> heartRate;

        // Non-optionals are *always* present
        public boolean alarmActive;
    }

    /**
     * Class which represents an activity which is updated with via DataBundle from this service
     */
    public static abstract class UpdateActivity extends AppCompatActivity {
        public abstract void serviceUpdate(DataBundle dataBundle);
    }

    /**
     * Updates the MonitorService using the provided intent
     * @param intent to use
     */
    public void updateFromIntent(Intent intent) {
        boolean lhrOn = intent.getBooleanExtra(App.LHR_ENABLED, false);
        boolean hhrOn = intent.getBooleanExtra(App.HHR_ENABLED, false);
        boolean alarmActive = intent.getBooleanExtra(App.ALARM_ON, false);
        int alarmSoundSetting = intent.getIntExtra(App.ALARM_SOUND_SETTING, AlarmSettings.NONSTOP);
        int hhrSetting = intent.getIntExtra(App.HHR_SETTING, ((App) this.getApplication()).state.getHhrSetting());
        int lhrSetting = intent.getIntExtra(App.LHR_SETTING, ((App) this.getApplication()).state.getLhrSetting());
        am.setAlarmActive(alarmActive);
        am.updateLowerLimit(lhrSetting);
        am.updateUpperLimit(hhrSetting);
        am.updateLowerAlarmStatus(lhrOn);
        am.updateUpperAlarmStatus(hhrOn);
        AlarmManager.AlarmMode aMode;
        switch (alarmSoundSetting) {
            case AlarmSettings.IMMEDIATE_STOP:
                aMode = AlarmManager.AlarmMode.INSTANT_STOP;
                break;
            case AlarmSettings.TIMED_STOP:
                aMode = AlarmManager.AlarmMode.DELAYED_STOP;
                break;
            case AlarmSettings.NONSTOP:
            default:
                aMode = AlarmManager.AlarmMode.NONSTOP;
                break;
        }
        am.updateAlarmMode(aMode);
        if (alarmActive) {
            String updatedContentTitle = "(Alarm Active)"
                    + (lhrOn ? " -L " + lhrSetting : "")
                    + (hhrOn ? " -H " + hhrSetting : "");
            updateWithTitle(Optional.of(updatedContentTitle), Optional.empty());
        } else {
            final String defaultTitle = "HeartAlarm Monitor (Polar H10)";
            updateWithTitle(Optional.of(defaultTitle), Optional.empty());
        }
    }

    /**
     * Getter for the heartRateSeries
     * @return heartRateSeries
     */
    public LineGraphSeries<DataPoint> getHeartRateSeries() {
        return heartRateSeries;
    }

    /**
     * Getter for the ECG data series
     * @return ecgSeries
     */
    public LineGraphSeries<DataPoint> getEcgSeries() {
        return ecgSeries;
    }


    // Private methods

    /**
     * General service-wide update function; possibly calls updateNotification and always invokes
     * the update function for the registered activity, if any.
     * @param notificationText Optional containing text if notification to be updated, otherwise
     *                         empty.
     * @param heartRate Optional containing heart rate, if available, otherwise empty.
     */
    private void update(Optional<String> notificationText, Optional<Integer> heartRate) {
        notificationText.ifPresent(this::updateNotification);
        if (boundActivity.isPresent()) {
            DataBundle bundle = new DataBundle();
            bundle.heartRate = heartRate;
            bundle.alarmActive = am.alarmActive();
            boundActivity.get().serviceUpdate(bundle);
        }
    }

    /**
     * General service-wide update function that possibly updates the title of the notification.
     * @param titleText Optional containing possible update title of notification
     * @param heartRate Optional containing heart rate if available
     */
    private void updateWithTitle(Optional<String> titleText, Optional<Integer> heartRate) {
        titleText.ifPresent(this::updateNotificationTitle);
        if (boundActivity.isPresent()) {
            DataBundle bundle = new DataBundle();
            bundle.heartRate = heartRate;
            bundle.alarmActive = am.alarmActive();
            boundActivity.get().serviceUpdate(bundle);
        }
    }



    /**
     * Updates the body text of the notification associated with the foreground service
     * @param newText to be set in the notification
     */
    private void updateNotification(String newText) {
        Log.w(TAG, "updateNotification()");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setContentText(newText);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } else {
            compatBuilder.setContentText(newText);
            notificationManager.notify(NOTIFICATION_ID, compatBuilder.build());
        }
    }

    /**
     * Updates the title text of the notification associated with the foreground service
     * @param newTitle to be set in the notification
     */
    private void updateNotificationTitle(String newTitle) {
        Log.w(TAG, "updateNotificationTitle()");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setContentText(newTitle);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } else {
            compatBuilder.setContentText(newTitle);
            notificationManager.notify(NOTIFICATION_ID, compatBuilder.build());
        }
    }
}
