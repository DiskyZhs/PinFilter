package com.pinssible.librecorder.filter;

import android.util.Log;

import com.pinssible.librecorder.base.FullFrameRect;
import com.pinssible.librecorder.base.Viewport;
import com.pinssible.librecorder.filter.program.GpuImageProgram;
import com.pinssible.librecorder.filter.program.LerpBlurGpuProgram;
import com.pinssible.librecorder.filter.program.Program;
import com.pinssible.librecorder.filter.program.Texture2dProgram;

/**
 * This class matches descriptive final int
 * variables to Texture2dProgram.ProgramType
 *
 */
public class Filters {
    private static final String TAG = "Filters";
    private static final boolean VERBOSE = false;

    // Camera filters; must match up with camera_filter_names in strings.xml
    public static final int FILTER_NONE = 0;
    public static final int FILTER_BLACK_WHITE = 1;
    public static final int FILTER_NIGHT = 2;
    public static final int FILTER_CHROMA_KEY = 3;
    public static final int FILTER_BLUR = 4;
    public static final int FILTER_SHARPEN = 5;
    public static final int FILTER_EDGE_DETECT = 6;
    public static final int FILTER_EMBOSS = 7;
    public static final int FILTER_SQUEEZE = 8;
    public static final int FILTER_TWIRL = 9;
    public static final int FILTER_TUNNEL = 10;
    public static final int FILTER_BULGE = 11;
    public static final int FILTER_DENT = 12;
    public static final int FILTER_FISHEYE = 13;
    public static final int FILTER_STRETCH = 14;
    public static final int FILTER_MIRROR = 15;
    public static final int FILTER_GPU_LERP_BLUR = 16;

    /**
     * Updates the filter on the provided FullFrameRect
     *
     * the int code of the new filter
     */
    public static void updateFilter(FullFrameRect rect, int newFilter, Viewport viewport) {
        Program.ProgramType programType;
        float[] kernel = null;
        float colorAdj = 0.0f;

        if (VERBOSE) Log.d(TAG, "Updating filter to " + newFilter);
        switch (newFilter) {
            case FILTER_NONE:
                programType = Program.ProgramType.TEXTURE_EXT;
                break;
            case FILTER_BLACK_WHITE:
                // (In a previous version the TEXTURE_EXT_BW variant was enabled by a flag called
                // ROSE_COLORED_GLASSES, because the shader set the red channel to the B&W color
                // and green/blue to zero.)
                programType = Program.ProgramType.TEXTURE_EXT_BW;
                break;
            case FILTER_NIGHT:
                programType = Program.ProgramType.TEXTURE_EXT_NIGHT;
                break;
            case FILTER_CHROMA_KEY:
                programType = Program.ProgramType.TEXTURE_EXT_CHROMA_KEY;
                break;
            case FILTER_SQUEEZE:
                programType = Program.ProgramType.TEXTURE_EXT_SQUEEZE;
                break;
            case FILTER_TWIRL:
                programType = Program.ProgramType.TEXTURE_EXT_TWIRL;
                break;
            case FILTER_TUNNEL:
                programType = Program.ProgramType.TEXTURE_EXT_TUNNEL;
                break;
            case FILTER_BULGE:
                programType = Program.ProgramType.TEXTURE_EXT_BULGE;
                break;
            case FILTER_DENT:
                programType = Program.ProgramType.TEXTURE_EXT_DENT;
                break;
            case FILTER_FISHEYE:
                programType = Program.ProgramType.TEXTURE_EXT_FISHEYE;
                break;
            case FILTER_STRETCH:
                programType = Program.ProgramType.TEXTURE_EXT_STRETCH;
                break;
            case FILTER_MIRROR:
                programType = Program.ProgramType.TEXTURE_EXT_MIRROR;
                break;
            case FILTER_BLUR:
                Log.e(TAG, "FILTER_BLUR");
                programType = Program.ProgramType.TEXTURE_EXT_FILT;  //使用卷积
                kernel = new float[]{
                        1f / 16f, 2f / 16f, 1f / 16f,
                        2f / 16f, 4f / 16f, 2f / 16f,
                        1f / 16f, 2f / 16f, 1f / 16f};
                break;
            case FILTER_SHARPEN:
                programType = Program.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[]{
                        0f, -1f, 0f,
                        -1f, 5f, -1f,
                        0f, -1f, 0f};
                break;
            case FILTER_EDGE_DETECT:
                programType = Program.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[]{
                        -1f, -1f, -1f,
                        -1f, 8f, -1f,
                        -1f, -1f, -1f};
                break;
            case FILTER_EMBOSS:
                programType = Program.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[]{
                        2f, 0f, 0f,
                        0f, -1f, 0f,
                        0f, 0f, -1f};
                colorAdj = 0.5f;
                break;
            case FILTER_GPU_LERP_BLUR:
                programType = Program.ProgramType.GPU_IMAGE_LERP_BLUR;
                break;
            default:
                throw new RuntimeException("Unknown filter mode " + newFilter);
        }

        // Do we need a whole new program?  (We want to avoid doing this if we don't have
        // too -- compiling a program could be expensive.)
        if (programType != rect.getProgram().getProgramType()) {
            if (programType == Program.ProgramType.GPU_IMAGE_LERP_BLUR) {
                rect.changeProgram(new LerpBlurGpuProgram(programType, viewport));
            } else if (programType == Program.ProgramType.GPU_IMAGE_ORIGIN) {
                rect.changeProgram(new GpuImageProgram(programType, viewport));
            } else {
                rect.changeProgram(new Texture2dProgram(programType, viewport));
            }
        }

        // Update the filter kernel (if any).
        if (kernel != null && rect.getProgram() instanceof Texture2dProgram) {
            ((Texture2dProgram) rect.getProgram()).setKernel(kernel, colorAdj);
        }
    }


