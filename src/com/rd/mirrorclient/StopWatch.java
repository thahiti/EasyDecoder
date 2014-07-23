package com.rd.mirrorclient;

import android.util.Log;

public class StopWatch {
	private final static String TAG = "Time Measure";
	private long startTime; 
	
	public StopWatch(){
		startTime = 0;
	}
	public void start(){
		startTime = System.currentTimeMillis();
	}
	
	public void stop(String operation){
		if(0 != startTime){
			Log.i(TAG,String.format("%d elapsed for %s", System.currentTimeMillis()-startTime, operation));
		}
	}
}
