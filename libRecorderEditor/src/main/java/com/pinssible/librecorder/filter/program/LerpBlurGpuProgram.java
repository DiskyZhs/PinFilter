package com.pinssible.librecorder.filter.program;

import android.opengl.GLES20;
import android.util.Log;

import com.pinssible.librecorder.base.GlUtil;
import com.pinssible.librecorder.base.Viewport;

import java.nio.FloatBuffer;

/**
 * Created by ZhangHaoSong on 2017/10/11.
 */

public class LerpBlurGpuProgram extends GpuImageProgram {
    private static final String TAG = "LerpBlurGpuProgram";


    /*************************************************************************** Program ***********************************************************************************************************************************************************************/
    //origin program  used for all origin.
    protected static final String vshUpScale = "" +
            "attribute vec2 vPosition;\n" +
            "varying vec2 texCoord;\n" +
            "void main()\n" +
            "{\n" +
            "   gl_Position = vec4(vPosition, 0.0, 1.0);\n" +
            "   texCoord = vPosition / 2.0 + 0.5;\n" +
            "}";

    //origin fragment program
    private static final String fshUpScale = "" +
            "precision mediump float;\n" +
            "varying vec2 texCoord;\n" +
            "uniform sampler2D inputImageTexture;\n" +

            "void main()\n" +
            "{\n" +
            "   gl_FragColor = texture2D(inputImageTexture, texCoord);\n" +
            "}";


    /*************************************************************************** Init ***********************************************************************************************************************************************************************/


    private int[] mTextureDownScale;

    //Scale Program
    private int mScaleProgramId;
    private final int mLevel = 30;
    private int maPositionLoc = 0;

    //frame
    private int mFramebufferID;

    //Intensity
    private int mIntensity = 10;

    //intensity >= 0 level 越大，模糊程度越高
    public void setIntensity(int intensity) {
        if(intensity == mIntensity)
            return;

        mIntensity = intensity;
        if(mIntensity > mLevel)
            mIntensity = mLevel;
    }

    public LerpBlurGpuProgram(ProgramType programType, Viewport previewViewport) {
        super(programType, previewViewport);
        initLocal();
        Log.e(TAG,"LerpBlurGpuProgram");
    }

    private void initLocal() {
        //texture
        genMipmaps(mLevel, 512, 512);

        //frameBuffer
        int[] buf = new int[1];
        GLES20.glGenFramebuffers(1, buf, 0);
        mFramebufferID = buf[0];

        //scale program
        mScaleProgramId = GlUtil.createProgram(vshUpScale, fshUpScale);
        //get vPosition in scale program
        GLES20.glBindAttribLocation(mScaleProgramId, maPositionLoc, POSITION_NAME);
    }

    @Override
    public void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex, int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
        if (mIntensity == 0 || mFramebufferID < 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            super.draw(mvpMatrix, vertexBuffer, firstVertex, vertexCount, coordsPerVertex, vertexStride, texMatrix, texBuffer, textureId, texStride);
            return;
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        frameBufferBindTexture(mFramebufferID, mTextureDownScale[0]);

        //重新设置第一层viewport
        mTexViewport.width = calcMips(512, 1);
        mTexViewport.height = calcMips(512, 1);
        GLES20.glViewport(mTexViewport.x, mTexViewport.y, mTexViewport.width, mTexViewport.height);

        //绘制原图像
        super.draw(mvpMatrix, vertexBuffer, firstVertex, vertexCount, coordsPerVertex, vertexStride, texMatrix, texBuffer, textureId, texStride);

        //切换Program
        GLES20.glUseProgram(mScaleProgramId);

        //绘制缩小模糊texture
        for (int i = 1; i < mIntensity; ++i) {
            frameBufferBindTexture(mFramebufferID, mTextureDownScale[i]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDownScale[i - 1]);
            GLES20.glViewport(0, 0, calcMips(512, i + 1), calcMips(512, i + 1));
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        for (int i = mIntensity - 1; i > 0; --i) {
            frameBufferBindTexture(mFramebufferID, mTextureDownScale[i - 1]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDownScale[i]);
            GLES20.glViewport(0, 0, calcMips(512, i), calcMips(512, i));
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        //调整显示区域（重新设置为全屏，预设播放区域）
        GLES20.glViewport(previewViewport.x, previewViewport.y, previewViewport.width, previewViewport.height);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDownScale[0]);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }


    @Override
    public void release() {
        super.release();

        GLES20.glDeleteProgram(mScaleProgramId);
        mScaleProgramId = -1;

        GLES20.glDeleteFramebuffers(1, new int[]{mFramebufferID}, 0);

        try {
            GLES20.glDeleteTextures(mTextureDownScale.length, mTextureDownScale, 0);
        } catch (Exception e) {
        }
    }

    /**
     * 生成多个Texture
     */
    private void genMipmaps(int level, int width, int height) {
        mTextureDownScale = new int[level];
        GLES20.glGenTextures(level, mTextureDownScale, 0);

        for (int i = 0; i < level; ++i) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDownScale[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, calcMips(width, i + 1), calcMips(height, i + 1), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);//指定纹理图像的宽度，必须是2的n次方
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR); // glTexParameteri 定如何把纹理象素映射成像素.
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        mTexViewport = new Viewport(0, 0, 512, 512);
    }

    //计算纹理的宽度，高度，根据设置的level来模糊纹理
    private int calcMips(int len, int level) {
        return len / (level + 1);
    }

    private void frameBufferBindTexture(int frameBufferId, int texID) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texID, 0); //glFramebufferTexture2D把一幅纹理图像关联到一个FBO
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("LerpBlurGpuProgram", "CGE::FrameBuffer::bindTexture2D - Frame buffer is not valid!");
        }
    }
}
