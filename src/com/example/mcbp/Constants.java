package com.example.mcbp;

public final class Constants {
	
	
	// Sensor Status: null sensor, gps, wifi, bluetooth, audio...
	public static final int STATUS_SENSOR_NULL = 0;
	public static final int STATUS_SENSOR_GPS = 1;
	public static final int STATUS_SENSOR_WIFI = 2;
	public static final int STATUS_SENSOR_BT = 4;
	public static final int STATUS_SENSOR_AUDIO = 8;
	public static final int STATUS_SENSOR_GPSCOORD = 16;
	public static final int STATUS_SENSOR_GWBA = 15;
	
	// Scan params
	public static final int AUDIO_PERIOD = 10;
	public static final int BT_LOCAL_RSSI = -40;
	
	// Server address
	public static final String SERVER_INET = "54.229.32.28"; 	
	public static final String UPLOAD_SERVER_INET = "54.229.32.28"; 
	public static final String UPLOAD_SERVER_DIR = "/bp/server/upload_bp.php";
	public static final String UPLOAD_SERVER_LOG_DIR = "/bp/server/upload_bp_log.php";
	public static final int SERVER_PORT = 4343; 
    
	// Message Ids
    public static final String REQ_UUID = "REQ_UUID";
    public static final String ACK_UUID = "ACK_UUID";
    public static final String UP_BIND = "UP_BIND";
    public static final String REQ_GETQ = "REQ_GETQ";
    public static final String ACK_GETQ = "ACK_GETQ";
    public static final String REQ_VALQ = "REQ_VALQ";
    public static final String ACK_VALQ = "ACK_VALQ";
    public static final String REQ_ALIVE = "REQ_ALIVE";
    public static final String ACK_ALIVE = "ACK_ALIVE";
    public static final String REQ_UNBIND = "REQ_UNBIND";
    public static final String ACK_UNBIND = "ACK_UNBIND";
    public static final String REQ_SCAN = "SCAN";
    public static final String ACK_SCAN = "SCAN";
    public static final String UNLOCK = "UNLOCK";
    
}
