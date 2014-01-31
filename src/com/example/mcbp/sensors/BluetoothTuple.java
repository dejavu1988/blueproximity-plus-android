package com.example.mcbp.sensors;
/*
 * BluetoothTuple: Bluetooth discovery info for each device
 * addr: MAC address string
 * rssi: signal strength in dBm, default is -100
 * count: total number of this device in 10 seconds
 */
public class BluetoothTuple {

	private String addr;
	//private String name;
	private int rssi;
	private int count;
	
	public BluetoothTuple(){
		this.addr = "";
		this.rssi = -100;
		this.count = 0;
	}
	
	public BluetoothTuple(String addr, int rssi){
		this.addr = addr;
		this.rssi = rssi;
		this.count = 0;
	}
	
	public BluetoothTuple(BluetoothTuple tuple){
		this.addr = tuple.getAddr();
		this.rssi = tuple.getRssi();
		this.count = tuple.getCount();
	}
	
	public String getAddr(){
		return this.addr;
	}
	
	public int getRssi(){
		return this.rssi;
	}
	
	public int getCount(){
		return this.count;
	}
		
	public void updateTuple(int rssi){
		this.rssi = (int) ((rssi + this.rssi * this.count) * 1.0 / (count + 1));
		this.count += 1;
	}
	
	@Override
	public String toString(){
		return this.addr + "#" + this.rssi;
	}
}
