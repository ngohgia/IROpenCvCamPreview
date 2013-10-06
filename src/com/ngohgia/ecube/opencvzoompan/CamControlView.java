package com.ngohgia.ecube.opencvzoompan;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

// Overloaded JavaCameraView class with multi-touch event handler implemented 
public class CamControlView extends JavaCameraView{
	private String LOG_TAG = "OpenCvCamZoomPan::TouchControl";
	
	private ScaleGestureDetector mScaleGestureDetector;
	private GestureDetectorCompat mGestureDetector;
	
	private float deltaX, deltaY, zoomScale;
	
	// Getters for the Panning amount and Zooming scale
	public float getDeltaX(){
		return deltaX;
	}
	
	public float getDeltaY(){
		return deltaY;
	}
	
	public float getZoomScale(){
		return zoomScale;
	}
	
	public CamControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
        // Sets up interactions
		mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
		mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mScaleGestureDetector.onTouchEvent(event);
        retVal = mGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);
    }
	
	private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
				= new ScaleGestureDetector.SimpleOnScaleGestureListener(){
		private float lastSpanX;
		private float lastSpanY;
		
		@Override
		public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector){
			lastSpanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector);
			lastSpanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector);
			return true;
		}
		
		@Override
		public boolean onScale(ScaleGestureDetector scaleGestureDetector){
			float spanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector);
			float spanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector);
			Log.i(LOG_TAG, "Scaled: " + spanX/lastSpanX + ", " + spanY/lastSpanY);
			
			if (spanX/lastSpanX < 1.0f || spanY/lastSpanY < 1.0f)
				zoomScale = Math.min(spanX/lastSpanX, spanY/lastSpanY);			// Zoom out
			else
				zoomScale = Math.max(spanX/lastSpanX, spanY/lastSpanY);			// Zoom in
			
			return true;
		}
	};
	
	private final GestureDetector.OnGestureListener mGestureListener
				= new GestureDetector.SimpleOnGestureListener(){
		@Override
		public boolean onDown(MotionEvent e){
			Log.i(LOG_TAG, "Touched at: " + e.getX() + ", " + e.getY());
			return true;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
			Log.i(LOG_TAG, "Scrolled: " + distanceX + ", " + distanceY);
			
			// Panning amount with an arbitrary constant to make the panning motion faster
			deltaX = 2.0f * distanceX;
			deltaY = 2.0f * distanceY;
			return true;
		}
	};
}
