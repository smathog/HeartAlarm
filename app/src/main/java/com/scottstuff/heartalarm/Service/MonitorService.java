package com.scottstuff.heartalarm.Service;

//My Imports
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

// Imports for Polar API Stuff
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

// Graph imports
import com.scottstuff.heartalarm.Activities.AlarmSettings;
import com.scottstuff.heartalarm.Activities.HeartAlarm;
import com.scottstuff.heartalarm.Alarm.AlarmManager;
import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.App.SharedPreferencesManager;
import com.scottstuff.heartalarm.App.State;
import com.scottstuff.heartalarm.DataDisplay.DataDisplay;
import com.scottstuff.heartalarm.DataSource.ECGDataSource;
import com.scottstuff.heartalarm.DataSource.HRDataSource;
import com.scottstuff.heartalarm.DataSource.StandardPolarDataSource;
import com.scottstuff.heartalarm.DataTypes.ECGData;
import com.scottstuff.heartalarm.DataTypes.HRData;
import com.scottstuff.heartalarm.R;
import com.scottstuff.heartalarm.SQL.ECGSQLiteRecorder;
import com.scottstuff.heartalarm.SQL.HrSQLiteRecorder;


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
    private HrSQLiteRecorder hrRecorder;

    // ECG recorder
    private ECGSQLiteRecorder ecgRecorder;

    // Android boilerplate management
    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "onCreate()");

        State state = State.getInstance();

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
        int mode = state.getAlarmSoundSetting();
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
        am = new AlarmManager(this, state.getLhrSetting(),
                false, state.getHhrSetting(),
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
        Log.d(TAG, "onDestroy()");
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
        Log.d(TAG, "onBind()");
        return binder;
    }

    public class LocalBinder extends Binder {
        public MonitorService getService() {
            Log.d(TAG, "getService()");
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
        Log.d(TAG, "registerActivity()");
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
        Log.d(TAG, "deregisterActivity()");
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
        Log.d(TAG, "updateFromIntent()");
        State state = State.getInstance();
        boolean lhrOn = intent.getBooleanExtra(SharedPreferencesManager.LHR_ENABLED, false);
        boolean hhrOn = intent.getBooleanExtra(SharedPreferencesManager.HHR_ENABLED, false);
        boolean alarmActive = intent.getBooleanExtra(SharedPreferencesManager.ALARM_ON, false);
        int alarmSoundSetting = intent.getIntExtra(SharedPreferencesManager.ALARM_SOUND_SETTING, AlarmSettings.NONSTOP);
        int hhrSetting = intent.getIntExtra(SharedPreferencesManager.HHR_SETTING, state.getHhrSetting());
        int lhrSetting = intent.getIntExtra(SharedPreferencesManager.LHR_SETTING, state.getLhrSetting());
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
     * Create an intent to update MonitorService
     * @param context originating the intent
     * @param alarmOn
     * @return intent
     */
    @NonNull
    public static Intent createMonitorServiceIntent(Context context, boolean alarmOn) {
        State state = State.getInstance();
        Intent serviceUpdateIntent = new Intent(context, MonitorService.class);
        serviceUpdateIntent.putExtra(SharedPreferencesManager.HHR_ENABLED, state.isHhrEnabled());
        serviceUpdateIntent.putExtra(SharedPreferencesManager.HHR_SETTING, state.getHhrSetting());
        serviceUpdateIntent.putExtra(SharedPreferencesManager.LHR_ENABLED, state.isLhrEnabled());
        serviceUpdateIntent.putExtra(SharedPreferencesManager.LHR_SETTING, state.getLhrSetting());
        serviceUpdateIntent.putExtra(SharedPreferencesManager.ALARM_SOUND_SETTING, state.getAlarmSoundSetting());
        serviceUpdateIntent.putExtra(SharedPreferencesManager.ALARM_ON, alarmOn);
        return serviceUpdateIntent;
    }

    /**
     * Update the service with a new HR datapoint
     * @param hrData - Relevant update data
     */
    public void receiveHRUpdate(HRData hrData) {
        Log.d(TAG, "receiveHRUpdate()");
        // Pass along to AlarmManager
        am.sendUpdate(hrData.getBpm());

        // Only update display if one present (e.g. activity with graph active)
        if (display != null) {
            display.updateHeartRateSeries(hrData);
        }

        // If currently recording, pass the datapoint to the recorder!
        if (hrRecorder != null) {
            hrRecorder.insertRecording(hrData);
        }
    }

    /**
     * Update the service with a new ECG datapoint
     * @param ecgData - ECG time series datapoint
     */
    public void receiveECGUpdate(ECGData ecgData) {
        Log.d(TAG, "receiveECGUpdate()");
        // Only update display if one present (e.g. activity with graph active)
        if (display != null) {
            display.updateECGSeries(ecgData);
        }

        // If currently recording, pass the datapoint to the recorder!
        if (ecgRecorder != null) {
            ecgRecorder.insertRecording(ecgData);
        }
    }

    /**
     * DataDisplay getter
     * @return MonitorService's DataDisplay instance
     */
    public DataDisplay getDataDisplay() {
        Log.d(TAG, "getDataDisplay()");
        return display;
    }

    // HR Recording Management

    /**
     * Order the MonitorService to start recording HR data
     */
    public void startRecordingHR() {
        Log.d(TAG, "startRecordingHR");
        if (hrRecorder == null) {
            hrRecorder = new HrSQLiteRecorder(this, System.currentTimeMillis(), 100);
        } else {
            // Already recording!
            Toast.makeText(this,
                            "The service is already recording heart rate!",
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Close down the hrRecorder and stop recording.
     */
    public void stopRecordingHR() {
        Log.d(TAG, "stopRecordingHR");
        hrRecorder.shutDown();
        hrRecorder = null;
    }

    // ECG Recording Management

    /**
     * Order the MonitorService to start recording ECG data
     */
    public void startRecordingECG() {
        Log.d(TAG, "startRecordingECG");
        if (ecgRecorder == null) {
            ecgRecorder = new ECGSQLiteRecorder(this, System.currentTimeMillis(), 1000);
        } else {
            // Already recording!
            Toast.makeText(this,
                            "The service is already recording ECG!",
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Close down the ecgRecorder and stop recording.
     */
    public void stopRecordingECG() {
        Log.d(TAG, "stopRecordingECG");
        ecgRecorder.shutDown();
        ecgRecorder = null;
    }

    /**
     * Helper function to indicate whether or not this MonitorService is currently recording
     * ECG readings.
     * @return whether the service is actively recording
     */
    public boolean isRecordingECG() {
        Log.d(TAG, "isRecordingECG()");
        return ecgRecorder != null;
    }

    /**
     * Helper function to indicate whether or not this MonitorService is currently recording
     * HR readings.
     * @return whether the service is actively recording
     */
    public boolean isRecordingHR() {
        Log.d(TAG, "isRecordingHR()");
        return hrRecorder != null;
    }

    /**
     * General service-wide update function; possibly calls updateNotification and always invokes
     * the update function for the registered activity, if any.
     * @param notificationText containing text if notification to be updated, otherwise
     *                         empty.
     * @param heartRate containing heart rate, if available, otherwise empty.
     */
    public void update(String notificationText, Integer heartRate) {
        Log.d(TAG, "update()");
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
        Log.d(TAG, "updateWithTitle()");
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
            builder.setContentTitle(newTitle);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } else {
            compatBuilder.setContentTitle(newTitle);
            notificationManager.notify(NOTIFICATION_ID, compatBuilder.build());
        }
    }
}
