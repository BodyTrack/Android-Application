package org.bodytrack.BodyTrack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bodytrack.BodyTrack.Activities.BodyTrackExceptionHandler;
import org.bodytrack.BodyTrack.Activities.HomeTabbed;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;


/** 
 * This class defines a service which runs in the background on
 * the phone to capture location data. It is controlled by the 
 * activity defined by GpsSvccontrol.java
 */
public class BTService extends Service{
	/*constants*/
	public static final String TAG = "BTService";
	
	
	private final long minTime = 0;
	private final long minDistance = 0;
	
	
	
	private LocationManager locMan;
	private SensorManager senseMan;
	private WifiManager wifiManager;
	
	private WifiScanTask curWifiScanner;
	private DbDataWriter dbUpdaterTask;
	
	private boolean[] isLogging = new boolean[NUM_LOGGERS];
	
	protected DbAdapter dbAdapter;
	

	private NotificationManager notMan;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private static final int NOTIFICATION = 5;
	
	public static final int NUM_LOGGERS = 5;
	
	public static final int GPS_LOGGING = 0;
	public static final int ACC_LOGGING = 1;
	public static final int GYRO_LOGGING = 2;
	public static final int WIFI_LOGGING = 3;
	public static final int MAG_LOGGING = 4;
	
	private List<Object[]> dataList = new LinkedList<Object[]>();

		
	@Override
	public void onCreate() {
		super.onCreate();
    	Log.v(TAG, "Starting GPS service");
				
		/*Get an instance of the location manager*/
		locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		senseMan = (SensorManager) getSystemService(SENSOR_SERVICE);
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		dbAdapter = new DbAdapter(this).open();
		
		
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
	    
	    dbUpdaterTask = new DbDataWriter();
	    dbUpdaterTask.execute();
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
	}
	
	@Override
	public void onDestroy() {
	    // Make sure our notification is gone.
	    stopForegroundCompat(NOTIFICATION);
	    
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return rpcBinder;
	}
	
	private void startLogging(int id) {
		if (!canLog(id))
			return;
		switch (id){
			case GPS_LOGGING:
		    	Log.v(TAG, "Starting GPS logging");
		    	
				/*Register the location listener with the location manager*/
				if (!isLogging[id]) {
					locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, locListen);
					locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, locListen);
				}
				break;
			case ACC_LOGGING:
				if (!isLogging[id]) {
					senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) , SensorManager.SENSOR_DELAY_GAME);
				}
				break;
			case GYRO_LOGGING:
				if (!isLogging[id]){
					senseMan.registerListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
				}
				break;
			case WIFI_LOGGING:
				if (!isLogging[id]){
					registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
					curWifiScanner = new WifiScanTask();
					curWifiScanner.execute();
				}
				break;
			default:
				return;
		}
		isLogging[id] = true;
		if (anyLogging()){
			bringToForeground();
		}
		
	}
	
	private void stopLogging(int id) {
		switch (id){
			case GPS_LOGGING:
				Log.v(TAG, "Stopping GPS logging");
				/*Stop getting location updates*/
				if (isLogging[id]) {
					locMan.removeUpdates(locListen);
				}
				break;
			case ACC_LOGGING:
				if (isLogging[id]) {
					senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
				}
				break;
			case GYRO_LOGGING:
				if (isLogging[id]) {
					senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
				}
				break;
			case WIFI_LOGGING:
				if (isLogging[id]){
					curWifiScanner.cancel(true);
					unregisterReceiver(wifiReceiver);
					curWifiScanner = null;
				}
				break;
			case MAG_LOGGING:
				if (isLogging[id]){
					senseMan.unregisterListener(sensorListener, senseMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
				}
			default:
				return;
		}
		isLogging[id] = false;
		if (!anyLogging()){
			stopForegroundCompat(NOTIFICATION);
		}
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
			case MAG_LOGGING:
				return senseMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
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
	
	private static final Class[] mStartForegroundSignature = new Class[] {
	    int.class, Notification.class};
	private static final Class[] mStopForegroundSignature = new Class[] {
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
		Object[] data = new Object[2];
		data[0] = GPS_LOGGING;
		data[1] = loc;
		dataList.add(data);
	}
	
	private void queueAcceleration(long timestamp, float[] values){
		Object[] data = new Object[5];
		data[0] = ACC_LOGGING;
		data[1] = timestamp;
		for (int i = 0; i < 3; i++){
			data[2+i] = values[i];
		}
		dataList.add(data);
	}
	
	private void queueGyroscope(long timestamp, float[] values){
		Object[] data = new Object[5];
		data[0] = GYRO_LOGGING;
		data[1] = timestamp;
		for (int i = 0; i < 3; i++){
			data[2+i] = values[i];
		}
		dataList.add(data);
	}
	
	private void queueWifi(long timestamp, String ssid, String bssid){
		Object[] data = new Object[4];
		data[0] = WIFI_LOGGING;
		data[1] = timestamp;
		data[2] = ssid;
		data[3] = bssid;
		dataList.add(data);
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
						queueAcceleration(event.timestamp,event.values);
					}
					break;
				case Sensor.TYPE_GYROSCOPE:
					if (isLogging[GYRO_LOGGING]){
						queueGyroscope(event.timestamp,event.values);
					}
					break;
				default:
			}
		}
		
	};
	
	private BroadcastReceiver wifiReceiver = new BroadcastReceiver(){

		public void onReceive(Context context, Intent intent) {
			if (isLogging[WIFI_LOGGING]){
				List<ScanResult> results = wifiManager.getScanResults();
				long resultTime = System.currentTimeMillis();
				
				for (ScanResult result : results){
					queueWifi(resultTime, result.SSID, result.BSSID);
					resultTime++;
				}
			}
		}
		
	};
	
	private class WifiScanTask extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected Void doInBackground(Void... objs) {
			Thread.setDefaultUncaughtExceptionHandler(new BodyTrackExceptionHandler());
			
			while (true){
				wifiManager.startScan();
				try {
					Thread.sleep(1000 * 30);
				} catch (InterruptedException e) {
				}
			}
		}
		
	}
	
	private class DbDataWriter extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			Thread.setDefaultUncaughtExceptionHandler(new BodyTrackExceptionHandler());
			
			while (true){
				while (dataList.size() > 0){
					Object[] data = dataList.remove(0);
					switch ((Integer) data[0]){
						case GPS_LOGGING:
							dbAdapter.writeLocation((Location) data[1]);
							break;
						case ACC_LOGGING:
						{
							float[] values = new float[3];
							long time = (Long) data[1];
							for (int i = 0; i < 3; i++){
								values[i] = (Float) data[i + 2];
							}
							dbAdapter.writeAcceleration(time, values);
							break;
						}
						case GYRO_LOGGING:
						{
							float[] values = new float[3];
							long time = (Long) data[1];
							for (int i = 0; i < 3; i++){
								values[i] = (Float) data[i + 2];
							}
							dbAdapter.writeGyro(time, values);
							break;
						}
						case WIFI_LOGGING:
						{
							long retVal = dbAdapter.writeWifi((Long) data[1], (String) data[2], (String) data[3]);
							if (retVal < 0){
								retVal++;
							}
							break;
						}
						case MAG_LOGGING:
							break;
					}
				}
				
				try {
					Thread.sleep(1000);
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
	};
		
}
