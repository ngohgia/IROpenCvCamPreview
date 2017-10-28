package com.ngohgia.ecube.iropencvcamarduino;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class CapturedViewResourceManager {
	private final String LOG_TAG = "IROpenCvArduino::ResourceManager";
	
	private int mIRTblRows = 4;
	private int mIRTblCols = 16;
	
	private DeviceInfo[] mCurViewDevice;
	
	private int mMaxViewCount = 0;
	private int mViewCount;
	
	public CapturedViewResourceManager(){
	};
	
	public int getRows(){
		return mIRTblRows;
	}
	
	public int getCols(){
		return mIRTblCols;
	}
	
	public void setIRTblSettings(int rows, int cols){
		mIRTblRows = rows;
		mIRTblCols = cols;
	}
	
	public void setMaxViewCount(int count){
		mMaxViewCount = count;
	}
	
	public void setViewCount(int count){
		mViewCount = count;
	}
	
	public int getViewCount(){
		return mViewCount;
	}
	
	public void updateResourceInfoFromFile(String[] data){
		mMaxViewCount = Integer.parseInt(data[0]);
		mViewCount = Integer.parseInt(data[1]);
		
		mIRTblRows = Integer.parseInt(data[2]);
		mIRTblCols = Integer.parseInt(data[3]);
	}
	
	public byte[] getResourceInfo(){
		String data = Integer.toString(mMaxViewCount);
		data = data + "\n" + Integer.toString(mViewCount);
		data = data + "\n" + Integer.toString(mIRTblRows);
		data = data + "\n" + Integer.toString(mIRTblCols);
		
		return data.getBytes();
	}
	
	public void updateNewViewInfoToFile(int viewIndex, Bitmap bmpData){

	}
}
