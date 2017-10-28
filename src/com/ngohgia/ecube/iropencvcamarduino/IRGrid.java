package com.ngohgia.ecube.iropencvcamarduino;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

// Helper Class to set up the IR Grid
public class IRGrid {
	// Variables concerning the grid
	private int rows, cols;
	private ColorPicker mColorPicker = new ColorPicker();
	private final float GRID_RATIO = 0.226f;	// Width/Length ratio constant of the IR Grid
	
	// Dimensions of the IR Table
	private static int mIRTblWidth;
	private static int mIRTblHeight;
	private static int mIRTblPaddingTop;
	
	// Saturated IR grid
	private int[][] mIRInt  = new int[][]   {{0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1 , 0, 0, 0, 0},
											{0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0 , 1, 0, 0, 0},
											{0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 1 , 1, 0, 0, 0},
											{0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0 , 1, 0, 0, 0}};
									
	// Miscellaneous Variables
	DecimalFormat d = new DecimalFormat("#.##");
	
	public IRGrid(Context context, LinearLayout mIRTbl, FrameLayout mIRTblParent){
		// Set the row and col dimensions of the grid
		rows = 4;
		cols = 16;
		
		initIRGrid(context, mIRTbl, mIRTblParent);
	}
	
	public int[][] getIRInt(){
		return mIRInt;
	}
	
	public int getIRTblRows(){
		return rows;
	}
	
	public int getIRTblCols(){
		return cols;
	}
	
	public int getIRTblWidth(){
		return mIRTblWidth;
	}
	
	public int getIRTblHeight(){
		return mIRTblHeight;
	}
	
	public int getIRTblPaddingTop(){
		return mIRTblPaddingTop;
	}
	
	// Initialize the grid following the dimension of the IR grid
	private void initIRGrid(Context context, final LinearLayout mIRTbl, final FrameLayout mIRTblParent){
		
		// Initialize the dimensions of the IR Grid
		ViewTreeObserver mVto = mIRTbl.getViewTreeObserver();
	    mVto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	    	public boolean onPreDraw() {
	    		mIRTblWidth = mIRTblParent.getMeasuredWidth();
	    		mIRTblHeight = (int) (mIRTblWidth * GRID_RATIO);
	    		mIRTblPaddingTop = (int) ((mIRTblParent.getMeasuredHeight() - mIRTblHeight) * 0.5);
	           
		   		FrameLayout.LayoutParams mIRTblParams = new FrameLayout.LayoutParams(mIRTblWidth, mIRTblHeight);
		   		mIRTblParams.setMargins(0, mIRTblPaddingTop, 0, mIRTblPaddingTop);

				mIRTbl.setLayoutParams(mIRTblParams);
				
				return true;
	        }
	    });
		
		LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
		LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);				
		
		//Log.e(LOG_TAG, "First Child count" + mIRTbl.getChildCount());
		for (int i = 0; i < rows; i++){
			LinearLayout mTblRow = new LinearLayout(context);
			mTblRow.setLayoutParams(rowParams);
			mTblRow.setOrientation(LinearLayout.HORIZONTAL);
			//mTblRow.setBackgroundResource(R.drawable.grid_cell);
			
			mIRTbl.addView(mTblRow);
		}
		
		for (int i = 0; i < mIRTbl.getChildCount() ; i++){
			LinearLayout row = (LinearLayout) mIRTbl.getChildAt(i);
			//Log.e(LOG_TAG, "Child " + row);
			for (int k = 0; k < cols; k++){
				TextView mText = new TextView(context);
				
				mText.setGravity(Gravity.CENTER);
				mText.setTextColor(Color.WHITE);
				mText.setLayoutParams(colParams);
				mText.setBackgroundResource(R.drawable.grid_cell);
				
				row.addView(mText);
			}
		}
	}
	
	// Update the grid cells according to the values array passed in
	public void updateIRGrid(float[] values, LinearLayout mIRTbl){
		String[] colors = mColorPicker.getColorMatrix(values);
		updateIRInt(0.5f, values);

		int counter = 0;
		for (int i = 0; i < mIRTbl.getChildCount(); i++){
			LinearLayout row = (LinearLayout) mIRTbl.getChildAt(i);
			for (int j = 0; j < row.getChildCount(); j++){
				TextView cell = (TextView) row.getChildAt(j);
				
				// Update the text and the color of the cells
				cell.setText(d.format(values[counter]));
				cell.setBackgroundColor(Color.parseColor(colors[counter]));
				counter = counter + 1;
			}
		}
	}
	
	// Update the saturated IR Grid values: grid cell with value above thres would be saved as 1
	private void updateIRInt(float thres, float[] vals){
		int counter = 0;
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++){
				if (vals[counter] > thres)
					mIRInt[i][j] = -1;
				else
					mIRInt[i][j] = 0;
				counter = counter + 1;
			}
		scanDevice();
	}
	
	// Scan for connected hot spots
	private void scanDevice(){
		int devIdx = 0;
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++){
				if (mIRInt[i][j] == -1){
					devIdx++;
					probeHotSpot(i, j, devIdx);
				}
			}
	}
	
	// Probe if the cells surrounding cell [i][j] have the same value
	private void probeHotSpot(int x, int y, int devIdx){
		mIRInt[x][y] = devIdx;
		for (int deltaX = -1; deltaX <=1; deltaX++)
			for (int deltaY = -1; deltaY <=1; deltaY++)
				if ((deltaX != 0 || deltaY != 0) && 
					x + deltaX >= 0 && x + deltaX < rows && 
					y + deltaY >= 0 && y + deltaY < cols &&
					mIRInt[x+deltaX][y+deltaY] == -1)
					probeHotSpot(x + deltaX, y + deltaY, devIdx);
	}
}
