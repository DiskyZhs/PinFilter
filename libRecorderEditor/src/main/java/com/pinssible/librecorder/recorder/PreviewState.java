package com.pinssible.librecorder.recorder;

import com.pinssible.librecorder.filter.Filters;

/**
 * Created by ZhangHaoSong on 2017/10/30.
 */

public class PreviewState {
    public enum Facing {BACKGROUND, FRONT}

    /**
     * flash
     */
    private boolean isFlashOpen = false;
    /**
     * face
     */
    private Facing facing = Facing.BACKGROUND;
    /**
     * {@see Filters}
     */
    private int filter = Filters.FILTER_NONE;
    /**
     * 模糊
     */
    private boolean isBlur = false;
    /**
     * 模糊程度，0~30
     */
    private int blurIntensity = 20;

    public PreviewState() {
    }

    public boolean isFlashOpen() {
        return isFlashOpen;
    }

    public PreviewState setFlashOpen(boolean flashOpen) {
        isFlashOpen = flashOpen;
        return this;
    }

    public Facing getFacing() {
        return facing;
    }

    public PreviewState setFacing(Facing facing) {
        this.facing = facing;
        return this;
    }

    public int getFilter() {
        return filter;
    }

    public PreviewState setFilter(int filter) {
        this.filter = filter;
        return this;
    }

    public boolean isBlur() {
        return isBlur;
    }

    public PreviewState setBlur(boolean blur) {
        isBlur = blur;
        return this;
    }

    public int getBlurIntensity() {
        return blurIntensity;
    }

    public PreviewState setBlurIntensity(int blurIntensity) {
        this.blurIntensity = blurIntensity;
        return this;
    }
}
