package com.rd.mirrorclient;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.rd.mirrorclient.FVideoDecoder;
import com.example.mirrorclient.R;

public class MainActivity extends Activity {
	private String TAG = "Client";
	private GLSurfaceView mGLView;
	private DataInputStream inputStream;

	private int mWidth;
	private int mHeight;
 
	private final static int option = 2;
	
	protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		mGLView = new YUVGLSurfaceView(this);
		
		setContentView(R.layout.activity_main);

		RelativeLayout layout = (RelativeLayout) findViewById(R.id.mainlayout);

        RelativeLayout.LayoutParams glParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

		layout.addView(mGLView, glParams);
		
	
		switch (option){
		case 1:
            inputStream = new DataInputStream(getResources().openRawResource(R.raw.maroon5_1280x720));
            mWidth = 1280;
            mHeight = 720;
			break;
		case 2:
			inputStream = new DataInputStream(getResources().openRawResource(R.raw.maroon5_320x240));
			mWidth = 320;
			mHeight = 240;
			break;
		}

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


	
	private HandlerThread videoFrameHandlerThread;
	private VideoFrameHandler videoFrameHandler; 

	private FVideoDecoder decoder;
	
	FileOutputStream fileoutput=null;

	void startTest(){
		decoder = new FVideoDecoder(mWidth, mHeight);
		
		decoder.setOnVideoDecoderEventListener(new FVideoDecoder.OnVideoDecoderEventListener() {
			public boolean onVideoBufferFilled(byte[] data, int size, long timestamp) {
				try{
					if(null != fileoutput)
						fileoutput.write(data);
				}catch(Exception e){
					e.printStackTrace();
				}
				((YUVGLSurfaceView) mGLView).updatePicture(data);
				
				runOnUiThread(new Runnable(){
					public void run() {
						mGLView.requestRender();
					}
				}); 

				return true; 
			}  
		});

		AVCFrameReader frameReader = new AVCFrameReader(inputStream);
		
		long ts=0;
		boolean needRecreated=true;
		try {
			while(true){
				byte[] frame = frameReader.readFrame(); 
				if(null != frame){
					decoder.decode(frame, frame.length, ts+=30);
					
					if(ts > 1000 && needRecreated){
						((YUVGLSurfaceView)mGLView).setSourceSize(mWidth, mHeight);
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
			if(null != fileoutput)
				fileoutput.close(); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
