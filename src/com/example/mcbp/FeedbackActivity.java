package com.example.mcbp;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.log.ConfigureLog4J;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FeedbackActivity extends Activity {

	private Button btn00, btn01, btn10, btn11;
	private TextView tv;
	Logger log;
	//private PrefManager pM;
	private CountDownTimer timer;
	private WakeLock wakeLock;
	private KeyguardLock keyguardLock;
	
	private boolean event = false, decision = false, cancel = false;
	private String ts;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feedback);
		
		log = Logger.getLogger(FeedbackActivity.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);   
        log.info("Entered feedback activity.");
        
        btn00 = (Button) findViewById(R.id.yesevent1);
		btn01 = (Button) findViewById(R.id.noevent1);
		btn10 = (Button) findViewById(R.id.yesevent2);
		btn11 = (Button) findViewById(R.id.noevent2);
		tv = (TextView) findViewById(R.id.event);
		btn00.setBackgroundColor(Color.GREEN);
		btn01.setBackgroundColor(Color.RED);
		btn10.setBackgroundColor(Color.RED);
		btn11.setBackgroundColor(Color.GREEN);
		
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
        KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE); 
        keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
        
        Intent intent = getIntent();
        Bundle extras = getIntent().getExtras();        
		if(extras != null){
			if(extras.containsKey("event")){
				event = extras.getBoolean("event");
			}
        	if(extras.containsKey("decision")){
        		decision = extras.getBoolean("decision");
        	}
        	if(extras.containsKey("ts")){
        		ts = extras.getString("ts");
        	}
        	if(extras.containsKey("cancel")){
        		cancel = extras.getBoolean("cancel");
        	}
        }
		
		if(event){
			tv.setText("unlocked");
			btn10.setVisibility(View.INVISIBLE);
			btn11.setVisibility(View.INVISIBLE);
		}else{
			tv.setText("locked");
			btn00.setVisibility(View.INVISIBLE);
			btn01.setVisibility(View.INVISIBLE);
		}
		
		//pM = new PrefManager(getApplicationContext());
		
		btn00.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				log.info("TP button clicked");	
				cancelNotification();
				DaemonService.mConsumer.sendFeedback("1", ts);
				FeedbackActivity.this.finish();
			}
			
		});
		
		btn01.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				log.info("FP button clicked");
				cancelNotification();
				DaemonService.mConsumer.sendFeedback("2", ts);
				FeedbackActivity.this.finish();
			}
			
		});
		
		btn10.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				log.info("FN button clicked");	
				cancelNotification();
				DaemonService.mConsumer.sendFeedback("4", ts);
				FeedbackActivity.this.finish();
			}
			
		});
		
		btn11.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				log.info("TN button clicked");
				cancelNotification();
				DaemonService.mConsumer.sendFeedback("3", ts);
				FeedbackActivity.this.finish();
			}
			
		});
		
		timer = new CountDownTimer(10000, 1000) {

			@Override
			public void onFinish() {
				// TODO Auto-generated method stub
				//showNotification();
				finish();
			}

			@Override
			public void onTick(long millisUntilFinished) {
				// TODO Auto-generated method stub
			}
			
		};
				
		
	    if(!cancel){
	    	Vibrator vib = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		    vib.vibrate(1000);
	    	showNotification();
		    try {
				DaemonService.mConsumer.notificationTimeout(0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }	    
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.feedback, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.stat:
    		/*	int fn = pM.getFalseEventCounter();
    			int tn = pM.getEventCounter();
    			
    		new AlertDialog.Builder(this).setTitle("Statistics").setMessage("Total unlock: "+tn+"\nFalse unlock: "+fn)
    		.setCancelable(false).setNegativeButton("Got it", new DialogInterface.OnClickListener()
    		{	// When choose not to open GPS, give a notice of disability
    			public void onClick(DialogInterface dialog, int which)
    			{
    				dialog.cancel();
    			}
    		}).show(); */
    		break;
    		
    		case R.id.unlock:
    			break;
    	default:
    		break;
    	}
		return true;
    	
    }
	
	@Override
	public void onResume(){
		super.onResume();
		
		//cancelNotification();
		if(!cancel){
		    timer.start();			
		}
	}
	
	
	@Override
	public void onStop(){
		
		log.info("onStop");
		if(!cancel){
			timer.cancel();
		}
		if(wakeLock.isHeld()){
			wakeLock.release();
		}
		
        keyguardLock.reenableKeyguard();
	    super.onStop();	    
	}
	
	/**
     * The notification is the icon and associated expanded entry in the
     * status bar.
     */
    protected void showNotification() {
        // look up the notification manager service
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        // The details of our fake message
        CharSequence from = "BlueProximity";
        CharSequence message = "pending feedback";
        
        Intent intent = new Intent(this, FeedbackActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("event", event);
        intent.putExtra("decision", decision);
        intent.putExtra("ts", ts);
        intent.putExtra("cancel", true);
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // The ticker text, this uses a formatted string so our message could be localized
        String tickerText = getString(R.string.imcoming_message_ticker_text, message);

        // construct the Notification object.
        Notification notif = new Notification(R.drawable.stat_sample, tickerText,
                System.currentTimeMillis());

        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo(this, from, message, contentIntent);

        // after a 100ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
        //notif.vibrate = new long[] { 100, 250, 100, 500};

        // Note that we use R.layout.incoming_message_panel as the ID for
        // the notification.  It could be any integer you want, but we use
        // the convention of using a resource id for a string related to
        // the notification.  It will always be a unique number within your
        // application.
        nm.notify(R.string.imcoming_message_ticker_text, notif);
        
        Log.e("FeedbackActivity", "Notification shown");
        
    }
    
    protected void cancelNotification(){
    	// look up the notification manager service
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // cancel the notification that we started in IncomingMessage
        nm.cancel(R.string.imcoming_message_ticker_text);
    }

}
