package com.rd.mirrorclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.example.mirrorclient.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.rd.mirrorclient.FVideoDecoder;

public class MainActivity extends Activity implements SurfaceHolder.Callback{
	private String TAG = "Client";
	private SurfaceView videoView;
	private SurfaceHolder videoSurfaceHolder;
	private Surface mSurface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
 
		videoView = (SurfaceView) findViewById(R.id.surface); 
		videoSurfaceHolder = videoView.getHolder(); 
		videoSurfaceHolder.addCallback(this);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
//		mGLView = new MyGLSurfaceView(this);
//		setContentView(mGLView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
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

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.i(TAG,"surface changed");
		mSurface = arg0.getSurface();
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		Log.i(TAG,"surface changed");
		mSurface = arg0.getSurface();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
	}

	@Override
	protected void onStart() {

		videoFrameHandlerThread = new HandlerThread("Video frame thread");
		videoFrameHandlerThread.start();
		videoFrameHandler = new VideoFrameHandler(videoFrameHandlerThread.getLooper());

		videoFrameHandler.post(new Runnable() {
			public void run() {
				startTest();
			}
		});

		// TODO Auto-generated method stub
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

	void startTest(){
 
		decoder = new FVideoDecoder(320, 240);

		decoder.setOnVideoDecoderEventListener(new FVideoDecoder.OnVideoDecoderEventListener() {
			//invoked by run()
			public void onVideoConfigUpdated(int _width, int _height, int _stride, int _sliceHeight) {
				Log.i(TAG,"video dec configuration updated");
			}
			//invoked by video handler thread. 
			public void onVideoSetupDone() {
				Log.i(TAG,"video dec setup done");
			} 
			//invoked by video handler thread.
			public void onVideoStopDone() {
				Log.i(TAG,"video dec stop done");
			}
			//invoked by run
			public boolean onVideoBufferFilled(int idx, int size, long presentationTimeUs, byte[] data) {
				return true;
			} 
		});

		while(null == mSurface){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}  
 
		AVCFrameReader frameReader = new AVCFrameReader("/mnt/sdcard/dump.h264");
		FileOutputStream outputStream=null;
		
		try {
			outputStream = new FileOutputStream(new File("/mnt/sdcard/parseResult.h264"));
		}catch(Exception e){
			e.printStackTrace(); 
		} 
		
		long ts=0;
		try {
			while(true){
				byte[] frame = frameReader.readFrame();
				if(null != frame){
					outputStream.write(frame);
					decoder.decode(frame, frame.length, ts+=30);
				}else{  
					Log.i(TAG, "reached EOF");
					break;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}  
		
		try {
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
