package com.scottstuff.heartalarm.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.Service.MonitorService;
import com.scottstuff.heartalarm.R;
import com.scottstuff.heartalarm.Utility.Utility;

public class AlarmSettings extends AppCompatActivity {
    //Logcat tag
    private static final String TAG = App.APP_TAG + ".AlarmSettings";

    private EditText editTextHHR;
    private EditText editTextLHR;
    private Button buttonEnableHHR;
    private Button buttonDisableHHR;
    private Button buttonEnableLHR;
    private Button buttonDisableLHR;
    private Button buttonAlarmSoundImmediateStop;
    private Button buttonAlarmSoundTimedStop;
    private Button buttonAlarmSoundNonstop;

    //Alarm Sound Setting constants
    public static final int IMMEDIATE_STOP = 1;
    public static final int TIMED_STOP = 2;
    public static final int NONSTOP = 3;

    //Global app access
    private App app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);
        app = (App) this.getApplication();
        editTextHHR = findViewById(R.id.hhr_alarm_setting);
        editTextHHR.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                app.state.setHhrSetting(Integer.parseInt(editTextHHR.getText().toString()));
            }
        });
        editTextLHR = findViewById(R.id.lhr_alarm_setting);
        editTextLHR.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                app.state.setLhrSetting(Integer.parseInt(editTextLHR.getText().toString()));
            }
        });
        buttonEnableHHR = findViewById(R.id.enable_hhr);
        buttonDisableHHR = findViewById(R.id.disable_hhr);
        buttonEnableLHR = findViewById(R.id.enable_lhr);
        buttonDisableLHR = findViewById(R.id.disable_lhr);
        buttonAlarmSoundImmediateStop = findViewById(R.id.duration_halt_immediate_button);
        buttonAlarmSoundTimedStop = findViewById(R.id.fixed_duration_halt_button);
        buttonAlarmSoundNonstop = findViewById(R.id.indefinite_duration_halt_button);
        loadSharedPreferenceData();
    }

    private void loadSharedPreferenceData() {
        Log.d(TAG, "loadSharedPreferenceData()");
        //HHR Buttons
        if (app.state.isHhrEnabled()) {
            ((RadioButton) buttonEnableHHR).setChecked(true);
            ((RadioButton) buttonDisableHHR).setChecked(false);
        } else {
            ((RadioButton) buttonEnableHHR).setChecked(false);
            ((RadioButton) buttonDisableHHR).setChecked(true);
        }
        //HHR Setting
        editTextHHR.setText(Integer.toString(app.state.getHhrSetting()));
        //LHR Buttons
        if (app.state.isLhrEnabled()) {
            ((RadioButton) buttonEnableLHR).setChecked(true);
            ((RadioButton) buttonDisableLHR).setChecked(false);
        } else {
            ((RadioButton) buttonEnableLHR).setChecked(false);
            ((RadioButton) buttonDisableLHR).setChecked(true);
        }
        //LHR Setting
        editTextLHR.setText(Integer.toString(app.state.getLhrSetting()));
        //Alarm Sound Setting
        switch (app.state.getAlarmSoundSetting()) {
            case IMMEDIATE_STOP:
                ((RadioButton) buttonAlarmSoundImmediateStop).setChecked(true);
                ((RadioButton) buttonAlarmSoundTimedStop).setChecked(false);
                ((RadioButton) buttonAlarmSoundNonstop).setChecked(false);
                break;
            case TIMED_STOP:
                ((RadioButton) buttonAlarmSoundImmediateStop).setChecked(false);
                ((RadioButton) buttonAlarmSoundTimedStop).setChecked(true);
                ((RadioButton) buttonAlarmSoundNonstop).setChecked(false);
                break;
            case NONSTOP:
                ((RadioButton) buttonAlarmSoundImmediateStop).setChecked(false);
                ((RadioButton) buttonAlarmSoundTimedStop).setChecked(false);
                ((RadioButton) buttonAlarmSoundNonstop).setChecked(true);
                break;
            default:
                break;
        }
    }

    public void onClickActivateAlarm(View view) {
        Log.d(TAG, "onClickActivateAlarm()");
        if (((App) this.getApplication()).state.isDeviceIDDefined()) {
            Utility.checkBluetooth(this);
            if (!((App) this.getApplication()).state.isHhrEnabled() && !((App) this.getApplication()).state.isLhrEnabled()) {
                Toast.makeText(this, "Error: neither upper nor lower heart rate alarms set to on!", Toast.LENGTH_LONG).show();
                return;
            }
            Intent serviceUpdateIntent = new Intent(this, MonitorService.class);
            serviceUpdateIntent.putExtra(App.HHR_ENABLED, ((App) this.getApplication()).state.isHhrEnabled());
            serviceUpdateIntent.putExtra(App.HHR_SETTING, ((App) this.getApplication()).state.getHhrSetting());
            serviceUpdateIntent.putExtra(App.LHR_ENABLED, ((App) this.getApplication()).state.isLhrEnabled());
            serviceUpdateIntent.putExtra(App.LHR_SETTING, ((App) this.getApplication()).state.getLhrSetting());
            serviceUpdateIntent.putExtra(App.ALARM_SOUND_SETTING, ((App) this.getApplication()).state.getAlarmSoundSetting());
            serviceUpdateIntent.putExtra(App.ALARM_ON, true);
            startService(serviceUpdateIntent);
        } else {
            Toast.makeText(this, "Error: no device ID to connect with!", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickDeactivateAlarm(View view) {
        Log.d(TAG, "onClickDeactivateAlarm()");
        Intent serviceUpdateIntent = new Intent(this, MonitorService.class);
        serviceUpdateIntent.putExtra(App.HHR_ENABLED, ((App) this.getApplication()).state.isHhrEnabled());
        serviceUpdateIntent.putExtra(App.HHR_SETTING, ((App) this.getApplication()).state.getHhrSetting());
        serviceUpdateIntent.putExtra(App.LHR_ENABLED, ((App) this.getApplication()).state.isLhrEnabled());
        serviceUpdateIntent.putExtra(App.LHR_SETTING, ((App) this.getApplication()).state.getLhrSetting());
        serviceUpdateIntent.putExtra(App.ALARM_SOUND_SETTING, ((App) this.getApplication()).state.getAlarmSoundSetting());
        serviceUpdateIntent.putExtra(App.ALARM_ON, false);
        startService(serviceUpdateIntent);
    }

    public void onClickHHR(View view) {
        Log.d(TAG, "onClickHHR");
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.enable_hhr:
                if (checked) {
                    app.state.setHhrEnabled(true);
                    Toast.makeText(this, "High Heart Rate Alarm Enabled!", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.disable_hhr:
                if (checked) {
                    app.state.setHhrEnabled(false);
                    Toast.makeText(this, "High Heart Rate Alarm Disabled!", Toast.LENGTH_LONG).show();
                }
            default:
                break;
        }
    }

    public void onClickLHR(View view) {
        Log.d(TAG, "onClickLHR");
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.enable_lhr:
                if (checked) {
                    app.state.setLhrEnabled(true);
                    Toast.makeText(this, "Low Heart Rate Alarm Enabled!", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.disable_lhr:
                if (checked) {
                    app.state.setLhrEnabled(false);
                    Toast.makeText(this, "Low Heart Rate Alarm Disabled!", Toast.LENGTH_LONG).show();
                }
            default:
                break;
        }
    }

    public void onClickAlarmDuration(View view) {
        Log.d(TAG, "onClickAlarmDuration()");
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.duration_halt_immediate_button:
                if (checked) {
                    try {
                        app.state.setAlarmSoundSetting(IMMEDIATE_STOP);
                        Toast.makeText(this, "Setting Alarm Sound to Immediate Stop.", Toast.LENGTH_LONG).show();
                    } catch (App.State.InvalidAlarmSettingException e) {
                        Toast.makeText(this, "Invalid Argument Passed!", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.fixed_duration_halt_button:
                if (checked) {
                    try {
                        app.state.setAlarmSoundSetting(TIMED_STOP);
                        Toast.makeText(this, "Setting Alarm Sound to Timed Stop.", Toast.LENGTH_LONG).show();
                    } catch (App.State.InvalidAlarmSettingException e) {
                        Toast.makeText(this, "Invalid Argument Passed!", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.indefinite_duration_halt_button:
                if (checked) {
                    try {
                        app.state.setAlarmSoundSetting(NONSTOP);
                        Toast.makeText(this, "Setting Alarm Sound to Nonstop.", Toast.LENGTH_LONG).show();
                    } catch (App.State.InvalidAlarmSettingException e) {
                        Toast.makeText(this, "Invalid Argument Passed!", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            default:
                break;
        }
    }
}