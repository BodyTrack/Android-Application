package org.bodytrack.BodyTrack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bodytrack.BodyTrack.Activities.HomeTabbed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class AccelerometerService extends Service implements SensorEventListener {
	
	
	
	private SensorManager manager;
	private boolean isLogging;
	
	protected DbAdapter dbAdapter;
	

	private NotificationManager notMan;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private static final int NOTIFICATION = 6;

		
	@Override
	public void onCreate() {
		super.onCreate();
				
		/*Get an instance of the sensor manager*/
    	manager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
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
	    		getString(R.string.accSvcRunning), when);
	    
	    //give the notification an intent so it links to something 
	    //also if this code is missing it crashes the system. sweet.
	    PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, 
	    		new Intent(ctx, HomeTabbed.class), 0);
	    foregroundSvcNotify.setLatestEventInfo(ctx, getString(R.string.accSvcTitle),
	    		getText(R.string.accSvcRunning), contentIntent);
	    
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

    	/*Bring service to foreground*/
    	bringToForeground();
    	
		/*Register the location listener with the location manager*/
		if (!isLogging) {
			manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) , SensorManager.SENSOR_DELAY_GAME);
		}
		isLogging = true;
	}
	
	private void stopLogging() {

    	/*Leave foreground state*/
	    stopForegroundCompat(NOTIFICATION);
    	
		/*Stop getting location updates*/
		if (isLogging) {
			manager.unregisterListener(this);
		}
		isLogging = false;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		dbAdapter.writeAcceleration(event.values);
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
	 * Implement the RPC binding interface
	 */
	private IBinder rpcBinder = new IAccSvcRPC.Stub(){
		public void startLogging() {
			AccelerometerService.this.startLogging();
		}
		
		public void stopLogging() {
			AccelerometerService.this.stopLogging();
		}
		
		public boolean isLogging() {
			return AccelerometerService.this.isLogging;
		}
	};
}
