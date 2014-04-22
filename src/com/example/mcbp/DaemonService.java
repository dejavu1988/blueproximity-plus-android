package com.example.mcbp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.example.mcbp.bind.BluetoothUtil;
import com.example.mcbp.crypt.CryptUtil;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

public class DaemonService extends Service {

	// Debugging
    private static final String TAG = "DaemonService";
    private static final boolean D = true;
    
    private PrefManager pM;
    //private static String uuid;
    
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new DaemonBinder();
    
    private static final Class[] mStartForegroundSignature = new Class[] {
        int.class, Notification.class};
    private static final Class[] mStopForegroundSignature = new Class[] {
        boolean.class};
    
    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    
    public static MessageConsumer mConsumer = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	if(D) Log.e(TAG, "-- ON CREATE --");
                
    	pM = new PrefManager(getApplicationContext());
    	if(!pM.isBond()){
    		this.stopSelf();
    	}
        String queue1 = pM.getUUID();
        String queue2 = pM.getBindID();
        if(queue1.isEmpty() || queue2.isEmpty()){
        	this.stopSelf();
        }
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported.", Toast.LENGTH_LONG).show();
            this.stopSelf();
        }else if (!mBluetoothAdapter.isEnabled()) {
        	Toast.makeText(this, "Please enable your Bluetooth.", Toast.LENGTH_LONG).show();
        }else if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
        	Toast.makeText(this, "Please set your Bluetooth visible permanantly.", Toast.LENGTH_LONG).show();
        }
        
        //uuid = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }  
        
        mConsumer = new MessageConsumer(this,"54.229.32.28", queue1, queue2);
        if(D) Log.e(TAG, "service created");
    }
    
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("DaemonService", "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("DaemonService", "Unable to invoke startForeground", e);
            }
            return;
        }
        
        
    }
    
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("DaemonService", "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("DaemonService", "Unable to invoke stopForeground", e);
            }
            return;
        }        
        
    }
        
    
    @Override
    public void onDestroy() {
    	// Make sure our notification is gone.
        stopForegroundCompat(R.string.foreground_service_started);
        //connected = false;      
        mConsumer.dispose();
        super.onDestroy();
    }
    

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if(D) Log.e(TAG, "-- ON START --");
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        //new Thread(new Daemon(this)).start(); 
        boolean t = mConsumer.connectToRabbitMQ();
        if(D) Log.e(TAG, "connection "+t);
        return START_NOT_STICKY;
    }
    
    void handleCommand(Intent intent) {
        //if (ACTION_FOREGROUND.equals(intent.getAction())) {
            // In this sample, we'll use the same text for the ticker and the expanded notification
            CharSequence text = getText(R.string.foreground_service_started);

            // Set the icon, scrolling text and timestamp
            Notification notification = new Notification(R.drawable.ic_bp, text,
                    System.currentTimeMillis());

            // The PendingIntent to launch our activity if the user selects this notification
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, BindActivity.class), 0);

            // Set the info for the views that show in the notification panel.
            notification.setLatestEventInfo(this, getText(R.string.foreground_service_label),
                           text, contentIntent);
            /*notification.setLatestEventInfo(this, getText(R.string.foreground_service_label),
                    text, null);*/
            
            startForegroundCompat(R.string.foreground_service_started, notification);
            
        //} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
        //    stopForegroundCompat(R.string.foreground_service_started);
        //}
    }
    
    public class DaemonBinder extends Binder {
        DaemonService getService() {
            return DaemonService.this;
        }
    }
    
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	public static boolean isNetworkOn(Context context){
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

}
