package com.example.mcbp.bak;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.Constants;
import com.example.mcbp.FeedbackActivity;
import com.example.mcbp.PrefManager;
import com.example.mcbp.R;
import com.example.mcbp.WorkerService;
import com.example.mcbp.R.drawable;
import com.example.mcbp.R.string;
import com.example.mcbp.log.ConfigureLog4J;
import com.example.mcbp.sensors.SensorManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

public class DaemonService extends Service{
	//static final String ACTION_FOREGROUND = "com.example.android.apis.FOREGROUND";
    //static final String ACTION_BACKGROUND = "com.example.android.apis.BACKGROUND";
    private PrefManager pM;
	private SensorManager sM;
	public static Socket socket = null;  
	public static BufferedReader in = null;  
	public static PrintWriter out = null; 
	public static String content = "";  
	//private String ver;
	public static boolean bindStatus = false;
	public static boolean taskStatus = false;
	//public static boolean aliveStatus = false;
	//public static long avgRTT = 0;
	//private static int countRTT = 0;
	//private static long lastR = 0;
	public static int bindToken = -1;
	//public static String qnum = "";
	public static String bindName = "";
	private Message message;
	//private String curver;
	private Gson gson = new Gson();
    private Type mapType;
    private boolean connected;
    private static String uuid = "";
    //public static long timeoutTimer = 0; 
    //public static long timeoutCounter = 0; 
    //public static boolean timeoutSet = false; 
    //public static long taskTimeout = 0;
    //public static boolean isTrigger = false;
    //public static long triggerTS = 0L;
    private Thread thrm;
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
    private Logger log;
    
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke startForeground", e);
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
                Log.w("ApiDemos", "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke stopForeground", e);
            }
            return;
        }
        
        
    }
    
    public class DaemonBinder extends Binder {
        DaemonService getService() {
            return DaemonService.this;
        }
    }
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	
        log = Logger.getLogger(DaemonService.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        log.info("onCreate");
        
        /*PackageInfo pInfo = null;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ver = pInfo.versionName;
		log.info("App Version: "+ver);
		curver = ver;*/
        
        uuid = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
        log.info("UUID set: "+uuid);
        
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
        
		pM = new PrefManager(getApplicationContext());
		sM = new SensorManager(getApplicationContext());
		
		if(sM.getSensorStatus() != Constants.STATUS_SENSOR_GWBA)
			Toast.makeText(getApplicationContext(), "Please enable all related sensors", Toast.LENGTH_LONG).show();
		
  		
        connected = true;
		mapType = new TypeToken<HashMap<String,String>>(){}.getType();
		
		thrm = new Thread(new Daemon(this));
		thrm.start();
    }

    public synchronized void stopThread(){
    	if(thrm != null){
    		Thread moribund = thrm;
    		thrm = null;
    		moribund.interrupt();
    	}
    }
    
    @Override
    public void onDestroy() {
        // Make sure our notification is gone.
        stopForegroundCompat(R.string.foreground_service_started);
        connected = false;        
        stopThread();
        
        log.info("onDestroy");
    }
    

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	log.info("onStartCommand");
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        //new Thread(new Daemon(this)).start(); 
                
        return START_NOT_STICKY;
    }
      
    
    void handleCommand(Intent intent) {
        //if (ACTION_FOREGROUND.equals(intent.getAction())) {
            // In this sample, we'll use the same text for the ticker and the expanded notification
            CharSequence text = getText(R.string.foreground_service_started);

            // Set the icon, scrolling text and timestamp
            Notification notification = new Notification(R.drawable.ic_launcher, text,
                    System.currentTimeMillis());

            // The PendingIntent to launch our activity if the user selects this notification
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, FeedbackActivity.class), 0);

            // Set the info for the views that show in the notification panel.
            notification.setLatestEventInfo(this, getText(R.string.foreground_service_label),
                           text, contentIntent);
            
            startForegroundCompat(R.string.foreground_service_started, notification);
            
        //} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
        //    stopForegroundCompat(R.string.foreground_service_started);
        //}
    }
    
    
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {  
            super.handleMessage(msg); 
            switch(msg.what){
            case 0:
            	// Prompt the feedback view
                break;
            case 1:
            	//Toast.makeText(getApplicationContext(), "Please update to the new stable version v"+curver, Toast.LENGTH_LONG).show();
      			
                break;
            default:
               	break;
            }
            
        }  
    };
	
	
	public class Daemon implements Runnable{
		//private boolean connected;
		private HashMap<String,String> msgObj;
		private String msgAck; 
	    
		public Daemon(Context context){
	    	
	    	connected = true;
	    	this.msgObj = new HashMap<String,String>();
	    	this.msgAck = "";
	    	
	    	log.info("Daemon thread initialed");
	    	
	    }

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
	        try {  
	            //socket = new Socket(Constants.SERVER_INET, Constants.SERVER_PORT);
	        	socket = new Socket();
	        	socket.connect(new InetSocketAddress(Constants.SERVER_INET, Constants.SERVER_PORT), 0);
	        	log.info("New Socket conn: "+socket);
	            in = new BufferedReader(new InputStreamReader(socket  
	                    .getInputStream()));  
	            log.info("BufferReader set: "+in);
	            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(  
	                    socket.getOutputStream())), true); 
	            log.info("PrintWriter set: "+out);
	            
	        } catch (IOException ex) {  
	            ex.printStackTrace();  
	            log.info("socket io exception: "+ ex.getMessage());   
	        } 
	        
	        /*avgRTT = 0;
            countRTT = 0;
            lastR = 0;*/
	        
	        
			try {  
	            while(connected){
	            	//log.info("in daemon thread loop");	            	
	              if (!socket.isClosed()) {  
	                  if (socket.isConnected()) {  
	                      if (!socket.isInputShutdown()  && in != null) {  
	                          if ((content = in.readLine()) != null) {  
	                              content += "\n";	
	                              log.info("MSG received: "+content);
	                            
	                            //long ts = SystemClock.elapsedRealtime();
	                          	msgObj = gson.fromJson(content, mapType);
	                          	String token = msgObj.get("id");
	                          	log.info("MSG token: "+token);
	                          	
	                          	if(token.contains(Constants.REQ_UUID)){
	                          		
	                          		log.info("MSG ack for REQ_UUID");
	                          			  	                          		
	                          		msgObj.clear();
	                          		msgObj.put("id", Constants.ACK_UUID);
	                          		msgObj.put("uuid", uuid);
	                          		//msgObj.put("ver", ver);
	                            	msgAck = gson.toJson(msgObj);
	                            	log.info("MSG ACK_UUID built: "+msgAck);
	                            	if (socket.isConnected()) {  
	                                    if (!socket.isOutputShutdown()) {  
	                                        out.println(msgAck); 
	                                        out.flush();
	                                        log.info("MSG ACK_UUID sent");
	                                    }  
	                                } 
	                            	
	                            	message = mHandler.obtainMessage(2);
	                          /*}else if(token.contains(Constants.UP_BIND)){
	                          		log.info("MSG ack for UP_BIND");
	                          		String aUuid = "";
	                          		String aName = "";
	                          		
	                          		aUuid = msgObj.get("bindId");
	                            	aName = msgObj.get("bindName");
	                            	
	                            	pM.updateBind(aUuid, aName, true);
	                            			                          		
	                          		message = mHandler.obtainMessage(2);*/
	                          	}else if(token.contains(Constants.ACK_SCAN)){
	                          		log.info("MSG ack for ACK_SCAN");
	                          		
	                          		taskStatus = true;
	                          		log.info("taskStatus started");	                        		
	                          		
	                          		Intent taskIntent = new Intent(getApplicationContext(), WorkerService.class);
	                          		startService(taskIntent);  
	                          		log.info("MSG ack: intent to WorkerService");
	                          		
	                          		message = mHandler.obtainMessage(2);
	                          	}else if(token.contains(Constants.UNLOCK)){
	                          		log.info("MSG ack for UNLOCK");	                          		
	                          		pM.updateEventCounter();
	                          		Intent feedbackIntent = new Intent(getApplicationContext(), FeedbackActivity.class);
		                      	    startService(feedbackIntent);  
	                          		log.info("MSG ack: feedback intent");
	                          		
	                          		message = mHandler.obtainMessage(0);
	                          	}
	                             
	                          	mHandler.sendMessage(message); 

	                          }  
	                      }  
	                  }  
	              }
	           }
			} catch (Exception e) {  
	          e.printStackTrace();  
			}  
		}
	}

}
