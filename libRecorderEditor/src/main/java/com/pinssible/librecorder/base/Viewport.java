package com.pinssible.librecorder.base;

/**
 * Created by ZhangHaoSong on 2017/10/17.
 */

public class Viewport {

    public int x, y;
    public int width, height;

    public Viewport() {
    }

    public Viewport(int _x, int _y, int _width, int _height) {
        x = _x;
        y = _y;
        width = _width;
        height = _height;
    }

    @Override
    public String toString() {
        return "Viewport{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
