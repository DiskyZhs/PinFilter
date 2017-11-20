package com.pinssible.librecorder.recorder;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;

import com.pinssible.librecorder.base.FullFrameRect;
import com.pinssible.librecorder.base.Viewport;
import com.pinssible.librecorder.camera.CameraInstance;
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
public class CameraSurfaceRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, RenderListener {
    public static final String LOG_TAG = "CameraSurfaceRender";
    private Context context;

    //显示
    private FullFrameRect mFullScreenCamera;       //Camera显示

    //Camera
    public CameraInstance cameraInstance;

    //recorder
    VideoEncoder movieEncoder;
    private boolean mRecordingEnabled = false;

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

    //config
    private RecorderConfig recorderConfig;

    // Keep track of selected filters + relevant state
    private boolean mIncomingSizeUpdated;
    private int mCurrentFilter;
    private int mNewFilter;
    private int mPrevFilter;

    //blur mode
    private int blurIntensity;
    private boolean blurIntensityChanged = false;
    private boolean isBlur = false;

    //display rotation
    private int displayRotation = 0;

    //preview
    private PreviewConfig previewConfig;
    private PreviewState previewState;


    public CameraSurfaceRender(Context context, GLSurfaceView preview, PreviewConfig config, int displayRotation) {
        this.context = context;

        this.mPreview = preview;
        preview.setEGLContextClientVersion(2);

        clearColor = new ClearColor();

        this.cameraInstance = CameraInstance.getInstance(context);

        previewConfig = config;

        mCurrentFilter = config.getInitState().getFilter();
        mNewFilter = mCurrentFilter;

        this.displayRotation = displayRotation;
        mFitFullView = config.isPreviewFitView();
    }


