package com.scottstuff.heartalarm.App;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import com.scottstuff.heartalarm.Activities.AlarmSettings;
import com.scottstuff.heartalarm.R;

public class App extends Application {
    //Logcat tags
    public static final String APP_TAG = "logHeartAlarm";
    private static final String TAG = APP_TAG + ".App";

    private State state;

    private static App instance;

    //Notification Channel
    public static final String CHANNEL_ID = "heartMonitorChannel";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        instance = this;
        state = State.getInstance();
        state.loadSharedPreferenceData();
        createNotificationChannel();
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    //Notification Channel Creator
    private void createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel()");
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
}
