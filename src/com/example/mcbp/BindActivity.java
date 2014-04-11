package com.example.mcbp;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.example.mcbp.bind.BluetoothUtil;
import com.example.mcbp.log.ConfigureLog4J;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class BindActivity extends Activity {

	// Debugging
    private static final String TAG = "BindActivity";
    private static final boolean D = true;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE = 4;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDR = "device_addr";
    
    Logger log;
    
    private TextView mBindStatus;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothUtil mConnService = null;
    private PrefManager pM;
    private boolean bindstatus; 
    //private Handler mHandler;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.activity_main);

        log = Logger.getLogger(BindActivity.class);  
        ConfigureLog4J.configure(this);  
        LogManager.getRootLogger().setLevel((Level)Level.DEBUG);
        
        mBindStatus = (TextView) findViewById(R.id.bindstatus);
        
        pM = new PrefManager(getApplicationContext());
        if(pM.isBond()){
        	bindstatus = true;
        	mBindStatus.setText("Bound to " + pM.getBindName() +":"+ pM.getBindAddr());
        	if(!(DaemonService.mConsumer!= null && DaemonService.mConsumer.isRunning())){
	        	Intent intentToService = new Intent(this, DaemonService.class);
	            startService(intentToService);
	            log.info("Intent to DaemonService started"); 
        	}
            
        }else{
        	bindstatus = false;
        	mBindStatus.setText("Not Bound");
        }
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        	
    }
	
	@Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        // Otherwise
        } else {
        	setup();      		
        }
    }
	
	@Override
	public synchronized void onResume(){
		super.onResume();		
	}
	
	private void bind(){
		// Stop DaemonService
		if(DaemonService.mConsumer!= null){
			if(DaemonService.mConsumer.isRunning()){
				Intent intentToService = new Intent(getApplicationContext(), DaemonService.class);
                stopService(intentToService);
			}
		}
		
		// Clean up
		pM.clearBind();
		bindstatus = false;
    	mBindStatus.setText("Not Bound");
    	
		if (mConnService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mConnService.getState() == mConnService.STATE_NONE) {
              // Start the Bluetooth chat services
            	mConnService.start();
            }
        }
	}
	
	public void setup(){
		if(!pM.isBTRegistered()){
    		pM.updateLocalBT(mBluetoothAdapter.getName(), mBluetoothAdapter.getAddress());
    	}
		if(mConnService == null)
			mConnService = new BluetoothUtil(this, mHandler);
	}
	
	private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case 1:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
            	setup();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            }
        }
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            Toast.makeText(this, "set visible for 300s", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.unlock:        	
        	//manually trigger lock in case of FalseNegative
        	if(bindstatus){
            	DaemonService.mConsumer.sendFeedback("FN","");        		
        	}
        	break;
        case R.id.bind:
        	bind();
        	break;
        }
        return false;
    }
    
    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }
    
    // The Handler that gets information back from the BluetoothUtil
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothUtil.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to));
                    break;
                case BluetoothUtil.STATE_LISTEN:
                	setStatus(getString(R.string.title_connecting));
                    break;
                case BluetoothUtil.STATE_NONE:
                    setStatus(getString(R.string.title_not_connected));
                    break;
                }
                break;
            case MESSAGE_WRITE:
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                if(readMessage.contains(":")){
                	String[] tmps = readMessage.split(":");
                	if(tmps[0].equalsIgnoreCase("ID")){
                		pM.updateBindID(tmps[1]);
                		mConnService.write(pM.getUUID().getBytes());
                	}else if(tmps[0].equalsIgnoreCase("DONE")){
                		bindstatus = true;
                    	mBindStatus.setText("Bound to " + pM.getBindID());
                		mConnService.stop();
                		Intent intentToService = new Intent(getApplicationContext(), DaemonService.class);
                        startService(intentToService);
                        log.info("Intent to DaemonService started"); 
                	}
                }
                
                break;
            case MESSAGE_DEVICE:
            	String device_name = msg.getData().getString(DEVICE_NAME);
            	String device_addr = msg.getData().getString(DEVICE_ADDR);
            	pM.updateBindBT(device_name, device_addr);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + device_name, Toast.LENGTH_SHORT).show();
            	break;
            }
        }
    };
}
