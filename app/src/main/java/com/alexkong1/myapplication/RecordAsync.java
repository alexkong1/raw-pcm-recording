package com.alexkong1.myapplication;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class RecordAsync extends AsyncTask<Void, Void, Boolean> {

    static final int SAMPLING_RATE = 22050;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    static final String AUDIO_RECORDING_FILE_NAME = "recording";

    private boolean mStop = false;
    private static final String LOGTAG = "TESTER";

    private RecordTask.TaskDone listener;
    private double startTimeInMillis;
    private String path;

    public RecordAsync(RecordTask.TaskDone listener) {
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        startTimeInMillis = System.currentTimeMillis();

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        Log.e(LOGTAG, "Starting recording…");

        byte audioData[] = new byte[BUFFER_SIZE];
        AudioRecord recorder = new AudioRecord(AUDIO_SOURCE,
                SAMPLING_RATE, CHANNEL_IN_CONFIG,
                AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();

        path = Environment.getExternalStorageDirectory().getPath()
                + "/" + AUDIO_RECORDING_FILE_NAME + ".pcm";
        BufferedOutputStream os = null;

        try {
            os = new BufferedOutputStream(new FileOutputStream(path));
        } catch (FileNotFoundException e) {
            Log.e(LOGTAG, "File not found for recording ", e);
        }

        while (!mStop) {

            int status = recorder.read(audioData, 0, audioData.length);

            listener.taskRunning(System.currentTimeMillis() - startTimeInMillis);

            if (status == AudioRecord.ERROR_INVALID_OPERATION ||
                    status == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(LOGTAG, "Error reading audio data!");
                return false;
            }

            try {
                os.write(audioData, 0, audioData.length);
            } catch (IOException e) {
                Log.e(LOGTAG, "Error saving recording ", e);
                return false;
            }
        }

        try {
            os.close();

            recorder.stop();
            recorder.release();

            Log.v(LOGTAG, "Recording done…");
            mStop = false;
            return true;

        } catch (IOException e) {
            Log.e(LOGTAG, "Error when releasing", e);
            return false;
        }
    }

    public void stopRecording() {
        mStop = true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        listener.finishTask(aBoolean, path);
    }
}
