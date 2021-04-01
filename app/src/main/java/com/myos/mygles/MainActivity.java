package com.myos.mygles;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private GLSurfaceView mPreview;
    private int mOriginTex;
    private static final float[] SCREEN_VERTEX_DATA = {
            // Vertex X, Y
            // Texture coordinates U, V
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f,
    };
    private static final float[] VERTEX_DATA = {
            // Vertex X, Y
            // Texture coordinates U, V
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f,
    };

    public static final float[] OFFSET = new float[]{0.0f, 1.0f, 2.0f, 3.0f, 4.0f};
    public static final float[] WEIGHT = new float[]{0.2270270270f, 0.1945945946f, 0.1216216216f, 0.0540540541f, 0.0162162162f};
    private static final int BYTES_PER_FLOAT = 4;
    private static final int VERTEX_COUNT = 4;
    private static final int POSITION_OFFSET = 0;
    private static final int POSITION_COUNT = 2;
    private static final int TEXTURE_COORDINATE_COUNT = 2;
    private static final int TEXTURE_COORDINATE_OFFSET = 2;
    private static final int STRIDE_BYTES =
            (TEXTURE_COORDINATE_COUNT + TEXTURE_COORDINATE_OFFSET) * BYTES_PER_FLOAT;
    private FloatBuffer verticesBuffer;
    private String vertexShaderCode;
    private String fragShaderCode;
    private int mVertexShader;
    private Bitmap mImage;
    private int mVGaussFrameBuffer;
    private int mVGaussFrameBufferTex;
    private int mHeight;
    private int mWidth;
    private FloatBuffer screenVerticesBuffer;
    private int mfragShader;
    private int mProgram;
    private int mPosition;
    private int mTexCoord;
    private int mInputTexture;
    private int mOffset;
    private int mWeight;
    private int mVertical;
    private int mHeightHandle;
    private int mWidthHandle;
    private int mHGaussFrameBuffer;
    private int mHGaussFrameBufferTex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        try (InputStream is = getResources().openRawResource(R.raw.sc)) {
            mImage = BitmapFactory.decodeStream(is, null, opt);
        } catch (IOException ex) {
            Log.e(TAG, "onCreate: ", ex);
            return;
        }
        Log.i(TAG, "onCreate: "+opt.outWidth+"x"+opt.outHeight);

        vertexShaderCode = readStringFromResRaw(this, R.raw.vertex_shader);
        fragShaderCode = readStringFromResRaw(this, R.raw.gauss_frag_shader);
        verticesBuffer = ByteBuffer.allocateDirect(VERTEX_COUNT * STRIDE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        verticesBuffer.position(0);
        verticesBuffer.put(VERTEX_DATA).position(0);

        screenVerticesBuffer = ByteBuffer.allocateDirect(VERTEX_COUNT * STRIDE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        screenVerticesBuffer.position(0);
        screenVerticesBuffer.put(SCREEN_VERTEX_DATA).position(0);





        mPreview = findViewById(R.id.preview);
        mPreview.setEGLContextClientVersion(2);
        mPreview.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                int[] texs = new int[3];
                GLES20.glGenTextures(3, texs, 0);
                checkGLError();
                mOriginTex = texs[0];
                mVGaussFrameBufferTex = texs[1];
                mHGaussFrameBufferTex = texs[2];
                initTexture(GLES20.GL_TEXTURE_2D, mImage.getWidth(), mImage.getHeight(), texs);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOriginTex);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0, mImage,0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

                int[] fb = new int[2];
                GLES20.glGenFramebuffers(2, fb, 0);
                mVGaussFrameBuffer = fb[0];
                mHGaussFrameBuffer = fb[1];
                initFrameBuffer(mVGaussFrameBuffer, mVGaussFrameBufferTex);
                initFrameBuffer(mHGaussFrameBuffer, mHGaussFrameBufferTex);

                mVertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
                Log.i(TAG, "compileShader: " + mVertexShader);
                mfragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragShaderCode);
                Log.i(TAG, "compileShader: " + mfragShader);
                mProgram = linkProgram(mVertexShader, mfragShader);
                Log.i(TAG, "linkProgram: " + mProgram);
                mPosition = GLES20.glGetAttribLocation(mProgram, "a_Position");
                mTexCoord = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");
                mOffset = GLES20.glGetUniformLocation(mProgram, "u_Offset");
                mWeight = GLES20.glGetUniformLocation(mProgram, "u_Weight");
                mVertical = GLES20.glGetUniformLocation(mProgram, "u_Vertical");
                mWidthHandle = GLES20.glGetUniformLocation(mProgram, "u_Width");
                mHeightHandle = GLES20.glGetUniformLocation(mProgram, "u_Height");
                mInputTexture = GLES20.glGetUniformLocation(mProgram, "u_InputTexture");
                checkGLError();

            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                Log.i(TAG, "onSurfaceChanged: "+width+"x"+height);
                mWidth = width;
                mHeight = height;
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                GLES20.glUseProgram(mProgram);
                GLES20.glViewport(0, 0, mImage.getWidth(), mImage.getHeight());
                verticesBuffer.position(POSITION_OFFSET);
                GLES20.glVertexAttribPointer(
                        mPosition,
                        POSITION_COUNT,
                        GLES20.GL_FLOAT,
                        false,
                        //x0y0s0t0 x1y1s1t1
                        STRIDE_BYTES,
                        verticesBuffer);
                GLES20.glEnableVertexAttribArray(mPosition);
                // Pass in the texture coordinates.
                verticesBuffer.position(TEXTURE_COORDINATE_COUNT);
                GLES20.glVertexAttribPointer(
                        mTexCoord,
                        TEXTURE_COORDINATE_COUNT,
                        GLES20.GL_FLOAT,
                        false,
                        STRIDE_BYTES,
                        verticesBuffer);
                GLES20.glEnableVertexAttribArray(mTexCoord);

                GLES20.glUniform1fv(mOffset, 5, OFFSET, 0);
                GLES20.glUniform1fv(mWeight, 5, WEIGHT, 0);
                GLES20.glUniform1f(mVertical, 0f);
                GLES20.glUniform1f(mWidthHandle, mImage.getWidth());
                GLES20.glUniform1f(mHeightHandle, mImage.getHeight());

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOriginTex);
                GLES20.glUniform1i(mInputTexture, /* x= */ 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mVGaussFrameBuffer);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* offset= */ VERTEX_COUNT);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

                GLES20.glUniform1f(mVertical, 1f);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mVGaussFrameBufferTex);
                GLES20.glUniform1i(mInputTexture, /* x= */ 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mHGaussFrameBuffer);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* offset= */ VERTEX_COUNT);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);



                GLES20.glViewport(0, 0, mWidth, mHeight);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mHGaussFrameBuffer);
                GLES20.glUniform1i(mInputTexture, /* x= */ 0);
                screenVerticesBuffer.position(TEXTURE_COORDINATE_COUNT);
                GLES20.glVertexAttribPointer(
                        mTexCoord,
                        TEXTURE_COORDINATE_COUNT,
                        GLES20.GL_FLOAT,
                        false,
                        STRIDE_BYTES,
                        screenVerticesBuffer);
                GLES20.glEnableVertexAttribArray(mTexCoord);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* offset= */ VERTEX_COUNT);
            }
        });
        mPreview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void initFrameBuffer(int frameBuffer, int texture) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mVGaussFrameBufferTex, 0);
        checkFramebufferStatus();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void initTexture(int target, int width, int height, int... textures) {
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

    private void checkGLError() {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "GLES error: " + error);
            throw new RuntimeException("GLES error: " + error);
        }
    }

    private static void checkFramebufferStatus() {
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


    private static int linkProgram(int vertexShaderHandle, int fragmentShaderHandle) {
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

    private static int compileShader(int glVertexShader, String shaderCode) {
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

}
