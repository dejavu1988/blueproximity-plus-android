package com.example.mcbp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompleted extends BroadcastReceiver {
	private final boolean ENABLE_STARTUP = false;
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(ENABLE_STARTUP){
			//we double check here for only boot complete event
			if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)){
				Log.d("BootCompleted", "BootCompleted received");
			    //here we start the service            
			    Intent serviceIntent = new Intent(context, DaemonService.class);
			    context.startService(serviceIntent);
			}
		}		
	}
}
