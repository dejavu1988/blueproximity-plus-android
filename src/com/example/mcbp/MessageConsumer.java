package com.example.mcbp;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.example.mcbp.connection.IConnectToRabbitMQ;
import com.example.mcbp.crypt.CryptUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.QueueingConsumer;

/**
 *Consumes messages from a RabbitMQ broker
 *
 */
public class MessageConsumer extends IConnectToRabbitMQ{
 
	// Debugging
    private static final String TAG = "MessageConsumer";
    private static final boolean D = true;
    
    //The Queue name for this consumer
    private Context mContext;
    private String mQueueRecv, mQueueSend;
    private String mIdRecv, mIdSend;
    private QueueingConsumer MySubscription;
    private HashMap<String,String> msgObj;
	private Gson gson;
    private Type mapType;
    //private PrefManager pM;
    private PrefManager pM;
    private boolean running;
    
    public MessageConsumer(Context context, String server, String idRecv, String idSend) {
        super(server);
        mContext = context;
        mIdRecv = idRecv;
        mIdSend = idSend;
        mQueueRecv = getHmac(mIdRecv);
        mQueueSend = getHmac(mIdSend);
        gson = new Gson();
        mapType = new TypeToken<HashMap<String,String>>(){}.getType();
        //pM = new PrefManager(mContext);
        pM = new PrefManager(context);
        running = false;
    }
 
    
 
    //last message to post back
    private byte[] mLastMessage;
 
    // An interface to be implemented by an object that is interested in messages(listener)
    /*public interface OnReceiveMessageHandler{
        public void onReceiveMessage(byte[] message);
    };*/
 
    //A reference to the listener, we can only have one at a time(for now)
    /*private OnReceiveMessageHandler mOnReceiveMessageHandler;*/
 
    /**
     *
     * Set the callback for received messages
     * @param handler The callback
     */
    /*public void setOnReceiveMessageHandler(OnReceiveMessageHandler handler){
        mOnReceiveMessageHandler = handler;
    };*/
 
    // One handler for the thread
    private Handler mHandler = new Handler();
    //private Handler mMessageHandler = new Handler();
    //private Handler mConsumeHandler = new Handler();
    //private Handler mConsumeExceptionHandler = new Handler();
    //private Handler mConnectExceptionHandler = new Handler();
    
 
    // Create runnable for posting back to main thread
    /*final Runnable mReturnMessage = new Runnable() {
        public void run() {
            mOnReceiveMessageHandler.onReceiveMessage(mLastMessage);
        }
    };*/
    final Runnable mReturnMessage = new Runnable() {
        public void run() {
        	String content = StringUtils.stripEnd(new String(mLastMessage), null);
        	if(D) Log.e(TAG, "msg received: "+ content);
        	String[] received = StringUtils.split(content, "$");
        	//if(D) Log.e(TAG, "msg received: "+ received.length);
        	if(received.length >= 2){
        		String msg = received[0];
        		String msg_hmac = received[1];
        		String tmp_hmac = getHmac(msg);
        		if(msg_hmac.equalsIgnoreCase(tmp_hmac)){
        			
        			msgObj = gson.fromJson(msg, mapType);
                	String id = msgObj.get("id");
                	if(id.contains("RS")){
                		String event = msgObj.get("event");
                		String decision = msgObj.get("val");
                		String ts = msgObj.get("ts");
                		if(event.contains("Y") && decision.contains("T")){
                			//event: unlock, decision: colocated
                			if(D) Log.e(TAG, "event: unlock, decision: colocated");
                			AskFeedback(true, true, ts);
                		}else if(event.contains("Y") && decision.contains("F")){
                			//event: unlock, decision: non-colocated
                			if(D) Log.e(TAG, "event: unlock, decision: non-colocated");
                			AskFeedback(true, false, ts);
                		}else if(event.contains("N") && decision.contains("T")){
                			//event: lock, decision: colocated
                			if(D) Log.e(TAG, "event: lock, decision: colocated");
                			AskFeedback(false, true, ts);
                		}else if(event.contains("N") && decision.contains("F")){
                			//event: lock, decision: non-colocated
                			if(D) Log.e(TAG, "event: lock, decision: non-colocated");
                			AskFeedback(false, false, ts);
                		}
                		
                	}else if(id.contains("SCAN")){
                		String ts = msgObj.get("ts");
                		Purge();
                		Scan(ts);
                	}/*else if(id.contains("ID")){
                		String uid = msgObj.get("uid");
                		pM.updateBindID(uid);
                		Purge();
                		sendUuid();
                	}*/
                	
        		}else{
        			if(D) Log.e(TAG, "HMAC unmatches.");
        		}
        	}
        	
        }
    };
    
