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
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private GLSurfaceView mPreview;
    private int mTexture;
    private static final float[] VERTEX_DATA = {
            // Vertex X, Y
            // Texture coordinates U, V
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f,
    };
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
    private int mVertexShaderHandler;
    private int mFragShaderHandler;
    private int mProgramHandler;
    private int mInputImageTextureHandler;
    private int mPositionHandler;
    private int mInputTextureCoordinateHandler;
    private Bitmap mImage;
    private int mFrameBufferHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vertexShaderCode = readStringFromResRaw(this, R.raw.vertex_shader);
        Log.i(TAG, "onCreate: "+vertexShaderCode);
        fragShaderCode = readStringFromResRaw(this, R.raw.frag_shader);
        verticesBuffer = ByteBuffer.allocateDirect(VERTEX_COUNT * STRIDE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        verticesBuffer.position(0);
        verticesBuffer.put(VERTEX_DATA).position(0);
        mImage = BitmapFactory.decodeResource(getResources(), R.drawable.sc);

        mPreview = findViewById(R.id.preview);
        mPreview.setEGLContextClientVersion(2);
        mPreview.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                int[] temp = new int[1];
                GLES20.glGenTextures(1, temp, 0);
                checkGLError();
                mTexture = temp[0];
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0, mImage,0);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,  GLES20.GL_LINEAR);
                checkGLError();

                GLES20.glGenFramebuffers(1, temp, 0);
                mFrameBufferHandler = temp[0];

                mVertexShaderHandler = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
                Log.i(TAG, "onSurfaceCreated: " + mVertexShaderHandler);
                mFragShaderHandler = compileShader(GLES20.GL_FRAGMENT_SHADER, fragShaderCode);
                Log.i(TAG, "onSurfaceCreated: " + mFragShaderHandler);
                mProgramHandler = linkProgram(mVertexShaderHandler, mFragShaderHandler);
                Log.i(TAG, "onSurfaceCreated: " + mProgramHandler);
                mPositionHandler = GLES20.glGetAttribLocation(mProgramHandler, "a_Position");
                mInputTextureCoordinateHandler = GLES20.glGetAttribLocation(mProgramHandler, "a_InputTextureCoordinate");
                mInputImageTextureHandler = GLES20.glGetUniformLocation(mProgramHandler, "u_InputImageTexture");
                checkGLError();

            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                GLES20.glViewport(0, 0, width, height);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(mProgramHandler);
                verticesBuffer.position(POSITION_OFFSET);
                GLES20.glVertexAttribPointer(
                        mPositionHandler,
                        POSITION_COUNT,
                        GLES20.GL_FLOAT,
                        false,
                        //x0y0s0t0 x1y1s1t1
                        STRIDE_BYTES,
                        verticesBuffer);
                GLES20.glEnableVertexAttribArray(mPositionHandler);
                // Pass in the texture coordinates.
                verticesBuffer.position(TEXTURE_COORDINATE_COUNT);
                GLES20.glVertexAttribPointer(
                        mInputTextureCoordinateHandler,
                        TEXTURE_COORDINATE_COUNT,
                        GLES20.GL_FLOAT,
                        false,
                        STRIDE_BYTES,
                        verticesBuffer);
                GLES20.glEnableVertexAttribArray(mInputTextureCoordinateHandler);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
                GLES20.glUniform1i(mInputImageTextureHandler, /* x= */ 0);


                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* offset= */ VERTEX_COUNT);
            }
        });
        mPreview.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
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
