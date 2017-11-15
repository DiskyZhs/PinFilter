/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pinssible.librecorder.remux;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.pinssible.librecorder.base.FullFrameRect;
import com.pinssible.librecorder.base.Viewport;
import com.pinssible.librecorder.filter.Filters;
import com.pinssible.librecorder.filter.program.LerpBlurGpuProgram;
import com.pinssible.librecorder.filter.program.Program;


/**
 * Holds state associated with a Surface used for MediaCodec decoder output.
 * <p>
 * The (width,height) constructor for this class will prepare GL, create a SurfaceTexture,
 * and then create a Surface for that SurfaceTexture.  The Surface can be passed to
 * MediaCodec.configure() to receive decoder output.  When a frame arrives, we latch the
 * texture with updateTexImage, then render the texture with GL to a pbuffer.
 * <p>
 * The no-arg constructor skips the GL preparation step and doesn't allocate a pbuffer.
 * Instead, it just creates the Surface and SurfaceTexture, and when a frame arrives
 * we just draw it on whatever surface is current.
 * <p>
 * By default, the Surface will be using a BufferQueue in asynchronous mode, so we
 * can potentially drop frames.
 */
class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "OutputSurface";
    private static final boolean VERBOSE = false;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
    private boolean mFrameAvailable;

    private FullFrameRect mFullScreenRect;
    private Viewport mPreviewViewport = new Viewport();
    protected int mTextureID;
    private float[] _transformMatrix = new float[16];

    // Keep track of selected filters + relevant state
    private boolean mIncomingSizeUpdated;
    private int mCurrentFilter;
    private int mNewFilter;
    private int mPrevFilter;

    //blur mode
    private int blurIntensity;
    private boolean blurIntensityChanged = false;
    private boolean isBlur = false;

    /**
     * Creates an OutputSurface backed by a pbuffer with the specifed dimensions.  The new
     * EGL context and surface will be made current.  Creates a Surface that can be passed
     * to MediaCodec.configure().
     */
    public OutputSurface(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }

        //tip;不需要启动新的egl环境
