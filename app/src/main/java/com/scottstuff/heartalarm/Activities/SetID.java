package com.scottstuff.heartalarm.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.App.State;
import com.scottstuff.heartalarm.R;

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
        State state = State.getInstance();
        if (state.isDeviceIDDefined()) {
            displayIDTextView.setText(state.getPolarDeviceID());
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
        State state = State.getInstance();
        state.setPolarDeviceID(editText.getText().toString());
        displayIDTextView.setText(state.getPolarDeviceID());
    }
}