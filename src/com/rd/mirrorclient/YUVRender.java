package com.rd.mirrorclient;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;


public class YUVRender implements GLSurfaceView.Renderer
{
	private Object lock;
	private final String TAG = "yuv renderer";
	private static final int MIN_TEXTURE_SIZE = 256; 
	private int mTextureWidth, mTextureHeight, mSourceWidth, mSourceHeight, mSurfaceWidth, mSurfaceHeight;

	//Matrix for world->view->projection transform.
	private float[] mRotationMatrix = new float[16];
	private float[] mModelMatrix = new float[16];
	private float[] mViewMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mMVPMatrix = new float[16];

	//Handle to a program, attributes, texture, mvp matrix
	private int mProgramObject;
	private int mPositionLoc;
	private int mTexCoordLoc;
	
	
	//buffer for texture
	private ByteBuffer pixelBufferY, pixelBufferU, pixelBufferV;
	private int mSamplerLocY, mSamplerLocU, mSamplerLocV;
	private int mTextureIdY, mTextureIdU, mTextureIdV;
	
	private int mMVPMatrixHandle;

	private FloatBuffer mVertices;
	private ShortBuffer mIndices;

	private boolean textureCreated, needTextureCreation;
	
	private final float[] mVerticesData ={ 
			-1f, 1f, 0.0f, // Position 0
			0.0f, 0.0f, // TexCoord 0
			-1f, -1f, 0.0f, // Position 1
			0.0f, 1.0f, // TexCoord 1
			1f, -1f, 0.0f, // Position 2
			1.0f, 1.0f, // TexCoord 2
			1f, 1f, 0.0f, // Position 3
			1.0f, 0.0f // TexCoord 3
			};

	private final short[] mIndicesData ={ 0, 1, 2, 0, 2, 3 };
	public YUVRender(Context context)
	{
		mSourceWidth = 0;
		mSourceHeight = 0;
		
		mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mVertices.put(mVerticesData).position(0);
		
		mIndices = ByteBuffer.allocateDirect(mIndicesData.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
		mIndices.put(mIndicesData).position(0);

		lock = new Object();
		textureCreated = false;
		needTextureCreation = false;
	}

	public void setSourceSize(int width, int height){
		mSourceWidth = width;
		mSourceHeight = height;
		
		mTextureWidth = decideTextureSize();
		mTextureHeight = decideTextureSize();
		
		pixelBufferY = ByteBuffer.allocateDirect(mTextureWidth*mTextureHeight);
		pixelBufferU = ByteBuffer.allocateDirect(mTextureWidth*mTextureHeight/4);
		pixelBufferV = ByteBuffer.allocateDirect(mTextureWidth*mTextureHeight/4);
		needTextureCreation = true;
	}
	
	public void release(){
		
	}
	
	//create texture object.
	private void createTextureObject()
	{
		Log.i(TAG, "pixel size: "+mSourceWidth+"X"+mSourceHeight+" texture size: "+mTextureWidth+"X"+mTextureHeight);

		GLES20.glPixelStorei ( GLES20.GL_UNPACK_ALIGNMENT, 1 );
		int[] textureId = new int[3];
		GLES20.glGenTextures ( 3, textureId, 0 );
		
		mSamplerLocU = GLES20.glGetUniformLocation (mProgramObject, "s_texture_u" );
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glUniform1i ( mSamplerLocU, 1 );
		GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, textureId[1] );
		GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mTextureWidth/2, mTextureHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, pixelBufferU );
		GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST );
		GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST );
		mTextureIdU = textureId[1];
		
		mSamplerLocV = GLES20.glGetUniformLocation (mProgramObject, "s_texture_v" );
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glUniform1i ( mSamplerLocV, 2 );
		GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, textureId[2] );
		GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mTextureWidth/2, mTextureHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, pixelBufferV );
		GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST );
		GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST );
		mTextureIdV = textureId[2];

		mSamplerLocY = GLES20.glGetUniformLocation (mProgramObject, "s_texture_y" );
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glUniform1i ( mSamplerLocY, 0 );
		GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, textureId[0] );
		GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mTextureWidth, mTextureHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, pixelBufferY );
		GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST );
		GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST );
		mTextureIdY = textureId[0];
		
		textureCreated = true;
		needTextureCreation = false;
	}

	public static int loadShader(int type, String shaderCode){
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, shaderCode);
		checkGlError("source shader");
		GLES20.glCompileShader(shader);
		checkGlError("compile shader");
		return shader;
	}

	
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
	{

		//Vertex Shader.
		String vShaderStr =
				"attribute vec4 a_position;   							\n"
						+ "uniform mat4 u_MVPMatrix;    				\n"
						+ "attribute vec2 a_texCoord;   				\n"
						+ "varying vec2 v_texCoord;     				\n"
						+ "void main()                  				\n"
						+ "{                            				\n"
						+ "   gl_Position = u_MVPMatrix * a_position; 	\n"
						+ "   v_texCoord = a_texCoord;  				\n"
						+ "}                            				\n";
		//Fragment Shader
		String fShaderStr = 
				"precision highp float;                            					\n"
						+ "varying vec2 v_texCoord;                            			\n"
						+ "uniform sampler2D s_texture_y;                        		\n"
						+ "uniform sampler2D s_texture_u;                       		\n"
						+ "uniform sampler2D s_texture_v;                        		\n"
						+ "void main()                                         			\n"
						+ "{                                                   			\n"
						+ "		highp float y = texture2D(s_texture_y, v_texCoord).r; 	\n"
						+ "		highp float u = texture2D(s_texture_u, v_texCoord).r; 	\n"
						+ "		highp float v = texture2D(s_texture_v, v_texCoord).r; 	\n"
						+ "		y = 1.1643 * (y - 0.0625);								\n"
						+ "		u = u - 0.5;											\n"
						+ "		v = v - 0.5;											\n"
						+ "		highp float r = y + 1.5958 * v;							\n"
						+ "		highp float g = y - 0.39173 * u - 0.81290 * v;			\n"
						+ "		highp float b = y + 2.017 * u;							\n"
						+ "  	gl_FragColor = highp vec4(r, g, b, 1.0);				\n"
						+ "}                                                   			\n";

		
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vShaderStr);
		checkGlError("load vertex shader");
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fShaderStr);
		checkGlError("load fragment shader");
		 
		mProgramObject = GLES20.glCreateProgram();     
		checkGlError("create program");
		Log.i(TAG, "mProgram: "+mProgramObject+" v shader: "+vertexShader+" f shader: "+fragmentShader);

		Log.i(TAG,"attach and link shaders");
		GLES20.glAttachShader(mProgramObject, vertexShader);   
		checkGlError("attach vertex shader");
		GLES20.glAttachShader(mProgramObject, fragmentShader); 
		checkGlError("attach fragment shader");
		GLES20.glLinkProgram(mProgramObject);  
		checkGlError("link program");

		// Get the attribute locations
		mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "a_position");
		mTexCoordLoc = GLES20.glGetAttribLocation(mProgramObject, "a_texCoord" );
		 
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramObject, "u_MVPMatrix");

		//set model matrix 
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, 0.0f);
		Matrix.setRotateM(mRotationMatrix, 0, 90f, 0, 0, 1.0f);

		//Prepare view transform matrix
		final float eyeX = 0.0f, eyeY = 0.0f, eyeZ = 3f;
		final float lookX = 0.0f, lookY = 0.0f, lookZ = -1.0f;
		final float upX = 0.0f, upY = 1.0f, upZ = 0.0f;
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
		
		
		//Prepare projection transform matrix
		float left = -1f;
		float right = -((float)mTextureWidth/2-mSourceWidth)/((float)mTextureWidth/2);
		float bottom = ((float)mTextureHeight/2-mSourceHeight)/((float)mTextureHeight/2);
		float top = 1f;
		
		float rotate_left = -1f;
		float rotate_right = -(bottom+(float)0.005);
		float rotate_top = right;
		float rotate_bottom = -1f;
		
		
		
		float near = 1f;
		float far = 10f;
