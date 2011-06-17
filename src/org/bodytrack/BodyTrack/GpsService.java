package org.bodytrack.BodyTrack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.bodytrack.BodyTrack.Activities.HomeTabbed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


/** 
 * This class defines a service which runs in the background on
 * the phone to capture location data. It is controlled by the 
 * activity defined by GpsSvccontrol.java
 */
public class GpsService extends Service{
	/*constants*/
	public static final String TAG = "GpsService";
	
	
	private final long minTime = 0;
	private final long minDistance = 0;
	
	
	
	private LocationListener locListen;
	private LocationManager locMan;
	private boolean isLogging;
	
	protected DbAdapter dbAdapter;
	

	private NotificationManager notMan;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private static final int NOTIFICATION = 5;

		
	@Override
	public void onCreate() {
		super.onCreate();
    	Log.v(TAG, "Starting GPS service");
				
		/*Get an instance of the location manager*/
		locMan = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		locListen = new myLocListen();
		
		dbAdapter = new DbAdapter(this).open();
		
		/*try{
			gpsFile = openFileOutput("gps.csv", MODE_APPEND);
			gpsWriter = new OutputStreamWriter(gpsFile);
		} catch(Exception e) {
	    	Log.e(TAG, "Failed to open file; exception: " + e.toString());	
		}*/
		
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
	
	private void startLogging() {
    	Log.v(TAG, "Starting GPS logging");

    	/*Bring service to foreground*/
    	bringToForeground();
    	
		/*Register the location listener with the location manager*/
		if (!isLogging) {
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, locListen);
			locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, locListen);
		}
		isLogging = true;
	}
	
	private void stopLogging() {
    	Log.v(TAG, "Stopping GPS logging");

    	/*Leave foreground state*/
	    stopForegroundCompat(NOTIFICATION);
    	
		/*Stop getting location updates*/
		if (isLogging) {
			locMan.removeUpdates(locListen);
		}
		isLogging = false;
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



	/*
	 * Private classes:
	 * These classes implement interfaces necessary to implement the GPS service:
	 * -The LocationListener class gives the  
	 */
	
	/*
	 * Implement a location listener
	 */
	private class myLocListen implements LocationListener{

		public void onLocationChanged(Location loc) {
			//Log.v(TAG, "Location changed. Location: " + loc);
			/*ArrayList<String> values = new ArrayList<String>();
			long time = System.currentTimeMillis();
			int number = (int) (time/1000);
			int last_num = (int) (time% 1000);
			values.add(Integer.toString(number) + "." + Integer.toString(last_num)+ "," + loc.getLatitude() + "," + loc.getLongitude() + "," 
			+ loc.getAccuracy() + "," + loc.getAltitude() + "," + loc.getBearing() + "," + 
			loc.getProvider() + "," + loc.getSpeed());
			Long result = dbAdapter.writeQuery("Latitude,Longitute,Accuracy,Altitude,Bearing,Provider,Speed", values);*/
			
			long result = dbAdapter.writeLocation(loc);
			
			if (result == -1) {
				Log.e(TAG, "Failed to write location to DB!");
			}
				
			/*try {
				GpsService.this.gpsWriter.write(loc.toString());
			} catch (IOException e) {
				Log.e(TAG, "GPS failed to write changed location");
			}*/
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
	}
		
	/*
	 * Implement the RPC binding interface
	 */
	private IBinder rpcBinder = new IGPSSvcRPC.Stub(){
		public void startLogging() {
			GpsService.this.startLogging();
		}
		
		public void stopLogging() {
			GpsService.this.stopLogging();
		}
		
		public boolean isLogging() {
			return GpsService.this.isLogging;
		}
	};
		
}
