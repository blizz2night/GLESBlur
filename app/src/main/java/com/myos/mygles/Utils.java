package com.myos.mygles;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

class Utils {
    private static final String TAG = "Utils";

    public static void checkGLError() {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "GLES error: " + error);
            throw new RuntimeException("GLES error: " + error);
        }
    }

    public static void checkFramebufferStatus() {
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            String msg = "";
            switch (status) {
                case GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
                    break;
                case GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
                    break;
                case GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
                    break;
                case GLES20.GL_FRAMEBUFFER_UNSUPPORTED:
                    msg = "GL_FRAMEBUFFER_UNSUPPORTED";
                    break;
            }
            throw new RuntimeException(msg + ":" + Integer.toHexString(status));
        }
    }


    public static int linkProgram(int vertexShaderHandle, int fragmentShaderHandle) {
        int programHandle = GLES20.glCreateProgram();
        if (programHandle != 0) {
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            GLES20.glLinkProgram(programHandle);
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, /* offset= */ 0);
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }
        return programHandle;
    }

    public static int compileShader(int glVertexShader, String shaderCode) {
        int handler = GLES20.glCreateShader(glVertexShader);
        if (handler != 0) {
            GLES20.glShaderSource(handler, shaderCode);
            GLES20.glCompileShader(handler);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(
                    handler, GLES20.GL_COMPILE_STATUS, compileStatus, /* offset= */ 0);
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(handler);
                handler = 0;
            }
        }
        return handler;
    }

    public static String readStringFromResRaw(Context context, int rawId){
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rawId)))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void setFullScreen(AppCompatActivity activity) {
        final Window window = activity.getWindow();
        int viewFlags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        int winFlags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            winFlags |= WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        }
        WindowManager.LayoutParams lp = window.getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }
        window.getDecorView().setSystemUiVisibility(viewFlags);
        window.addFlags(winFlags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }


    public static void initFrameBuffer(int frameBuffer, int texture) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);
        checkFramebufferStatus();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static void initTexture(int target, int width, int height, int... textures) {
        for (int texture : textures) {
            GLES20.glBindTexture(target, texture);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER,  GLES20.GL_LINEAR);
            checkGLError();
        }
        GLES20.glBindTexture(target, 0);
    }

    public static final float SQRT_2PI = (float) Math.sqrt(Math.PI * 2);
    // 1/(2*PI*sigma^2) * Math.exp((x^2+y^2)/(-2 * sigma^2))
    public static float[] createKernel(float sigma, int size){
        if (size % 2 == 0) {
            size = size+1;
        }
        int radius = size / 2;
        float pow_sigma = sigma * sigma;
        final float[] floats = new float[radius + 1];
        float sum = 0;
        for (int i = 0; i < floats.length; i++) {
            floats[i] = (float) Math.exp(-(i*i)/(2*pow_sigma));
//            if (floats[i] < 0.02f) {
//                floats[i] = 0;
//            }
            sum += floats[i];
        }
        Log.i(TAG, "createKernel: " + Arrays.toString(floats));
        sum = sum * 2 - floats[0];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = floats[i] / sum;
        }
        Log.i(TAG, "createKernel: "+ Arrays.toString(floats));
        return floats;
    }

    public static void createKernelValue(float sigma, int size) {

        double scale = - 1 / (2 * sigma * sigma);
        double cons = 1 / (2 * Math.PI * sigma * sigma);

        int center = size / 2;
        float[] mKernelValue = new float[size];
        double sum = 0;
        for (int i = 0; i < size; i++) {
            int x = i - center;
            mKernelValue[i] = (float) (cons * Math.exp(scale * x * x));
            sum += mKernelValue[i];
        }

        // The sum of the weighted averages is 1,
        // so each item needs to be divided by sum.
        for (int i = 0; i < size; i++) {
            mKernelValue[i] /= sum;
        }
        Log.i(TAG, "createKernelValue: " + Arrays.toString(mKernelValue));
    }
}
