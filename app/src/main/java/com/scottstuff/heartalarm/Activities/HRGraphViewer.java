package com.scottstuff.heartalarm.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.scottstuff.heartalarm.App.App;
import com.scottstuff.heartalarm.R;

public class HRGraphViewer extends AppCompatActivity {
    private static final String TAG = App.APP_TAG + ".HRGraphViewer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrgraph_viewer);
    }
}