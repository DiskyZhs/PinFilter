package com.pinssible.librecorder.recorder;

/**
 * Created by ZhangHaoSong on 2017/10/30.
 */

public class PreviewConfig {
    public class Size {
        int width;
        int height;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    private PreviewState initState;
    private Size previewSize;
    private boolean previewFitView;


    public PreviewConfig(int width, int height){
        this.previewFitView = true;
        //size
        previewSize = new Size(width, height);
        //state
        initState = new PreviewState();
    }

    public PreviewConfig(int width, int height, boolean previewFitView, boolean isBackCamera){
        this.previewFitView = previewFitView;
        //size
        previewSize = new Size(width, height);
        //state
        initState = new PreviewState();
        PreviewState.Facing face = PreviewState.Facing.BACKGROUND;
        if (!isBackCamera) face = PreviewState.Facing.FRONT;
        initState.setFacing(face);
    }

    public PreviewConfig(int width, int height, boolean previewFitView, boolean isBackCamera, int filter, boolean isBlur) {
        this.previewFitView = previewFitView;
        //size
        previewSize = new Size(width, height);
        //state
        initState = new PreviewState();
        PreviewState.Facing face = PreviewState.Facing.BACKGROUND;
        if (!isBackCamera) face = PreviewState.Facing.FRONT;
        initState.setBlur(isBlur)
                .setFacing(face)
                .setFilter(filter);
    }

    public PreviewState getInitState() {
        return initState;
    }

    public void setInitState(PreviewState initState) {
        this.initState = initState;
    }

    public Size getPreviewSize() {
        return previewSize;
    }

    public void setPreviewSize(Size previewSize) {
        this.previewSize = previewSize;
    }

    public boolean isPreviewFitView() {
        return previewFitView;
    }

    public void setPreviewFitView(boolean previewFitView) {
        this.previewFitView = previewFitView;
    }
}
