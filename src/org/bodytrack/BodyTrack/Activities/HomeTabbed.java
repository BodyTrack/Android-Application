package org.bodytrack.BodyTrack.Activities;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.PreferencesAdapter;
import org.bodytrack.BodyTrack.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;

/**
 * This class defines a tabbed UI that allows the user to see the app's main
 * features. It is what is shown when the app is launched.
 */
public class HomeTabbed extends TabActivity {
	public DbAdapter dbAdapter;
	public static final String TAG = "HomeTabbed";
	public static final int ACTIVITY_PREFERENCES = 1;
	
	private static final long MAX_DATA_UPLOAD = 1000; //maximum data points to upload at a time.
	private static final long UPLOAD_RATE = 5000; //milliseconds between upload checks
	
	private PreferencesAdapter prefAdapter;
	private AlertDialog prefConfigDialog;
	
	private JSONArray gpsChannelArray = new JSONArray();
	private final String[] gpsChannelArrayElements = {"latitude","longitude","altitude","uncertainty in meters","speed","bearing","provider"};

	private JSONArray accChannelArray = new JSONArray();
	private final String[] accChannelArrayElements = {"acceleration_x","acceleration_y","acceleration_z"};
	
	private JSONArray wifiChannelArray = new JSONArray();
	private final String[] wifiChannelArrayElements = {"SSID", "BSSID"};
	
	private Menu mMenu;
	
	private boolean uploading = false;
	
	private WifiManager wifiManager;

	public void onCreate(Bundle savedInstanceState) {
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
	    super.onCreate(savedInstanceState);
	    for (int i = 0; i < gpsChannelArrayElements.length; i++){
	    	gpsChannelArray.put(gpsChannelArrayElements[i]);
	    }
	    for (int i = 0; i < accChannelArrayElements.length; i++){
	    	accChannelArray.put(accChannelArrayElements[i]);
	    }
	    for (int i = 0; i < wifiChannelArrayElements.length; i++){
	    	wifiChannelArray.put(wifiChannelArrayElements[i]);
	    }
	    setContentView(R.layout.tabbed_home);
	    dbAdapter = new DbAdapter(this).open();
	    prefAdapter = new PreferencesAdapter(this);
	    //Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, GpsSvcControl.class);
	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("gps").setIndicator("GPS")
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, CameraReview.class);
	    spec = tabHost.newTabSpec("camera").setIndicator("Camera")
	    	.setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, BarcodeReview.class);
	    spec = tabHost.newTabSpec("barcode").setIndicator("Barcodes")
	    	.setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, Sensors.class);
	    spec = tabHost.newTabSpec("accelerometer").setIndicator("Sensors")
	    		.setContent(intent);
	    tabHost.addTab(spec);
	    
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
            .setTitle(R.string.pref_config)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
	                dialog.dismiss();
	                Intent intent = new Intent(getApplicationContext(), BTPrefs.class);
	                startActivityForResult(intent, ACTIVITY_PREFERENCES);
                }
            });
        prefConfigDialog = builder.create();
        
        Thread.setDefaultUncaughtExceptionHandler(new BodyTrackExceptionHandler());
	    
        if (!prefAdapter.prefsAreGood()){
        	prefConfigDialog.setMessage(getString(R.string.pref_need_config));
        	prefConfigDialog.show();
        }	    
	    try {
			new File(getFilesDir().getAbsolutePath() + "/test.txt").createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		new UploaderTask().execute();
	}

		
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mMenu = menu;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.prefs:
	    	Intent intent = new Intent(getApplicationContext(), BTPrefs.class);
	    	startActivityForResult(intent, ACTIVITY_PREFERENCES);
			return true;
		}
		return false;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        switch(requestCode) {
	        case ACTIVITY_PREFERENCES:
	        	prefAdapter.obtainUserID(this);
	        	
	            break;
        }
        
    }
	
	public void onUserIDUpdated(){
		if (!prefAdapter.prefsAreGood()){
    		prefConfigDialog.setMessage(getString(R.string.pref_invalid));
    		prefConfigDialog.show();
    	}
	}
	
