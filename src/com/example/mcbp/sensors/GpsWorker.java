package com.example.mcbp.sensors;

import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.WorkerService;
import com.example.mcbp.log.ConfigureLog4J;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class GpsWorker extends Service implements GpsStatus.Listener, LocationListener{

	private LocationManager locMgr;
	private GpsStatus gpsStatus;
	private CountDownTimer timer;
	private long metaTS;
	Logger log;
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		log = Logger.getLogger(GpsWorker.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        //log.info("onCreate");		      
        
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		WorkerService.gpsTask = true;
		metaTS = SystemClock.elapsedRealtime();
		locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		
		timer = new CountDownTimer(10000, 1000) {

			@Override
			public void onFinish() {
				// TODO Auto-generated method stub
				locMgr.removeGpsStatusListener(GpsWorker.this);
				locMgr.removeUpdates(GpsWorker.this);
				log.info("Gps scan finished");
				WorkerService.gpsTaskDone = true;
				stopSelf();
			}

			@Override
			public void onTick(long millisUntilFinished) {
				// TODO Auto-generated method stub
				
			}
			
		};
		
		locMgr.addGpsStatusListener(this);
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
		timer.start();
		log.info("Gps scan started for 10 sec");
		
		return START_NOT_STICKY;		
	}
	
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub		
		Log.i("subtask", "gps location updated.");
		//long ts = SystemClock.elapsedRealtime() - WorkerService.metaTS;
		WorkerService.sensorManager.putGpsCoordInfo(location.getLongitude(), location.getLatitude(), location.getAltitude(), location.getAccuracy());
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGpsStatusChanged(int event) {
		// TODO Auto-generated method stub
		Log.i("subtask", "gps prns updated.");
		int ts = (int) ((SystemClock.elapsedRealtime() - metaTS)/1000);
		gpsStatus =locMgr.getGpsStatus(null);
		if(gpsStatus != null && GpsStatus.GPS_EVENT_SATELLITE_STATUS == event){			
			
			String prns = "";
			Iterable<GpsSatellite> iSatellites =gpsStatus.getSatellites();
            Iterator<GpsSatellite> it = iSatellites.iterator();
            while(it.hasNext()){
            	GpsSatellite oSat = (GpsSatellite) it.next();
            	if(oSat.getPrn() <= 32){
            		 WorkerService.sensorManager.putGpsSatEntry(oSat.getPrn(), (int) oSat.getSnr());
                     
                     if(prns.length() != 0){
                     	prns += ",";           	
                     }                
                     prns += String.valueOf(oSat.getPrn());	
            	}               		
            }
            WorkerService.sensorManager.putGpsTsSatEntry(ts, prns);
            
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
