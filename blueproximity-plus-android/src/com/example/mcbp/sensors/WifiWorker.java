package com.example.mcbp.sensors;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.WorkerService;
import com.example.mcbp.log.ConfigureLog4J;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.os.IBinder;

public class WifiWorker extends Service {

	private WifiManager wifiManager;	
	private BroadcastReceiver wifiReceiver;
	private List<ScanResult> wifiList;
	
	private CountDownTimer timer;
	
	Logger log;
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		log = Logger.getLogger(WifiWorker.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        //log.info("onCreate");		        
        
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		WorkerService.wifiTask = true;	
		
		log.info("Subtask Wifi");
    	wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	IntentFilter wifiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		
    	wifiReceiver = new BroadcastReceiver(){
    		
			@Override
			public void onReceive(Context context, Intent intent) {
				//long ts = SystemClock.elapsedRealtime() - WorkerService.metaTS;
				log.info("Subtask wifi received");	
				wifiList = wifiManager.getScanResults();
				for (ScanResult s: wifiList) {
					WorkerService.sensorManager.putWifiEntry(s.BSSID, s.level);
				}
				
				wifiManager.startScan();
				log.info("Subtask Wifi scan restarted ");
			}
		};
		
		timer = new CountDownTimer(10000, 1000) {

			@Override
			public void onFinish() {
				// TODO Auto-generated method stub
				if(wifiReceiver != null)
					unregisterReceiver(wifiReceiver);
				log.info("Wifi scan finished");
				WorkerService.wifiTaskDone = true;
				stopSelf();
			}

			@Override
			public void onTick(long millisUntilFinished) {
				// TODO Auto-generated method stub
			}
			
		};
		
		registerReceiver(wifiReceiver, wifiFilter);
		wifiManager.startScan();
		timer.start();
		log.info("Subtask Wifi scan started ");
		
		
		return START_NOT_STICKY;		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
