package com.rd.mirrorclient;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import android.util.Log;

public class AVCFrameReader {
	private final static String TAG = "AVC Frame Reader";
	private final static int BUFFER_SIZE = 1024*1024*5;  
	private DataInputStream inputStream;
	private byte[] readBuffer;
	private int bufferSize, currentPosition;
	private ByteBuffer bufferForProcessing;
	
	public AVCFrameReader(DataInputStream stream){
		try {
			inputStream = stream;
		}catch(Exception e){  
			e.printStackTrace();     
		}  
		
		bufferSize = BUFFER_SIZE;
		readBuffer = new byte[bufferSize];
		
		readChunk();
	}
	
	private int readChunk(){
		int read=0;
		try {
			read = inputStream.read(readBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		currentPosition = 0;
		bufferForProcessing = ByteBuffer.wrap(readBuffer);
		return read;
	}
	
	private int findNextNalPosition(){
//		Log.i(TAG, "position: "+currentPosition);
		
		if(bufferForProcessing.hasRemaining() && bufferForProcessing.getInt() == 0x00000001){
			try{
				while(!(bufferForProcessing.get() == 0x00 && bufferForProcessing.get() == 0x00 && bufferForProcessing.get() == 0x00 && bufferForProcessing.get() == 0x01)) {
				}
			}catch(BufferUnderflowException e){
				return -1;
			}
			int nextPosition = bufferForProcessing.position()-4;
			bufferForProcessing.position(nextPosition);

			return nextPosition; 
		}else{
			Log.e(TAG, "reached end of buffer or start position is wrong");
			return -1;
		}
	}
	
	public byte[] readFrame(){
		int nextPosition = findNextNalPosition();
		
		if(currentPosition < nextPosition){
			byte[] frame = new byte[nextPosition-currentPosition];
			System.arraycopy(readBuffer, currentPosition, frame, 0, frame.length);
			currentPosition = nextPosition;
			printBufferHead(frame);
			return frame;
		}else{
			return null;
		}
		 
	}
	
	private void printBufferHead(byte [] data){
		String s = new String("");
		for(int i=0; i<20 && i<data.length; ++i){
			s += String.format(" %02X", data[i]);
		}
	}
}
