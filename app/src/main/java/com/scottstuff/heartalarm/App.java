package com.scottstuff.heartalarm;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.util.Log;

import com.scottstuff.heartalarm.Activities.AlarmSettings;

public class App extends Application {
    //Logcat tags
    public static final String APP_TAG = "logHeartAlarm";
    private static final String TAG = APP_TAG + ".App";

    //Relevant tags for shared preferences
    public static final String SHARED_PREFERENCES = "sharedPrefsHeartAlarm";
    public static final String DEVICE_ID = "polarDeviceID";
    public static final String HHR_ENABLED = "hhrEnabled";
    public static final String LHR_ENABLED = "lhrEnabled";
    public static final String HHR_SETTING = "hhrSetting";
    public static final String LHR_SETTING = "lhrSetting";
    public static final String ALARM_SOUND_SETTING = "alarmSoundSetting";
    public static final String SERVICE_ON = "serviceOn";
    public static final String ALARM_ON = "alarmOn";

    //Global app state settings
    private String polarDeviceID;
    private boolean hhrEnabled;
    private boolean lhrEnabled;
    private int hhrSetting;
    private int lhrSetting;
    private int alarmSoundSetting;
    private boolean serviceRunning;
    public State state = new State();

    //Notification Channel
    public static final String CHANNEL_ID = "heartMonitorChannel";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        state.loadSharedPreferenceData();
        createNotificationChannel();
    }

    //Notification Channel Creator
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d(TAG, "createNotificationChannel()");
            NotificationChannel monitorChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Polar H10 Monitor Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            monitorChannel.setSound(null, null);
            monitorChannel.setVibrationPattern(null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(monitorChannel);
        }
    }

    class State {
        //Logcat tag
        private static final String TAG = App.TAG + ".State";

        //State management and memory
        public void loadSharedPreferenceData() {
            Log.d(TAG, "loadSharedPreferenceDad()");
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            polarDeviceID = sharedPreferences.getString(DEVICE_ID, "");
            hhrEnabled = sharedPreferences.getBoolean(HHR_ENABLED, false);
            lhrEnabled = sharedPreferences.getBoolean(LHR_ENABLED, false);
            hhrSetting = sharedPreferences.getInt(HHR_SETTING, Integer.parseInt(App.this.getResources().getString(R.string.alarmSettingsDefaultHighHeartRate)));
            lhrSetting = sharedPreferences.getInt(LHR_SETTING, Integer.parseInt(App.this.getResources().getString(R.string.alarmSettingsDefaultLowHeartRate)));
            alarmSoundSetting = sharedPreferences.getInt(ALARM_SOUND_SETTING, AlarmSettings.NONSTOP);
            serviceRunning = sharedPreferences.getBoolean(SERVICE_ON, false);
        }

        public boolean isDeviceIDDefined() {
            Log.d(TAG, "isDeviceIDDDefine()");
            return !polarDeviceID.isEmpty();
        }

        public String getPolarDeviceID() {
            Log.d(TAG, "getPolarDeviceID()");
            return polarDeviceID;
        }

        public void setPolarDeviceID(String polarDeviceID) {
            Log.d(TAG, "setPolarDeviceID()");
            App.this.polarDeviceID = polarDeviceID;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(DEVICE_ID, polarDeviceID);
            editor.apply();
        }

        public boolean isHhrEnabled() {
            Log.d(TAG, "isHhrEnabled()");
            return hhrEnabled;
        }

        public void setHhrEnabled(boolean hhrEnabled) {
            Log.d(TAG, "setHhrEnabled()");
            App.this.hhrEnabled = hhrEnabled;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(HHR_ENABLED, hhrEnabled);
            editor.apply();
        }

        public boolean isLhrEnabled() {
            Log.d(TAG, "isLhrEnabled()");
            return lhrEnabled;
        }

        public void setLhrEnabled(boolean lhrEnabled) {
            Log.d(TAG, "setLhrEnabled");
            App.this.lhrEnabled = lhrEnabled;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(LHR_ENABLED, lhrEnabled);
            editor.apply();
        }

        public int getHhrSetting() {
            Log.d(TAG, "getHhrSetting()");
            return hhrSetting;
        }

        public void setHhrSetting(int hhrSetting) {
            Log.d(TAG, "setHhrSetting()");
            App.this.hhrSetting = hhrSetting;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(HHR_SETTING, hhrSetting);
            editor.apply();
        }

        public int getLhrSetting() {
            Log.d(TAG, "getLhrSetting()");
            return lhrSetting;
        }

        public void setLhrSetting(int lhrSetting) {
            Log.d(TAG, "setLhrSetting()");
            App.this.lhrSetting = lhrSetting;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(LHR_SETTING, lhrSetting);
            editor.apply();
        }

        public int getAlarmSoundSetting() {
            Log.d(TAG, "getAlarmSoundSetting()");
            return alarmSoundSetting;
        }

        public class InvalidAlarmSettingException extends Exception {
            public InvalidAlarmSettingException(String message) {
                super(message);
                Log.d(TAG + ".InvalidAlarmSettingException", "InvalidAlarmSettingException()");
            }
        }

        public void setAlarmSoundSetting(int alarmSoundSetting) throws InvalidAlarmSettingException {
            Log.d(TAG, "setAlarmSoundSetting()");
            if (alarmSoundSetting < 1 || alarmSoundSetting > 3) {
                throw new InvalidAlarmSettingException("Invalid Alarm Setting Provided");
            }
            App.this.alarmSoundSetting = alarmSoundSetting;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(ALARM_SOUND_SETTING, alarmSoundSetting);
            editor.apply();
        }

        public boolean isServiceRunning() {
            Log.d(TAG, "isServiceRunning()");
            return serviceRunning;
        }

        public void setServiceRunning(boolean serviceRunning) {
            Log.d(TAG, "setServiceRunning");
            App.this.serviceRunning = serviceRunning;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SERVICE_ON, serviceRunning);
            editor.apply();
        }
    }

}
