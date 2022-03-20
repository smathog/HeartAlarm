package com.scottstuff.heartalarm.Alarm;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import com.scottstuff.heartalarm.R;

public class AlarmManager {
    //Relevant SoundPool members
    private SoundPool sp;
    //High alarm soundID
    private int soundID1;
    //High alarm streamID
    private int highStreamID;
    //Low alarm soundID
    private int soundID2;
    //Low alarm streamID
    private int lowStreamID;

    //Alarm management
    private boolean lowerAlarmPlaying;
    private boolean lowerLimitAlarmActive;
    private int lowerLimit;
    private boolean upperAlarmPlaying;
    private boolean  upperLimitAlarmActive;
    private int upperLimit;
    private AlarmMode currentMode;
    boolean alarmActive;

    //Log tag
    private static final String TAG = "AlarmManager";

    public enum AlarmMode {
        INSTANT_STOP, DELAYED_STOP, NONSTOP
    }

    public AlarmManager(Context context, int lowerLimit, boolean lowerLimitAlarmActive, int upperLimit, boolean upperLimitAlarmActive, AlarmMode currentMode, boolean alarmActive) {
        Log.d(TAG, "AlarmManager()");
        this.lowerLimit = lowerLimit;
        this.lowerLimitAlarmActive = lowerLimitAlarmActive;
        this.upperLimit = upperLimit;
        this.upperLimitAlarmActive = upperLimitAlarmActive;
        this.currentMode = currentMode;
        this.alarmActive = alarmActive;

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
        soundID1 = sp.load(context, R.raw.wail_siren2, 1);
        soundID2 = sp.load(context, R.raw.yelp_siren, 1);
        this.lowerLimitAlarmActive = false;
        this.upperLimitAlarmActive = false;
    }

    public boolean alarmActive() {
        Log.d(TAG, "alarmActive()");
        return alarmActive;
    }

    public void setAlarmActive(boolean state) {
        Log.d(TAG, "setAlarmActive()");
        this.alarmActive = state;
        if (!alarmActive)
        {
            if (upperAlarmPlaying) {
                sp.stop(highStreamID);
                upperAlarmPlaying = false;
            }
            if (lowerAlarmPlaying) {
                sp.stop(lowStreamID);
                lowerAlarmPlaying = false;
            }
        }
    }

    public void updateLowerLimit(int lowerLimit) {
        Log.d(TAG, "updateLowerLimit()");
        this.lowerLimit = lowerLimit;
    }

    public void updateLowerAlarmStatus(boolean status) {
        Log.d(TAG, "updateLowerAlarmStatus()");
        this.lowerLimitAlarmActive = status;
        if (!lowerLimitAlarmActive && lowerAlarmPlaying) {
            sp.stop(lowStreamID);
            lowerAlarmPlaying = false;
        }
    }

    public void updateUpperLimit(int upperLimit) {
        Log.d(TAG, "updateUpperLimit()");
        this.upperLimit = upperLimit;
    }

    public void updateUpperAlarmStatus(boolean status) {
        Log.d(TAG, "updateUpperAlarmStatus()");
        this.upperLimitAlarmActive = status;
        if (!upperLimitAlarmActive && upperAlarmPlaying) {
            sp.stop(highStreamID);
            upperAlarmPlaying = false;
        }
    }

    public void updateAlarmMode(AlarmMode newMode) {
        Log.d(TAG, "updateAlarmMode()");
        this.currentMode = newMode;
    }

    public void sendUpdate(int num) {
        Log.d(TAG, "sendUpdate()");
        if (!alarmActive)
            return;
        if (upperLimitAlarmActive) {
            if (num > upperLimit) {
                if (!upperAlarmPlaying) {
                    upperAlarmPlaying = true;
                    if (lowerAlarmPlaying) {
                        lowerAlarmPlaying = false;
                        sp.stop(lowStreamID);
                    }
                    highStreamID = sp.play(soundID1, 1, 1, 1, -1, 1);
                }
                return;
            } else {
                if (upperAlarmPlaying) {
                    switch (currentMode) {
                        case INSTANT_STOP:
                        case DELAYED_STOP:
                            upperAlarmPlaying = false;
                            sp.stop(highStreamID);
                            break;
                        case NONSTOP:
                        default:
                            break;
                    }
                }
            }
        }
        if (lowerLimitAlarmActive) {
            if (num < lowerLimit) {
                if (!lowerAlarmPlaying) {
                    lowerAlarmPlaying = true;
                    if (upperAlarmPlaying) {
                        upperAlarmPlaying = false;
                        sp.stop(highStreamID);
                    }
                    lowStreamID = sp.play(soundID2, 1, 1, 1, -1, 1);
                }
                return;
            } else {
                if (lowerAlarmPlaying) {
                    switch (currentMode) {
                        case INSTANT_STOP:
                        case DELAYED_STOP:
                            lowerAlarmPlaying = false;
                            sp.stop(lowStreamID);
                            break;
                        case NONSTOP:
                        default:
                            break;
                    }
                }
            }
        }
    }

    public void shutDown() {
        Log.d(TAG, "shutDown()");
        if (upperAlarmPlaying) {
            sp.stop(highStreamID);
        }
        if (lowerAlarmPlaying) {
            sp.stop(lowStreamID);
        }
    }
}
