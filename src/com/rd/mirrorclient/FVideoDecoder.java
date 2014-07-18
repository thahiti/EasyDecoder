package com.rd.mirrorclient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;


public class FVideoDecoder {
	private String TAG = "FVideoDecoder";
	private byte [] decodedYUVBuffer;
	private int mWidth;
	private int mHeight; 
	FileOutputStream outputStream;
	static {
		System.loadLibrary("fvideodecoder");
	}

	public FVideoDecoder(int width, int height){
		decodedYUVBuffer = new byte[width*height*3/2];
		mWidth = width;
		mHeight = height;
		init();
	}

	public interface OnVideoDecoderEventListener{
		void onVideoConfigUpdated(int width, int height, int stride, int sliceHeight);
		void onVideoSetupDone();
		void onVideoStopDone();
		boolean onVideoBufferFilled(int idx, int dataSize, long presentationTimeUs, byte[] data);
	}

	private OnVideoDecoderEventListener listener = null;

	public void setOnVideoDecoderEventListener(OnVideoDecoderEventListener l){
		listener = l;
	} 

	public void init(){ 
		nativeInit(mWidth, mHeight);
		try {
			outputStream = new FileOutputStream(new File("/mnt/sdcard/ffjava_dump.yuv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}    

	public void deinit(){
		try {
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		nativeDeinit();   
	} 

	public void decode(byte[] frame, int size, long ts){
		int resultSize = nativeDecode(frame, size, ts, decodedYUVBuffer);
		if(0 < resultSize){
			Log.i(TAG,"result size: "+resultSize);
			
			try {
				outputStream.write(decodedYUVBuffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private native int nativeInit(int width, int height);
	private native int nativeDeinit();
	private native int nativeDecode(byte[] frame, int size, long ts, byte[] result);
}
