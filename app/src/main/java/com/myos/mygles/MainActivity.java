package com.myos.mygles;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.myos.mygles.Utils.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private GLSurfaceView mPreview;
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
    public static final float[] WEIGHT_DEFAULT = new float[]{1f};
    public static final int SIZE_DEFAULT = 1;
    public static final float[] OFFSET_DEFAULT_H = new float[]{1f, 0f};
    public static final float[] OFFSET_DEFAULT_V = new float[]{0f, 1f};
    private static final int BYTES_PER_FLOAT = 4;
    private static final int VERTEX_COUNT = 4;
    private static final int POSITION_OFFSET = 0;
    private static final int POSITION_COUNT = 2;
    private static final int TEXTURE_COORDINATE_COUNT = 2;
    private static final int TEXTURE_COORDINATE_OFFSET = 2;
    private static final int STRIDE_BYTES =
            (TEXTURE_COORDINATE_COUNT + TEXTURE_COORDINATE_OFFSET) * BYTES_PER_FLOAT;
    private float[] mOffsetH;
    private float[] mOffsetV;
    private float[] mWeight;

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
    private int mOffsetHandle;
    private int mWeightHandle;
    private int mVertical;
    private int mHeightHandle;
    private int mWidthHandle;
    private int mHGaussFrameBuffer;
    private int mHGaussFrameBufferTex;
    private int mDrawWidth;
    private int mDrawHeight;
    private AppCompatSeekBar mBlurTimesSeekBar;
    private int mOriginTex;
    private int mBlurTimes = 0;
    private int mSizeHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen(this);
        setContentView(R.layout.activity_main);

        BitmapFactory.Options opt = new BitmapFactory.Options();
        try (InputStream is = getResources().openRawResource(R.raw.sc)) {
            mImage = BitmapFactory.decodeStream(is, null, opt);
        } catch (IOException ex) {
            Log.e(TAG, "onCreate: ", ex);
            return;
        }
        Log.i(TAG, "onCreate: "+opt.outWidth+"x"+opt.outHeight);
        final float[] kernel = createKernel(1f, 13);
//        mOffsetH = ByteBuffer.allocateDirect(kernel.length * 2 * BYTES_PER_FLOAT).asFloatBuffer();
//        mOffsetH.position(0);
//        mOffsetV = ByteBuffer.allocateDirect(kernel.length * 2 * BYTES_PER_FLOAT).asFloatBuffer();
//        mOffsetV.position(0);
//        for (int i = 2; i < kernel.length; i+=2) {
//            mOffsetH.put((float)i / mImage.getWidth());
//            mOffsetH.put(0f);
//            mOffsetV.put(0f);
//            mOffsetV.put((float)i / mImage.getHeight());
//        }
//        Log.i(TAG, "onCreate: "+mOffsetV.toString());
//        mWeight = ByteBuffer.allocateDirect(kernel.length * BYTES_PER_FLOAT).asFloatBuffer();
//        mWeight.position(0);
//        mWeight.put(kernel);
//        mWeight = Arrays.copyOf(kernel,256);
        mWeight = kernel;
        mOffsetV = new float[kernel.length * 2];
        mOffsetH = new float[kernel.length * 2];
        for (int i = 2; i < kernel.length * 2; i += 2) {
            mOffsetH[i] = i / (float) mImage.getWidth();
            mOffsetH[i+1] = 0f;
            mOffsetV[i] = 0f;
            mOffsetV[i+1] = i / (float) mImage.getHeight();
        }
        Log.i(TAG, "onCreate: "+ Arrays.toString(mOffsetH));
        Log.i(TAG, "onCreate: "+ Arrays.toString(mOffsetV));

        vertexShaderCode = readStringFromResRaw(this, R.raw.vertex_shader);
        fragShaderCode = readStringFromResRaw(this, R.raw.gaussian_frag_shader);
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
                mVGaussFrameBufferTex = texs[0];
                mHGaussFrameBufferTex = texs[1];
                mOriginTex = texs[2];
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
                mOffsetHandle = GLES20.glGetUniformLocation(mProgram, "u_Offset");
                mWeightHandle = GLES20.glGetUniformLocation(mProgram, "u_Weight");
                mVertical = GLES20.glGetUniformLocation(mProgram, "u_Vertical");
                mInputTexture = GLES20.glGetUniformLocation(mProgram, "u_InputTexture");
                mSizeHandle = GLES20.glGetUniformLocation(mProgram, "u_Size");
                checkGLError();

            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                Log.i(TAG, "onSurfaceChanged: "+width+"x"+height);
                mWidth = width;
                mHeight = height;
                final float wRatio = mWidth / (float)mImage.getWidth();
                final float hRatio = mHeight / (float)mImage.getHeight();
                float ratio = Math.min(wRatio, hRatio);
                mDrawWidth = (int) (mImage.getWidth()* ratio);
                mDrawHeight = (int) (mImage.getHeight() * ratio);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                Log.i(TAG, "onDrawFrame: ");
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

//                GLES20.glUniform1fv(mOffsetHandle, 5, OFFSET, 0);
//                GLES20.glUniform1fv(mWeightHandle, 5, WEIGHT, 0);
//                GLES20.glUniform1f(mWidthHandle, mImage.getWidth());
//                GLES20.glUniform1f(mHeightHandle, mImage.getHeight());
                GLES20.glUniform1fv(mWeightHandle, mWeight.length, mWeight, 0);
                Log.i(TAG, "onDrawFrame: "+Arrays.toString(mWeight));
                GLES20.glUniform1i(mSizeHandle, mWeight.length);

                for (int i = 0; i < mBlurTimes; i++) {
                    GLES20.glUniform1f(mVertical, 0f);
                    GLES20.glUniform2fv(mOffsetHandle, mOffsetV.length / 2, mOffsetV, 0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, i == 0 ? mOriginTex : mHGaussFrameBufferTex);
                    GLES20.glUniform1i(mInputTexture, /* x= */ 0);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mVGaussFrameBuffer);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* offset= */ VERTEX_COUNT);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

                    GLES20.glUniform1f(mVertical, 1f);
                    GLES20.glUniform2fv(mOffsetHandle, mOffsetH.length / 2, mOffsetH, 0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mVGaussFrameBufferTex);
                    GLES20.glUniform1i(mInputTexture, /* x= */ 0);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mHGaussFrameBuffer);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* offset= */ VERTEX_COUNT);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                }

                GLES20.glUniform1f(mVertical, -1f);
                GLES20.glViewport(((mWidth-mDrawWidth)/2), (mHeight-mDrawHeight)/2, mDrawWidth, mDrawHeight);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBlurTimes > 0 ? mHGaussFrameBuffer : mOriginTex);
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

        mBlurTimesSeekBar = findViewById(R.id.blurTimesSeekBar);
        mBlurTimesSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mBlurTimes = progress;
                    if (mPreview != null) {
                        mPreview.requestRender();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }



}
