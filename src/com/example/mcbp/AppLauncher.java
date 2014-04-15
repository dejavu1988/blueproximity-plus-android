package com.example.mcbp;


import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.DaemonService;
import com.example.mcbp.log.ConfigureLog4J;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.widget.Toast;

public class AppLauncher extends Activity {
	
	// Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
	private PrefManager pM;
	private Logger log;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		log = Logger.getLogger(AppLauncher.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        
		log.info("AppLauncher onCreate");	
		
		pM = new PrefManager(getApplicationContext());
				
		if(!pM.isUuidRegistered()){
			String uuid = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
			pM.updateUUID(uuid);
		}					
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_LONG).show();
            log.info("Bluetooth is not supported.");
            finish();
        }
        
        setup();
	}
		
	private void setup(){
		Intent intentToActivity = new Intent(this, BindActivity.class);
        startActivity(intentToActivity);
        log.info("Intent to BindActivity started");
        
        finish();
	}
		
}
