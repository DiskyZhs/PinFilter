package com.pinssible.librecorder.filter.program;

import android.view.MotionEvent;

import com.pinssible.librecorder.base.Viewport;

import java.nio.FloatBuffer;

/**
 * Created by ZhangHaoSong on 2017/10/10.
 */

public abstract class Program {

    public enum ProgramType {
        //Texture2dProgram ProgramType
        TEXTURE_2D, TEXTURE_EXT, TEXTURE_EXT_BW, TEXTURE_EXT_NIGHT, TEXTURE_EXT_CHROMA_KEY,
        TEXTURE_EXT_SQUEEZE, TEXTURE_EXT_TWIRL, TEXTURE_EXT_TUNNEL, TEXTURE_EXT_BULGE,
        TEXTURE_EXT_DENT, TEXTURE_EXT_FISHEYE, TEXTURE_EXT_STRETCH, TEXTURE_EXT_MIRROR,
        TEXTURE_EXT_FILT,
        //GpuImageProgram ProgramType
        GPU_IMAGE_ORIGIN,
        GPU_IMAGE_LERP_BLUR
    }

    /**
     * Preview viewport
     */
    protected Viewport previewViewport;


    protected abstract void init(ProgramType programType, Viewport previewViewport);


    /**
     * Releases the program.
     */
    public abstract void release();

    /**
     * Returns the program type.
     */
    public abstract ProgramType getProgramType();

    /**
     * Creates a texture object suitable for use with this program.（主要用于存储CameraPreview纹理）
     * <p>
     * On exit, the texture will be bound.
     */
    public abstract int createTextureObject();


    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix       The 4x4 projection matrix.
     * @param vertexBuffer    Buffer with vertex position data.
     * @param firstVertex     Index of first vertex to use in vertexBuffer.
     * @param vertexCount     Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride    Width, in bytes, of the position data for each vertex (often
     *                        vertexCount * sizeof(float)).
     * @param texMatrix       A 4x4 transformation matrix for texture coords.
     * @param texBuffer       Buffer with vertex texture data.
     * @param texStride       Width, in bytes, of the texture data for each vertex.
     */
    public abstract void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                              int vertexCount, int coordsPerVertex, int vertexStride,
                              float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride);

    /**
     * Sets the size of the texture.  This is used to find adjacent texels when filtering.
     */
    public abstract void setTexSize(int width, int height);


    /**
     * Configures the effect offset
     * <p>
     * This only has an effect for programs that
     * use positional effects like SQUEEZE and MIRROR
     */
    public abstract void handleTouchEvent(MotionEvent ev);

    /**
     * Create a Default Program for show
     */
    public static Program getDefaultProgram(Viewport viewport) {
        return new Texture2dProgram(ProgramType.TEXTURE_EXT, viewport);
        //return  new GpuImageProgram(ProgramType.GPU_IMAGE_ORIGIN,viewport);
        //return new LerpBlurGpuProgram(ProgramType.GPU_IMAGE_LERP_BLUR, viewport);
    }

    /**
     * Create a Default Program for show
     */
    public static Program.ProgramType getDefaultProgramType() {
        return ProgramType.TEXTURE_EXT;
    }
}