    final Runnable mConsumeRunner = new Runnable() {
        public void run() {
            Consume();
        }
    };
    
    final Runnable mReconnectRunner = new Runnable() {
    	public void run() {
    		connectToRabbitMQ();
    	}
    };
    
    final Runnable mWaitForNetworkOnRunner = new Runnable() {
    	public void run() {
    		WaitForNetworkOn();
    	}
    };
    
 
    /**
     * Create Exchange and then start consuming. A binding needs to be added before any messages will be delivered
     */
    //@Override
    public void connectToRabbitMQ()
    {
    	Thread thread = new Thread(){
    		@Override
            public void run(){
    			if(MessageConsumer.super.iConnectToRabbitMQ())
    		       {
    		    	   if(D) Log.e(TAG, "super connection successful");
    		           try {
    		               //mQueue = mModel.queueDeclare().getQueue();
    		        	   mModel.queueDeclare(mQueueRecv, false, false, false, null);
    		        	   mModel.queueDeclare(mQueueSend, false, false, false, null);
    		        	   mModel.queuePurge(mQueueRecv);
    		        	   mModel.queuePurge(mQueueSend);
    		        	   if(D) Log.e(TAG, "connection: queue declared and purged");
    		               MySubscription = new QueueingConsumer(mModel);
    		               mModel.basicConsume(mQueueRecv, true, MySubscription);
    		            } catch (IOException e) {
    		                //e.printStackTrace();
    		            	mHandler.post(mReconnectRunner);
    		                //return false;
    		            }
    		             
    		            Running = true;
    		            //mConsumeHandler.post(mConsumeRunner);
    		            mHandler.post(mConsumeRunner);
    		 
    		           //return true;
    		       }else{
    		    	   if(D) Log.e(TAG, "super connection failed");  
        		       mHandler.post(mWaitForNetworkOnRunner);
        		       //return false;
    		       }
    				
    		}
    	};
    	thread.start();
       
       
    }
 
    /*
     * Purge context info from queueSend
     */
    private void Purge(){
    	Thread thread = new Thread(){
    		@Override
            public void run(){
    			if(running){
    				try {    				
        				mModel.queuePurge(mQueueSend);
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					//e.printStackTrace();
    					running = false;
    					mHandler.post(mReconnectRunner);
    				}
    			}    			
    		}
    	};
    	thread.start();
    }
    
    /*
     * Scan context info by starting WOrkService
     */
    private void Scan(final String ts){
    	Thread thread = new Thread(){
    		
    		@Override
            public void run() {
    			Intent taskIntent = new Intent(mContext, WorkerService.class);
    			taskIntent.putExtra("ts", ts);
    			mContext.startService(taskIntent); 
    		}    		
    	};    	
    	thread.start();
    }
    
    /*
     * Given event (lock/unlock), go to feedback activity
     */
    private void AskFeedback(final boolean e, final boolean d, final String ts){
    	Thread thread = new Thread(){
    		
    		@Override
            public void run() {
        		Intent feedIntent = new Intent(mContext, FeedbackActivity.class);
        		feedIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		feedIntent.putExtra("event", e);
        		feedIntent.putExtra("decision", d);
        		feedIntent.putExtra("ts", ts);
        		feedIntent.putExtra("cancel", false);
        		mContext.startActivity(feedIntent); 
    		}    
    	};    	
    	thread.start();
    }
    
