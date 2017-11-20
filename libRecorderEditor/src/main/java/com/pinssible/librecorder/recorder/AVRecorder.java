package com.pinssible.librecorder.recorder;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.pinssible.librecorder.view.GLTextureView;

import java.io.File;
import java.io.IOException;

/**
 * Created by ZhangHaoSong on 2017/9/26.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AVRecorder {
    private String LOG_TAG = "AvRecorder";
    //config
    private RecorderConfig recorderConfig;
    private String outputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/NeonRecordTest.mp4";

    //Camera
    private CameraSurfaceRender cameraRender;

    //Audio
    private MicrophoneEncoder microphoneEncoder;

    //state
    public boolean isRecording = false;

    //display rotation
    private int displayRotation = 0;

    private View previewView;

    public AVRecorder(PreviewConfig previewConfig, RecorderConfig config, GLSurfaceView cameraRecordGLSurfaceView2) throws Exception {
        this.recorderConfig = config;
        this.previewView = cameraRecordGLSurfaceView2;
        displayRotation = getDisplayRotation(cameraRecordGLSurfaceView2.getContext());
        this.cameraRender = new CameraSurfaceRender(cameraRecordGLSurfaceView2.getContext(), cameraRecordGLSurfaceView2, previewConfig, displayRotation);
        cameraRecordGLSurfaceView2.setRenderer(cameraRender);
        this.microphoneEncoder = new MicrophoneEncoder(config);
        Log.e(LOG_TAG, "displayRotation = " + displayRotation);
        //size
        if (previewConfig.getPreviewSize().getWidth() != config.videoEncoderConfig.mWidth || previewConfig.getPreviewSize().getHeight() != config.videoEncoderConfig.mHeight) {
            throw new Exception("preview Size should equal record size");
        }
    }


    public AVRecorder(PreviewConfig previewConfig, RecorderConfig config, GLTextureView glTextureView) throws Exception {
        this.recorderConfig = config;
        this.previewView = glTextureView;
        displayRotation = getDisplayRotation(glTextureView.getContext());
        this.cameraRender = new CameraSurfaceRender(glTextureView.getContext(), glTextureView, previewConfig, displayRotation);
        glTextureView.setRenderer(cameraRender);
        this.microphoneEncoder = new MicrophoneEncoder(config);
        Log.e(LOG_TAG, "displayRotation = " + displayRotation);
        //size
        if (previewConfig.getPreviewSize().getWidth() != config.videoEncoderConfig.mWidth || previewConfig.getPreviewSize().getHeight() != config.videoEncoderConfig.mHeight) {
            throw new Exception("preview Size should equal record size");
        }
    }

    /**
     * Prepare for a subsequent recording. Must be called after {@link #stopRecording()}(用于录制第二段视频之前)
     * and before {@link #release()}
     *
     * @param config
     */
    public void reset(RecorderConfig config) throws IOException {
        cameraRender.setRecorderConfig(config);
        microphoneEncoder.reset(config);
        recorderConfig = config;
        isRecording = false;
    }

    public void release() {
        cameraRender.release();
        // MicrophoneEncoder releases all it's resources when stopRecording is called
        // because it doesn't have any meaningful state
        // between recordings. It might someday if we decide to present
        // persistent audio volume meters etc.
        // Until then, we don't need to write MicrophoneEncoder.release()
        if (isRecording) {
            recorderConfig.muxer.forceStop();
        }
        recorderConfig.muxer.release();
    }

    public void startRecording() {
        //create file
        if (!recorderConfig.mOutputFile.exists()) {
            try {
                recorderConfig.mOutputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        isRecording = true;
        cameraRender.startRecording(recorderConfig);
        microphoneEncoder.startRecording();
    }

    public void stopRecording() {
        isRecording = false;
        cameraRender.stopRecording();
        microphoneEncoder.stopRecording();
    }

    /**
     * 截屏
     */
    public void takeShot(CameraSurfaceRender.TakePictureCallback callback) {
        cameraRender.takeShot(callback);
    }

    /**
     * 获取缩略图
     *
     * @param callback
     * @param destWidth
     * @param destHeight
     */
    public void getThumbnail(CameraSurfaceRender.TakePictureCallback callback, int destWidth, int destHeight) {
        cameraRender.takeShot(callback, destWidth, destHeight);
    }

    /**
     * 设置滤镜（部分内置滤镜）
     */
    public void setFilter(int filterType) {
        cameraRender.applyFilter(filterType);
    }

    /**
     * 切换摄像头
     *
     * @param isCameraBackForward
     */
    public void changeCameraFacing(boolean isCameraBackForward) {
        cameraRender.changeCameraFacing(isCameraBackForward);
    }

    public boolean isBackgroundCamera() {
        return cameraRender.getCameraFacing() == Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    public void openFlash() {
        cameraRender.setFlashLightMode(Camera.Parameters.FLASH_MODE_TORCH);
    }

    public void closeFlash() {
        cameraRender.setFlashLightMode(Camera.Parameters.FLASH_MODE_OFF);
    }

    public boolean isFlashOpen() {
        if (!TextUtils.isEmpty(cameraRender.getFlashMode())) {
            return !(cameraRender.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF));
        } else {
            return true;
        }
    }

    /**
     * 开启模糊
     *
     * @param isBlur
     * @param blurIntensity
     */
    public void setBlurMode(boolean isBlur, int blurIntensity) {
        cameraRender.changeBlurMode(isBlur, blurIntensity);
    }

    /**
     * 设置模糊程度，越大越模糊
     *
     * @param blurIntensity 0~30
     */
    public void setBlurIntensity(int blurIntensity) {
        cameraRender.setBlurIntensity(blurIntensity);
    }

    /**
     * 聚焦
     */
    public void focusAtPoint(float x, float y, final Camera.AutoFocusCallback callback) {
        cameraRender.focusAtPoint(x, y, callback);
    }

    public synchronized void autoFocus() {
        cameraRender.autoFocus();
    }

    public void stopPreview() {
        cameraRender.stopPreview();
    }

    public void resumePreview() {

    }

    private int getDisplayRotation(Context context) {
        if (context instanceof Activity) {
            return ((Activity) context).getWindowManager().getDefaultDisplay().getRotation();
        }
        return 0;
    }

    private RecorderConfig createDefaultRecordConfig() {
        //File
        File mOutputFile = new File(outputPath);
        if (!mOutputFile.exists()) {
            try {
                mOutputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "create New file fail");
            }
        }

        //码率计算
        //宽*高*帧率*像素数据量，如果隔行扫描再减半，得到的就是比特率，单位bps
        //720*480*29.97*24=248583168bps=248583.168Kbps≈248.6Mbps
        RecorderConfig.VideoEncoderConfig videoConfig = new RecorderConfig.VideoEncoderConfig(480, 640,
                5 * 1000 * 1000, EGL14.eglGetCurrentContext());
        RecorderConfig.AudioEncoderConfig audioConfig = new RecorderConfig.AudioEncoderConfig(1, 96 * 1000, 44100);
        RecorderConfig recorderConfig = new RecorderConfig(videoConfig, audioConfig,
                mOutputFile, RecorderConfig.SCREEN_ROTATION.VERTICAL);
        return recorderConfig;
    }


}
