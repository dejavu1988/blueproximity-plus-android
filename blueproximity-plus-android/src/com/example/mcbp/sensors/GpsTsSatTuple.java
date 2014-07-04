package com.example.mcbp.sensors;
/*
 * GpsTsSatTuple: unit of recording GPS satellite info for each second
 * ts: int second from 0 to 9
 * prns: string of prns at this second, delimited with ','
 */
public class GpsTsSatTuple {
	
	private int ts;
	private String prns;
	//private String snrs;
	
	public GpsTsSatTuple(){
		this.ts = 0;
		this.prns = "";
	}
	
	public GpsTsSatTuple(int ts, String prns){
		this.ts = 0;
		this.prns = "";
	}
	
	public int getTs(){
		return this.ts;
	}
	
	public String getPrns(){
		return this.prns;
	}
	
	@Override
	public String toString(){
		return this.ts + "#" + this.prns;
	}
}