//        eglSetup(width, height);
//        makeCurrent();

        setup(width, height);
    }

    public OutputSurface(int width, int height, int filter) {
        this(width, height);
        applyFilter(filter);
    }

    /**
     * Creates instances of TextureRender and SurfaceTexture, and a Surface associated
     * with the SurfaceTexture.
     */
    private void setup(int width, int height) {
        //viewport //todo size 调整（SurfaceSizeCHanged以及VideoSizeChanged）
        mPreviewViewport.width = width;
        mPreviewViewport.height = height;
        mPreviewViewport.x = 0;
        mPreviewViewport.y = 0;

        //rect
        mFullScreenRect = new FullFrameRect(
                Program.getDefaultProgram(mPreviewViewport));
        mIncomingSizeUpdated = true;

        // Even if we don't access the SurfaceTexture after the constructor returns, we
        // still need to keep a reference to it.  The Surface doesn't retain a reference
        // at the Java level, so if we don't either then the object can get GCed, which
        // causes the native finalizer to run.
        mTextureID = mFullScreenRect.createTextureObject();
        mSurfaceTexture = new SurfaceTexture(mTextureID);

        // This doesn't work if OutputSurface is created on the thread that CTS started for
        // these test cases.
        //
        // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
        // create a Handler that uses it.  The "frame available" message is delivered
        // there, but since we're not a Looper-based thread we'll never see it.  For
        // this to do anything useful, OutputSurface must be created on a thread without
        // a Looper, so that SurfaceTexture uses the main application Looper instead.
        //
        // Java language note: passing "this" out of a constructor is generally unwise,
        // but we should be able to get away with it here.
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mSurface = new Surface(mSurfaceTexture);

        //filter
        mCurrentFilter = -1;
        mNewFilter = mCurrentFilter;
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
     */
    private void eglSetup(int width, int height) {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
        }

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                attrib_list, 0);
        checkEglError("eglCreateContext");
        if (mEGLContext == null) {
            throw new RuntimeException("null context");
        }

        // Create a pbuffer surface.  By using this for output, we can use glReadPixels
        // to test values in the output.
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);
        checkEglError("eglCreatePbufferSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }

        mSurface.release();
        mFullScreenRect.release(false);

        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        //mSurfaceTexture.release();

        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLSurface = EGL14.EGL_NO_SURFACE;

        mFullScreenRect = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Returns the Surface that we draw onto.
     */
    public Surface getSurface() {
        return mSurface;
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    public void awaitNewImage() {
        final int TIMEOUT_MS = 500;

        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }


        //fit filter
        adjustFilterAndSize();

        //updateTexImage 根据内容流中最近的图像更新SurfaceTexture对应的GL纹理对象
        mSurfaceTexture.updateTexImage();
    }


    private void adjustFilterAndSize() {
        //blur
        if (blurIntensityChanged && isBlur && mFullScreenRect.getProgram() instanceof LerpBlurGpuProgram) {
            ((LerpBlurGpuProgram) mFullScreenRect.getProgram()).setIntensity(blurIntensity);
        }
        //filter
        if (mCurrentFilter != mNewFilter) {
            Log.e(TAG, "change filter = " + mNewFilter);
            mPrevFilter = mCurrentFilter;
            if (mNewFilter == Filters.FILTER_GPU_LERP_BLUR && blurIntensity > 0) {
                Filters.change2BlurMode(mFullScreenRect, mPreviewViewport, blurIntensity);
            } else {
                Filters.updateFilter(mFullScreenRect, mNewFilter, mPreviewViewport);
            }
            mCurrentFilter = mNewFilter;
            mIncomingSizeUpdated = true;
        }


        //change texure
        if (mIncomingSizeUpdated) { //为了防止加载滤镜时候的闪屏 (同时也是为了调整Texture的Size)
            mFullScreenRect.getProgram().setTexSize(mPreviewViewport.width, mPreviewViewport.height); //在部分使用了Kernel的滤镜时候有效
            mIncomingSizeUpdated = false;
            Log.d(TAG, "setTexSize on display Texture");
        }
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    public void drawImage() {
        mSurfaceTexture.getTransformMatrix(_transformMatrix);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);
        mFullScreenRect.drawFrame(mTextureID, _transformMatrix); //纹理变化
        GLES20.glDisable(GLES20.GL_BLEND);
        // mTextureRender.drawFrame(mSurfaceTexture);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        if (VERBOSE) Log.d(TAG, "new frame available");
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }

    /**
     * Checks for EGL errors.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }


    /*************************************************************************  Filter *****************************************************************************************************************/
    /**
     * Apply a filter to the camera input
     *
     * @param filter
     */
    public void applyFilter(int filter) {
        if (filter >= 0 && filter <= Filters.FILTER_GPU_LERP_BLUR) {
            mNewFilter = filter;
            //movieEncoder.changeFilterMode(filter);
            isBlur = (filter == Filters.FILTER_GPU_LERP_BLUR);
            blurIntensityChanged = false;
        }
    }

    /**
     * 切换是否模糊
     *
     * @param isBlur
     * @param intensity
     */
    public void changeBlurMode(boolean isBlur, int intensity) {
        this.blurIntensity = intensity;
        this.isBlur = isBlur;
        if (isBlur) {
            mNewFilter = Filters.FILTER_GPU_LERP_BLUR;
        } else {
            applyFilter(mPrevFilter);
        }
        // movieEncoder.changeBlurMode(isBlur, intensity);
    }

    /**
     * 切换模糊度
     */
    public void setBlurIntensity(int intensity) {
        blurIntensityChanged = true;
        this.blurIntensity = intensity;
        //movieEncoder.setBlurIntensity(intensity);
    }

}
