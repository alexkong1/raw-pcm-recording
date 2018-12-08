package com.alexkong1.myapplication;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RecordTask implements Runnable {

    public static final int SAMPLING_RATE = 22050;
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    public static final String AUDIO_RECORDING_FILE_NAME = "recording.pcm";

    boolean mStop = false;
    public static final String LOGTAG = "TESTER";

    public TaskDone listener;

    public RecordTask(TaskDone listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        Log.e(LOGTAG, "Starting recording…");

        byte audioData[] = new byte[BUFFER_SIZE];
        AudioRecord recorder = new AudioRecord(AUDIO_SOURCE,
                SAMPLING_RATE, CHANNEL_IN_CONFIG,
                AUDIO_FORMAT, BUFFER_SIZE);
        //findAudioRecord().startRecording();
        recorder.startRecording();

        String filePath = Environment.getExternalStorageDirectory().getPath()
                + "/" + AUDIO_RECORDING_FILE_NAME;
        BufferedOutputStream os = null;

        try {
            os = new BufferedOutputStream(new FileOutputStream(filePath));
        } catch (FileNotFoundException e) {
            Log.e(LOGTAG, "File not found for recording ", e);
        }

        while (!mStop) {

            int status = recorder.read(audioData, 0, audioData.length);

            listener.taskRunning(recorder.getState());

            if (status == AudioRecord.ERROR_INVALID_OPERATION ||
                    status == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(LOGTAG, "Error reading audio data!");
                listener.finishTask(false);
                return;
            }

            try {
                os.write(audioData, 0, audioData.length);
            } catch (IOException e) {
                Log.e(LOGTAG, "Error saving recording ", e);
                listener.finishTask(false);
                return;
            }
        }

        try {
            os.close();

            recorder.stop();
            recorder.release();

            Log.v(LOGTAG, "Recording done…");
            mStop = false;
            listener.finishTask(true);

        } catch (IOException e) {
            Log.e(LOGTAG, "Error when releasing", e);
            listener.finishTask(false);
        }
    }

    public void stopRecording() {
        mStop = true;
    }

    private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };

    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d(LOGTAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        Log.e(LOGTAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }

    public interface TaskDone {
        void taskRunning(double timeInMillis);
        void finishTask(boolean isSuccess);
        void finishTask(boolean isSuccess, String path);
    }
}
