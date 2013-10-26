package com.ngohgia.ecube.iropencvcamarduino;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class ViewSpecsHandler {
	private static final String LOG_TAG = "View_Specs_Handler";
	private static final String FILENAME = "viewSpecs.log";
	DecimalFormat d = new DecimalFormat("#.##");	
	private Context mContext;
	
	public ViewSpecsHandler(Context context){
		mContext = context;
	}
	
	public void writeSpecs(float zoomScale, int deltaX, int deltaY){
		try {
			OutputStreamWriter ow = new OutputStreamWriter(mContext.openFileOutput(FILENAME, Context.MODE_PRIVATE));
			ow.write(d.format(zoomScale) + '\t' + Integer.toString(deltaX) + '\t' + Integer.toString(deltaY));
			ow.close();
		} catch (IOException e){
			Log.e(LOG_TAG, "Writing to view specs log failed" + e.toString());
		}
	}
	
	public String readSpecs(){
		String ret = "";
		
		try {
			InputStream inputStream = mContext.openFileInput(FILENAME);
			
			if (inputStream != null){
				InputStreamReader is = new InputStreamReader(inputStream);
				BufferedReader br = new BufferedReader(is);
				String receivedString = "";
				StringBuilder sb = new StringBuilder();
				
				while ((receivedString = br.readLine()) != null){
					sb.append(receivedString);
				}
				
				inputStream.close();
				ret = sb.toString();
			}
		} catch (FileNotFoundException e){
			Log.e(LOG_TAG, "File not found: " + e.toString());
		} catch (IOException e){
			Log.e(LOG_TAG, "Cannot read file: " + e.toString());
		}
		
		return ret;
	}
}
