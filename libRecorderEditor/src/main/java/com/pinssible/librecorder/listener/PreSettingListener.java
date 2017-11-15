package com.pinssible.librecorder.listener;

import com.pinssible.librecorder.camera.CameraInstance;
import com.pinssible.librecorder.recorder.CameraSurfaceRender;

/**
 * Created by ZhangHaoSong on 2017/9/27.
 */

public interface PreSettingListener {
    /**
     * 在CameraOpen之前的预加载设置
     */
    void preSetting(CameraSurfaceRender render, CameraInstance camera);
}
