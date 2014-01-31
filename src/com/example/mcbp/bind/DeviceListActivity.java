package com.example.mcbp.bind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import com.example.mcbp.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;
    private static final boolean DEBUG_SHOW_BOND_LIST = false;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String TARGET_SERVICE_ID = "94f39d29-7d6d-437d-973b-fba39e49d4ee";
    // Member fields
    private BluetoothAdapter mBtAdapter;
    //private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ArrayList<BluetoothDevice> btDeviceList = new ArrayList<BluetoothDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        //mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
       
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	
    	doDiscovery();
    	
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        btDeviceList.clear();

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();
            btDeviceList.clear();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
    
   
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            	if (D) Log.d(TAG, "Device found: " + device.getName() );
                // If it's already paired, skip it, because it's been listed already
            	if(DEBUG_SHOW_BOND_LIST){	// Show all devices
            		btDeviceList.add(device);
            	}else{	// Show unbond devices
            		if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        //mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        btDeviceList.add(device);
                    }
            	}                
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                
                Iterator<BluetoothDevice> itr = btDeviceList.iterator();
                while (itr.hasNext()) {
                	// Get Services for paired devices
                	BluetoothDevice device = itr.next();
                  	if (D) Log.d(TAG, "Device got: " + device.getName() );
                  	/*Method method;
                  	ParcelUuid[] uuids = null;
					try {
						method = device.getClass().getMethod("getUuids", null);
		                uuids = (ParcelUuid[]) method.invoke(device, null);
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
                  /*ParcelUuid[] uuids = servicesFromDevice(device); 
                  if(uuids != null){
                	  if (D) Log.d(TAG, "UUIDs got: " + uuids.length);
                	  for (ParcelUuid pUuid: uuids) {
                		  if (D) Log.d(TAG, "UUIDs: " + pUuid.toString());
                    		if(pUuid.equals(ParcelUuid.fromString(TARGET_SERVICE_ID))){
                    			if (D) Log.d(TAG, "Match got.");
                    			mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    			break;
                    		}
                	  }
                  }else*/{
                	  if (D) Log.d(TAG, "Null uuids: fetchUuidsWithSdp");
                	  //servicesFromDevice(device);
                	  if(!device.fetchUuidsWithSdp()) {
                    	  if (D) Log.d(TAG, "SDP discovery failed for " + device.getName());
                      }
                	  
                	  //mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                  }
              	
                 /* if(!device.fetchUuidsWithSdp()) {
                	  if (D) Log.d(TAG, "SDP discovery failed for " + device.getName());
                  } */             
                }
                /*if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);*/
                
            }else if(BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if (D) Log.d(TAG, "Device got: " + device.getName());
                if(uuidExtra != null){
                	if (D) Log.d(TAG, "Action UUID: " + uuidExtra.length);
                    for (int i=0; i<uuidExtra.length; i++) {
                    	if (D) Log.d(TAG, "Action UUID: " + uuidExtra[i].toString());
                		if(uuidExtra[i].toString().contains(TARGET_SERVICE_ID)){
                			if (D) Log.d(TAG, "Match got.");
                			mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                			DeviceListActivity.this.setProgressBarIndeterminateVisibility(false);
                			DeviceListActivity.this.setTitle(R.string.select_device);
                			break;
                		}
                	}
                }else{
                	if (D) Log.d(TAG, "Null uuids: fetchUuidsWithSdp");
                	if(!device.fetchUuidsWithSdp()) {
                  	  if (D) Log.d(TAG, "SDP discovery failed for " + device.getName());
                    }
                	/*if (D) Log.d(TAG, "Null uuids got.");
                	mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        			DeviceListActivity.this.setProgressBarIndeterminateVisibility(false);
        			DeviceListActivity.this.setTitle(R.string.select_device);*/
                }
                
            }
        }
    };
    
    public ParcelUuid[] servicesFromDevice(BluetoothDevice device) {
        try {
            Class cl = Class.forName("android.bluetooth.BluetoothDevice");
            Class[] par = {};
            Method method = cl.getMethod("getUuids", par);
            Object[] args = {};
            ParcelUuid[] retval = (ParcelUuid[]) method.invoke(device, args);
            return retval;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    

}
