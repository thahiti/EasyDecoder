package com.rd.mirrorclient;

public class FVideoDecoder {
	private String TAG = "FVideoDecoder";
	private byte [] decodedYUVBuffer;
	private int mWidth;
	private int mHeight; 
	private boolean doColorConvert;
	static {
		System.loadLibrary("fvideodecoder");
	}

	public FVideoDecoder(int width, int height, boolean needColorConvert){
		decodedYUVBuffer = new byte[width*height*3/2];
		mWidth = width;
		mHeight = height;
		init(); 
	}

	public interface OnVideoDecoderEventListener{
		boolean onVideoBufferFilled(byte[] yuvFrame, int size, long timestamp);
	}

	private OnVideoDecoderEventListener listener = null;

	public void setOnVideoDecoderEventListener(OnVideoDecoderEventListener l){
		listener = l;
	} 

	public void init(){
		
		nativeInit(mWidth, mHeight, doColorConvert); 
	}    

	public void deinit(){
		nativeDeinit();   
	} 

	public void decode(byte[] frame, int size, long ts){
		long timestamp = nativeDecode(frame, size, ts, decodedYUVBuffer);
		if(0 <= timestamp){
			listener.onVideoBufferFilled(decodedYUVBuffer, mWidth*mHeight*3/2, timestamp);
		}
	}

	private native int nativeInit(int width, int height, boolean doColorConvert);
	private native int nativeDeinit();
	private native long nativeDecode(byte[] frame, int size, long ts, byte[] result);
}
