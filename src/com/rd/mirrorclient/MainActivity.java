package com.rd.mirrorclient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.example.mirrorclient.R;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.rd.mirrorclient.FVideoDecoder;

public class MainActivity extends Activity {
	private String TAG = "Client";
	private GLSurfaceView mGLView;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.mainlayout);
		Button extra = new Button(this);
		extra.setText("extra");
		layout.addView(extra);
		
		mGLView = new YUVGLSurfaceView(this);
		setContentView(mGLView);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onStart() {
		videoFrameHandlerThread = new HandlerThread("Video frame thread");
		videoFrameHandlerThread.start();
		videoFrameHandler = new VideoFrameHandler(videoFrameHandlerThread.getLooper());

		videoFrameHandler.post(new Runnable() {
			public void run() {
				startTest();
			}
		});

		super.onStart();
	}

	private static class VideoFrameHandler extends Handler{
		public VideoFrameHandler(Looper looper){
			super(looper);
		}
	}

	private final static int WIDTH = 1280;
	private final static int HEIGHT = 720;
	
	private HandlerThread videoFrameHandlerThread;
	private VideoFrameHandler videoFrameHandler;

	private FVideoDecoder decoder;
	
	FileOutputStream fileoutput=null;

	void startTest(){
		decoder = new FVideoDecoder(WIDTH, HEIGHT);
		
		decoder.setOnVideoDecoderEventListener(new FVideoDecoder.OnVideoDecoderEventListener() {
			public boolean onVideoBufferFilled(byte[] data, int size, long timestamp) {
//				try{
//					fileoutput.write(data);
//					Log.i(TAG,"frame decoded");
//				}catch(Exception e){
//					e.printStackTrace();
//				}
				((YUVGLSurfaceView) mGLView).updatePicture(data);
				
				runOnUiThread(new Runnable(){
					public void run() {
						mGLView.requestRender();
					}
				}); 

				return true; 
			}  
		});

		AVCFrameReader frameReader = new AVCFrameReader("/mnt/sdcard/Maroon.h264");
		
		try {
			fileoutput = new FileOutputStream(new File("/mnt/sdcard/testdump.yuv"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		((YUVGLSurfaceView)mGLView).setSourceSize(WIDTH/2, HEIGHT/2);
		
		long ts=0;
		boolean needRecreated=true;
		try {
			while(true){
				byte[] frame = frameReader.readFrame(); 
				if(null != frame){
					decoder.decode(frame, frame.length, ts+=30);
					
					if(ts > 1000 && needRecreated){
						((YUVGLSurfaceView)mGLView).setSourceSize(WIDTH, HEIGHT);
						needRecreated = false;
					}
				}else{  
					Log.i(TAG, "reached EOF");
					break;
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		try {
			fileoutput.close(); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
