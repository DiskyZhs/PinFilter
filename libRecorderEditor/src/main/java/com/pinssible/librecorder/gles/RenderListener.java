package com.pinssible.librecorder.gles;

/**
 * Created by ZhangHaoSong on 2017/11/20.
 */

public interface RenderListener {

    void onSurfaceCreated();

    void onSurfaceChanged(int width,int height);

    void onDrawFrame();
}
