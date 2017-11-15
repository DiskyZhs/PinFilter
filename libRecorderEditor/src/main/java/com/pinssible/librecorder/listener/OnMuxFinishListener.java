package com.pinssible.librecorder.listener;

/**
 * not in UI thread ，you should change to ui thread to do something mostTime
 * Created by ZhangHaoSong on 2017/9/27.
 */

public interface OnMuxFinishListener {
    /**
     * not in UI thread ，you should change to ui thread to do something mostTime
     */
    void onMuxFinish();

    void onMuxFail(Exception e);
}
