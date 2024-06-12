package com.scottstuff.heartalarm.App;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.util.Log;

import com.scottstuff.heartalarm.Activities.AlarmSettings;
import com.scottstuff.heartalarm.Alarm.InvalidAlarmSettingException;
import com.scottstuff.heartalarm.R;

/**
 * Singleton to handle retrieving and updating shared preference data
 */
public class SharedPreferencesManager {
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


    private final SharedPreferences sharedPreferences;
    private static SharedPreferencesManager instance;

    private SharedPreferencesManager() {
        sharedPreferences = App.getAppContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
    }

    public static SharedPreferencesManager getInstance() {
        if (instance == null) {
            instance = new SharedPreferencesManager();
        }
        return instance;
    }

    public String getDeviceId() {
        return sharedPreferences.getString(DEVICE_ID, "");
    }

    public void setPolarDeviceID(String polarDeviceID) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DEVICE_ID, polarDeviceID);
        editor.apply();
    }

    public boolean getHhrEnabled() {
        return sharedPreferences.getBoolean(HHR_ENABLED, false);
    }

    public void setHhrEnabled(boolean hhrEnabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(HHR_ENABLED, hhrEnabled);
        editor.apply();
    }

    public boolean getLhrEnabled() {
        return sharedPreferences.getBoolean(LHR_ENABLED, false);
    }

    public void setLhrEnabled(boolean lhrEnabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(LHR_ENABLED, lhrEnabled);
        editor.apply();
    }

    public int getHhrSetting() {
        return sharedPreferences.getInt(HHR_SETTING, Integer.parseInt(App.getAppContext().getResources().getString(R.string.alarmSettingsDefaultHighHeartRate)));
    }

    public void setHhrSetting(int hhrSetting) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(HHR_SETTING, hhrSetting);
        editor.apply();
    }

    public int getLhrSetting() {
        return sharedPreferences.getInt(LHR_SETTING, Integer.parseInt(App.getAppContext().getResources().getString(R.string.alarmSettingsDefaultLowHeartRate)));
    }

    public void setLhrSetting(int lhrSetting) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(LHR_SETTING, lhrSetting);
        editor.apply();
    }

    public int getAlarmSoundSetting() {
        return sharedPreferences.getInt(ALARM_SOUND_SETTING, AlarmSettings.NONSTOP);
    }

    public void setAlarmSoundSetting(int alarmSoundSetting) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(ALARM_SOUND_SETTING, alarmSoundSetting);
        editor.apply();
    }

    public boolean getServiceRunning() {
        return sharedPreferences.getBoolean(SERVICE_ON, false);
    }

    public void setServiceRunning(boolean serviceRunning) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SERVICE_ON, serviceRunning);
        editor.apply();
    }
}
