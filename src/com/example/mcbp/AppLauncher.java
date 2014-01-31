package com.example.mcbp;


import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.DaemonService;
import com.example.mcbp.bind.BluetoothClient;
import com.example.mcbp.log.ConfigureLog4J;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;

public class AppLauncher extends Activity {
	
	
	private PrefManager pM;
	private Logger log;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		log = Logger.getLogger(AppLauncher.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        //log.info("log configured.");
        
		log.info("onCreate");	
		pM = new PrefManager(getApplicationContext());
		/*PackageInfo pInfo = null;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String version = pInfo.versionName;
		log.info("App Version: "+version);*/
		
		String uuid = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
		if(!pM.isUuidRegistered())
			pM.updateUUID(uuid);
		/*log.info("UUID initialed: "+uuid);
		pM.updateUUID(uuid);
		log.info("UUID updated in pM");*/
		

        Intent intentToService = new Intent(this, DaemonService.class);
        startService(intentToService);
        log.info("Intent to DaemonService started");
        
        Intent intentToActivity = new Intent(this, BluetoothClient.class);
        startActivity(intentToActivity);
        log.info("Intent to BluetoothClient started");
        
		finish();
	}
	
}
