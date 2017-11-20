package com.pinssible.librecorder.player;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.pinssible.librecorder.base.FullFrameRect;
import com.pinssible.librecorder.base.Viewport;
import com.pinssible.librecorder.filter.Filters;
import com.pinssible.librecorder.filter.program.LerpBlurGpuProgram;
import com.pinssible.librecorder.filter.program.Program;
import com.pinssible.librecorder.gles.RenderListener;
import com.pinssible.librecorder.view.GLTextureView;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ZhangHaoSong on 2017/9/27.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class PlayerRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, RenderListener {
    public static final String LOG_TAG = "CameraSurfaceRender";
    private Context context;

    //显示
    private FullFrameRect mFullScreenRect;       //Camera显示

    //frameCount
    private int mFrameCount = 0;

    //gl ClearColor
    public class ClearColor {
        public float r, g, b, a;
    }

    public ClearColor clearColor;

    //surfaceTexture 显示
    protected SurfaceTexture mSurfaceTexture;
    protected int mTextureID;

    //preview
    private View mPreview;
    private float[] _transformMatrix = new float[16];
    private Viewport mPreviewViewport = new Viewport();
    public boolean mFitFullView = true; //当RecordSize和PreviewSize不一致时是否进行缩放

    // Keep track of selected filters + relevant state
    private boolean mIncomingSizeUpdated;
    private int mCurrentFilter;
    private int mNewFilter;
    private int mPrevFilter;

    //blur mode
    private int blurIntensity;
    private boolean blurIntensityChanged = false;
    private boolean isBlur = false;

    //player
    private SimpleExoPlayer simpleExoPlayer;


    public PlayerRender(Context context, GLSurfaceView preview, SimpleExoPlayer simpleExoPlayer) {
        this.context = context;

        this.mPreview = preview;
        preview.setEGLContextClientVersion(2);

        clearColor = new ClearColor();
        clearColor.r = 0.0f;
        clearColor.g = 0.0f;
        clearColor.b = 0.0f;
        clearColor.a = 1.0f;

        mCurrentFilter = -1;
        mNewFilter = mCurrentFilter;

        //player
        this.simpleExoPlayer = simpleExoPlayer;
    }

    public PlayerRender(Context context, GLTextureView preview, SimpleExoPlayer simpleExoPlayer) {
        this.context = context;

        this.mPreview = preview;

        clearColor = new ClearColor();
        clearColor.r = 0.0f;
        clearColor.g = 0.0f;
        clearColor.b = 0.0f;
        clearColor.a = 1.0f;

        mCurrentFilter = -1;
        mNewFilter = mCurrentFilter;

        //player
        this.simpleExoPlayer = simpleExoPlayer;
    }


    public PlayerRender(Context context, GLSurfaceView preview, SimpleExoPlayer simpleExoPlayer, boolean previewFitView) {
        this(context, preview, simpleExoPlayer);
        mFitFullView = previewFitView;
    }

    /********************************************************************* Render 回调 *************************************************************************************************************************************/
    @Override
    public void onSurfaceCreated() {
        Log.e(LOG_TAG, "onSurfaceCreated");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.e(LOG_TAG, String.format("onSurfaceChanged: %d x %d", width, height));
        GLES20.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a); //设置Clear的颜色

        //get ViewSize
        if (mSurfaceTexture == null) { //need init
            initWithViewSize(width, height);
        } else {
            //todo resetSize for preview and encoder
        }
    }

    @Override
    public void onDrawFrame() {
        //Log.e(LOG_TAG,"onDrawFrame");
        //blur
        if (blurIntensityChanged && isBlur && mFullScreenRect.getProgram() instanceof LerpBlurGpuProgram) {
            ((LerpBlurGpuProgram) mFullScreenRect.getProgram()).setIntensity(blurIntensity);
        }
        //filter
        if (mCurrentFilter != mNewFilter) {
            Log.e(LOG_TAG, "change filter = " + mNewFilter);
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
            Log.d(LOG_TAG, "setTexSize on display Texture");
        }

        //updateTexImage 根据内容流中最近的图像更新SurfaceTexture对应的GL纹理对象
        mSurfaceTexture.updateTexImage();  //适用于GL_TEXTURE_EXTERNAL_OES texture target，从的图像流中更新纹理图像到最近的帧中，只能在openGl以及GLThread中使用

        //获取显示的Matrix并设置给OpenGL ES
        //Retrieve the 4x4 texture coordinate transform matrix associated with the texture image set by the most recent call to updateTexImage. This transform matrix maps 2D homogeneous texture coordinates of the form (s, t, 0, 1) with s and t in the inclusive range [0, 1] to the texture coordinate that should be used to sample that location from the texture. Sampling the texture outside of the range of this transform is undefined. The matrix is stored in column-major order so that it may be passed directly to OpenGL ES via the glLoadMatrixf or glUniformMatrix4fv function
        mSurfaceTexture.getTransformMatrix(_transformMatrix); //


        //start to draw frame
        //清空ColorBuffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_BLEND);
        mFullScreenRect.drawFrame(mTextureID, _transformMatrix); //纹理变化
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        onDrawFrame();
    }

    /**
     * 获取到预览viewsize后计算previewSize并初始化preview，record
     */
    private void initWithViewSize(int width, int height) {
        //viewport //todo size 调整（SurfaceSizeCHanged以及VideoSizeChanged）
        mPreviewViewport.width = width;
        mPreviewViewport.height = height;
        mPreviewViewport.x = 0;
        mPreviewViewport.y = 0;


        //preview
        mFullScreenRect = new FullFrameRect(
                Program.getDefaultProgram(mPreviewViewport));

        mTextureID = mFullScreenRect.createTextureObject();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        //GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        //player
        Surface surface = new Surface(mSurfaceTexture);  //todo 考虑一下是否使用WindowSurface进行替换
        this.simpleExoPlayer.setVideoSurface(surface);

        //size
        mIncomingSizeUpdated = true;        // Force texture size update on next onDrawFrame
        //encoder
        //movieEncoder = new VideoEncoder(mPreviewViewport);
        int[] textures = new int[1];
        textures[0] = mTextureID;
        // GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE,textures , 0);
    }

    /********************************************************************* SurfaceTexture 回调 *************************************************************************************************************************************/

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    /********************************************************************* Others Setting *************************************************************************************************************************************/

    /**
     * 设置默认ClearColor
     */
    public void setClearColor(float r, float g, float b, float a) {
        clearColor.r = r;
        clearColor.g = g;
        clearColor.b = b;
        clearColor.a = a;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                GLES20.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            }
        });
    }


    /**
     * 搞一张照片
     *
     * @param callback
     */
    public synchronized void takeShot(final TakePictureCallback callback) {
        takeShot(callback, 0, 0);
    }

    /**
     * 搞一张照片 (可以用来设置缩略图)
     *
     * @param callback
     */
    public synchronized void takeShot(final TakePictureCallback callback, final int width, final int height) {
        assert callback != null : "callback must not be null!";

        if (mFullScreenRect.getProgram() == null) {
            Log.e(LOG_TAG, "Recorder not initialized!");
            callback.takePictureOK(null);
            return;
        }

        queueEvent(new Runnable() {
            @Override
            public void run() {
                final int bitmapBuffer[] = new int[mPreviewViewport.width * mPreviewViewport.height];
                final int bitmapSource[] = new int[mPreviewViewport.width * mPreviewViewport.height];

                IntBuffer buffer = IntBuffer.wrap(bitmapBuffer);
                GLES20.glViewport(0, 0, mPreviewViewport.width, mPreviewViewport.height);
                GLES20.glReadPixels(0, 0, mPreviewViewport.width, mPreviewViewport.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

                //异步，防止阻塞
                new Thread() {
                    @Override
                    public void run() {
                        //remember, that OpenGL bitmap is incompatible with Android bitmap
                        //and so, some correction need
                        int offset1, offset2;
                        for (int i = 0; i < mPreviewViewport.height; i++) {
                            offset1 = i * mPreviewViewport.width;
                            offset2 = (mPreviewViewport.height - i - 1) * mPreviewViewport.width;
                            for (int j = 0; j < mPreviewViewport.width; j++) {
                                int texturePixel = bitmapBuffer[offset1 + j];
                                int blue = (texturePixel >> 16) & 0xff;
                                int red = (texturePixel << 16) & 0x00ff0000;
                                int pixel = (texturePixel & 0xff00ff00) | red | blue;
                                bitmapSource[offset2 + j] = pixel;
                            }
                        }

                        Bitmap bmp = Bitmap.createBitmap(bitmapSource, mPreviewViewport.width, mPreviewViewport.height, Bitmap.Config.ARGB_8888);
                        final Bitmap result;

                        //scale
                        if (width > 0 && height > 0) {
                            result = Bitmap.createScaledBitmap(bmp, width, height, false);
                        } else {
                            result = bmp;
                        }

                        //switch to ui thread;
                        if (context instanceof Activity) {
                            ((Activity) context).runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            callback.takePictureOK(result);
                                        }
                                    }
                            );
                        }

                    }
                }.start();
            }
        });
    }

    public interface TakePictureCallback {
        /**
         * You can recycle the bitmap.You can chose other thread do this
         */
        void takePictureOK(Bitmap bmp);
    }

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

    /********************************************************************* Others *************************************************************************************************************************************/

    private void requestRender() {
        if (mPreview instanceof GLSurfaceView) {
            ((GLSurfaceView) mPreview).requestRender();
        } else if (mPreview instanceof GLTextureView) {
            ((GLTextureView) mPreview).requestRender();
        }
    }

    private void queueEvent(Runnable runnable) {
        if (mPreview instanceof GLSurfaceView) {
            ((GLSurfaceView) mPreview).queueEvent(runnable);
        } else if (mPreview instanceof GLTextureView) {
            ((GLTextureView) mPreview).queueEvent(runnable);
        }
    }

    /**
     * Draws a red box in the corner.
     */
    private void drawBox() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(0, 0, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }


    protected void calcViewport() {
        //todo 暂时设置为viewsize（后面再通过获取时机尺寸进行计算）
        int viewWidth = mPreview.getWidth();
        int viewHeight = mPreview.getHeight();

        mPreviewViewport.width = viewWidth;
        mPreviewViewport.height = viewHeight;
        mPreviewViewport.x = (viewWidth - mPreviewViewport.width) / 2;
        mPreviewViewport.y = (viewHeight - mPreviewViewport.height) / 2;
        Log.e(LOG_TAG, String.format("View port: %d, %d, %d, %d", mPreviewViewport.x, mPreviewViewport.y, mPreviewViewport.width, mPreviewViewport.height));
    }

}
