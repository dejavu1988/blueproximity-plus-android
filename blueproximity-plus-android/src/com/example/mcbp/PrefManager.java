package com.example.mcbp;

import org.apache.log4j.Logger;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PrefManager {
	// Shared Preferences
    private SharedPreferences pref;
     
    // Editor for Shared preferences
    private Editor editor;
     
    // Context
    private Context _context;

    // Shared pref mode
    private int PRIVATE_MODE = 0;
    
    //Shared 
    private static final String PREF_NAME = "BPPref";
    
    // local UUID
    private static final String ID = "DeviceID";
    
    // local name
    private static final String NAME = "DeviceName";
    
    // local address
    private static final String ADDR = "DeviceAddr";
    
    // Bound device UUID
    private static final String BINDID = "TerminalID";
    
    // Bound device name
    private static final String BINDNAME = "TerminalName";
    
    // Bound device address
    private static final String BINDADDR = "TerminalAddr";
    
    // Total UNLOCK event counter
    private static final String EventCounter = "EventCounter";
        
    // Total NEGATIVE UNLOCK event counter
    private static final String FalseEventCounter = "FalseEventCounter";
                               
    Logger log;
    
    
    // Constructor
    public PrefManager(Context context){
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
        //_pref = PreferenceManager.getDefaultSharedPreferences(_context);
        //_editor = _pref.edit();        
    }    
        
    /**
     * update local UUID
     * */
    public void updateUUID(String uuid){
        
        editor.putString(ID, uuid);
         
        // commit changes
        editor.commit();
    }  
    
    /**
     * update local Name
     * */
    public void updateName(String name){
        
        editor.putString(NAME, name);
         
        // commit changes
        editor.commit();
    } 
    
    /**
     * update local Address
     * */
    public void updateAddr(String addr){
        
        editor.putString(ADDR, addr);
         
        // commit changes
        editor.commit();
    } 
    
    /**
     * update local device info
     * */
    public void updateLocal(String id, String name, String addr){
        
    	// Storing bind uuid as string
        editor.putString(ID, id);
         
        // Storing bind name as string
        editor.putString(NAME, name);
        
        // Storing bind address as string
        editor.putString(ADDR, addr);
                
        // commit changes
        editor.commit();
    }    
    
    public void updateLocalBT(String name, String addr){
        
        // Storing bind name as string
        editor.putString(NAME, name);
        
        // Storing bind address as string
        editor.putString(ADDR, addr);
                
        // commit changes
        editor.commit();
    }    
    
    
    /**
     * update bind
     * */
    public void updateBind(String id, String name, String addr){
        
    	// Storing bind uuid as string
        editor.putString(BINDID, id);
         
        // Storing bind name as string
        editor.putString(BINDNAME, name);
        
        // Storing bind address as string
        editor.putString(BINDADDR, addr);
                
        // commit changes
        editor.commit();
    }
    
    public void updateBindBT(String name, String addr){
        
        // Storing bind name as string
        editor.putString(BINDNAME, name);
        
        // Storing bind address as string
        editor.putString(BINDADDR, addr);
                
        // commit changes
        editor.commit();
    }
    
    public void updateBindID(String id){
        
    	// Storing bind uuid as string
        editor.putString(BINDID, id);
         
        // commit changes
        editor.commit();
    }
    
    /**
     * check UUID: 
     * @return UUID as string
     * */
    public String getUUID(){
		return pref.getString(ID, "");     
    }  
    
    public String getName(){
		return pref.getString(NAME, "");     
    } 
    
    public String getAddr(){
		return pref.getString(ADDR, "");     
    }
         
    public String getBindID(){
		return pref.getString(BINDID, "");     
    } 
    
    public String getBindName(){
		return pref.getString(BINDNAME, "");     
    } 
    
    public String getBindAddr(){
		return pref.getString(BINDADDR, "");     
    } 
    
    public boolean isBond(){
		return (pref.getString(BINDID, "") != "");     
    } 
    
    public boolean isUuidRegistered(){
		return (pref.getString(ID, "") != "");     
    } 
    
    public boolean isBTRegistered(){
		return (pref.getString(NAME, "") != "");     
    } 
    
    public void clearBind(){
    	updateBind("","","");
    }
    
    public int getEventCounter(){
    	return pref.getInt(EventCounter, 0);
    }
    
    public int getFalseEventCounter(){
    	return pref.getInt(FalseEventCounter, 0);
    }
    
    public void updateEventCounter(){
    	editor.putInt(EventCounter, getEventCounter() + 1);
    	editor.commit();
    }
    
    public void updateFalseEventCounter(){
    	editor.putInt(FalseEventCounter, getFalseEventCounter() + 1);
    	editor.commit();
    }
                
}
