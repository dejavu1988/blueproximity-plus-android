package com.example.mcbp.bind;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.example.mcbp.BindActivity;
import com.example.mcbp.bak.BluetoothClient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothUtil {

	// Debugging
    private static final String TAG = "BluetoothUtil";
    private static final boolean D = true;
    
	// Unique UUID for this application
    private static final UUID MY_UUID =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final String NAME = "BP-PLUS";
    
    private Context mContext;
    private final BluetoothAdapter mBluetoothAdapter;
    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectedThread;
    private Handler mHandler;
    private int mState;
    
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    //public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
	public BluetoothUtil(Context context, Handler handler) {
		// TODO Auto-generated constructor stub
		mContext = context;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = handler;
		mState = STATE_NONE;
	}
	
	/**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BluetoothClient.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        mAcceptThread = new AcceptThread();
    	mAcceptThread.start();
    	
    }
    
    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mAcceptThread != null) {
        	mAcceptThread.cancel();
        	mAcceptThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }        

        setState(STATE_NONE);
    }
    
    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        /*Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/

        // Start the service over to restart listening mode
        BluetoothUtil.this.start();
    }
    
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void manageConnectedSocket(BluetoothSocket socket){    	
    	BluetoothDevice device = socket.getRemoteDevice();
    	if (D) Log.d(TAG, "Socket connected to " + device.getName());
    	
    	// Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
        	mAcceptThread.cancel();
        	mAcceptThread = null;
        }
        
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }        

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BindActivity.MESSAGE_DEVICE);
        Bundle bundle = new Bundle();
        bundle.putString(BindActivity.DEVICE_NAME, device.getName());
        bundle.putString(BindActivity.DEVICE_ADDR, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }
        
	private class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
	        } catch (IOException e) { 
	        	Log.e(TAG, "Socket listen() failed", e);
	        }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	    	if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
	        BluetoothSocket socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (mState != STATE_CONNECTED) {
	            try {
	                socket = mmServerSocket.accept();
	            } catch (IOException e) {
	            	Log.e(TAG, "Socket accept() failed", e);
	                break;
	            }
	            // If a connection was accepted
	            if (socket != null) {
	            	synchronized (BluetoothUtil.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        	// Do work to manage the connection (in a separate thread)
                        	manageConnectedSocket(socket);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }	                
	            }
	        }
	        if (D) Log.i(TAG, "END mAcceptThread");
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	    	if (D) Log.d(TAG, "Server Socket cancel " + this);
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) {
	        	Log.e(TAG, "Server Socket close() failed", e);
	        }
	    }
	}
	
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	    	Log.d(TAG, "create ConnectedThread " + this);
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { 
	        	Log.e(TAG, "temp sockets not created", e);
	        }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	    	Log.d(TAG, "BEGIN mConnectedThread");
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	        boolean done = false;
	        
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                
	                String readMessage = new String(buffer, 0, bytes, "UTF-8");
	                Log.d(TAG, "Received: " + readMessage);
	                
	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(BindActivity.MESSAGE_READ, bytes, -1, buffer)
	                        .sendToTarget();
	            } catch (IOException e) {
	            	Log.e(TAG, "disconnected", e);
                    connectionLost();
	                break;
	            }	            
	            
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	            String wroteMessage = new String(bytes);
                Log.d(TAG, "Sent: " + wroteMessage);
	        } catch (IOException e) {
	        	Log.e(TAG, "Exception during write", e);
	        }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	    	if (D) Log.d(TAG, "Socket cancel " + this);
	        try {
	            mmSocket.close();
	        } catch (IOException e) {
	        	Log.e(TAG, "close() of connect socket failed", e);
	        }
	    }
	}

}
