package com.example.mcbp.sensors;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.Constants;
import com.example.mcbp.WorkerService;
import com.example.mcbp.log.ConfigureLog4J;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

public class BluetoothWorker extends Service {

	//private HashMap<String,Integer> btDevices;	
	private BluetoothAdapter btAdapter;
	private BroadcastReceiver btReceiver;
	
	Logger log;
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		log = Logger.getLogger(BluetoothWorker.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        //log.info("onCreate");		        
        
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		WorkerService.btTask = true;	
		
		
		log.info("Subtask Bluetoth");
    	IntentFilter btFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    	btFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
    	btFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		
    	btReceiver = new BroadcastReceiver(){
    		
    		@Override
    		public void onReceive(Context context, Intent intent) {
    			//long ts = SystemClock.elapsedRealtime() - WorkerService.metaTS;
    			log.info("Subtask Bluetooth received");
    			String action = intent.getAction();
    			
    			if(BluetoothDevice.ACTION_FOUND.equals(action)) {    				
					
    				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    				int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);	//RSSI in dBm
    				WorkerService.sensorManager.putBluetoothEntry(device.getAddress(), rssi);

    			}else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
					Log.i("subtask", "bluetooth discovery started.");
					WorkerService.sensorManager.putBluetoothEntry(btAdapter.getAddress(), Constants.BT_LOCAL_RSSI);
					
				}else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					Log.i("subtask", "bluetooth discovery finished.");
					if (btAdapter != null) {
						btAdapter.cancelDiscovery();
					}
					if(btReceiver != null){
						unregisterReceiver(btReceiver);
					}
					log.info("BT scan finished");
					WorkerService.btTaskDone = true;
					stopSelf();
				}
    			
    		}
    	};
		
    	CountDownTimer timer = new CountDownTimer(10000, 1000) {

			@Override
			public void onFinish() {
				// TODO Auto-generated method stub
				if (btAdapter != null) {
					btAdapter.cancelDiscovery();
				}
				if(btReceiver != null){
					unregisterReceiver(btReceiver);
				}
				log.info("BT scan finished");
				WorkerService.btTaskDone = true;
				stopSelf();
			}

			@Override
			public void onTick(long millisUntilFinished) {
				// TODO Auto-generated method stub
			}
			
		};
		
		registerReceiver(btReceiver, btFilter);			
		// Getting the Bluetooth adapter
    	btAdapter = BluetoothAdapter.getDefaultAdapter();
    	btAdapter.startDiscovery();
    	timer.start();
    	log.info("Subtask Bluetooth scan started ");
		
		
		return START_NOT_STICKY;		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
