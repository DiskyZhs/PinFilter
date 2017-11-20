package com.pinssible.camerarecorder.camerarecorderdemo;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.pinssible.librecorder.listener.OnMuxFinishListener;
import com.pinssible.librecorder.recorder.AVRecorder;
import com.pinssible.librecorder.recorder.CameraSurfaceRender;
import com.pinssible.librecorder.recorder.PreviewConfig;
import com.pinssible.librecorder.recorder.RecorderConfig;
import com.pinssible.librecorder.view.GLTextureView;

import java.io.File;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class CameraCaptureActivity extends AppCompatActivity {

    private AVRecorder recorder;

    private RecorderConfig recorderConfig;

    private PreviewConfig previewConfig;

    private boolean hasRecorder = false;

    private int filterType = 1;

    private boolean isBackCamera = true;

    private boolean isBlur = false;

    private GLTextureView preview;

    private Button recordBnt, photoBtn, filterBtn, faceBtn, flashBtn, blurBtn;

    private SeekBar blurIntensityBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //view
        preview = findViewById(R.id.surface_show);
        recordBnt = findViewById(R.id.btn_record);
        photoBtn = findViewById(R.id.btn_pic);
        filterBtn = findViewById(R.id.btn_filter);
        faceBtn = findViewById(R.id.btn_switch_camera);
        flashBtn = findViewById(R.id.btn_flash);
        blurBtn = findViewById(R.id.btn_blur);
        blurIntensityBar = findViewById(R.id.seek_blur_intensity);

        //create config
        recorderConfig = createRecorderConfig();
        previewConfig = createPreviewConfig();
        Log.e("Test", "recorderConfig = " + recorderConfig.toString());

        //create recorder
        try {
            recorder = new AVRecorder(previewConfig, recorderConfig, preview);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(CameraCaptureActivity.this, "Sorry!Create recorder fail!", Toast.LENGTH_SHORT).show();
        }

        //click
        //record
        recordBnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorder.isRecording) {
                    recorder.stopRecording();
                    recordBnt.setText("Start Recording");
                    if (!hasRecorder) {
                        hasRecorder = true;
                    }
                } else {
                    if (hasRecorder) {
                        try {
                            recorderConfig = createRecorderConfig();
                            recorder.reset(recorderConfig);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(CameraCaptureActivity.this, "Sorry!reset recorder fail!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    recorder.startRecording();
                    recordBnt.setText("Stop Recording");
                }
            }
        });

        //take photo
        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recorder.takeShot(new CameraSurfaceRender.TakePictureCallback() {
                    @Override
                    public void takePictureOK(Bitmap bmp) {
                        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Neon";
                        FileUtils.createOrExistsDir(path);
                        String outputPath = path + "/NeonShotTest_" + System.currentTimeMillis() + ".jpg";
                        Observable.just(ImageUtils.save(bmp, outputPath, Bitmap.CompressFormat.JPEG, true))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean aBoolean) throws Exception {
                                        ToastUtils.showShort("Save bmp $it");
                                    }
                                });
                    }
                });
            }
        });

        //filter
        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int type = (filterType++) % 17;
                Log.e("Test", "filterType = " + type);
                recorder.setFilter(type);
                blurIntensityBar.setVisibility(View.GONE);
            }
        });

        //change camera
        faceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isBackCamera = !recorder.isBackgroundCamera();
                recorder.changeCameraFacing(isBackCamera);
            }
        });

        //toggle flash
        flashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorder.isFlashOpen()) {
                    recorder.closeFlash();
                } else {
                    recorder.openFlash();
                }
            }
        });

        //toggle blur
        blurBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isBlur) {
                    recorder.setBlurMode(true, 10);
                    blurIntensityBar.setVisibility(View.VISIBLE);
                } else {
                    recorder.setBlurMode(false, 10);
                    blurIntensityBar.setVisibility(View.GONE);
                }
            }
        });

        //blur Intensity
        blurIntensityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                recorder.setBlurIntensity(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private RecorderConfig createRecorderConfig() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Neon";
        String outputPath = path + "/NeonRecordTest_" + System.currentTimeMillis() + ".mp4";
        FileUtils.createOrExistsDir(path);
        Log.e("Test", "createRecorderConfig dir path = " + FileUtils.createOrExistsDir(path));
        //file
        File mOutputFile = new File(outputPath);

        Log.e("Test", "createRecorderConfig dir path2 = " + FileUtils.createOrExistsFile(outputPath));
        //setting
        RecorderConfig.VideoEncoderConfig videoConfig = new RecorderConfig.VideoEncoderConfig(480, 640,
                5 * 1000 * 1000, EGL14.eglGetCurrentContext());
        RecorderConfig.AudioEncoderConfig audioConfig = new RecorderConfig.AudioEncoderConfig(1, 96 * 1000, 44100);

        OnMuxFinishListener listener = new OnMuxFinishListener() {
            @Override
            public void onMuxFinish() {
                Log.e("TestActivity", "OnMuxFinish");
                CameraCaptureActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CameraCaptureActivity.this, "Saving mp4 finish!😀", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onMuxFail(Exception e) {

            }
        };
        return new RecorderConfig(videoConfig, audioConfig,
                mOutputFile, RecorderConfig.SCREEN_ROTATION.VERTICAL, listener);
    }

    private PreviewConfig createPreviewConfig() {
        return new PreviewConfig(480, 640);
    }


    @Override
    protected void onDestroy() {
        recorder.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        recorder.resumePreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        recorder.stopPreview();
    }
}
