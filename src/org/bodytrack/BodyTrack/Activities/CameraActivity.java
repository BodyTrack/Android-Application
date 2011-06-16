package org.bodytrack.BodyTrack.Activities;

import java.io.IOException;

import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.R;
import org.bodytrack.BodyTrack.R.id;
import org.bodytrack.BodyTrack.R.layout;
import org.bodytrack.BodyTrack.R.string;

import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * This class provides a UI to take photos (tap to focus/take a photo)
 * and saves them to the database.
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback{
	public static final String TAG = "cameraActivity";
	
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;
	Camera mCamera;
	boolean previewRunning;
	protected DbAdapter dbAdapter;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	//setup as fullscreen
    	getWindow().setFormat(PixelFormat.TRANSLUCENT);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
    	WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	//set up surface view to show camera preview
    	setContentView(R.layout.camera_layout);
    	mSurfaceView = (SurfaceView)findViewById(R.id.camera_disp);
    	mSurfaceHolder = mSurfaceView.getHolder();
    	mSurfaceHolder.addCallback(this);
    	mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	
    	//set it so touching the screen takes a picture
    	mSurfaceView.setOnClickListener(takePicture);
    	
    	//connect to database
		dbAdapter = new DbAdapter(this).open();
    }

	private View.OnClickListener takePicture = new View.OnClickListener(){
		public void onClick(View v) {
			//have mPictureCallback receive the JPEG
			mCamera.takePicture(null, null, mPictureCallback);
		}
	};
	
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (Exception e){
			e.printStackTrace();
		}
		mCamera.startPreview();
	}	
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}


	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
		previewRunning = false;
		mCamera.release();
		
	}
	
	Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {
			
			try {
				if (dbAdapter.writePicture(data) != 0) {
					Toast.makeText(CameraActivity.this, R.string.tookPic,
							Toast.LENGTH_SHORT).show();
				}
				else{
					Toast.makeText(CameraActivity.this, R.string.failedToSavePic, Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				Toast.makeText(CameraActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
			}
			setResult(RESULT_OK);
			finish();
			
		}
		
	};
	
}