//		Matrix.orthoM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
		Matrix.orthoM(mProjectionMatrix, 0, rotate_left, rotate_right, rotate_bottom, rotate_top, near, far);
//		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

        
		//Prepare Model x View x Projection transform matrix.
		Matrix.multiplyMM(mModelMatrix, 0, mRotationMatrix, 0, mModelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	}

	private void applyTexture(){
		pixelBufferU.position(0);  
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTextureIdU );
		GLES20.glUniform1i ( mSamplerLocU, 1 );
		GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mTextureWidth/2, mTextureHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, pixelBufferU );
		
		pixelBufferV.position(0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTextureIdV );
		GLES20.glUniform1i ( mSamplerLocV, 2 );
		GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mTextureWidth/2, mTextureHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, pixelBufferV );

		pixelBufferY.position(0);  
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTextureIdY );
		GLES20.glUniform1i ( mSamplerLocY, 0 );
		GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mTextureWidth,   mTextureHeight,   0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, pixelBufferY );
	}

	public void onDrawFrame(GL10 glUnused)
	{
		synchronized (lock) {
			if(needTextureCreation){
				createTextureObject();
			}
			
			if(textureCreated){
				applyTexture();
	
				GLES20.glUseProgram(mProgramObject);
				GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
	
				mVertices.position(0);
				GLES20.glVertexAttribPointer ( mPositionLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, mVertices );
				mVertices.position(3);
				GLES20.glVertexAttribPointer ( mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, mVertices );
	
				GLES20.glEnableVertexAttribArray ( mPositionLoc );
				GLES20.glEnableVertexAttribArray ( mTexCoordLoc );
	
				GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
				GLES20.glDrawElements ( GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mIndices );
			}
		}
	}

	public void onSurfaceChanged(GL10 glUnused, int width, int height)
	{
		mSurfaceWidth = width;
		mSurfaceHeight = height;
	}

	public static void checkGlError(String glOperation) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
//			throw new RuntimeException(glOperation + ": glError " + error);
			Log.e("YUV render", glOperation + ": glError " + error);
		}
	}

	public void updatePicture(byte[] yuvFrame){
		synchronized (lock) {
			if(textureCreated){
				pixelBufferY.clear();
				pixelBufferU.clear();
				pixelBufferV.clear();

				int offsetU = mSourceWidth*mSourceHeight;
				int offsetV = (mSourceWidth*mSourceHeight) + (mSourceWidth*mSourceHeight)/4;

				for(int i=0; i<mSourceHeight; ++i){
					pixelBufferY.put(yuvFrame, i*mSourceWidth, mSourceWidth);
					pixelBufferY.position(i*mTextureWidth);
				}

				for(int i=0; i<mSourceHeight/2; ++i){
					pixelBufferU.put(yuvFrame, offsetU + (i*mSourceWidth/2), mSourceWidth/2);
					pixelBufferU.position(i*mTextureWidth/2);

					pixelBufferV.put(yuvFrame, offsetV + (i*mSourceWidth/2), mSourceWidth/2);
					pixelBufferV.position(i*mTextureWidth/2);
				}
			}
		}
	}
	
	private int decideTextureSize(){
		int size = MIN_TEXTURE_SIZE;
		int biggerNum = (mSourceWidth > mSourceHeight) ? mSourceWidth : mSourceHeight;
		while(biggerNum>size){size*=2;}
		return size;
	}
}