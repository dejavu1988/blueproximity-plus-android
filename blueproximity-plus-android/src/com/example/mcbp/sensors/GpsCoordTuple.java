package com.example.mcbp.sensors;

public class GpsCoordTuple {

	private double longitude;
	private double latitude;
	private double altitude;
	private float accuracy;
	
	public GpsCoordTuple(){
		this.longitude = 0;
		this.latitude = 0;
		this.altitude = 0;
		this.accuracy = 0;
	}
	
	public GpsCoordTuple(double lo, double la, double al, float accu){
		this.longitude = lo;
		this.latitude = la;
		this.altitude = al;
		this.accuracy = accu;
	}
	
	public void setTuple(double lo, double la, double al, float accu){
		this.longitude = lo;
		this.latitude = la;
		this.altitude = al;
		this.accuracy = accu;
	}
	
	@Override
	public String toString(){
		return this.longitude + "," + this.latitude + "," + this.altitude + "#" + this.accuracy;				
	}
	
}
