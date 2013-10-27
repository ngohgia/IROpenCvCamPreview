package com.ngohgia.ecube.iropencvcamarduino;

public class DeviceInfo {
	private String 		mDeviceName = "";
	private Integer		mDevicePower = null;
	private Integer[]	mDeviceIRCells = null;
	
	public String getDeviceName(){
		return mDeviceName;
	}
	
	public Integer getDevicePower(){
		return mDevicePower;
	}
	
	public Integer[] getDeviceIRCells(){
		return mDeviceIRCells;
	}
	
	private void updateDeviceName(String name){
		mDeviceName = name;
	}
	
	private void updateDevicePower(Integer power){
		mDevicePower = power;
	}
	
	private void updateDeviceIRCells(Integer[] cells){
		mDeviceIRCells = cells;
	}
}
