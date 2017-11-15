package com.pinssible.librecorder.player;

import android.content.Context;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

/**
 * Created by ZhangHaoSong on 2017/10/20.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class PinMediaPlayer {
    private String TAG = "PinMediaPlayer";

    private Uri sourceUri; //暂定 本地播放 本地播放地址
    private Context context;
    private SimpleExoPlayer exoPlayer;
    private GLSurfaceView preview;
    private PlayerRender render;

    public PinMediaPlayer(Context context, Uri sourcePath) {
        this(context, sourcePath, null);
    }

    public PinMediaPlayer(Context context, Uri sourcePath,boolean isLoop) {
        this(context, sourcePath, null,true);
    }

    public PinMediaPlayer(Context context, Uri sourcePath, GLSurfaceView preview,boolean isLoop){
        this.sourceUri = sourcePath;
        this.context = context;
        this.preview = preview;
        //create exoPlayer
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        //create MediaSource
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.getApplicationInfo().name), bandwidthMeter);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new ExtractorMediaSource(sourceUri,
                dataSourceFactory, extractorsFactory, null, null);
        //create render
        if (preview != null) {
            render = new PlayerRender(context, preview, exoPlayer);
            preview.setRenderer(render);
        }
        if(isLoop) {
            // Prepare the player with the source.
            LoopingMediaSource loopMediaSource = new LoopingMediaSource(videoSource);
            exoPlayer.prepare(loopMediaSource);
        }else{
            exoPlayer.prepare(videoSource);
        }
    }

    public PinMediaPlayer(Context context, Uri sourcePath, GLSurfaceView preview) {
        this(context, sourcePath, preview,false);
    }

    public void setPreview(GLSurfaceView preview) {
        if (exoPlayer != null) {
            this.preview = preview;
            render = new PlayerRender(context, preview, exoPlayer);
            preview.setRenderer(render);
        }
    }

    public void start() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);
        }
    }


    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
    }

    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
        }
    }

    public void resume() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);
        }
    }

    public void seekTo(long positionMs) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(positionMs);
        }
    }

    public long getVideoDuration() {
        if (exoPlayer != null) {
            return exoPlayer.getDuration();
        }
        return 0;
    }

    public SimpleExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    public GLSurfaceView getPreview() {
        return preview;
    }

    /**
     * 设置滤镜（部分内置滤镜）
     */
    public void setFilter(int filterType) {
        render.applyFilter(filterType);
    }
}