    /*
     * Consumer to receive msg and post to handler
     */
    private void Consume(){
        Thread thread = new Thread()
        { 
             @Override
             public void run() {
            	 if(D) Log.e(TAG, "consume");
            	 try{
            		 while(Running){
                         QueueingConsumer.Delivery delivery;
                         try {
                         	running = true;
                             delivery = MySubscription.nextDelivery();
                             mLastMessage = delivery.getBody();
                             //if(D) Log.e(TAG, "msg received: " + new String(mLastMessage));
                             //mMessageHandler.post(mReturnMessage);
                             mHandler.post(mReturnMessage);
                         } catch (InterruptedException ie) {
                             //ie.printStackTrace();
                         	running = false;
                         }
                      }
            	 }catch(Exception e){
            		 running = false;
            		 //mConsumeExceptionHandler.post(mReconnectRunner);
            		 mHandler.post(mReconnectRunner);
            	 }
                 
             }
        };
        thread.start();
 
    }
    
    /*
     * Send message
     */
    private void Publish(final String message){
    	Thread thread = new Thread(){
    		@Override
            public void run(){
    			if(running){
    				String signedMessage = StringUtils.stripEnd(message, null) + "$" + getHmac(message) + "$";
    				try {    				
    					mModel.basicPublish("", mQueueSend, null, signedMessage.getBytes());
    					if(D) Log.e(TAG, "msg sent: " + signedMessage);
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					//e.printStackTrace();
    					running = false;
    					mHandler.post(mReconnectRunner);
    				}
    			}    			
    		}
    	};
    	thread.start();
    }
    
    /*
     * Send feedback msg using Publish()
     */
    public void sendFeedback(String fb, String ts){
    	HashMap<String,String> tmpMsgObj = new HashMap<String,String>();
    	tmpMsgObj.put("id",	"FB");
    	tmpMsgObj.put("val", fb);
    	tmpMsgObj.put("ts", ts);
    	String tmpAck = gson.toJson(tmpMsgObj);
    	Publish(tmpAck);
    }
    
    /*
     * Send CSV msg using Publish()
     */
    public void sendCSV(String ts, String csv){
    	HashMap<String,String> tmpMsgObj = new HashMap<String,String>();
    	tmpMsgObj.put("id",	"CSV");
    	tmpMsgObj.put("ts", ts);
    	tmpMsgObj.put("val", csv);
    	String tmpAck = gson.toJson(tmpMsgObj);
    	Publish(tmpAck);
    }
    
    /*
     * Send Uuid msg using Publish()
     */
    public void sendUuid(){
    	HashMap<String,String> tmpMsgObj = new HashMap<String,String>();
    	tmpMsgObj.put("id",	"ID");
    	tmpMsgObj.put("uid", pM.getUUID());
    	String tmpAck = gson.toJson(tmpMsgObj);
    	Publish(tmpAck);
    }
    
    /*
     * Send WAV msg using Publish()
     */
    public void sendWAV(String ts, String wav){
    	HashMap<String,String> tmpMsgObj = new HashMap<String,String>();
    	tmpMsgObj.put("id",	"WAV");
    	tmpMsgObj.put("ts", ts);
    	tmpMsgObj.put("val", wav);
    	String tmpAck = gson.toJson(tmpMsgObj);
    	Publish(tmpAck);
    }
    
    public void notificationTimeout(final int t) throws InterruptedException{
		Thread thread = new Thread(){
			@Override
			public void run(){
				try {
					Thread.sleep(20000-t);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(D) Log.e(TAG, "notification timeout");
				// look up the notification manager service
		        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		        // cancel the notification that we started in IncomingMessage
		        nm.cancel(R.string.imcoming_message_ticker_text);
			}
		};
		thread.start();
	}
    
    public void dispose(){
        Running = false;
        //Dispose();
    }
    
    
    private void WaitForNetworkOn(){
        Thread thread = new Thread()
        { 
             @Override
             public void run() {
            	 if(D) Log.e(TAG, "check network");
            	 boolean flag = true;
            	 while(flag){
            		 if(DaemonService.IsReachable(mContext)){
            			 flag = false;
            			 //mConnectExceptionHandler.post(mReconnectRunner);
            			 mHandler.post(mReconnectRunner);
            		 }
            		 try {
						Thread.sleep(40000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
            	 }                 
             }
        };
        thread.start();
 
    }
    
    private String getHmac(String msg){
    	return CryptUtil.hmac(msg, mIdSend+mIdRecv);
    }
    
}
