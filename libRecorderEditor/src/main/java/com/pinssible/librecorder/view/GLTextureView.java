/*
 *
 *  *
 *  *  * Copyright (C) 2016 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.pinssible.librecorder.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.pinssible.librecorder.gles.EglContextWrapper;
import com.pinssible.librecorder.gles.GLThread;
import com.pinssible.librecorder.gles.RenderListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chilling on 2016/10/31.
 * Can be used in ScrollView or ListView.
 * Can make it not opaque by setOpaque(false).
 */
public class GLTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    protected GLThread mGLThread;
    protected GLThread.Builder glThreadBuilder;
    private List<Runnable> cacheEvents = new ArrayList<>();
    private SurfaceTextureListener surfaceTextureListener;
    public GLThread.OnCreateGLContextListener onCreateGLContextListener;

    private boolean hasCreateGLThreadCalledOnce = false;
    private boolean surfaceAvailable = false;
    private RenderListener renderer;
    private volatile boolean created;

    public GLTextureView(Context context) {
        super(context);
        init();
    }

    public GLTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GLTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("BaseGLTextureView", "onSizeChanged: " + w +" "+ h);
        super.onSizeChanged(w, h, oldw, oldh);
        if (mGLThread != null) {
            mGLThread.onWindowResize(w, h);
        }
    }

    public void onPause() {
        if (mGLThread != null) {
            mGLThread.onPause();
        }
    }

    public void onResume() {
        if (mGLThread != null) {
            mGLThread.onResume();
        }
    }

    public void queueEvent(Runnable r) {
        if (mGLThread == null) {
            if (hasCreateGLThreadCalledOnce) {
                createGLThread();
            }
            cacheEvents.add(r);
            return;
        }
        mGLThread.queueEvent(r);
    }

    public void requestRender() {
        if (mGLThread != null) {
            mGLThread.requestRender();
        }
    }

    public void requestRenderAndWait() {
        if (mGLThread != null) {
            mGLThread.requestRenderAndWait();
        }
    }

    protected void surfaceCreated() {
        mGLThread.surfaceCreated();
    }

    protected void surfaceDestroyed() {
        // Surface will be destroyed when we return
        if (mGLThread != null) {
            mGLThread.surfaceDestroyed();
            mGLThread.requestExitAndWait();
        }
        hasCreateGLThreadCalledOnce = false;
        surfaceAvailable = false;
        mGLThread = null;
        created = false;
    }

    protected void surfaceChanged(int w, int h) {
        mGLThread.onWindowResize(w, h);
    }

    protected void surfaceRedrawNeeded() {
        if (mGLThread != null) {
            mGLThread.requestRenderAndWait();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mGLThread != null) {
            mGLThread.requestExitAndWait();
        }
        super.onDetachedFromWindow();
    }

    public void release() {
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mGLThread != null) {
                // GLThread may still be running if this view was never
                // attached to a window.
                mGLThread.requestExitAndWait();
            }
        } finally {
            super.finalize();
        }
    }

    protected void init() {
        super.setSurfaceTextureListener(this);
    }

    /**
     * @return If the context is not created, then EGL10.EGL_NO_CONTEXT will be returned.
     */
    @Nullable
    public EglContextWrapper getCurrentEglContext() {
        return mGLThread == null ? null : mGLThread.getEglContext();
    }

    public void setOnCreateGLContextListener(GLThread.OnCreateGLContextListener onCreateGLContextListener) {
        if (created) {
            this.onCreateGLContextListener = onCreateGLContextListener;
            onCreateGLContextListener.onCreate();
        } else {
            this.onCreateGLContextListener = onCreateGLContextListener;
        }
    }

    public void setSurfaceTextureListener(SurfaceTextureListener surfaceTextureListener) {
        this.surfaceTextureListener = surfaceTextureListener;
    }

    protected int getRenderMode() {
        return GLThread.RENDERMODE_WHEN_DIRTY;
    }

    public void setRenderer(RenderListener renderer) {
        this.renderer = renderer;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        surfaceAvailable = true;
        glThreadBuilder = new GLThread.Builder();
        if (mGLThread == null) {
            glThreadBuilder.setRenderMode(getRenderMode())
                    .setSurface(surface)
                    .setRenderer(renderer);
            if (!hasCreateGLThreadCalledOnce) {
                createGLThread();
            }
        } else {
            mGLThread.setSurface(surface);
            freshSurface(width, height);
        }
        if (surfaceTextureListener != null) {
            surfaceTextureListener.onSurfaceTextureAvailable(surface, width, height);
        }
    }

    protected void createGLThread() {
        hasCreateGLThreadCalledOnce = true;
        if (!surfaceAvailable) {
            return;
        }
        mGLThread = glThreadBuilder.createGLThread();
        mGLThread.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (onCreateGLContextListener != null) {
                            onCreateGLContextListener.onCreate();
                        }
                        created = true;
                    }
                });
            }
        });
        mGLThread.start();
        freshSurface(getWidth(), getHeight());
        for (Runnable cacheEvent : cacheEvents) {
            mGLThread.queueEvent(cacheEvent);
        }
        cacheEvents.clear();
    }

    private void freshSurface(int width, int height) {
        surfaceCreated();
        surfaceChanged(width, height);
        surfaceRedrawNeeded();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d("BaseGLTextureView", "onSurfaceTextureSizeChanged: ");
        surfaceChanged(width, height);
        surfaceRedrawNeeded();
        if (surfaceTextureListener != null) {
            surfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("BaseGLTextureView", "onSurfaceTextureDestroyed: ");
        surfaceDestroyed();
        if (surfaceTextureListener != null) {
            surfaceTextureListener.onSurfaceTextureDestroyed(surface);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (surfaceTextureListener != null) {
            surfaceTextureListener.onSurfaceTextureUpdated(surface);
        }
    }

}
