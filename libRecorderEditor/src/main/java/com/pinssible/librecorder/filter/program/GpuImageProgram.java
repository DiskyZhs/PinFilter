package com.pinssible.librecorder.filter.program;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;

import com.pinssible.librecorder.base.GlUtil;
import com.pinssible.librecorder.base.Viewport;

import java.nio.FloatBuffer;

/**
 * Created by ZhangHaoSong on 2017/10/10.
 */

public class GpuImageProgram extends Program {
    private static final String TAG = "GpuImageProgram";


    /*************************************************************************** Program ***********************************************************************************************************************************************************************/
    //origin program  used for all origin.
    protected static final String vshDrawDefault = "" +
            "attribute vec2 vPosition;\n" +  // attribute 是将要传入的参数  只读定点数据，只在这里使用，可以是浮点类型的标量向量或者矩阵
            "varying vec2 texCoord;\n" +  //varying 是传递给fragmentshader的参数，顶点着色器输出，
            "uniform mat4 transform;\n" +  //uniform 一致变量，执行期间值不会改变，和fragmentshader共享，和matrix相关联   mat4 是个4*4的矩阵
            "uniform mat2 rotation;\n" + //
            "uniform vec2 flipScale;\n" +
            "void main()\n" +
            "{\n" +
            "   gl_Position = vec4(vPosition, 0.0, 1.0);\n" +   // glPosition 是一个特殊的变量，用来存储最终的位置，所有的定点着色器必须写这个值
            "   vec2 coord = flipScale * (vPosition / 2.0 * rotation) + 0.5;\n" +
            "   texCoord = (transform * vec4(coord, 0.0, 1.0)).xy;\n" + //将这个值重新赋值
            "}";

    //origin fragment program
    protected static final String fshDrawOrigin =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 texCoord;\n" +
                    "uniform samplerExternalOES inputImageTexture;\n" +
                    "void main()\n" +
                    "{\n" +
                    "   gl_FragColor = texture2D(inputImageTexture, texCoord);\n" +
                    "}";


    /*************************************************************************** Init ***********************************************************************************************************************************************************************/
    private ProgramType mProgramType;

    //text viewport
    protected Viewport mTexViewport;
    protected int mTexWidth;
    protected int mTexHeight;

    private int mTextureTarget;
    private int mProgramHandle; //programId

    //program 变量
    protected int mRotationLoc, mFlipScaleLoc, mTransformLoc, maPositionLoc = 0;
    protected static final String POSITION_NAME = "vPosition";
    protected static final String ROTATION_NAME = "rotation";
    protected static final String FLIPSCALE_NAME = "flipScale";
    protected static final String TRANSFORM_NAME = "transform";

    public GpuImageProgram(ProgramType programType,Viewport previewViewport) {
        Log.e(TAG,"GpuImageProgram");
        init(programType,previewViewport);
    }

    @Override
    protected void init(ProgramType programType,Viewport previewViewport) {
        //Viewport
        this.previewViewport = previewViewport;
        //调整显示区域（重新设置为全屏，预设播放区域）
        GLES20.glViewport(previewViewport.x, previewViewport.y, previewViewport.width, previewViewport.height);

        mProgramType = programType;

        //initProgram
        mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        mProgramHandle = GlUtil.createProgram(vshDrawDefault, fshDrawOrigin);

        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        Log.d(TAG, "Created program " + mProgramHandle + " (" + programType + ")");

        //绑定变量
        GLES20.glBindAttribLocation(mProgramHandle, maPositionLoc, POSITION_NAME);

        mRotationLoc = GLES20.glGetUniformLocation(mProgramHandle, ROTATION_NAME);
        GlUtil.checkLocation(mRotationLoc, ROTATION_NAME);

        mFlipScaleLoc = GLES20.glGetUniformLocation(mProgramHandle, FLIPSCALE_NAME);
        GlUtil.checkLocation(mFlipScaleLoc, FLIPSCALE_NAME);

        mTransformLoc = GLES20.glGetUniformLocation(mProgramHandle, TRANSFORM_NAME);
        GlUtil.checkLocation(mTransformLoc, TRANSFORM_NAME);

        //初始化变化
        setRotation(0.0f);
        setFlipscale(1.0f, 1.0f);
        setTransform(new float[]{
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
    }

    @Override
    public void release() {
        Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    @Override
    public ProgramType getProgramType() {
        return mProgramType;
    }

    @Override
    public int createTextureObject() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GlUtil.checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
        GlUtil.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GlUtil.checkGlError("glTexParameter");

        return texId;
    }


    @Override
    public void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                     int vertexCount, int coordsPerVertex, int vertexStride,
                     float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
        GlUtil.checkGlError("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        GlUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //glActiveTexture选择哪一个纹理在后面的纹理状态改变时有效，
        GLES20.glBindTexture(mTextureTarget, textureId); //再次绑定OES纹理

        // Enable the "vPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "vPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GlUtil.checkGlError("glVertexAttribPointer");

        // Draw the rect.（绘制矩形）（目前就绘制4个点，一个矩形，所有画面）
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);  //绘制，运行Shader,GlProgram
        GlUtil.checkGlError("glDrawArrays");

//        // Done -- disable vertex array, texture, and program.
//        GLES20.glDisableVertexAttribArray(maPositionLoc);
//        GLES20.glBindTexture(mTextureTarget, 0);
//        GLES20.glUseProgram(0);
    }


    @Override
    public void setTexSize(int width, int height) {
        mTexHeight = height;
        mTexWidth = width;
        mTexViewport = new Viewport(0, 0, mTexWidth, mTexHeight);
    }

    @Override
    public void handleTouchEvent(MotionEvent ev) {
        //todo
    }

    //设置界面旋转弧度 -- 录像时一般是 PI / 2 (也就是 90°) 的整数倍
    public void setRotation(float rad) {
        Log.e(TAG, "setRotation");
        final float cosRad = (float) Math.cos(rad);
        final float sinRad = (float) Math.sin(rad);

        float rot[] = new float[]{
                cosRad, sinRad,
                -sinRad, cosRad
        };

        GLES20.glUseProgram(mProgramHandle);
        GLES20.glUniformMatrix2fv(mRotationLoc, 1, false, rot, 0);
    }

    public void setFlipscale(float x, float y) {
        GLES20.glUseProgram(mProgramHandle);
        GLES20.glUniform2f(mFlipScaleLoc, x, y);
    }

    public void setTransform(float[] matrix) {
        GLES20.glUseProgram(mProgramHandle);
        GLES20.glUniformMatrix4fv(mTransformLoc, 1, false, matrix, 0);
    }
}
