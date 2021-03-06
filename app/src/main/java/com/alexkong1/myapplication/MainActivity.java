package com.alexkong1.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements RecordTask.TaskDone {

    private TextView startText;
    private TextView stopText;
    private TextView saveText;

    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 23945;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 23946;

    //private final RecordTask recordingTask = new RecordTask(this);
    private RecordAsync recordingTask = new RecordAsync(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPostCreate(@android.support.annotation.Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        startText = findViewById(R.id.record);
        startText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.RECORD_AUDIO)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        checkWritePermsissions();
                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

                        // MY_PERMISSIONS_REQUEST_RECORD_AUDIO is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        recordAudio();
                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }
                } else {
                    recordAudio();
                }
            }
        });

        stopText = findViewById(R.id.stop);
        stopText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordingTask.stopRecording();
            }
        });

        saveText = findViewById(R.id.save);
        saveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    saveText.setText("CONVERTING...");
                    saveText.setClickable(false);
                    File convertedWav = Utils.rawToWave();
                    if (convertedWav != null) saveText.setText("CONVERSION SUCCESSFUL");
                } catch (IOException e) {
                    saveText.setText("CONVERSION ERROR");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkWritePermsissions();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recordAudio();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    private void checkWritePermsissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                recordAudio();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void recordAudio() {
        startText.setText("RECORDING...");
        startText.setClickable(false);
        stopText.setVisibility(View.VISIBLE);
        saveText.setVisibility(View.GONE);
        if (recordingTask.getStatus() == AsyncTask.Status.FINISHED) recordingTask = new RecordAsync(this);
        recordingTask.execute();
    }

    @Override
    public void finishTask(boolean isSuccess) {
        finishTask(isSuccess, "");
    }

    @Override
    public void finishTask(boolean isSuccess, String path) {
        startText.setText(isSuccess ? "FINISHED...CLICK TO RESTART \n Saved At " + path : "ERROR...TRY AGAIN?");
        startText.setClickable(true);
        stopText.setVisibility(View.GONE);
        if (isSuccess) saveText.setVisibility(View.VISIBLE);
    }

    @Override
    public void taskRunning(final double timeInMillis) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
                if (stopText.getVisibility() != View.VISIBLE) stopText.setVisibility(View.VISIBLE);
                startText.setText("RECORDING..." + timeInMillis);
            }
        });

    }
}
