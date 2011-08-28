package org.bodytrack.BodyTrack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bodytrack.BodyTrack.Activities.HomeTabbed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;


/** 
 * This class defines a service which runs in the background on
 * the phone to capture location data. It is controlled by the 
 * activity defined by GpsSvccontrol.java
 */
public class BTService extends Service implements PreferencesChangeListener{
	/*constants*/
	public static final String TAG = "BTService";
	
	private static final int SENSOR_TYPE_GRAVITY = 9;
	private static final int SENSOR_TYPE_LINEAR_ACCELERATION = 10;
	
	private final long minDistance = 0;
	
	private int gpsDelay;
	private int sensorDelay;
	
	private boolean gpsIdleMode = false;
	private boolean gpsNoFixSleepMode = false;
	
	private final static long GPS_MAX_FIX_TIME = 1000 * 20;
	private final static long GPS_MIN_TIME_FOR_SLEEP = 1000 * 30;
	
	private boolean fixFound = false;
	
	private final static int[] sensorDelays = {SensorManager.SENSOR_DELAY_UI, SensorManager.SENSOR_DELAY_NORMAL,  
						SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_FASTEST};
	private final static String[] sensorDelayNames = {"Slow", "Normal", "Fast", "Fastest"};
	
	private Handler gpsHandler;
	
	private boolean accSplitEnabled = false;
	
	private final static long[] gpsDelays = {0,1000,5000,10000,20000,30000,60000,120000,300000,600000,900000,1200000,1800000,3600000};
	
	private final static String[] gpsDelayNames = {"Realtime","Every Second","Every 5 Seconds","Every 10 Seconds", "Every 20 Seconds",
									"Every 30 Seconds", "Every Minute", "Every 2 Minutes", "Every 5 Minutes", "Every 10 Minutes",
									"Every 15 minutes", "Every 20 Minutes", "Every Half Hour", "Every Hour"};
	
	private boolean lowBattery = false;
	private boolean lowStorage = false;
	private boolean noTrackLowMode = false;	
	private boolean noTrackLowStoreMode = false;
	
	
	private LocationManager locMan;
	private SensorManager senseMan;
	private WifiManager wifiManager;
	private ConnectivityManager conMan;
	private PowerManager powerMan;
	
	private BTStatisticTracker btStats;
	
	private PowerManager.WakeLock wakeLock;
	
	private WifiScanTask curWifiScanner;
	private DbDataWriter dbUpdaterTask;
	private WifiWatcher wWatcher;
	
	private boolean[] isLogging = new boolean[NUM_LOGGERS];
	
	protected DbAdapter dbAdapter;
	

	private NotificationManager notMan;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private static final int NOTIFICATION = 5;
	
	public static final int NUM_LOGGERS = 8;
	
	public static final int GPS_LOGGING = 0;
	public static final int ACC_LOGGING = 1;
	public static final int GYRO_LOGGING = 2;
	public static final int WIFI_LOGGING = 3;
	public static final int TEMP_LOGGING = 4;
	public static final int ORNT_LOGGING = 5;
	public static final int LIGHT_LOGGING = 6;
	public static final int PRESS_LOGGING = 7;
	
	private static final int GRAVITY_ACC = NUM_LOGGERS;
	private static final int LINEAR_ACC = NUM_LOGGERS + 1;
	
	private boolean foregroundEnabled = false;
	
	private String[] logNames = {"GPS", "Accelerometer", "Gyroscope", "WiFi", "Temperature",
			"Orientation", "Illuminance", "Barometric Pressure"
	};
	
	private List<List<Object[]>> dataLists = new ArrayList<List<Object[]>>();
	