    public CameraSurfaceRender(Context context, GLTextureView preview, PreviewConfig config, int displayRotation) {
        this.context = context;

        this.mPreview = preview;

        clearColor = new ClearColor();

        this.cameraInstance = CameraInstance.getInstance(context);

        previewConfig = config;

        mCurrentFilter = config.getInitState().getFilter();
        mNewFilter = mCurrentFilter;

        this.displayRotation = displayRotation;
        mFitFullView = config.isPreviewFitView();
    }


//    public CameraSurfaceRender(Context context, GLSurfaceView preview, RecorderConfig config, int displayRotation) {
//        this.context = context;
//
//        this.mPreview = preview;
//        preview.setEGLContextClientVersion(2);
//
//        clearColor = new ClearColor();
//
//        this.cameraInstance = CameraInstance.getInstance(context);
//
//        recorderConfig = config;
//
//        mCurrentFilter = -1;
//        mNewFilter = mCurrentFilter;
//
//        this.displayRotation = displayRotation;
//    }
//
//    public CameraSurfaceRender(Context context, GLSurfaceView preview, RecorderConfig config, int displayRotation, boolean previewFitView) {
//        this(context, preview, config, displayRotation);
//        mFitFullView = previewFitView;
//    }

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
            initWithViewSize();
        } else {
            //todo resetSize for preview and encoder
        }

        //Camera preview
        if (!cameraInstance.isPreviewing()) {
            cameraInstance.startPreview(mSurfaceTexture); //camera show texture
        }
    }

    @Override
    public void onDrawFrame() {
        //blur
        if (blurIntensityChanged && isBlur && mFullScreenCamera.getProgram() instanceof LerpBlurGpuProgram) {
            ((LerpBlurGpuProgram) mFullScreenCamera.getProgram()).setIntensity(blurIntensity);
        }
        //filter
        if (mCurrentFilter != mNewFilter) {
            Log.e(LOG_TAG, "change filter = " + mNewFilter);
            mPrevFilter = mCurrentFilter;
            if (mNewFilter == Filters.FILTER_GPU_LERP_BLUR && blurIntensity > 0) {
                Filters.change2BlurMode(mFullScreenCamera, mPreviewViewport, blurIntensity);
            } else {
                Filters.updateFilter(mFullScreenCamera, mNewFilter, mPreviewViewport);
            }
            mCurrentFilter = mNewFilter;
            mIncomingSizeUpdated = true;
        }

        //change texure
        if (mIncomingSizeUpdated) { //为了防止加载滤镜时候的闪屏 (同时也是为了调整Texture的Size)
            mFullScreenCamera.getProgram().setTexSize(mPreviewViewport.width, mPreviewViewport.height); //在部分使用了Kernel的滤镜时候有效
            mIncomingSizeUpdated = false;
            Log.d(LOG_TAG, "setTexSize on display Texture");
        }

        //Log.e(LOG_TAG,"Test updateTexImage on Thread = " + Thread.currentThread().getName());
        //updateTexImage 根据内容流中最近的图像更新SurfaceTexture对应的GL纹理对象
        mSurfaceTexture.updateTexImage();  //适用于GL_TEXTURE_EXTERNAL_OES texture target，从的图像流中更新纹理图像到最近的帧中，只能在openGl以及GLThread中使用

        //获取显示的Matrix并设置给OpenGL ES
        //Retrieve the 4x4 texture coordinate transform matrix associated with the texture image set by the most recent call to updateTexImage. This transform matrix maps 2D homogeneous texture coordinates of the form (s, t, 0, 1) with s and t in the inclusive range [0, 1] to the texture coordinate that should be used to sample that location from the texture. Sampling the texture outside of the range of this transform is undefined. The matrix is stored in column-major order so that it may be passed directly to OpenGL ES via the glLoadMatrixf or glUniformMatrix4fv function
        mSurfaceTexture.getTransformMatrix(_transformMatrix); //

        //start to draw frame
        // http://www.cnblogs.com/leven20061001/archive/2012/08/28/2724692.html
        //停止向FBO输出（同时让图像显示）
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        //清空ColorBuffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_BLEND);
        mFullScreenCamera.drawFrame(mTextureID, _transformMatrix); //纹理变化
        GLES20.glDisable(GLES20.GL_BLEND);

        // Draw a flashing box if we're recording.  This only appears on screen.
//        if (mRecordingEnabled && (++mFrameCount & 0x04) == 0) {
//            drawBox();
//        }

        //to record
        movieEncoder.setTextureId(mTextureID);
        movieEncoder.frameAvailable(mSurfaceTexture);
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
    private void initWithViewSize() {
        //Log.e(LOG_TAG,"Test initWithViewSize on Thread = " + Thread.currentThread().getName());
        //viewport
        calcViewport();

        //camera
        cameraInstance.setPreferPreviewSize(mPreviewViewport.width, mPreviewViewport.height);
        openCamera(previewConfig.getInitState().getFacing() == PreviewState.Facing.BACKGROUND);
        mIncomingSizeUpdated = true;        // Force texture size update on next onDrawFrame
        //encoder
        movieEncoder = new VideoEncoder(mPreviewViewport);

        //preview
        mFullScreenCamera = new FullFrameRect(
                Program.getDefaultProgram(mPreviewViewport));
        //init filter
        if (Filters.getProgramType(previewConfig.getInitState().getFilter()) != Program.getDefaultProgramType()) {
            applyFilter(previewConfig.getInitState().getFilter());
        }

        mTextureID = mFullScreenCamera.createTextureObject();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    /********************************************************************* SurfaceTexture 回调 *************************************************************************************************************************************/

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Log.e(LOG_TAG,"Test onFrameAvailable on Thread = " + Thread.currentThread().getName());
        requestRender();
    }

    /********************************************************************* Camera *************************************************************************************************************************************/

    private void openCamera(boolean isCameraBackForward) {
        //打开Camera
        if (!cameraInstance.isCameraOpened()) {
            int facing = isCameraBackForward ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
            if (!cameraInstance.tryOpenCamera(null, facing, displayRotation)) {
                Log.e(LOG_TAG, "camera open failed");
            }
        }
    }

    /**
     * 前后摄像头切换
     */
    public void changeCameraFacing(boolean isCameraBackForward) {
        int facing = isCameraBackForward ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        if (facing != getCameraFacing()) {
            cameraInstance.stopCamera();
            cameraInstance.tryOpenCamera(new CameraInstance.CameraOpenCallback() {
                @Override
                public void cameraReady() {
                    if (!cameraInstance.isPreviewing()) {
                        Log.i(LOG_TAG, "## switch camera -- start preview...");
                        cameraInstance.startPreview(mSurfaceTexture);
                    }
                }
            }, facing, displayRotation);
            requestRender();
        }
    }

    public int getCameraFacing() {
        return cameraInstance.getFacing();
    }

    /**
     * {@link Camera.Parameters}
     */
    public synchronized boolean setFlashLightMode(String mode) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Log.e(LOG_TAG, "No flash light is supported by current device!");
            return false;
        }

        Camera.Parameters parameters = cameraInstance.getParams();
        if (parameters == null)
            return false;

        try {

            if (!parameters.getSupportedFlashModes().contains(mode)) {
                Log.e(LOG_TAG, "Invalid Flash Light Mode!!!");
                return false;
            }

            parameters.setFlashMode(mode);
            cameraInstance.setParams(parameters);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Switch flash light failed, check if you're using front camera.");
            return false;
        }

        return true;
    }

    public synchronized String getFlashMode() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Log.e(LOG_TAG, "No flash light is supported by current device!");
            return "";
        }
        Camera.Parameters parameters = cameraInstance.getParams();
        if (parameters == null)
            return "";
        return parameters.getFlashMode();
    }

    public void stopPreview() {
        if (cameraInstance.isPreviewing())
            cameraInstance.stopPreview();
    }


    public void resumePreview() {
        if (!cameraInstance.isPreviewing() && mSurfaceTexture != null) {
            cameraInstance.startPreview(mSurfaceTexture); //camera show texture
        }
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


    public void startRecording() {
        if (recorderConfig != null) {
            mRecordingEnabled = true;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (movieEncoder != null) {
                        movieEncoder.startRecording(recorderConfig);
                        movieEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    }
                }
            });
        }
    }


    public void startRecording(RecorderConfig config) {
        mRecordingEnabled = true;
        this.recorderConfig = config;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (movieEncoder != null) {
                    movieEncoder.startRecording(recorderConfig);
                    movieEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                }
            }
        });
    }

    public void stopRecording() {
        mRecordingEnabled = false;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (movieEncoder != null)
                    movieEncoder.stopRecording();
            }
        });
    }

    public void setRecorderConfig(RecorderConfig recorderConfig) {
        this.recorderConfig = recorderConfig;
    }

    public void release() {
        movieEncoder.releaseEncoder();
        cameraInstance.stopCamera();
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

        if (mFullScreenCamera.getProgram() == null) {
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
            movieEncoder.changeFilterMode(filter);
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
        movieEncoder.changeBlurMode(isBlur, intensity);
    }

    /**
     * 切换模糊度
     */
    public void setBlurIntensity(int intensity) {
        blurIntensityChanged = true;
        this.blurIntensity = intensity;
        movieEncoder.setBlurIntensity(intensity);
    }

    /**
     * 聚焦
     */
    public void focusAtPoint(float x, float y, final Camera.AutoFocusCallback callback) {
        cameraInstance.focusAtPoint(x, y, callback);
    }

    public synchronized void autoFocus() {
        cameraInstance.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
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
        float scaling = previewConfig.getPreviewSize().getWidth() / (float) previewConfig.getPreviewSize().getHeight();
        int viewWidth = mPreview.getWidth();
        int viewHeight = mPreview.getHeight();

        float viewRatio = viewWidth / (float) viewHeight;
        float s = scaling / viewRatio;

        int w, h;

        if (mFitFullView) {
            //撑满全部view(内容大于view)
            if (s > 1.0) {
                w = (int) (viewHeight * scaling);
                h = viewHeight;
            } else {
                w = viewWidth;
                h = (int) (viewWidth / scaling);
            }
        } else {
            //显示全部内容(内容小于view)
            if (s > 1.0) {
                w = viewWidth;
                h = (int) (viewWidth / scaling);
            } else {
                h = viewHeight;
                w = (int) (viewHeight * scaling);
            }
        }

        mPreviewViewport.width = w;
        mPreviewViewport.height = h;
        mPreviewViewport.x = (viewWidth - mPreviewViewport.width) / 2;
        mPreviewViewport.y = (viewHeight - mPreviewViewport.height) / 2;
        Log.e(LOG_TAG, String.format("View port: %d, %d, %d, %d", mPreviewViewport.x, mPreviewViewport.y, mPreviewViewport.width, mPreviewViewport.height));
    }

}
