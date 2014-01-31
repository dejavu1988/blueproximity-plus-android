package com.example.mcbp.sensors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.util.Base64;
import android.util.Log;

import com.example.mcbp.Constants;
import com.example.mcbp.file.FileHelper;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.provider.Settings.Secure;

@SuppressLint("UseSparseArrays")
public class SensorManager {
	private Context _context;
	private Map<String,WifiTuple> wifiAPs;
	private Map<String,BluetoothTuple> btDevices;
	private Map<Integer,GpsSatTuple> gpsSats;
	private Map<Integer,GpsTsSatTuple> gpsTsSats;
	private GpsCoordTuple gpsCoordInfo;
	
	public SensorManager(Context context){
		this._context = context;
		this.wifiAPs = new HashMap<String,WifiTuple>();
		this.btDevices = new HashMap<String,BluetoothTuple>();
		this.gpsSats = new HashMap<Integer,GpsSatTuple>();
		this.gpsTsSats = new HashMap<Integer,GpsTsSatTuple>();
		this.gpsCoordInfo = new GpsCoordTuple();
	}
	
	public boolean isGPSOn(){
		String provider = Settings.Secure.getString(_context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
	    
		return provider.contains("gps");
	}
	
	public boolean isBTOn(){
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		return mBluetoothAdapter.isEnabled();
	}
	
	public boolean isWifiOn(){
		WifiManager mainWifi = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
		
		return mainWifi.isWifiEnabled();
	}
	
	public boolean isNetworkOn(){
		ConnectivityManager connMgr = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}
	
	public int getSensorStatus(){	//realtime
		int flag = Constants.STATUS_SENSOR_AUDIO;
		if(isGPSOn())	flag |= Constants.STATUS_SENSOR_GPS;
		if(isWifiOn())	flag |= Constants.STATUS_SENSOR_WIFI;
		if(isBTOn())	flag |= Constants.STATUS_SENSOR_BT;
				
		return flag;
		
	}
	
	public void clear(){
		this.wifiAPs.clear();
		this.btDevices.clear();
		this.gpsSats.clear();
		this.gpsTsSats.clear();
	}
	
	public void putWifiEntry(String bssid, int level){
		if(this.wifiAPs.containsKey(bssid)){
			WifiTuple tmp = new WifiTuple(this.wifiAPs.get(bssid));
			tmp.updateTuple(level);
			this.wifiAPs.put(bssid, tmp);
		}else{
			WifiTuple tmp = new WifiTuple(bssid,level);
			this.wifiAPs.put(bssid, tmp);
		}
	}
	
	public void putBluetoothEntry(String addr, int rssi){
		if(this.btDevices.containsKey(addr)){
			BluetoothTuple tmp = new BluetoothTuple(this.btDevices.get(addr));
			tmp.updateTuple(rssi);
			this.btDevices.put(addr, tmp);
		}else{
			BluetoothTuple tmp = new BluetoothTuple(addr,rssi);
			this.btDevices.put(addr, tmp);
		}
	}
	
	public void putGpsSatEntry(int prn, int snr){
		if(this.gpsSats.containsKey(prn)){
			GpsSatTuple tmp = new GpsSatTuple(this.gpsSats.get(prn));
			tmp.updateTuple(snr);
			this.gpsSats.put(prn, tmp);
		}else{
			GpsSatTuple tmp = new GpsSatTuple(prn,snr);
			this.gpsSats.put(prn, tmp);
		}
	}
	
	public void putGpsTsSatEntry(int ts, String prns){
		GpsTsSatTuple tmp = new GpsTsSatTuple(ts,prns);
		this.gpsTsSats.put(ts, tmp);
	}
	
	public void putGpsCoordInfo(double lo, double la, double al, float accu){
		this.gpsCoordInfo.setTuple(lo, la, al, accu);
	}
	
	public String exportToCsv() throws IOException{
		String uuid = Secure.getString(_context.getContentResolver(),Secure.ANDROID_ID);
		String fileName = "1.txt";
		String filePath = "";
		
		FileHelper fh = new FileHelper();		
		String root = fh.getDir();
		
		File sd = new File(root);

		if (sd.canWrite()){		
	    	
			filePath = root + "/" + fileName;
	    	File file = new File(sd, fileName);
	    	FileWriter filewriter = new FileWriter(file);  
	        BufferedWriter out = new BufferedWriter(filewriter);
	        
	        out.write("0#"+uuid+"#"+"\n");
	        
	        for(java.util.Map.Entry<String, WifiTuple> entry : this.wifiAPs.entrySet()){
	        	out.write("1#"+entry.getValue()+"\n");
	        }
	        
	        for(java.util.Map.Entry<String, BluetoothTuple> entry : this.btDevices.entrySet()){
	        	out.write("2#"+entry.getValue()+"\n");
	        }
	        
	        for(java.util.Map.Entry<Integer, GpsSatTuple> entry : this.gpsSats.entrySet()){
	        	out.write("3#"+entry.getValue()+"\n");
	        }
	        
	        for(java.util.Map.Entry<Integer, GpsTsSatTuple> entry : this.gpsTsSats.entrySet()){
	        	out.write("4#"+entry.getValue()+"\n");
	        }
	        
	        out.write("5#"+this.gpsCoordInfo);
	        		    
		    out.close();
		}
		
		return filePath;
	}
	
	public String serializeCsv(){
		String uuid = Secure.getString(_context.getContentResolver(),Secure.ANDROID_ID);
		String tmp = "0#"+uuid+"#"+";";
        
        for(java.util.Map.Entry<String, WifiTuple> entry : this.wifiAPs.entrySet()){
        	tmp += "1#"+entry.getValue() +";";
        }
        
        for(java.util.Map.Entry<String, BluetoothTuple> entry : this.btDevices.entrySet()){
        	tmp += "2#"+entry.getValue() +";";
        }
        
        for(java.util.Map.Entry<Integer, GpsSatTuple> entry : this.gpsSats.entrySet()){
        	tmp += "3#"+entry.getValue() +";";
        }
        
        for(java.util.Map.Entry<Integer, GpsTsSatTuple> entry : this.gpsTsSats.entrySet()){
        	tmp += "4#"+entry.getValue() +";";
        }
        
        tmp += "5#"+this.gpsCoordInfo +";";
		return tmp;
		
	}
	
	public String serializeWav(){
		
		String fileName = "1.wav";
		
		FileHelper fh = new FileHelper();		
		String root = fh.getDir();
		byte[] buffer = null;
		String filePath = root + "/" + fileName;
		File file = new File(filePath);			
		try {
			InputStream fis = new FileInputStream(file);
			buffer = new byte[(int)file.length()];
			fis.read(buffer, 0, buffer.length);
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String tmp = Base64.encodeToString(buffer, Base64.DEFAULT); 
		Log.e("SensorManager","WAV Base64 String size " + tmp.length());
		return tmp;
		
	}
	
}
