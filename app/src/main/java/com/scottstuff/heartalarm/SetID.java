package com.scottstuff.heartalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class SetID extends AppCompatActivity {
    //Logcat tag
    private static final String TAG = App.APP_TAG + ".SetID";

    private EditText editText;
    private TextView displayIDTextView;
    private App app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_i_d);
        app = (App) this.getApplication();
        displayIDTextView = findViewById(R.id.polarIDSetting);
        if (app.state.isDeviceIDDefined()) {
            displayIDTextView.setText(app.state.getPolarDeviceID());
        } else {
            displayIDTextView.setText("No ID Defined.");
        }
        editText = findViewById(R.id.hhr_alarm_setting);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
    }

    //TODO: still need to implement actual device connection, currently just sets ID
    public void onClickSetIDConnect(View view) {
        Log.d(TAG, "onClickSetIDConnect()");
        //Set ID
        app.state.setPolarDeviceID(editText.getText().toString());
        displayIDTextView.setText(app.state.getPolarDeviceID());
    }
}