package com.example.mcbp.sensors;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.Constants;
import com.example.mcbp.WorkerService;
import com.example.mcbp.file.FileHelper;
import com.example.mcbp.log.ConfigureLog4J;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;

public class AudioWorker extends Service {

	Logger log;
	private CountDownTimer timer;
	private ExtAudioRecorder extAudioRecorder;
	//private static long ts = 0L;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public void onCreate(){
		super.onCreate();
		
		log = Logger.getLogger(AudioWorker.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        //log.info("onCreate");		   
        
        
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		WorkerService.audioTask = true;
		log.info("Subtask Audio");
		//WorkerService.audList.clear();
		extAudioRecorder = ExtAudioRecorder.getInstanse(false);
		
		timer = new CountDownTimer(Constants.AUDIO_PERIOD * 1000, 1000) {

			@Override
			public void onFinish() {
				// TODO Auto-generated method stub
				
				log.info("Audio scan finished");
				extAudioRecorder.stop();
				extAudioRecorder.reset();
				extAudioRecorder.release();
				
		        WorkerService.audioTaskDone = true;	
				
				stopSelf();
			}

			@Override
			public void onTick(long millisUntilFinished) {
				// TODO Auto-generated method stub
				
			}
			
		};
		
		FileHelper fh = new FileHelper();
		
		String root = fh.getDir();
		
		//ts = SystemClock.elapsedRealtime() - WorkerService.metaTS;
		
		WorkerService.wavName = "1.wav";
		WorkerService.wavPath = root + "/" + WorkerService.wavName;
		extAudioRecorder.setOutputFile(WorkerService.wavPath);
		
		try {
			extAudioRecorder.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		extAudioRecorder.start();
		timer.start();
		
		
		return START_NOT_STICKY;		
	}
}
