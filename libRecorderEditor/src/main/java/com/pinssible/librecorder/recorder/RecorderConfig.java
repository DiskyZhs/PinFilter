package com.pinssible.librecorder.recorder;

import android.opengl.EGLContext;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.pinssible.librecorder.listener.OnMuxFinishListener;

import java.io.File;

/**
 * Created by ZhangHaoSong on 2017/9/26.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RecorderConfig {
    public enum SCREEN_ROTATION {LANDSCAPE, VERTICAL}

    public VideoEncoderConfig videoEncoderConfig;
    public AudioEncoderConfig audioEncoderConfig;
    public File mOutputFile;
    public Muxer muxer;
    public SCREEN_ROTATION screenRotation = SCREEN_ROTATION.VERTICAL;

    public RecorderConfig(VideoEncoderConfig videoEncoderConfig, AudioEncoderConfig audioEncoderConfig, File outputFile) {
        this.videoEncoderConfig = videoEncoderConfig;
        this.audioEncoderConfig = audioEncoderConfig;
        this.mOutputFile = outputFile;
        this.muxer = AndroidMuxer.create(outputFile.toString(), Muxer.FORMAT.MPEG4);
    }

    public RecorderConfig(VideoEncoderConfig videoEncoderConfig, AudioEncoderConfig audioEncoderConfig, File outputFile, SCREEN_ROTATION screenRotation) {
        this.videoEncoderConfig = videoEncoderConfig;
        this.audioEncoderConfig = audioEncoderConfig;
        this.mOutputFile = outputFile;
        this.muxer = AndroidMuxer.create(outputFile.toString(), Muxer.FORMAT.MPEG4);
        this.screenRotation = screenRotation;
    }

    public RecorderConfig(VideoEncoderConfig videoEncoderConfig, AudioEncoderConfig audioEncoderConfig, File outputFile, SCREEN_ROTATION screenRotation, OnMuxFinishListener muxFinishListener) {
        this.videoEncoderConfig = videoEncoderConfig;
        this.audioEncoderConfig = audioEncoderConfig;
        this.mOutputFile = outputFile;
        this.muxer = AndroidMuxer.create(outputFile.toString(), Muxer.FORMAT.MPEG4);
        this.muxer.setMuxFinishListener(muxFinishListener);
        this.screenRotation = screenRotation;
    }

    public void setMuxFinishListener(OnMuxFinishListener muxFinishListener) {
        if (muxer != null) {
            muxer.setMuxFinishListener(muxFinishListener);
        }
    }

    public static class VideoEncoderConfig {
        public int mWidth;
        public int mHeight;
        public int mBitRate;
        public EGLContext mEglContext;

        public VideoEncoderConfig(int width, int height, int bitRate,
                                  EGLContext sharedEglContext) {
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "VideoEncoderConfig{" +
                    "mWidth=" + mWidth +
                    ", mHeight=" + mHeight +
                    ", mBitRate=" + mBitRate +
                    ", mEglContext=" + mEglContext +
                    '}';
        }
    }

    public static class AudioEncoderConfig {
        public int numChannels;
        public int bitRate;
        public int sampleRate;

        public AudioEncoderConfig(int numChannels, int bitRate, int sampleRate) {
            this.numChannels = numChannels;
            this.bitRate = bitRate;
            this.sampleRate = sampleRate;
        }

        @Override
        public String toString() {
            return "AudioEncoderConfig{" +
                    "numChannels=" + numChannels +
                    ", bitRate=" + bitRate +
                    ", sampleRate=" + sampleRate +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "RecorderConfig{" +
                "videoEncoderConfig=" + videoEncoderConfig +
                ", audioEncoderConfig=" + audioEncoderConfig +
                ", mOutputFile=" + mOutputFile +
                ", muxer=" + muxer +
                '}';
    }
}
