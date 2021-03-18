package com.scottstuff.heartalarm;

//My Imports
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

//Imports for Polar API Stuff
import androidx.core.app.NotificationCompat;

import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarAccelerometerData;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarEcgData;
import polar.com.sdk.api.model.PolarExerciseEntry;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarOhrPPGData;
import polar.com.sdk.api.model.PolarOhrPPIData;
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

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "onCreate()");

        //Setup SoundPool
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
                updateNotification("Connected to Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID());
                if (soundConnectStreamID != -1)
                    sp.stop(soundConnectStreamID);
                if (soundDisconnectStreamID != -1)
                    sp.stop(soundDisconnectStreamID);
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"CONNECTING: " + polarDeviceInfo.deviceId);
                updateNotification("Connecting to Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID());
                soundConnectStreamID = sp.play(soundConnectID, 1, 1, 1, -1, 1);
            }

            @Override
            public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG,"DISCONNECTED: " + polarDeviceInfo.deviceId);
                updateNotification("Disconnected from Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID());
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
                // ecg streaming can be started now if needed
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
                updateNotification("HR Notifications Ready From Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID());
            }

            @Override
            public void disInformationReceived(String identifier, UUID uuid, String value) {
                Log.d(TAG,"uuid: " + uuid + " value: " + value);

            }

            @Override
            public void batteryLevelReceived(String identifier, int level) {
                Log.d(TAG,"BATTERY LEVEL: " + level);
                updateNotification("Device "
                        + ((App) MonitorService.this.getApplication()).state.getPolarDeviceID()
                        + " Battery Level: " + level);

            }

            @Override
            public void hrNotificationReceived(String identifier, PolarHrData data) {
                Log.d(TAG,"HR value: " + data.hr + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + "," + data.contactStatusSupported);
                updateNotification("HR: " + data.hr + " RR: " + data.rrsMs);
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                builder.setContentTitle(updatedContentTitle);
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            } else {
                compatBuilder.setContentTitle(updatedContentTitle);
                notificationManager.notify(NOTIFICATION_ID, compatBuilder.build());
            }
        } else {
            final String defaultTitle = "HeartAlarm Monitor (Polar H10)";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                builder.setContentTitle(defaultTitle);
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            } else {
                compatBuilder.setContentTitle(defaultTitle);
                notificationManager.notify(NOTIFICATION_ID, compatBuilder.build());
            }
        }
        return START_REDELIVER_INTENT;
    }

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

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
