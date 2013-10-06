package com.ngohgia.ecube.opencvzoompan;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

public class OpenCvZoomPanMainActivity extends Activity implements CvCameraViewListener2 {
	private String LOG_TAG = "CamControlZoomPan::Activity";
	private CamControlView mOpenCvCameraView;
	
	private boolean mReceivedScene = false;

	// Mats used for camera display and processing
	private Mat			mRgba;
	private Mat			mIntermediateMat;
	private Mat			mRealView;

	// Original dimensions of the camera view Mat
	private Size mOriginalSize;
	private int mViewOriginalWidth;
	private int mViewOriginalHeight;
	private int mViewOriginalCols;
	private int mViewOriginalRows;
	
	// Parameters of the displaying submat
	private float mZoomScale = 1.0f;
	// Dimensions of the displaying submat
	private int	mViewCols;
	private int mViewRows;
	// Position of the displaying submat on the original camera Mat
	private int mViewX = 0;
	private int mViewY = 0;
	
	// Threshold of change 
	private float EPSILON = 0.000000001f;
	
	private float mLastZoomScale = 1.0f;
	private float mLastDeltaX = 0.0f;
	private float mLastDeltaY = 0.0f;
	
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(LOG_TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    
                    mRealView = new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.activity_open_cv_zoom_pan_main);
		
		mOpenCvCameraView = (CamControlView) findViewById(R.id.cam_control_zoom_pan_preview);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        
        mIntermediateMat.release();
        mRealView.release();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		
		return true;
	}

	
	
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	return true;
    }

	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		
	}

	// Update the dimensions and position of the displaying submat upon a zoom action
	private void zoomView(float scale){
		Log.i(LOG_TAG, "Attempt to zoom");
		float mNewScale = mZoomScale * scale;
		
		// Arbitrary upper zoom scale set as 5.0
		if (mNewScale < 5.0f){
			// Determine the displaying submat dimensions
			int mNewCols = (int) ((float) mViewOriginalCols / mNewScale);
			int mNewRows = (int) ((float) mViewOriginalRows / mNewScale);
			
			// Determine the position of the displaying submat on the original Camera view Mat
			int mNewX = mViewX + mViewCols/2 - mNewCols/2;
			int mNewY = mViewY + mViewRows/2 - mNewRows/2;
			
			// Correct the position of the out of bound submat 
			if (mNewX < 0)
				mNewX = 0;
			if (mNewY < 0)
				mNewY = 0;
			
			// These conditions to update the submat are fail-safe 
			if (mNewX >= 0 && mNewX <= mViewOriginalCols &&
				mNewY >= 0 && mNewY <= mViewOriginalRows &&
				mNewX  + mNewCols >= 0 && mNewX  + mNewCols <= mViewOriginalCols &&
				mNewY  + mNewRows >= 0 && mNewY  + mNewRows <= mViewOriginalRows){
				mViewX = mNewX;
				mViewY = mNewY;
				
				mViewCols = mNewCols;
				mViewRows = mNewRows;
				
				mZoomScale = mNewScale;
			}
		}
	}
	
	// Update the position of the displaying submat upon a panning action
	private void panView(float deltaX, float deltaY){
		Log.i(LOG_TAG, "Attempt to pan");
		
		int mNewX = mViewX + (int) deltaX;
		int mNewY = mViewY + (int) deltaY;
		
		if (mNewX < 0)
			mNewX = 0;
		if (mNewY < 0)
			mNewY = 0;
		if (mNewX + mViewCols > mViewOriginalCols)
			mNewX = mViewOriginalCols - mViewCols;
		if (mNewY + mViewRows > mViewOriginalRows)
			mNewY = mViewOriginalRows - mViewRows;
		
		mViewX = mNewX;
		mViewY = mNewY;
	}
	
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        
        // Initialize the original dimensions of the Camera View Mat
        if (mReceivedScene == false){
        	mViewOriginalHeight = mOpenCvCameraView.getHeight();
        	mViewOriginalWidth = mOpenCvCameraView.getWidth();
        	
            mOriginalSize = mRgba.size();
        	mViewOriginalCols = mRgba.cols();
        	mViewOriginalRows = mRgba.rows();
        	
        	mViewCols = mViewOriginalCols;
        	mViewRows = mViewOriginalRows;
        	
        	mReceivedScene = true;
        	
            Log.i(LOG_TAG, "Original View: " + mViewOriginalWidth + " x " + mViewOriginalHeight);
        	Log.i(LOG_TAG, "Original Mat: " + mViewOriginalCols + " x " + mViewOriginalRows);
        }
    	
        // Receive the scrolling and zooming values from the multi-touch handlers
    	float touchDeltaX = mOpenCvCameraView.getDeltaX();
    	float touchDeltaY = mOpenCvCameraView.getDeltaY();
    	float touchZoomScale = mOpenCvCameraView.getZoomScale();
    	
    	// Update the displaying submat if the changes from the touch exceeds the threshold
    	if (Math.abs(touchDeltaX - mLastDeltaX) > EPSILON){
    		mLastDeltaX = touchDeltaX;
    		panView(mLastDeltaX, 0.0f);
    	}
    	
    	if (Math.abs(touchDeltaY - mLastDeltaY) > EPSILON){
    		mLastDeltaY = touchDeltaY;
    		panView(0.0f, mLastDeltaY);
    	}
    	
    	if (Math.abs(touchZoomScale - mLastZoomScale) > EPSILON){
    		mLastZoomScale = touchZoomScale;
    		zoomView(mLastZoomScale);
    	}
    	
    	Log.i(LOG_TAG, "New View Size: " + mViewCols + " x " + mViewRows);
		Log.i(LOG_TAG, "New View Pos: " + mViewX + " , " + mViewY);
    	
    	// Submat the original camera view Mat
        mIntermediateMat = mRgba.submat(mViewY, mViewY + mViewRows, mViewX, mViewX + mViewCols);
        // Expand the submat to fit the original camera view Mat
    	Imgproc.resize(mIntermediateMat, mRealView, mOriginalSize);
        
        return mRealView;
	}
}