	private PreferencesAdapter prefAdapter;
	private boolean externalPowerPresent = false;
	private boolean externalPowerIsAC = false;

		
	@Override
	public void onCreate() {
		wWatcher = new WifiWatcher();
		gpsHandler = new Handler();
		super.onCreate();
    	Log.v(TAG, "Starting GPS service");
    	
    	for (int i = 0; i < NUM_LOGGERS + 2; i++){
    		dataLists.add(new LinkedList<Object[]>());
    	}
    	
    	btStats = BTStatisticTracker.getInstance();
    	btStats.out.println("BTService Started");
				
		/*Get an instance of the location manager*/
		locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		senseMan = (SensorManager) getSystemService(SENSOR_SERVICE);
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		powerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BTService WakeLock");
		
		dbAdapter = DbAdapter.getDbAdapter(getApplicationContext());
		
		
	    notMan = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    try {
	        mStartForeground = getClass().getMethod("startForeground",
	                mStartForegroundSignature);
	        mStopForeground = getClass().getMethod("stopForeground",
	                mStopForegroundSignature);
	    } catch (NoSuchMethodException e) {
	        // Running on an older platform.
	        mStartForeground = mStopForeground = null;
	    }
	    
	    prefAdapter = PreferencesAdapter.getInstance(getApplicationContext());
	    prefAdapter.addPreferencesChangeListener(this);
	    
	    gpsDelay = prefAdapter.getGPSDelay();
	    sensorDelay = prefAdapter.getSensorDelay();
	    loadSensorSettings();
	    
	    dbUpdaterTask = new DbDataWriter();
	    dbUpdaterTask.execute();
	    
	    registerReceiver(lowBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_LOW));
	    registerReceiver(batteryOkReceiver, new IntentFilter(Intent.ACTION_BATTERY_OKAY));
	    registerReceiver(lowStorageReceiver, new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW));
	    registerReceiver(storageOkReceiver, new IntentFilter(Intent.ACTION_DEVICE_STORAGE_OK));
	    registerReceiver(externalPowerReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    new UploaderTask().execute();
	}
	
	private void loadSensorSettings(){
		int value = prefAdapter.getSensorSettings();
		for (int i = 0; i < NUM_LOGGERS; i++){
			if ((value & (2 << i)) != 0)
				startLogging(i);
		}
	}
	
	private void saveSensorSettings(){
		int value = 0;
		for (int i = 0; i < NUM_LOGGERS; i++){
			if (isLogging(i))
				value |= 2 << i;
		}
		prefAdapter.setSensorSettings(value);
	}
	
	public static String[] getAllGpsDelayNames(){
		return gpsDelayNames.clone();
	}
	
	
	public static long getGPSDelayValue(int index){
		try{
			return gpsDelays[index];
		}
		catch (ArrayIndexOutOfBoundsException e){
			return gpsDelays[0];
		}
	}
	
	public static String getGPSDelayName(int index){
		try{
			return gpsDelayNames[index];
		}
		catch (ArrayIndexOutOfBoundsException e){
			return gpsDelayNames[0];
		}
	}
	
	public boolean canSplitAcc(){
		return senseMan.getDefaultSensor(SENSOR_TYPE_GRAVITY) != null && senseMan.getDefaultSensor(SENSOR_TYPE_LINEAR_ACCELERATION) != null;
	}
	
	private void setGPSDelay(int index){
		if (gpsDelay != index){
			gpsDelay = index;
			prefAdapter.setGPSDelay(index);
			if (isLogging(GPS_LOGGING)){
				locMan.removeUpdates(locListen);
				if (!gpsIdleMode){
					locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, getGPSDelayValue(gpsDelay), minDistance, locListen);
					invokeGPSSleepLogic();
				}
				locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, getGPSDelayValue(gpsDelay), minDistance, locListen);
			}
			btStats.out.println("Updated gps refresh to " + getGPSDelayName(index));
		}
	}
	
	private int getGPSDelayIndex(){
		return gpsDelay;
	}
	
	private int getSensorDelay(){
		return sensorDelay;
	}
	
	public static String[] getAllSensorDelayNames(){
		return sensorDelayNames.clone();
	}
	
	public static String getSensorDelayName(int index){
		try{
			return sensorDelayNames[index];
		}
		catch (ArrayIndexOutOfBoundsException e){
			return sensorDelayNames[0];
		}
	}
	
	public static int getSensorDelayValue(int index){
		try{
			return sensorDelays[index];
		}
		catch (ArrayIndexOutOfBoundsException e){
			return sensorDelays[0];
		}
	}
	
	
	
	private void setSensorDelay(int index){
		if (sensorDelay != index){
			sensorDelay = index;
			prefAdapter.setSensorDelay(index);
			for (int i = 0; i < NUM_LOGGERS; i++){
				switch (i){
					case GPS_LOGGING:
					case WIFI_LOGGING:
						break;
					default:
						if (isLogging(i)){
							stopLoggingI(i);
						}
						break;
				}
				
			} //all sensors must be shut off before turned back on in order to slow update rate
			for (int i = 0; i < NUM_LOGGERS; i++){
				switch (i){
					case GPS_LOGGING:
					case WIFI_LOGGING:
						break;
					default:
						if (isLogging(i)){
							startLoggingI(i);
						}
						break;
				}
				
			}
			btStats.out.println("Updated sensor refresh to " + getSensorDelayName(index));
		}
	}
	
	
	/**
	 * Run the service in the foreground so Android won't kill it.
	 * For use while logging.
	 * Shows a notification telling the user what's burning up battery power.
	 */
	private void bringToForeground() {
		Context ctx = getApplicationContext();
		
		//instantiate the notification
	    long when = System.currentTimeMillis();
	    Notification foregroundSvcNotify = new Notification(R.drawable.svc_icon,
	    		getString(R.string.svcRunning), when);
	    
	    //give the notification an intent so it links to something 
	    //also if this code is missing it crashes the system. sweet.
	    PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, 
	    		new Intent(ctx, HomeTabbed.class), 0);
	    foregroundSvcNotify.setLatestEventInfo(ctx, getString(R.string.svcTitle),
	    		getText(R.string.svcRunning), contentIntent);
	    
	    //Run the service in the foreground using the compatibility method
	    startForegroundCompat(NOTIFICATION, foregroundSvcNotify);
	    wakeLock.acquire();
	    foregroundEnabled = true;
	    btStats.out.println("Passive WakeLock acquired");
	}
	
	@Override
	public void onDestroy() {
		prefAdapter.removePreferencesChangeListener(this);
	    // Make sure our notification is gone.
	    stopForegroundCompat(NOTIFICATION);
	    btStats.out.println("BTService Destroyed!");
		super.onDestroy();
	}
	
	public void onLowMemory(){
		btStats.out.println("System low on memory!");
		super.onLowMemory();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return rpcBinder;
	}
	
	private void invokeGPSSleepLogic(){
		haltGPSSleepLogic();
		if (getGPSDelayValue(gpsDelay) >= GPS_MIN_TIME_FOR_SLEEP){
			gpsHandler.postDelayed(gpsFixer, GPS_MAX_FIX_TIME);
			btStats.out.println("gps fix stopper started!");
		}
		else{
			btStats.out.println("gps delay too low for stopper!");
		}
	}
	
	private void haltGPSSleepLogic(){
		gpsHandler.removeCallbacks(gpsFixer);
		gpsHandler.removeCallbacks(gpsRestarter);
		gpsNoFixSleepMode = false;
		btStats.out.println("gps fix stopper halted!");
	}
	
	private void startLoggingI(int id){
		switch (id){
			case GPS_LOGGING:
				if (!gpsIdleMode){
					locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, getGPSDelayValue(gpsDelay), minDistance, locListen);
					invokeGPSSleepLogic();
				}
				locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, minDistance, locListen);
				break;
			case ACC_LOGGING:
				if (!accSplitEnabled)
					senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) , getSensorDelayValue(sensorDelay));
				else{
					senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(SENSOR_TYPE_GRAVITY), getSensorDelayValue(sensorDelay));
					senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(SENSOR_TYPE_LINEAR_ACCELERATION), getSensorDelayValue(sensorDelay));
				}
				break;
			case GYRO_LOGGING:
				senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_GYROSCOPE), getSensorDelayValue(sensorDelay));
				break;
			case WIFI_LOGGING:
				registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				curWifiScanner = new WifiScanTask();
				curWifiScanner.execute();
				break;
			case ORNT_LOGGING:
				senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_ORIENTATION), getSensorDelayValue(sensorDelay));
				break;
			case LIGHT_LOGGING:
				senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_LIGHT), getSensorDelayValue(sensorDelay));
				break;
			case TEMP_LOGGING:
				senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_TEMPERATURE), getSensorDelayValue(sensorDelay));
				break;
			case PRESS_LOGGING:
				senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_PRESSURE), getSensorDelayValue(sensorDelay));
				break;
			default:
				return;
		}
	}
	
	private void startLogging(int id) {
		if (!canLog(id))
			return;
		if (!isLogging[id] && !noTrackLowMode && !noTrackLowStoreMode)
			startLoggingI(id);
		btStats.out.println(logNames[id] + " tracking enabled");
		isLogging[id] = true;
		if (!foregroundEnabled && anyLogging()){
			bringToForeground();
		}
		saveSensorSettings();
	}
	
	private void stopLoggingI(int id){
		switch (id){
			case GPS_LOGGING:
				locMan.removeUpdates(locListen);
				haltGPSSleepLogic();
				break;
			case ACC_LOGGING:
				if (!accSplitEnabled)
					senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
				else{
					senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(SENSOR_TYPE_GRAVITY));
					senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(SENSOR_TYPE_LINEAR_ACCELERATION));
				}
				break;
			case GYRO_LOGGING:
				senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
				break;
			case WIFI_LOGGING:
				curWifiScanner.cancel(true);
				unregisterReceiver(wifiReceiver);
				curWifiScanner = null;
				if (gpsIdleMode && isLogging[GPS_LOGGING]){
					locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, getGPSDelayValue(gpsDelay), minDistance, locListen);
				}
				wWatcher.clear();
				gpsIdleMode = false;
				break;
			case ORNT_LOGGING:
				senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_ORIENTATION));
				break;
			case LIGHT_LOGGING:
				senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_LIGHT));
				break;
			case TEMP_LOGGING:
				senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_TEMPERATURE));
				break;
			case PRESS_LOGGING:
				senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_PRESSURE));
				break;
			default:
				return;
		}
	}
	
	private void stopLogging(int id) {
		if (isLogging[id])
			stopLoggingI(id);
		btStats.out.println(logNames[id] + " tracking disabled");
		isLogging[id] = false;
		if (foregroundEnabled && !anyLogging()){
			stopForegroundCompat(NOTIFICATION);
		}
		saveSensorSettings();
	}
	
	private boolean canLog(int id){
		switch (id){
			case GPS_LOGGING:
				return locMan.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
						locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			case ACC_LOGGING:
				return senseMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
			case GYRO_LOGGING:
				return senseMan.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
			case WIFI_LOGGING:
				return wifiManager.isWifiEnabled();
			case ORNT_LOGGING:
				return senseMan.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null;
			case LIGHT_LOGGING:
				return senseMan.getDefaultSensor(Sensor.TYPE_LIGHT) != null;
			case TEMP_LOGGING:
				return senseMan.getDefaultSensor(Sensor.TYPE_TEMPERATURE) != null;
			case PRESS_LOGGING:
				return senseMan.getDefaultSensor(Sensor.TYPE_PRESSURE) != null;
			default:
		}
		return false;
	}
	
	private boolean isLogging(int id){
		if (id >= 0 && id < NUM_LOGGERS)
			return isLogging[id];
		return false;
	}
	
	private boolean anyLogging(){
		for (int i = 0; i < NUM_LOGGERS; i++)
			if (isLogging[i])
				return true;
		return false;
	}
	
	private static final Class<?>[] mStartForegroundSignature = new Class[] {
	    int.class, Notification.class};
	private static final Class<?>[] mStopForegroundSignature = new Class[] {
	    boolean.class};

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
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

	    // Fall back on the old API.
	    setForeground(true);
	    notMan.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
	    // If we have the new stopForeground API, then use it.
		wakeLock.release();
		foregroundEnabled = false;
		btStats.out.println("Passive WakeLock released");
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

	    // Fall back on the old API.  Note to cancel BEFORE changing the
	    // foreground state, since we could be killed at that point.
	    notMan.cancel(id);
	    setForeground(false);
	}
	
	private void queueLocation(Location loc){
		Object[] data = new Object[1];
		data[0] = loc;
		dataLists.get(GPS_LOGGING).add(data);
	}
	
	private void queueAcceleration(long timestamp, float[] values){
		Object[] data = new Object[4];
		data[0] = timestamp;
		for (int i = 0; i < 3; i++){
			data[1+i] = values[i];
		}
		dataLists.get(ACC_LOGGING).add(data);
	}
	
	private void queueGravityAcceleration(long timestamp, float[] values){
		Object[] data = new Object[4];
		data[0] = timestamp;
		for (int i = 0; i < 3; i++){
			data[1+i] = values[i];
		}
		dataLists.get(GRAVITY_ACC).add(data);
	}
	
	private void queueLinearAcceleration(long timestamp, float[] values){
		Object[] data = new Object[4];
		data[0] = timestamp;
		for (int i = 0; i < 3; i++){
			data[1+i] = values[i];
		}
		dataLists.get(LINEAR_ACC).add(data);
	}
	
	private void queueGyroscope(long timestamp, float[] values){
		Object[] data = new Object[4];
		data[0] = timestamp;
		for (int i = 0; i < 3; i++){
			data[1+i] = values[i];
		}
		dataLists.get(GYRO_LOGGING).add(data);
	}
	
	private void queueWifis(long timestamp, List<ScanResult> results){
		Object[][] data = new Object[results.size()][6];
		int i = 0;
		for (ScanResult result : results){
			data[i][0] = timestamp++;
			data[i][1] = result.SSID;
			data[i][2] = result.BSSID;
			data[i][3] = result.capabilities;
			data[i][4] = result.frequency;
			data[i][5] = result.level;
			i++;
		}
		dataLists.get(WIFI_LOGGING).add(data);
	}
	
	private void queueLight(long timestamp, float lux){
		Object[] data = new Object[2];
		data[0] = timestamp;
		data[1] = lux;
		dataLists.get(LIGHT_LOGGING).add(data);
	}
	
	private void queuePressure(long timestamp, float pressure){
		Object[] data = new Object[2];
		data[0] = timestamp;
		data[1] = pressure;
		dataLists.get(PRESS_LOGGING).add(data);
	}
	
	private void queueTemp(long timestamp, float temp){
		Object[] data = new Object[2];
		data[0] = timestamp;
		data[1] = temp;
		dataLists.get(TEMP_LOGGING).add(data);
	}
	
	private void queueOrientation(long timestamp, float[] values){
		Object[] data = new Object[4];
		data[0] = timestamp;
		for (int i = 0; i < 3; i++){
			data[1+i] = values[i];
		}
		dataLists.get(ORNT_LOGGING).add(data);
	}
	
	public void setAccSplitting(boolean enabled){
		if (!canSplitAcc()){
			return;
		}
		accSplitEnabled = enabled;
		if (isLogging[ACC_LOGGING]){
			if (accSplitEnabled){
				senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(SENSOR_TYPE_GRAVITY), getSensorDelayValue(sensorDelay));
				senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(SENSOR_TYPE_LINEAR_ACCELERATION), getSensorDelayValue(sensorDelay));
				senseMan.unregisterListener(sensorListener,senseMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
			}
			else{
				senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), getSensorDelayValue(sensorDelay));
				senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(SENSOR_TYPE_GRAVITY));
				senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(SENSOR_TYPE_LINEAR_ACCELERATION));
			}
		}
	}
	
	public boolean isSplittingAcc(){
		return accSplitEnabled;
	}



	/*
	 * Private classes:
	 * These classes implement interfaces necessary to implement the GPS service:
	 * -The LocationListener class gives the  
	 */
	
	/*
	 * Implement a location listener
	 */
	private LocationListener locListen = new LocationListener(){

		public void onLocationChanged(Location loc) {
			if (loc.getProvider().equals(LocationManager.GPS_PROVIDER)){
				btStats.out.println("Found a gps fix, restarting logic!");
				invokeGPSSleepLogic();
			}
			queueLocation(loc);
		}

		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private SensorEventListener sensorListener = new SensorEventListener(){

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			switch (event.sensor.getType()){
				case Sensor.TYPE_ACCELEROMETER:
					if (isLogging[ACC_LOGGING]){
						queueAcceleration(System.currentTimeMillis(),event.values);
					}
					break;
				case Sensor.TYPE_GYROSCOPE:
					if (isLogging[GYRO_LOGGING]){
						queueGyroscope(System.currentTimeMillis(),event.values);
					}
					break;
				case Sensor.TYPE_LIGHT:
					if (isLogging[LIGHT_LOGGING]){
						queueLight(System.currentTimeMillis(),event.values[0]);
					}
					break;
				case Sensor.TYPE_TEMPERATURE:
					if (isLogging[TEMP_LOGGING]){
						queueTemp(System.currentTimeMillis(),event.values[0]);
					}
					break;
				case Sensor.TYPE_ORIENTATION:
					if (isLogging[ORNT_LOGGING]){
						queueOrientation(System.currentTimeMillis(),event.values);
					}
					break;
				case SENSOR_TYPE_GRAVITY:
					if (isLogging[ACC_LOGGING]){
						queueGravityAcceleration(System.currentTimeMillis(),event.values);
					}
					break;
				case SENSOR_TYPE_LINEAR_ACCELERATION:
					if (isLogging[ACC_LOGGING]){
						queueLinearAcceleration(System.currentTimeMillis(),event.values);
					}
					break;
				case Sensor.TYPE_PRESSURE:
					if (isLogging[PRESS_LOGGING]){
						queuePressure(System.currentTimeMillis(),event.values[0]);
					}
					break;
				default:
			}
		}
		
	};
	
	private BroadcastReceiver externalPowerReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			int status = intent.getIntExtra("status", -1);
			if (status != -1){
				boolean oldConnected = externalPowerPresent;
				externalPowerPresent = status != BatteryManager.BATTERY_STATUS_DISCHARGING;
				if (oldConnected != externalPowerPresent)
					btStats.out.println("External power " + (externalPowerPresent ? "connected" : "disconnected"));
			}
			int plugged = intent.getIntExtra("plugged", -1);
			if (plugged != -1){
				boolean oldAC = externalPowerIsAC;
				externalPowerIsAC = plugged == BatteryManager.BATTERY_PLUGGED_AC;
				if (oldAC != externalPowerIsAC)
					btStats.out.println("AC power is " + (externalPowerIsAC ? "present" : "absent"));
			}
		}
		
	};
	
	private BroadcastReceiver lowBatteryReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			lowBattery = true;
			if (prefAdapter.noTrackingOnLowBat()){
				noTrackLowMode = true;
				btStats.out.println("Battery low! Turning off all tracking!");
				for (int i = 0; i < NUM_LOGGERS; i++){
					if (isLogging[i])
						stopLoggingI(i);
				}
			}
		}
		
	};
	
	private BroadcastReceiver batteryOkReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			lowBattery = false;
			if (noTrackLowMode){
				btStats.out.println("Battery okay! Turning tracking back on!");
				for (int i = 0; i < NUM_LOGGERS; i++)
					if (isLogging[i])
						startLoggingI(i);
			}
			noTrackLowMode = false;
		}
		
	};
	
	private BroadcastReceiver lowStorageReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			lowStorage = true;
			if (prefAdapter.noTrackingOnLowStorage()){
				noTrackLowStoreMode = true;
				btStats.out.println("Storage low! Turning off all tracking!");
				for (int i = 0; i < NUM_LOGGERS; i++){
					if (isLogging[i])
						stopLoggingI(i);
				}
			}
		}
		
	};
	
	private BroadcastReceiver storageOkReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			lowStorage = false;
			if (noTrackLowStoreMode){
				btStats.out.println("Storage okay! Turning tracking back on!");
				for (int i = 0; i < NUM_LOGGERS; i++)
					if (isLogging[i])
						startLoggingI(i);
			}
			noTrackLowStoreMode = false;
		}
		
	};
	
	private BroadcastReceiver wifiReceiver = new BroadcastReceiver(){

		public void onReceive(Context context, Intent intent) {
			if (isLogging[WIFI_LOGGING]){
				List<ScanResult> results = wifiManager.getScanResults();
				if (results != null && results.size() != 0){
					long resultTime = System.currentTimeMillis();
					queueWifis(resultTime, results);
				}
				else{
					results = new LinkedList<ScanResult>();
				}
				wWatcher.addScanResults(results);
				if (gpsIdleMode != wWatcher.isIdlingNearAP()){
					gpsIdleMode = wWatcher.isIdlingNearAP();
					if (gpsIdleMode){
						if (isLogging[GPS_LOGGING]){
							locMan.removeUpdates(locListen);
							haltGPSSleepLogic();
							locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, minDistance, locListen);
						}
						btStats.out.println("Idling near a wifi ap for longer than 5 minutes. Disabling GPS!");
					}
					else{
						if (isLogging[GPS_LOGGING]){
							locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, getGPSDelayValue(gpsDelay), minDistance, locListen);
							invokeGPSSleepLogic();
						}
						btStats.out.println("No longer idling near a wifi ap. Enabling GPS!");
					}
				}
			}
		}
		
	};
	
	private class WifiScanTask extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected Void doInBackground(Void... objs) {
			
			while (true){
				wifiManager.startScan();
				try {
					Thread.sleep(1000 * 30);
				} catch (InterruptedException e) {
				}
			}
		}
		
	}
	
	private long[] lastUpdate = new long[NUM_LOGGERS+2];
	
	private class DbDataWriter extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			
			while (true){
				for (int i = 0; i < dataLists.size(); i++){
					//grab list and insert empty one
					List<Object[]> currentList = dataLists.get(i);
					dataLists.set(i, new LinkedList<Object[]>());
					Object[][] data = currentList.toArray(new Object[][]{});
					HomeTabbed instance = HomeTabbed.instance;
					if (instance != null){
						
						double rate = 0;
						long timeDiff = 5000;
						if (lastUpdate[i] != 0){
							timeDiff = System.currentTimeMillis() - lastUpdate[i];
						}
						rate = data.length * 1000.0 / timeDiff;
						lastUpdate[i] = System.currentTimeMillis();
						switch (i){
							default:
								instance.onSampleRateChanged(i,rate);
								break;
							case GRAVITY_ACC:
							case LINEAR_ACC:
								if (isSplittingAcc())
									instance.onSampleRateChanged(ACC_LOGGING,rate);
								break;
						}
						
					}
					//write data to database
					if (data.length != 0){
						switch (i){
							case GPS_LOGGING:
								dbAdapter.writeLocations(data);
								break;
							case ACC_LOGGING:
								dbAdapter.writeAccelerations(data);
								break;
							case GYRO_LOGGING:
								dbAdapter.writeGyros(data);
								break;
							case WIFI_LOGGING:
								dbAdapter.writeWifis(data);
								break;
							case ORNT_LOGGING:
								dbAdapter.writeOrientations(data);
								break;
							case LIGHT_LOGGING:
								dbAdapter.writeLights(data);
								break;
							case TEMP_LOGGING:
								dbAdapter.writeTemps(data);
								break;
							case GRAVITY_ACC:
								dbAdapter.writeGravityAccelerations(data);
								break;
							case LINEAR_ACC:
								dbAdapter.writeLinearAccelerations(data);
								break;
							case PRESS_LOGGING:
								dbAdapter.writePressures(data);
								break;
						}
					}
				}				
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
			}
		}
	}
		
	/*
	 * Implement the RPC binding interface
	 */
	private IBinder rpcBinder = new IBTSvcRPC.Stub(){
		public void startLogging(int id) {
			BTService.this.startLogging(id);
		}
		
		public void stopLogging(int id) {
			BTService.this.stopLogging(id);
		}
		
		public boolean isLogging(int id) {
			return BTService.this.isLogging(id);
		}

		@Override
		public boolean canLog(int id) throws RemoteException {
			return BTService.this.canLog(id);
		}

		@Override
		public void setGPSDelay(int index) throws RemoteException {
			BTService.this.setGPSDelay(index);
		}

		@Override
		public int getGPSDelayIndex() throws RemoteException {
			return BTService.this.getGPSDelayIndex();
		}

		@Override
		public boolean canSplitAcc() throws RemoteException {
			return BTService.this.canSplitAcc();
		}

		@Override
		public void setAccSplitting(boolean enabled) throws RemoteException {
			BTService.this.setAccSplitting(enabled);
		}

		@Override
		public boolean isSplittingAcc() throws RemoteException {
			return BTService.this.isSplittingAcc();
		}

		@Override
		public void setSensorDelay(int index) throws RemoteException {
			BTService.this.setSensorDelay(index);
		}

		@Override
		public int getSensorDelayIndex() throws RemoteException {
			return BTService.this.getSensorDelay();
		}
	};
	
	private long prevTimeOff = 0;
	private long prevTime = 0;
	
	private boolean allowedToUpload(){
		if (!prefAdapter.prefsAreGood())
			return false;
		if (!prefAdapter.isNetworkEnabled())
			return false;
		if (lowBattery && prefAdapter.noUploadingOnLowBat())
			return false;
		if (prefAdapter.uploadRequiresExternalPower()){
			if (!externalPowerPresent)
				return false;
			if (prefAdapter.uploadRequiresACPower() && !externalPowerIsAC)
				return false;
		}
		return true;
	}
	
	private class UploaderTask extends AsyncTask<Void, Void, Void> {
		//TODO: clean this up!
	     protected Void doInBackground(Void... params) {	
	    	//upload gps data
	    	 
	    	
	    	WifiInfo address = wifiManager.getConnectionInfo();
			
			while (true){
				
				NetworkInfo curNetwork = conMan.getActiveNetworkInfo();
				
				if (curNetwork != null && curNetwork.isConnected() && (!curNetwork.isRoaming() || prefAdapter.canUse3G()) && allowedToUpload()){
				
					//upload gps data
					dbAdapter.uploadLocations(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
		    	 
		 			
		 			//upload accelerometer data
					if (allowedToUpload())
						dbAdapter.uploadAccelerations(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					if (allowedToUpload())
						dbAdapter.uploadGravityAccelerations(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					if (allowedToUpload())
						dbAdapter.uploadLinearAccelerations(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					
					//upload gyroscope data
					if (allowedToUpload())
						dbAdapter.uploadGyros(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					
					//upload orientation data
					if (allowedToUpload())
						dbAdapter.uploadOrientations(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					
					//upload light data
					if (allowedToUpload())
						dbAdapter.uploadIlluminances(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					
					//upload temperature data
					if (allowedToUpload())
						dbAdapter.uploadTemperatures(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
		 			
		 			//upload wifi accesspoint data
					if (allowedToUpload())
						dbAdapter.uploadWifis(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					
					//upload barcodes
					if (allowedToUpload())
						dbAdapter.uploadBarcodes(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					
					//upload pressure data
					if (allowedToUpload())
						dbAdapter.uploadPressures(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName());
					
		 			//upload 1 picture
					if (allowedToUpload()){
			 			Cursor c = dbAdapter.fetchFirstPendingUploadPic();
			 			if (c != null){
							if (c.moveToFirst()){
								long id = c.getLong(c.getColumnIndex(DbAdapter.PIX_KEY_ID));
								c.close();
								dbAdapter.uploadPhoto(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName(), id);
							}
							else {
								c.close();
							}
			 			}
					}		
					//upload 1 log comment
					if (allowedToUpload()){
						Cursor c = dbAdapter.fetchOldestComment();
						if (c != null){
							long id = c.getLong(c.getColumnIndex(DbAdapter.KEY_TIME));
							dbAdapter.uploadComment(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName(), id);
						}
					}
					//sleep a bit in case thread is eating too much cpu
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
				else{ //sleep 5 seconds if no network is present
					try{
						Thread.sleep(5000);
					} catch (InterruptedException e){
						
					}
				}
				HomeTabbed instance = HomeTabbed.instance;
				if (instance != null){
					instance.onLogRemainingChanged(btStats.getStoreRate(), btStats.getTotalDataStorageBytes(), btStats.getUploadRate(), btStats.getTotalDataUploadBytes());
				}
			}
	     }
	 }
	
	private Runnable gpsFixer = new Runnable(){
		public void run(){
			if (!gpsNoFixSleepMode && isLogging[GPS_LOGGING] && !gpsIdleMode){
				gpsNoFixSleepMode = true;
				long restartdelay = getGPSDelayValue(gpsDelay) - GPS_MAX_FIX_TIME;
				btStats.out.println("No GPS fix in " + GPS_MAX_FIX_TIME + " ms restarting in " + restartdelay + " ms.");
				gpsHandler.postDelayed(gpsRestarter, restartdelay);
				locMan.removeUpdates(locListen);
				locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, getGPSDelayValue(gpsDelay), minDistance, locListen);
			}
		}
	};
	
	private Runnable gpsRestarter = new Runnable(){
		public void run(){
			if (gpsNoFixSleepMode && isLogging[GPS_LOGGING] && !gpsIdleMode){
				btStats.out.println("Restarting gps for new fix.");
				locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, getGPSDelayValue(gpsDelay), minDistance, locListen);
				invokeGPSSleepLogic();
			}
			gpsNoFixSleepMode = false;
		}
	};

	@Override
	public void onPreferencesChanged() {
		if (prefAdapter.noTrackingOnLowBat()){
			if (lowBattery && !noTrackLowMode){
				noTrackLowMode = true;
				btStats.out.println("Trarcking disabled on low battery! Turning off all tracking!");
				for (int i = 0; i < NUM_LOGGERS; i++){
					if (isLogging[i])
						stopLoggingI(i);
				}
			}
		}
		else{
			if (noTrackLowMode){
				noTrackLowMode = false;
				btStats.out.println("Tracking allowed on low battery! Turning on all tracking!");
				for (int i = 0; i < NUM_LOGGERS; i++)
					if (isLogging[i])
						startLoggingI(i);
			}
		}
		if (prefAdapter.noTrackingOnLowStorage()){
			if (lowStorage && !noTrackLowStoreMode){
				noTrackLowStoreMode = true;
				btStats.out.println("Trarcking disabled on low storage! Turning off all tracking!");
				for (int i = 0; i < NUM_LOGGERS; i++){
					if (isLogging[i])
						stopLoggingI(i);
				}
			}
		}
		else{
			if (noTrackLowStoreMode){
				noTrackLowStoreMode = false;
				btStats.out.println("Tracking allowed on low storage! Turning on all tracking!");
				for (int i = 0; i < NUM_LOGGERS; i++)
					if (isLogging[i])
						startLoggingI(i);
			}
		}
	}
		
}
