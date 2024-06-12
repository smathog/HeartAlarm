package com.scottstuff.heartalarm.App;

import android.content.SharedPreferences;
import android.util.Log;

import com.scottstuff.heartalarm.Activities.AlarmSettings;
import com.scottstuff.heartalarm.Alarm.InvalidAlarmSettingException;
import com.scottstuff.heartalarm.R;

/**
 * Singleton that handles access to global state needed across the entire app
 */
public class State {
    //Global app state settings
    private String polarDeviceID;
    private boolean hhrEnabled;
    private boolean lhrEnabled;
    private int hhrSetting;
    private int lhrSetting;
    private int alarmSoundSetting;
    private boolean serviceRunning;

    // Shared Preferences Access
    private SharedPreferencesManager sharedPreferencesManager;

    private static State instance;

    private State() {
        sharedPreferencesManager = SharedPreferencesManager.getInstance();
    }

    public static State getInstance() {
        if (instance == null) {
            instance = new State();
        }
        return instance;
    }

    //State management and memory
    public void loadSharedPreferenceData() {
        polarDeviceID = sharedPreferencesManager.getDeviceId();
        hhrEnabled = sharedPreferencesManager.getHhrEnabled();
        lhrEnabled = sharedPreferencesManager.getLhrEnabled();
        hhrSetting = sharedPreferencesManager.getHhrSetting();
        lhrSetting = sharedPreferencesManager.getLhrSetting();
        alarmSoundSetting = sharedPreferencesManager.getAlarmSoundSetting();
        serviceRunning = sharedPreferencesManager.getServiceRunning();
    }

    public boolean isDeviceIDDefined() {
        return !polarDeviceID.isEmpty();
    }

    public String getPolarDeviceID() {
        return polarDeviceID;
    }

    public void setPolarDeviceID(String polarDeviceID) {
        this.polarDeviceID = polarDeviceID;
        sharedPreferencesManager.setPolarDeviceID(polarDeviceID);
    }

    public boolean isHhrEnabled() {
        return hhrEnabled;
    }

    public void setHhrEnabled(boolean hhrEnabled) {
        this.hhrEnabled = hhrEnabled;
        sharedPreferencesManager.setHhrEnabled(hhrEnabled);
    }

    public boolean isLhrEnabled() {
        return lhrEnabled;
    }

    public void setLhrEnabled(boolean lhrEnabled) {
        this.lhrEnabled = lhrEnabled;
        sharedPreferencesManager.setLhrEnabled(lhrEnabled);
    }

    public int getHhrSetting() {
        return hhrSetting;
    }

    public void setHhrSetting(int hhrSetting) {
        this.hhrSetting = hhrSetting;
        sharedPreferencesManager.setHhrSetting(hhrSetting);
    }

    public int getLhrSetting() {
        return lhrSetting;
    }

    public void setLhrSetting(int lhrSetting) {
        this.lhrSetting = lhrSetting;
        sharedPreferencesManager.setLhrSetting(lhrSetting);
    }

    public int getAlarmSoundSetting() {
        return alarmSoundSetting;
    }

    public void setAlarmSoundSetting(int alarmSoundSetting) throws InvalidAlarmSettingException {
        if (alarmSoundSetting < 1 || alarmSoundSetting > 3) {
            throw new InvalidAlarmSettingException("Invalid Alarm Setting Provided");
        }
        this.alarmSoundSetting = alarmSoundSetting;
        sharedPreferencesManager.setAlarmSoundSetting(alarmSoundSetting);
    }

    public boolean isServiceRunning() {
        return serviceRunning;
    }

    public void setServiceRunning(boolean serviceRunning) {
        this.serviceRunning = serviceRunning;
        sharedPreferencesManager.setServiceRunning(serviceRunning);
    }
}