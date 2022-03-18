package com.scottstuff.heartalarm.Service;

//My Imports
import android.app.Activity;
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
import com.scottstuff.heartalarm.DataDisplay.DataDisplay;
import com.scottstuff.heartalarm.DataSource.ECGDataSource;
import com.scottstuff.heartalarm.DataSource.HRDataSource;
import com.scottstuff.heartalarm.DataSource.StandardPolarDataSource;
import com.scottstuff.heartalarm.R;
import com.scottstuff.heartalarm.SQL.hrSQLRecorder;

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
    private UpdateFromService boundActivity;

    // ECG Data Provider:
    private ECGDataSource ecgDataSource;

    // HR Data Provider:
    private HRDataSource hrDataSource;

    // Data Display
    private DataDisplay display;

    // HR recorder
    private hrSQLRecorder hrRecorder;

    // Android boilerplate management
    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "onCreate()");

        // Set Datasources
        //TODO: make this actually a selection
        StandardPolarDataSource temp = new StandardPolarDataSource(this);
        ecgDataSource = temp;
        hrDataSource = temp;

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
        update(null, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ecgDataSource.shutdownECGDataSource();
        hrDataSource.shutdownHRDataSource();
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
     */
    public void registerActivity(UpdateFromService activity) {
        boundActivity = activity;

        // Set up data display
        this.display = new DataDisplay();
        display.bindActivity((Activity) activity);
        Log.w(TAG, String.format("display bound to activity %s", activity.getClass()));
    }

    /**
     * De-registers the activity bound to this service.
     */
    public void deregisterActivity() {
        String boundClass = boundActivity.getClass().toString();
        boundActivity = null;

        // If no activity is bound to the service, no need for an active DataDisplay
        display.bindActivity(null);
        display = null;
        Log.w(TAG, String.format("display unbound from activity %s", boundClass));
    }

    /**
     * Data class for passing information to activities via update method
     */
    public static class DataBundle {
        // May or may not be present at the time of the request, so null check these
        public Integer heartRate;

        // *ALWAYS* present
        public boolean alarmActive;
    }

    /**
     * Interface which represents an ability to be updated from MonitorService.
     * Updated via DataBundle provided by this service
     */
    public interface UpdateFromService {
        void serviceUpdate(DataBundle dataBundle);
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
            updateWithTitle(updatedContentTitle, null);
        } else {
            final String defaultTitle = "HeartAlarm Monitor (Polar H10)";
            updateWithTitle(defaultTitle, null);
        }
    }

    /**
     * Update the service with a new HR datapoint
     * @param hr - Relevant update data
     */
    public void receiveHRUpdate(int hr) {
        // Only update display if one present (e.g. activity with graph active)
        if (display != null) {
            display.updateHeartRateSeries(hr);
        }
    }

    /**
     * Update the service with a new ECG datapoint
     * @param timeStamp - Time ecg voltage was recorded
     * @param ecgData - ECG voltage in uv
     */
    public void receiveECGUpdate(long timeStamp, int ecgData) {
        // Only update display if one present (e.g. activity with graph active)
        if (display != null) {
            display.updateECGSeries(timeStamp, ecgData);
        }
    }

    /**
     * DataDisplay getter
     * @return MonitorService's DataDisplay instance
     */
    public DataDisplay getDataDisplay() {
        return display;
    }

    public boolean isRecordingHR() {
        return hrRecorder == null;
    }

    /**
     * General service-wide update function; possibly calls updateNotification and always invokes
     * the update function for the registered activity, if any.
     * @param notificationText containing text if notification to be updated, otherwise
     *                         empty.
     * @param heartRate containing heart rate, if available, otherwise empty.
     */
    public void update(String notificationText, Integer heartRate) {
        if (notificationText != null) {
            updateNotification(notificationText);
        }
        if (boundActivity != null) {
            DataBundle bundle = new DataBundle();
            bundle.heartRate = heartRate;
            bundle.alarmActive = am.alarmActive();
            boundActivity.serviceUpdate(bundle);
        }
    }

    /**
     * General service-wide update function that possibly updates the title of the notification.
     * @param titleText containing possible update title of notification
     * @param heartRate containing heart rate if available
     */
    private void updateWithTitle(String titleText, Integer heartRate) {
        if (titleText != null) {
            updateNotificationTitle(titleText);
        }
        if (boundActivity != null) {
            DataBundle bundle = new DataBundle();
            bundle.heartRate = heartRate;
            bundle.alarmActive = am.alarmActive();
            boundActivity.serviceUpdate(bundle);
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
