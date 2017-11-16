package com.pinssible.librecorder.recorder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AndroidMuxer extends Muxer {
    private static final String TAG = "AndroidMuxer";
    private static final boolean VERBOSE = false;

    private MediaMuxer mMuxer;
    private boolean mStarted;

    private AndroidMuxer(String outputFile, FORMAT format) {
        super(outputFile, format);
        Log.e(TAG, "AndroidMuxer create path = " + outputFile);
        try {
            switch (format) {
                case MPEG4:
                    mMuxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized format!");
            }
        } catch (IOException e) {
            throw new RuntimeException("MediaMuxer creation failed", e);
        }
        mStarted = false;
    }

    public static AndroidMuxer create(String outputFile, FORMAT format) {
        return new AndroidMuxer(outputFile, format);
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        super.addTrack(trackFormat);
        Log.e(TAG, "addTrack trackFormat = " + trackFormat.toString());
        if (mStarted)
            throw new RuntimeException("format changed twice");
        int track = mMuxer.addTrack(trackFormat);

        if (allTracksAdded()) {
            start();
        }
        return track;
    }

    protected void start() {
        mMuxer.start();
        mStarted = true;
    }


    @Override
    public void stop() {
        Log.e(TAG, "stop muxer is Started = " + mStarted);
        if (mMuxer != null) {
            mMuxer.stop();
            super.stop();
        }
        mStarted = false;
    }

    @Override
    public void release() {
        super.release();
        mMuxer.release();
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void writeSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        super.writeSampleData(encoder, trackIndex, bufferIndex, encodedData, bufferInfo);
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // MediaMuxer gets the codec config info via the addTrack command
            if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            encoder.releaseOutputBuffer(bufferIndex, false);
            return;
        }

        if (bufferInfo.size == 0) {
            if (VERBOSE) Log.d(TAG, "ignoring zero size buffer");
            encoder.releaseOutputBuffer(bufferIndex, false);
            return;
        }

        if (!mStarted) {
            Log.e(TAG, "writeSampleData called before muxer started. Ignoring packet. Track index: " + trackIndex + " tracks added: " + mNumTracks);
            encoder.releaseOutputBuffer(bufferIndex, false);
            return;
        }

        Log.e(TAG, "writeSampleData presentationTimeUs = " + bufferInfo.presentationTimeUs);

        bufferInfo.presentationTimeUs = getNextRelativePts(bufferInfo.presentationTimeUs, trackIndex);

        mMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);

        encoder.releaseOutputBuffer(bufferIndex, false);

        if (allTracksFinished()) {
            stop();
            release(); //停止释放;
        }
    }

    @Override
    public void forceStop() {
        Log.e(TAG, "forceStop");
        try {
            stop();
            release();
        } catch (Exception e) {
            if (muxFinishListener != null) {
                muxFinishListener.onMuxFail(e);
            }
        }
    }
}
