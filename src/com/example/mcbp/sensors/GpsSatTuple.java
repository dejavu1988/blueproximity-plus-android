package com.example.mcbp.sensors;
/*
 * GpsSatTuple: GPS satellite info for each prn
 * prn: GPS PRN number in [1,32]
 * snr: combined average SNR in 10 seconds, default is 0
 * count: total number of this prn in 10 seconds
 */
public class GpsSatTuple {

	private int prn;
	private int snr;
	//private float azimuth;
	//private float elevation;
	private int count;
	
	public GpsSatTuple(){
		this.prn = 0;
		this.snr = 0;
		this.count = 0;
	}
	
	public GpsSatTuple(int prn, int snr){
		this.prn = prn;
		this.snr = snr;
		this.count = 0;
	}
	
	public GpsSatTuple(GpsSatTuple tuple){
		this.prn = tuple.getPrn();
		this.snr = tuple.getSnr();
		this.count = tuple.getCount();
	}
	
	public int getPrn(){
		return this.prn;
	}
	
	public int getSnr(){
		return this.snr;
	}
	
	public int getCount(){
		return this.count;
	}
	
	public void updateTuple(int snr){
		this.snr = (int) ((snr + this.snr * this.count) * 1.0 / (count + 1));
		this.count += 1;	
	}
	
	@Override
	public String toString(){
		return this.prn + "#" + this.snr;
	}
}
