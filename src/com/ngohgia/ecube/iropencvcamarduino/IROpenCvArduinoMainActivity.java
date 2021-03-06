package com.ngohgia.ecube.iropencvcamarduino;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import com.ngohgia.ecube.iropencvcamarduino.R;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class IROpenCvArduinoMainActivity extends Activity implements CvCameraViewListener2 {
	private final String LOG_TAG = "IROpenCvArduino::Activity";
	private final String IR_GRID_VAL_KEY = "IR Grid Values";
	private CamControlView mOpenCvCameraView;
	private ViewSpecsHandler mViewSpecsHandler;
	
	private boolean mReceivedScene = false;

	// Mats used for camera display and processing
	private Mat	mRgba;
	private Mat	mIntermediateMat;
	private Mat	mRealView;

	// Original dimensions of the camera view Mat
	private Size mOriginalSize;
	private int mViewOriginalWidth;
	private int mViewOriginalHeight;
	private int mViewOriginalCols;
	private int mViewOriginalRows;
	

	// Dimensions of the displaying submat
	private int	mViewCols;
	private int mViewRows;
	// Parameters of the displaying submat
	private float mZoomScale = 1.0f;
	private int mViewX = 0;
	private int mViewY = 0;
	
	// Threshold of change 
	private final float EPSILON = 0.000000001f;
	
	private float mLastZoomScale = 1.0f;
	private float mLastDeltaX = 0.0f;
	private float mLastDeltaY = 0.0f;
	
	// Variable for the IR Grid
	private LinearLayout mIRTbl;
	private FrameLayout mIRTblParent;
	private IRGrid mIRGrid;
	
	// Variable for GUI
	private TextView mZoomInput;
	private TextView mDeltaXInput;
	private TextView mDeltaYInput;
	private Button mLockViewBtn;
	private Button mGetSpecsBtn;
	private MenuItem mLoadCapturedViewItem = null;
	
	private boolean isViewLocked = false;
	private float touchDeltaX;
	private float touchDeltaY;
	private float touchZoomScale;
	
	// Resource Manager
	private CapturedViewResourceManager mCapturedViewManager;
	private static String mResourceLogFile = "resourceLog.txt";
	
	// Variables for the USB Communication
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private Boolean mPermissionRequestPending;
	
	private UsbAccessory mUsbAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;
	
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private final byte[] mBuf = new byte[256];
    public final float[] mIRReading = new float[64];
	DecimalFormat d = new DecimalFormat("#.##");  
    
    // Handler to update GUI
	private static class myHandler extends Handler {
		private final WeakReference<IROpenCvArduinoMainActivity> mActivity;
		
		public myHandler(IROpenCvArduinoMainActivity activity){
			mActivity = new WeakReference<IROpenCvArduinoMainActivity>(activity);
		}
		
		@Override
		public void handleMessage(Message msg) {
			IROpenCvArduinoMainActivity activity = mActivity.get();
			
			Bundle bundle = msg.getData();
			float[] colorVal = bundle.getFloatArray(activity.IR_GRID_VAL_KEY);
			activity.mIRGrid.updateIRGrid(colorVal, activity.mIRTbl);

			
			activity.mZoomInput.setText(activity.d.format(activity.mZoomScale));
			activity.mDeltaXInput.setText(Integer.toString(activity.mViewX));
			activity.mDeltaYInput.setText(Integer.toString(activity.mViewY));
			
			if (activity.isViewLocked){
				activity.mLockViewBtn.setText("Unlock View");
			} else {
				activity.mLockViewBtn.setText("Lock View");
			}
		 };
	};
	
	private final myHandler mHandler = new myHandler(this);
	
	// Obtain permission to communicate with the USB Accessory
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(LOG_TAG, "Permission denied for accessory."
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mUsbAccessory)) {
					closeAccessory();
				}
			}
		}
	};	
	
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
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		setContentView(R.layout.activity_open_cv_zoom_pan_main);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));
		
		mOpenCvCameraView = (CamControlView) findViewById(R.id.cam_control_zoom_pan_preview);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		
		mViewSpecsHandler = new ViewSpecsHandler(this);
		
		// Initialize the IR Grid and its parent element
		mIRTbl = (LinearLayout) findViewById(R.id.ir_grid_tbl);
		mIRTblParent = (FrameLayout) findViewById(R.id.ir_open_cv_cam_main_layout);
		mIRGrid = new IRGrid(this.getApplicationContext(), mIRTbl, mIRTblParent);
		
		// Initialize GUI
		mZoomInput = (TextView) findViewById(R.id.zoom_input);
		mDeltaXInput = (TextView) findViewById(R.id.delta_x_input);
		mDeltaYInput = (TextView) findViewById(R.id.delta_y_input);
		mLockViewBtn = (Button) findViewById(R.id.set_zoom_btn);
		mGetSpecsBtn = (Button) findViewById(R.id.get_prev_zoom_specs);
		
		// Initialize USB Communication
		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		if (getLastNonConfigurationInstance() != null) {
			mUsbAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mUsbAccessory);
		}
		
		//Stub
		/*
		float[] colorValues = new float[64];
		for (int i = 0; i < 64; i++){
			colorValues[i] = (float) Math.random();
			//Log.e(LOG_TAG, "ColorValues: " + 255 * colorValues[i]);
		}
		mIRGrid.updateIRGrid(colorValues, mIRTbl);*/
		
		//Initialize Resource Manager
		mCapturedViewManager = new CapturedViewResourceManager();
		mCapturedViewManager.setMaxViewCount(10);
		mCapturedViewManager.setIRTblSettings(mIRGrid.getIRTblRows(), mIRGrid.getIRTblCols());
		mCapturedViewManager.setViewCount(0);
		writeToFile(mResourceLogFile, mCapturedViewManager.getResourceInfo());
	}

    @SuppressWarnings("deprecation")
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mUsbAccessory != null) {
			return mUsbAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}	
	
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
		closeAccessory();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        
        // Reconnect OpenCVLoader
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        
        // Reconnect with USB Accessories
		Intent intent = getIntent();
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(LOG_TAG, "mAccessory is null");
		}
		
		String[] resourceInfo = readFromFile(mResourceLogFile);
		mCapturedViewManager.updateResourceInfoFromFile(resourceInfo);
    }
    
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        
        mIntermediateMat.release();
        mRealView.release();
        
		unregisterReceiver(mUsbReceiver);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		mLoadCapturedViewItem = menu.add("Load Captured Views");
		return true;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mLoadCapturedViewItem){
    		Intent intent = new Intent(this, CapturedViewLoader.class);
    		startActivity(intent);
    	}
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
		if (mNewScale < 1.0f)
			mNewScale = 1.0f;
		if (mNewScale > 5.0f)
			mNewScale = 5.0f;
		
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
	
	public void loadViewSpecs(View view){
		String input = mViewSpecsHandler.readSpecs();
		Toast.makeText(this, "READ VIEW SPECS " + input, Toast.LENGTH_LONG).show();
		String specs[] = input.split("\t");
		
		mZoomScale = Float.parseFloat(specs[0]);
		mViewX = Integer.parseInt(specs[1]);
		mViewY = Integer.parseInt(specs[2]);
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
    	
        // If the view is unlocked
        // Receive the scrolling and zooming values from the multi-touch handlers
        if (isViewLocked == false){
	    	touchDeltaX = mOpenCvCameraView.getDeltaX();
	    	touchDeltaY = mOpenCvCameraView.getDeltaY();
	    	touchZoomScale = mOpenCvCameraView.getZoomScale();
	    	
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
        }
    	
    	Log.i(LOG_TAG, "New View Size: " + mViewCols + " x " + mViewRows);
		Log.i(LOG_TAG, "New View Pos: " + mViewX + " , " + mViewY);
    	
    	// Submat the original camera view Mat
        mIntermediateMat = mRgba.submat(mViewY, mViewY + mViewRows, mViewX, mViewX + mViewCols);
        // Expand the submat to fit the original camera view Mat
    	Imgproc.resize(mIntermediateMat, mRealView, mOriginalSize);
        
    	if (mOpenCvCameraView.isImageTaken()){
    		Bitmap tmpBmp = Bitmap.createBitmap(mRealView.cols(), mRealView.rows(), Bitmap.Config.ARGB_8888);
    		Utils.matToBitmap(mRealView, tmpBmp);   		
    		Log.i(LOG_TAG, "Image taken of size " + tmpBmp.getHeight() + " x " + tmpBmp.getWidth());
    		
            FileOutputStream fos = null;
            try {
            	fos = this.openFileOutput("view_img_" + "0" + ".jpg", Context.MODE_PRIVATE);
            } catch (FileNotFoundException e) {
            	Log.i(LOG_TAG, "Error writing image to file: " + e.toString());
            }
            tmpBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
    		
    		mOpenCvCameraView.setImageUnTaken();
    		
    		// Save deviceTbl
    		int[][] mDeviceGrid = mIRGrid.getIRInt();
    		int rows = mDeviceGrid.length;
    		int cols = mDeviceGrid[0].length;
    		String mDeviceGridLinear = "";
    		for (int i = 0; i < rows; i++)
    			for (int j = 0; j < cols; j++){
    				mDeviceGridLinear += mDeviceGrid[i][j];
    				if (i < rows-1 || j < cols-1)
    					mDeviceGridLinear += "\t";
    			}
    		Log.i(LOG_TAG, "Captured IRTbl: " + mDeviceGridLinear);
    		byte[] tmpByte = mDeviceGridLinear.getBytes();
    		writeToFile("device_grid_view_0.txt", tmpByte);
    	}
    		
    	
        return mRealView;
	}
	
	// Function to start the communication with the USB Accessory
	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mUsbAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, comRunnable , "DemoKit");
			thread.start();
			Log.d(LOG_TAG, "Accessory opened");
			enableControls(true);
		} else {
			Log.d(LOG_TAG, "Accessory open fail");
		}
	}
	
	private void enableControls(boolean b) {
		// TODO write code to disable the GUI
		
	}
	
	// Close communication with the USB Accessory
	private void closeAccessory() {
		enableControls(false);

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mUsbAccessory = null;
		}
	}
	
	// Function to toggle the locking of view specs
	public void toggleViewLock(View view){
		if (isViewLocked)
			isViewLocked = false;
		else {
			isViewLocked = true;
			mViewSpecsHandler.writeSpecs(mZoomScale, mViewX, mViewY);
			Toast.makeText(this, "READ VIEW SPECS " + d.format(mZoomScale) + "\t" + Integer.toString(mViewX)+ "\t" + Integer.toString(mViewY), Toast.LENGTH_LONG).show();
		}
	}
	
	Runnable comRunnable = new Runnable(){
		@Override
		public void run(){
			int ret = 0;

			while (ret >= 0) {
				try {
					ret = mInputStream.read(mBuf);
				} catch (IOException e) {
					Log.e(LOG_TAG, "IOException", e);
					break;
				}
				
				float mMaxTemp = 0.0f;
				float mMinTemp = 50.0f;
				
				for(int i = 0; i < 256; i = i + 4){
					float mTemperature = (((mBuf[i] & 0xFF)<<24)
							+((mBuf[i+1] & 0xFF)<<16)
							+((mBuf[i+2] & 0xFF)<<8)
							+(mBuf[i+3] & 0xFF))
							/10.0f;
					
					mIRReading[i/4%4*16 + i/4/4] = mTemperature;
					if(mTemperature > mMaxTemp)
						mMaxTemp = mTemperature;
					if(mTemperature < mMinTemp)
						mMinTemp = mTemperature;
				}
				
				float[] mColorVal = new float[64];
				for (int i = 0; i < 64; i++){
					mColorVal[i] = (mIRReading[i] - mMinTemp)/(mMaxTemp - mMinTemp);
				}
				
				Message mGuiMsg = mHandler.obtainMessage();
				Bundle mTempBundle = new Bundle();
				mTempBundle.putFloatArray(IR_GRID_VAL_KEY, mColorVal);
				mGuiMsg.setData(mTempBundle);
				mHandler.sendMessage(mGuiMsg);
		     }
		}
	};
	
	// Function to get data from a file
	private String[] readFromFile(String fileName){
		ArrayList<String> info = new ArrayList<String>();		
		try {
			InputStream inputStream = openFileInput(fileName);
			
			if (inputStream != null){
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String buf = "";
				
				while ((buf = bufferedReader.readLine()) != null){
					info.add(buf);
				}
				
				inputStream.close();
			}
		} catch (FileNotFoundException e){
			Log.e(LOG_TAG, "File not found: " + e.toString());
		} catch (IOException e) {
			Log.e(LOG_TAG, "Can not read file: " + e.toString());
		}
		
		String[] outputArr = new String[info.size()];
	    outputArr = info.toArray(outputArr);
		return outputArr;
	}
	
	// Function to write generic String data to a file
	private void writeToFile(String fileName, byte[] data){
        try {
        	Log.i(LOG_TAG, "Prepare to write to " + fileName + " with " + data.toString());
        	FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
        	fos.write(data);
        	fos.close();
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "File writing failed: " + e.toString());
        } 
	}
}