private class UploaderTask extends AsyncTask<Void, Void, Void> {
		//TODO: clean this up!
	     protected Void doInBackground(Void... params) {	
	    	 Thread.setDefaultUncaughtExceptionHandler(new BodyTrackExceptionHandler());
	    	//upload gps data
	    	
	    	HttpClient mHttpClient = new DefaultHttpClient();
			HttpPost postToServer = new HttpPost(prefAdapter.getUploadAddress());
			WifiInfo address = wifiManager.getConnectionInfo();
			
			while (true){
	    	 
	 			Cursor c = dbAdapter.fetchLocations(MAX_DATA_UPLOAD);
	 			
	 			JSONArray dataArray = new JSONArray();
	 			
	 			ArrayList<Long> ids = new ArrayList<Long>();
	 			
	 			if (c.moveToFirst()){
	 				while (!c.isAfterLast()){
						Location loc = dbAdapter.getLocationData(c);
						ids.add(dbAdapter.getLocationId(c));
						
						try{
							JSONArray locData = new JSONArray();
							locData.put(loc.getTime() / 1000.0);
							locData.put(loc.getLatitude());
							locData.put(loc.getLongitude());
							locData.put(loc.getAltitude());
							locData.put(loc.getAccuracy());
							locData.put(loc.getSpeed());
							locData.put(loc.getBearing());
							locData.put(loc.getProvider());
							dataArray.put(locData);
						}
						catch (JSONException e){
							
						}
	 					
	 					
	 					c.moveToNext();
	 				}
	 			}
	 			c.close();
	 			
	 			if (dataArray.length() > 0){
	 		    	try {
	 		    		List<NameValuePair> postRequest = new ArrayList<NameValuePair>();
	 			    	postRequest.add(new BasicNameValuePair("device_id", address.getMacAddress()));
	 			    	postRequest.add(new BasicNameValuePair("timezone","UTC"));
	 			    	postRequest.add(new BasicNameValuePair("device_class",android.os.Build.MODEL));
	 			    	postRequest.add(new BasicNameValuePair("dev_nickname",prefAdapter.getNickName()));
	 			    	postRequest.add(new BasicNameValuePair("channel_names", gpsChannelArray.toString()));
	 			    	postRequest.add(new BasicNameValuePair("data", dataArray.toString()));
	 		    		postToServer.setEntity(new UrlEncodedFormEntity(postRequest));
	 		    		HttpResponse response = mHttpClient.execute(postToServer);
	 		    		int statusCode = response.getStatusLine().getStatusCode();
	 		    		if (statusCode >= 200 && statusCode < 300)
		 		    		while (ids.size() > 0){
		 		    			dbAdapter.deleteLocation(ids.remove(0));
		 		    		}
	 		    	} catch (Exception e) {
	 		    		e.printStackTrace();
	 		    	}
	 			}
	 			else{
	 				while (ids.size() > 0){
	 	    			dbAdapter.deleteLocation(ids.remove(0));
	 	    		}
	 			}
	 			//upload accelerometer data
	 			c = dbAdapter.fetchAccelerations(MAX_DATA_UPLOAD);
	 			
	 			dataArray = new JSONArray();
	 			
	 			ids.clear();
	 			
	 			if (c.moveToFirst()){
	 				while (!c.isAfterLast()){
	 					float x = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_X));
			    		float y = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_Y));
			    		float z = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_Z));
			    		long time = c.getLong(c.getColumnIndex(DbAdapter.ACCEL_KEY_TIME));
			    		ids.add(c.getLong(c.getColumnIndex(DbAdapter.ACCEL_KEY_ID)));
						
						try{
							JSONArray accData = new JSONArray();
							accData.put(time / 1000.0);
							accData.put(x);
							accData.put(y);
							accData.put(z);
							dataArray.put(accData);
						}
						catch (JSONException e){
							
						}
	 					
	 					
	 					c.moveToNext();
	 				}
	 			}
	 			c.close();
	 			
	 			if (dataArray.length() > 0){
	 		    	try {
	 		    		List<NameValuePair> postRequest = new ArrayList<NameValuePair>();
	 			    	postRequest.add(new BasicNameValuePair("device_id", address.getMacAddress()));
	 			    	postRequest.add(new BasicNameValuePair("timezone","UTC"));
	 			    	postRequest.add(new BasicNameValuePair("device_class",android.os.Build.MODEL));
	 			    	postRequest.add(new BasicNameValuePair("dev_nickname",prefAdapter.getNickName()));
	 			    	postRequest.add(new BasicNameValuePair("channel_names", accChannelArray.toString())); 
	 			    	postRequest.add(new BasicNameValuePair("data", dataArray.toString()));
	 		    		postToServer.setEntity(new UrlEncodedFormEntity(postRequest));
	 		    		HttpResponse response = mHttpClient.execute(postToServer);
	 		    		int statusCode = response.getStatusLine().getStatusCode();
	 		    		if (statusCode >= 200 && statusCode < 300)
		 		    		while (ids.size() > 0){
		 		    			dbAdapter.deleteAcceleration(ids.remove(0));
		 		    		}
	 		    	} catch (Exception e) {
	 		    		e.printStackTrace();
	 		    	}
	 			}
	 			else{
	 				while (ids.size() > 0){
	 	    			dbAdapter.deleteAcceleration(ids.remove(0));
	 	    		}
	 			}
	 			
	 			//upload accelerometer data
	 			c = dbAdapter.fetchWifis(MAX_DATA_UPLOAD);
	 			
	 			dataArray = new JSONArray();
	 			
	 			ids.clear();
	 			
	 			if (c.moveToFirst()){
	 				while (!c.isAfterLast()){
	 					String ssid = c.getString(c.getColumnIndex(DbAdapter.WIFI_KEY_SSID));
			    		String bssid = c.getString(c.getColumnIndex(DbAdapter.WIFI_KEY_BSSID));
			    		long time = c.getLong(c.getColumnIndex(DbAdapter.WIFI_KEY_TIME));
			    		ids.add(c.getLong(c.getColumnIndex(DbAdapter.WIFI_KEY_ID)));
						
						try{
							JSONArray wifiData = new JSONArray();
							wifiData.put(time / 1000.0);
							wifiData.put(ssid);
							wifiData.put(bssid);
							dataArray.put(wifiData);
						}
						catch (JSONException e){
							
						}
	 					
	 					
	 					c.moveToNext();
	 				}
	 			}
	 			c.close();
	 			
	 			if (dataArray.length() > 0){
	 		    	try {
	 		    		List<NameValuePair> postRequest = new ArrayList<NameValuePair>();
	 			    	postRequest.add(new BasicNameValuePair("device_id", address.getMacAddress()));
	 			    	postRequest.add(new BasicNameValuePair("timezone","UTC"));
	 			    	postRequest.add(new BasicNameValuePair("device_class",android.os.Build.MODEL));
	 			    	postRequest.add(new BasicNameValuePair("dev_nickname",prefAdapter.getNickName()));
	 			    	postRequest.add(new BasicNameValuePair("channel_names", wifiChannelArray.toString())); 
	 			    	
	 			    	JSONObject wifiSpecs = new JSONObject();
	 			    	JSONObject ssidInfo = new JSONObject();
	 			    	ssidInfo.put("type", "String");
	 			    	wifiSpecs.put("SSID", ssidInfo);
	 			    	wifiSpecs.put("BSSID", ssidInfo);
	 			    	
	 			    	postRequest.add(new BasicNameValuePair("channel_specs", wifiSpecs.toString()));
	 			    	
	 			    	postRequest.add(new BasicNameValuePair("data", dataArray.toString()));
	 		    		postToServer.setEntity(new UrlEncodedFormEntity(postRequest));
	 		    		HttpResponse response = mHttpClient.execute(postToServer);
	 		    		int statusCode = response.getStatusLine().getStatusCode();
	 		    		if (statusCode >= 200 && statusCode < 300)
		 		    		while (ids.size() > 0){
		 		    			dbAdapter.deleteWifi(ids.remove(0));
		 		    		}
	 		    	} catch (Exception e) {
	 		    		e.printStackTrace();
	 		    	}
	 			}
	 			else{
	 				while (ids.size() > 0){
	 	    			dbAdapter.deleteWifi(ids.remove(0));
	 	    		}
	 			}
	 			
				
				try {
					
					c = dbAdapter.fetchFirstPendingUploadPic();
					final long id = c.getLong(c.getColumnIndex(DbAdapter.PIX_KEY_ID));
					String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
				    c.close();
				    
				    FileInputStream fis = new FileInputStream(picFileName);
				    ByteArrayOutputStream bos = new ByteArrayOutputStream();
				    
				    byte[] buffer = new byte[1024];
				    int read = 0;
				    
				    while (read >= 0){
				    	read = fis.read(buffer);
				    	if (read > 0){
				    		bos.write(buffer,0,read);
				    	}
				    }
				    
				    fis.close();
				    
				    
					ByteArrayBody bin = new ByteArrayBody(bos.toByteArray(), "image/jpeg", picFileName);
					MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
					reqEntity.addPart("device_id",new StringBody(address.getMacAddress()));
					reqEntity.addPart("device_class",new StringBody(android.os.Build.MODEL));
					reqEntity.addPart("dev_nickname",new StringBody(prefAdapter.getNickName()));
					reqEntity.addPart("photo",bin);
					postToServer.setEntity(reqEntity);
					HttpResponse response = mHttpClient.execute(postToServer);
					StatusLine status = response.getStatusLine();
					if (status.getStatusCode() >= 200 && status.getStatusCode() < 300){
						dbAdapter.setPictureUploadState(id,DbAdapter.PIC_UPLOADED);
						final CameraReview camRev = CameraReview.activeInstance;
						if (camRev != null){
							HomeTabbed.this.runOnUiThread(new Runnable(){
								public void run(){
									camRev.onImageUploaded(id);
								}
							});
							
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
				try {
					Thread.sleep(UPLOAD_RATE);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	     }
	 }
}
