package com.ngohgia.ecube.iropencvcamarduino;

public class ColorPicker {
	public ColorPicker(){}
	
	private String colorFloatToHex(float value){
		int redVal, greenVal, blueVal, alphaVal;
		
		if (value < 0.5f){
			redVal = (int) (255 * value / 0.5f);
			greenVal = 255;
		} else {
			redVal = 255;
			greenVal = (int) (255 * (1-value) / 0.5f);
		}
		
		blueVal = 5;
		alphaVal = 80;
			
		
		String red = Integer.toHexString(redVal);
		String green = Integer.toHexString(greenVal);
		String blue = Integer.toHexString(blueVal);	// arbitrary blue value;
		String alpha = Integer.toHexString(alphaVal); // arbitrary alpha value
		
		if (red.length() == 1){
			red = "0" + red;
		}
		if (green.length() == 1){
			green = "0" + green;
		}
		if (blue.length() ==1){
			blue = "0" + blue;
		}
		
		String colorHex = "#" + alpha + red + green + blue;
		return colorHex;
	}
	
	public String[] getColorMatrix(float[] values){
		String[] colorMatrix = new String[values.length]; 
		for (int i = 0; i < values.length; i++){
			colorMatrix[i] = colorFloatToHex(values[i]);
		}
		
		return colorMatrix;
	}
}
