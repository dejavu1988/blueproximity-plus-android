package com.example.mcbp.sensors;
/*
 * WifiTuple: WiFi scan info for each AP
 * bssid: BSSID string
 * level: signal level in dBm, default is -100
 * count: total number of this BSSID in 10 seconds
 */
public class WifiTuple {
	
	private String bssid;
	//private String SSID;
	private int level;
	private int count;
	
	public WifiTuple(){
		this.bssid = "";
		this.level = -100;
		this.count = 0;
	}
	
	public WifiTuple(String bssid, int level){
		this.bssid = bssid;
		this.level = level;
		this.count = 0;
	}
	
	public WifiTuple(WifiTuple tuple){
		this.bssid = tuple.getBssid();
		this.level = tuple.getLevel();
		this.count = tuple.getCount();
	}
	
	public String getBssid(){
		return this.bssid;
	}
	
	public int getLevel(){
		return this.level;
	}
	
	public int getCount(){
		return this.count;
	}
		
	public void updateTuple(int level){
		this.level = (int) ((level + this.level * this.count) * 1.0 / (count + 1));
		this.count += 1;
	}
	
	@Override
	public String toString(){
		return this.bssid + "#" + this.level;
	}
}
