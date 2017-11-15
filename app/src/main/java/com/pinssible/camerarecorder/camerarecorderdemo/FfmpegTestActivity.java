package com.pinssible.camerarecorder.camerarecorderdemo;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

public class FfmpegTestActivity extends AppCompatActivity {
    private final String TAG = "FfmpegTestActivity";

    static {
        System.loadLibrary("ffmpeg");
    }

    private TextView testTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_test);
        testTextView = findViewById(R.id.tv_test);
        testTextView.setText(helloTest());

        //Load ffmpeg
        loadFfmpeg();
    }


    public native String helloTest();

    private void loadFfmpeg() {
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.e(TAG, "load ffmpeg onStart");
                }

                @Override
                public void onFailure() {
                    Log.e(TAG, "load ffmpeg onFailure");
                }

                @Override
                public void onSuccess() {
                    Log.e(TAG, "load ffmpeg onSuccess");
                }

                @Override
                public void onFinish() {
                    Log.e(TAG, "load ffmpeg onFinish");
                    queryVideoInfo();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            Log.e(TAG, "load ffmpeg exception = " + e.toString());
        }
    }


    private void queryVideoInfo() {
        String outputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.mp4";
        String[] cmd = {"-version"};
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.e(TAG, "queryVideoInfo onStart");
                }

                @Override
                public void onProgress(String message) {
                    Log.e(TAG, "queryVideoInfo onProgress msg = " + message);
                }

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, "queryVideoInfo onFailure msg = " + message);
                }

                @Override
                public void onSuccess(String message) {
                    Log.e(TAG, "queryVideoInfo onSuccess msg = " + message);
                }

                @Override
                public void onFinish() {
                    Log.e(TAG, "queryVideoInfo onFinish");
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            Log.e(TAG, "queryVideoInfo exception = " + e.toString());
        }
    }
}
