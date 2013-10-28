package com.ngohgia.ecube.iropencvcamarduino;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CapturedViewLoader extends Activity implements OnPreDrawListener, OnTouchListener {
	private ImageView mCapturedView;
	private static final String LOG_TAG = "IROpenCvArduino::CapturedViewLoader";
	private static final int NON_DEVICE_IDX = 0;
	
	private MenuItem mShowDevicesItem;
	
	private CapturedViewResourceManager mCapturedViewManager;
	private static String mResourceLogFile = "resourceLog.txt";
	
	private LinearLayout mIRTbl;
	private FrameLayout mIRTblParent;
	private final float GRID_RATIO = 0.226f;	// Width/Length ratio constant of the IR Grid
	private int mIRTblCols;
	private int mIRTblRows;
	private int mIRTblWidth;
	private int mIRTblHeight;
	private int mIRTblPaddingTop;

	private int[][] mDeviceTbl = new int[][] {{0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 , 0, 0, 0, 0},
												{0, 0, 0, 1, 1, 1, 0, 0, 0, 2, 2, 2 , 0, 0, 0, 0},
												{0, 0, 0, 1, 1, 1, 0, 0, 0, 2, 2, 2 , 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 , 0, 0, 0, 0}};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_captured_view_loader);
		
		mCapturedViewManager = new CapturedViewResourceManager();
		String[] resourceInfo = readFromFile(mResourceLogFile);
		mCapturedViewManager.updateResourceInfoFromFile(resourceInfo);

		mIRTblCols = mCapturedViewManager.getCols();
		mIRTblRows = mCapturedViewManager.getRows();
		
		/*
		StringBuilder builder = new StringBuilder();
		for(String s : resourceInfo) {
		    builder.append(s);
		    builder.append(" ");
		}*/
		//Toast.makeText(this, "Resource Info: " + builder.toString() , Toast.LENGTH_LONG).show();
		
		mCapturedView = (ImageView) findViewById(R.id.captured_view);
		Bitmap bitmap = BitmapFactory.decodeFile(this.getApplicationContext().getFilesDir() + "/view_img_0.jpg");
		mCapturedView.setImageBitmap(bitmap);
		ViewTreeObserver mVto = mCapturedView.getViewTreeObserver();
	    mVto.addOnPreDrawListener(this);
		
		mIRTbl = (LinearLayout) findViewById(R.id.ir_grid_tbl);
		mIRTblParent = (FrameLayout) findViewById(R.id.captured_view_layout);
	}
	
	public void getDevices(){
   		FrameLayout.LayoutParams mIRTblParams = new FrameLayout.LayoutParams(mIRTblWidth, mIRTblHeight);
   		mIRTblParams.setMargins(0, mIRTblPaddingTop, 0, mIRTblPaddingTop);

		mIRTbl.setLayoutParams(mIRTblParams);
				
   		LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
		LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
		
		for (int i = 0; i < mIRTblRows; i++){
			LinearLayout mTblRow = new LinearLayout(this);
			mTblRow.setLayoutParams(rowParams);
			mTblRow.setOrientation(LinearLayout.HORIZONTAL);
			//mTblRow.setBackgroundResource(R.drawable.grid_cell);
			
			mIRTbl.addView(mTblRow);
		}
		
		for (int i = 0; i < mIRTbl.getChildCount() ; i++){
			LinearLayout row = (LinearLayout) mIRTbl.getChildAt(i);
			for (int k = 0; k < mIRTblCols; k++){
				Button mIRCell = new Button(this);
				mIRCell.setLayoutParams(colParams);
				
				row.addView(mIRCell);
			}
		}
		
		resetIRCellState();
		
		for (int i = 0; i < mIRTbl.getChildCount() ; i++){
			LinearLayout row = (LinearLayout) mIRTbl.getChildAt(i);
			for (int k = 0; k < mIRTblCols; k++){
				Button mIRCell = (Button) row.getChildAt(k);
				
				mIRCell.setOnClickListener(new IRCellOnClickListener(this, i, k));
				mIRCell.setOnLongClickListener(new IRCellOnLongClickListener(this));
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		mShowDevicesItem = menu.add("Get Devices");
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mShowDevicesItem){
    		getDevices();
    	}
    	return true;
    }


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

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onPreDraw() {
		mIRTblWidth = mCapturedView.getMeasuredWidth();
		mIRTblHeight = (int) (mIRTblWidth * GRID_RATIO);
		mIRTblPaddingTop = (int) ((mCapturedView.getMeasuredHeight() - mIRTblHeight) * 0.5);
		return true;
	}
	
	private class IRCellOnLongClickListener implements OnLongClickListener{
		private Context mContext;
		
		public IRCellOnLongClickListener(Context context){
			mContext = context;
		}
		
		@Override
		public boolean onLongClick(View arg0) {
			//Toast.makeText(mContext, "Long tap", Toast.LENGTH_LONG).show();
			
			showDeviceInfoEditDialog();
			return false;
		}
	}
	
	private class IRCellOnClickListener implements OnClickListener {
		private int mRow;
		private int mCol;
		private Context mContext;
		
		public IRCellOnClickListener(Context context, int row, int col){
			mRow = row;
			mCol = col;
			mContext = context;
		}
		
		@Override
		public void onClick(View v) {
			resetIRCellState();
			
			int selectedDeviceIdx = getDeviceIndexFromDeviceTbl(mRow, mCol);
			Toast.makeText(mContext, "Device " + selectedDeviceIdx + " is selected", Toast.LENGTH_LONG).show();
			
			if (selectedDeviceIdx != NON_DEVICE_IDX){
				Button[] selectedIRCells = getIRCellsFromDeviceNo(selectedDeviceIdx);
				for (int i = 0; i < selectedIRCells.length; i++)
					setIRCellBg(selectedIRCells[i], R.drawable.ir_cell_bg_pressed);
			}
			
			showDeviceInfoDialog();
		}
	};
	
	private void resetIRCellState(){
		for (int i = 0; i < mDeviceTbl.length; i++)
			for (int j = 0; j < mDeviceTbl[i].length; j++){
				Button mIRCell = (Button) ((LinearLayout) mIRTbl.getChildAt(i)).getChildAt(j);
				if (mDeviceTbl[i][j] != NON_DEVICE_IDX)
					mIRCell.setBackgroundResource(R.drawable.ir_cell_bg_normal);
				else
					mIRCell.setBackgroundResource(Color.TRANSPARENT);
			}
	}
	
	private int getDeviceIndexFromDeviceTbl(int row, int col){
		return mDeviceTbl[row][col];
	}
	
	private Button[] getIRCellsFromDeviceNo(int selectedDeviceIdx){
		ArrayList<Button> mCellList = new ArrayList<Button>();
		
		for (int i = 0; i < mDeviceTbl.length; i++)
			for (int j = 0; j < mDeviceTbl[i].length; j++){
				if (mDeviceTbl[i][j] == selectedDeviceIdx){
					Button mIRCell = (Button) ((LinearLayout) mIRTbl.getChildAt(i)).getChildAt(j);
					mCellList.add(mIRCell);
				}
			}
		
		Button[] mCellArr = new Button[mCellList.size()];
		mCellList.toArray(mCellArr);
		
		return mCellArr;
	}
	
	private void setIRCellBg(Button irCell, int resid){
		irCell.setBackgroundResource(resid);
	}
	
	private void showDeviceInfoEditDialog(){
		// get prompts.xml view
		LayoutInflater li = LayoutInflater.from(this);
		View mDeviceInfoEditDialog= li.inflate(R.layout.device_info_edit_dialog, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		alertDialogBuilder.setView(mDeviceInfoEditDialog);

		alertDialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
							  public void onClick(DialogInterface dialog,int id) {
	
							    }
							  })
						 .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						    public void onClick(DialogInterface dialog,int id) {
							dialog.cancel();
						    }
						  });

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
	
	private void showDeviceInfoDialog(){
		// get prompts.xml view
		LayoutInflater li = LayoutInflater.from(this);
		View mDeviceInfoEditDialog= li.inflate(R.layout.device_info_dialog, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		alertDialogBuilder.setView(mDeviceInfoEditDialog);

		alertDialogBuilder.setTitle("Device")
						  .setPositiveButton("OK", new DialogInterface.OnClickListener() {
							  public void onClick(DialogInterface dialog,int id) {
								  dialog.cancel();
							    }
							  });

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	private void getIRTbl(int viewIdx){
		String[] tmp = readFromFile("device_grid_view_" + viewIdx + ".txt");
		String[] parts = tmp[0].split("\t");
		
		Log.i(LOG_TAG, "Device grid: " + parts.length);

		if (parts.length == mIRTblRows * mIRTblCols){
			mDeviceTbl = new int[mIRTblRows][mIRTblCols];
			for (int i = 0; i < mIRTblRows; i++)
				for (int j = 0; j < mIRTblCols; j++)
					mDeviceTbl[i][j] = Integer.parseInt(parts[i*mIRTblCols+j]);
		}
	}
}