    public static Program.ProgramType getProgramType(int filter) {
        Program.ProgramType programType = Program.ProgramType.TEXTURE_EXT;
        switch (filter) {
            case FILTER_NONE:
                programType = Program.ProgramType.TEXTURE_EXT;
                break;
            case FILTER_BLACK_WHITE:
                programType = Program.ProgramType.TEXTURE_EXT_BW;
                break;
            case FILTER_NIGHT:
                programType = Program.ProgramType.TEXTURE_EXT_NIGHT;
                break;
            case FILTER_CHROMA_KEY:
                programType = Program.ProgramType.TEXTURE_EXT_CHROMA_KEY;
                break;
            case FILTER_SQUEEZE:
                programType = Program.ProgramType.TEXTURE_EXT_SQUEEZE;
                break;
            case FILTER_TWIRL:
                programType = Program.ProgramType.TEXTURE_EXT_TWIRL;
                break;
            case FILTER_TUNNEL:
                programType = Program.ProgramType.TEXTURE_EXT_TUNNEL;
                break;
            case FILTER_BULGE:
                programType = Program.ProgramType.TEXTURE_EXT_BULGE;
                break;
            case FILTER_DENT:
                programType = Program.ProgramType.TEXTURE_EXT_DENT;
                break;
            case FILTER_FISHEYE:
                programType = Program.ProgramType.TEXTURE_EXT_FISHEYE;
                break;
            case FILTER_STRETCH:
                programType = Program.ProgramType.TEXTURE_EXT_STRETCH;
                break;
            case FILTER_MIRROR:
                programType = Program.ProgramType.TEXTURE_EXT_MIRROR;
                break;
            case FILTER_BLUR:
                Log.e(TAG, "FILTER_BLUR");
                programType = Program.ProgramType.TEXTURE_EXT_FILT;  //使用卷积
                break;
            case FILTER_SHARPEN:
                programType = Program.ProgramType.TEXTURE_EXT_FILT;
                break;
            case FILTER_EDGE_DETECT:
                programType = Program.ProgramType.TEXTURE_EXT_FILT;
                break;
            case FILTER_EMBOSS:
                programType = Program.ProgramType.TEXTURE_EXT_FILT;

                break;
            case FILTER_GPU_LERP_BLUR:
                programType = Program.ProgramType.GPU_IMAGE_LERP_BLUR;
                break;
            default:
                programType = Program.ProgramType.TEXTURE_EXT;
        }
        return programType;
    }

    /**
     * 是否进行模糊模式的切换
     *
     * @param rect
     * @param viewport
     * @param intensity 模糊程度
     */
    public static void change2BlurMode(FullFrameRect rect, Viewport viewport, int intensity) {
        LerpBlurGpuProgram program = new LerpBlurGpuProgram(Program.ProgramType.GPU_IMAGE_LERP_BLUR, viewport);
        program.setIntensity(intensity);
        rect.changeProgram(program);
    }
}
