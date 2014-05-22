package com.example.mcbp;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.DaemonService;
import com.example.mcbp.file.HttpFileUploader;
import com.example.mcbp.log.ConfigureLog4J;
import com.example.mcbp.sensors.SensorManager;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.widget.Toast;

public class WorkerService extends Service{	

	private static final String GPS_TASK_ACTION =  "com.example.mcbp.GPS_TASK_ACTION";
	private static final String WIFI_TASK_ACTION =  "com.example.mcbp.WIFI_TASK_ACTION";
	private static final String BT_TASK_ACTION =  "com.example.mcbp.BT_TASK_ACTION";
	private static final String AUDIO_TASK_ACTION =  "com.example.mcbp.AUDIO_TASK_ACTION";
	//public static boolean taskStatus = false; 
	public static boolean uploadStatus = false;
	public static boolean gpsTask, btTask, wifiTask, audioTask = false;
	public static boolean gpsTaskDone, btTaskDone, wifiTaskDone, audioTaskDone = false;
	//public static List<Entry> gpsSatList, gpsLocList, wifiList, btList = null;
	//public static long metaTS = 0L;
	public static String wavPath = "";
	public static String wavName = "";
	private static String[] furis = {"",""};
	//private PrefManager pM;
	public static SensorManager sensorManager;
	private int responseCode1, responseCode2;
	private int flag;
	public static String uuid;
	private String ts = "";		// timestamp for task
	private int mask = 7;		// sensor mask for task
	Intent taskIntent1, taskIntent2, taskIntent3, taskIntent4;
	Logger log;
	
	
	public void onCreate(){
		super.onCreate();
		log = Logger.getLogger(WorkerService.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        log.info("onCreate");
        
        //metaList = new ArrayList<Entry>();
        //gpsSatList = new ArrayList<Entry>();
        //gpsLocList = new ArrayList<Entry>();
        //wifiList = new ArrayList<Entry>();
        //btList = new ArrayList<Entry>();
        //audList = new ArrayList<Entry>();
        
        uuid = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
        
        /*flag = 0;
        gpsTask = false;
        wifiTask = false;
        btTask = false;
        audioTask = false;
        gpsTaskDone = false;
        wifiTaskDone = false;
        btTaskDone = false;
        audioTaskDone = false;*/
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)  {
		// TODO Auto-generated method stub
	
		flag = 0;
        gpsTask = false;
        wifiTask = false;
        btTask = false;
        audioTask = false;
        gpsTaskDone = false;
        wifiTaskDone = false;
        btTaskDone = false;
        audioTaskDone = false;
        
        Bundle extras = intent.getExtras();        
		if(extras != null){
        	if(extras.containsKey("ts")){
        		ts = extras.getString("ts");
        	}
        	if(extras.containsKey("mask")){
        		mask = extras.getInt("mask");
        	}
        }
		
		//pM = new PrefManager(getApplicationContext());
		sensorManager = new SensorManager(getApplicationContext());
		
  	  	log.info("Worker Service started.");
		//taskStatus = true;
				
                        
		taskIntent1 = new Intent(GPS_TASK_ACTION); 
		taskIntent2 = new Intent(WIFI_TASK_ACTION);
		taskIntent3 = new Intent(BT_TASK_ACTION);
		taskIntent4 = new Intent(AUDIO_TASK_ACTION);
		
        flag = sensorManager.getSensorStatus();
        flag = flag & Constants.STATUS_SENSOR_GWBA & mask;
        if(flag != 0){	//allowed only when at least one sensor enabled
        	//detach subtasks for modalities
        	sensorManager.clear();
        	//metaTS = SystemClock.elapsedRealtime();
        	
            if((flag & Constants.STATUS_SENSOR_AUDIO) == Constants.STATUS_SENSOR_AUDIO){
    			log.info("Audio task scheduled");
    			audioTask = true;    			
    			startService(taskIntent4);
    		}
            
            if((flag & Constants.STATUS_SENSOR_GPS) == Constants.STATUS_SENSOR_GPS){
            	log.info("GPS task scheduled");
            	gpsTask = true;    			
    			startService(taskIntent1);
    		}
    		if((flag & Constants.STATUS_SENSOR_WIFI) == Constants.STATUS_SENSOR_WIFI){
    			log.info("Wifi task scheduled");
    			wifiTask = true;
    			startService(taskIntent2);
    		}
    		if((flag & Constants.STATUS_SENSOR_BT) == Constants.STATUS_SENSOR_BT){
    			log.info("Bluetooth task scheduled");
    			btTask = true;    			
    			startService(taskIntent3);
    		}
    		
    		log.info("WorkerService completed plugins schedule");
    		new Thread(null, OnTask, "MonitorTask").start();
    		
        }else{
        	//DaemonService.taskStatus = false;
    		log.info("taskStatus ended");
    		stopSelf();
        }
		
		return START_NOT_STICKY;
		
	}
	
	@Override
	public void onDestroy() {	
		
		super.onDestroy();
	}
	
	Runnable OnTask = new Runnable() {
		private Handler mHandler = new Handler(Looper.getMainLooper());
		
		
        public void run() {
        	boolean flag1 = true;
        	int counter = 0;
        	while(flag1){
        		
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		if(!(gpsTask ^ gpsTaskDone) && !(wifiTask ^ wifiTaskDone) && !(btTask ^ btTaskDone) && !(audioTask ^ audioTaskDone)){
        			log.info("All tasks done.");
        			flag1 = false;
        		}        
        		counter = counter + 1;
        		if(counter > 12) 
        			flag1 = false;
        	}
    		
    		//DaemonService.taskStatus = false;
    		log.info("taskStatus ended");
    		      	  	
      	  
      	  try {
      		  furis[0] = sensorManager.exportToCsv();
      	  } catch (IOException e) {
      		  // TODO Auto-generated catch block
      		  e.printStackTrace();
      	  }
      	  
      	  DaemonService.mConsumer.sendCSV(ts, sensorManager.serializeCsv());
      	  DaemonService.mConsumer.sendWAV(ts, sensorManager.serializeWav());
  		
  		
      	  /*if(furis[0] != null){
      		  //log.info("Upload file: "+furi);
      		mHandler.post(new Runnable() {
                public void run() {
                	new UploadTask().execute(furis);
                }
             });
      		  //new UploadTask().execute(furi);	//upload csv file	
      	  }else{
      		  log.info("upload file uri not found");
      		  stopSelf();
      	  }*/
    
        }
	};
	
		
	/*private class UploadTask extends AsyncTask<String, Void, Void> {
	    @Override
	    protected Void doInBackground(String... txtPath) {
	    	try {
	    		uploadStatus = true;
	    		log.info("uploadStatus started");
	    		log.info("Upload file: "+txtPath[0]);
    			HttpFileUploader htfu1 = new HttpFileUploader(txtPath[0]);
    			responseCode1 = htfu1.doUpload(false);
    			HttpFileUploader htfu2 = new HttpFileUploader(wavPath);
    			responseCode2 = htfu2.doUpload(false);
    			
	        } catch (Exception e) {
	          e.printStackTrace();
	        }
	        return null;
	    }

	    @Override
	    protected void onPostExecute(final Void unused) {
	    	if(responseCode1 == 200 && responseCode2 == 200){
	    		Toast.makeText(getApplicationContext(), "DataFile is Uploaded Successfully!", Toast.LENGTH_LONG).show();
	    	}
	    	uploadStatus = false;	   
	    	//new File(furis[0]).delete();
	    	//new File(wavPath).delete();
	    	log.info("uploadStatus ended");
	    	stopSelf();
	    }
	}*/
	
	/*public String[] exportToCSV(Context context) throws IOException{
		String result = "";
		String furi = "";
		FileHelper fh = new FileHelper(context);
		String uuid = Secure.getString(context.getContentResolver(),Secure.ANDROID_ID);
		
		String root = fh.getDir();
		File sd = new File(root);
		
		if (sd.canWrite()){
			
			String backupDBPath = "1.txt";
	    	furi = root + "/" + backupDBPath;
	    	
	    	File file = new File(sd, backupDBPath);
	    	FileWriter filewriter = new FileWriter(file);  
	        BufferedWriter out = new BufferedWriter(filewriter);
	        
	        String meta = uuid+"\n";
	        out.write(meta);
	        
	        for(Entry e: gpsSatList){
	        	result = e.getTS() +";" + e.getMT() +";" + e.getFP() +"\n";
	        	out.write(result);
	        }
	        for(Entry e: gpsLocList){
	        	result = e.getTS() +";" + e.getMT() +";" + e.getFP() +"\n";
	        	out.write(result);
	        }
	        for(Entry e: wifiList){
	        	result = e.getTS() +";" + e.getMT() +";" + e.getFP() +"\n";
	        	out.write(result);
	        }
	        for(Entry e: btList){
	        	result = e.getTS() +";" + e.getMT() +";" + e.getFP() +"\n";
	        	out.write(result);
	        }	
	        /*for(Entry e: audList){
	        	result = e.getTS() +";" + e.getMT() +";" + e.getFP() +"\n";
	        	out.write(result);
	        }*
	        		    
		    out.close();
		}
		String[] res = {furi, ""};
		return res;
	}*/
	
}
