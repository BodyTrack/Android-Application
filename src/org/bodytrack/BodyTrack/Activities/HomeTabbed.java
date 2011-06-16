package org.bodytrack.BodyTrack.Activities;


import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.http.util.EntityUtils;
import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.PreferencesAdapter;
import org.bodytrack.BodyTrack.R;
import org.bodytrack.BodyTrack.Activities.CameraReview.ImageAdapter;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * This class defines a tabbed UI that allows the user to see the app's main
 * features. It is what is shown when the app is launched.
 */
public class HomeTabbed extends TabActivity {
	public DbAdapter dbAdapter;
	public static final String TAG = "HomeTabbed";
	public static final int ACTIVITY_PREFERENCES = 1;
	
	private PreferencesAdapter prefAdapter;
	private AlertDialog prefConfigDialog;
	
	private JSONArray gpsChannelArray = new JSONArray();
	private final String[] gpsChannelArrayElements = {"latitude","longitude","altitude","uncertainty in meters","speed","bearing"};

	private Menu mMenu;
	
	private boolean uploading = false;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    for (int i = 0; i < gpsChannelArrayElements.length; i++){
	    	gpsChannelArray.put(gpsChannelArrayElements[i]);
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
	    
	  /**  intent = new Intent().setClass(this, BarcodeReview.class);
	    spec = tabHost.newTabSpec("barcode").setIndicator("Barcodes")
	    	.setContent(intent);
	    tabHost.addTab(spec);**/
	    
	    intent = new Intent().setClass(this, Accelerometer.class);
	    spec = tabHost.newTabSpec("accelerometer").setIndicator("Accelerometer")
	    		.setContent(intent);
	    tabHost.addTab(spec);
	    Timer time = new Timer();
	    final Handler handler = new Handler();
	    
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
	    
        if (!checkPrefs(prefAdapter)){
        	prefConfigDialog.setMessage(getString(R.string.pref_need_config));
        	prefConfigDialog.show();
        }
	    
	    //This is the timer that sends the data after every 15 seconds.
	    time.scheduleAtFixedRate(new TimerTask()
	    {
	   	public void run()
	    	{
	    	handler.post(new Runnable(){
	    	public void run()
	    	{
	    		sendData();
	    	}
	    	});
	    	}
	    }
	    ,15000,15000);
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
	public void sendData() throws NumberFormatException
	{
		/*Cursor queData = dbAdapter.fetchAllQueries();
		JSONArray data = new JSONArray();
		JSONArray channel = new JSONArray();
		if(queData.moveToFirst())
		{
		for(String chan : queData.getString(queData.getColumnIndex(DbAdapter.STACK_KEY_CHANNEL)).split(","))
		{
			channel.put(chan);
		}
		ArrayList<String> fields = new ArrayList<String>();
		String[] infoData = queData.getString(queData.getColumnIndex(DbAdapter.STACK_KEY_DATA)).split(",");
		Log.i("LENGTH", Integer.toString(infoData.length));
		fields.add(infoData[0]);
		for(int i=1; i <= infoData.length; i++)
		{
			Log.i("I", Integer.toString(i));
			if(i==infoData.length)
			{
				JSONArray queryField = new JSONArray(fields);
				data.put(queryField);
				fields.clear();
			}
			else if(isCorrectTime(infoData[i]))
			{
				JSONArray queryField = new JSONArray(fields);
				data.put(queryField);
				fields.clear();
				fields.add(infoData[i]);
			}
			else
			{
				fields.add(infoData[i]);
			}
		}
    	queData.close();
    	fields.clear();*/
		
		if (!uploading){
			uploading = true;
			new UploaderTask().execute();
		}
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
		if (!checkPrefs(prefAdapter)){
    		prefConfigDialog.setMessage(getString(R.string.pref_invalid));
    		prefConfigDialog.show();
    	}
	}
	
	public boolean isCorrectTime(String value)
	{
		try
		{
			if(Double.valueOf(value) > 10000000.0)
			{
				return true;
			}
			return false;
		}
		catch(NumberFormatException e)
		{
			return false;
		}
	}
	
	//checks for invalid preferences. Returns true if preferences are valid.
	private boolean checkPrefs(PreferencesAdapter prefAdapter){
		return prefAdapter.getUserID() != PreferencesAdapter.INVALID_USER_ID;
	}
	
	public void onUploadFinished(){
		uploading = false;
	}
	
private class UploaderTask extends AsyncTask<Void, Void, Void> {
		
		private int numUploaded = 0;
		private int totalToUpload;
		
	     protected Void doInBackground(Void... params) {	    			
 			Cursor c = dbAdapter.fetchAllLocations();
 			
 			JSONArray dataArray = new JSONArray();
 			
 			ArrayList<Long> locationIds = new ArrayList<Long>();
 			
 			if (c.moveToFirst()){
 				while (!c.isAfterLast()){
					Location loc = dbAdapter.getLocationData(c);
					locationIds.add(dbAdapter.getLocationId(c));
					
					try{
						JSONArray locData = new JSONArray();
						locData.put(loc.getTime() / 1000.0);
						locData.put(loc.getLatitude());
						locData.put(loc.getLongitude());
						locData.put(loc.getAltitude());
						locData.put(loc.getAccuracy());
						locData.put(loc.getSpeed());
						locData.put(loc.getBearing());
						dataArray.put(locData);
					}
					catch (JSONException e){
						
					}
 					
 					
 					c.moveToNext();
 				}
 			}
 			c.close();
 			if (dataArray.length() > 0){
 				HttpClient mHttpClient = new DefaultHttpClient();
 		    	HttpPost postToServer = new HttpPost(prefAdapter.getUploadAddress());
 		    	WifiManager wifiManager = (WifiManager) HomeTabbed.this.getSystemService(Context.WIFI_SERVICE);
 		    	WifiInfo address = wifiManager.getConnectionInfo();
 		    	try {
 		    		List<NameValuePair> postRequest = new ArrayList<NameValuePair>();
 			    	postRequest.add(new BasicNameValuePair("device_id", address.getMacAddress()));
 			    	postRequest.add(new BasicNameValuePair("timezone","UTC"));
 			    	postRequest.add(new BasicNameValuePair("device_class",android.os.Build.MODEL));
 			    	postRequest.add(new BasicNameValuePair("channel_names", gpsChannelArray.toString())); 
 			    	postRequest.add(new BasicNameValuePair("data", dataArray.toString()));
 		    		postToServer.setEntity(new UrlEncodedFormEntity(postRequest));
 		    		HttpResponse response = mHttpClient.execute(postToServer);
 		    		while (locationIds.size() > 0){
 		    			dbAdapter.deleteLocation(locationIds.remove(0));
 		    		}
 		    	} catch (Exception e) {
 		    		e.printStackTrace();
 		    	}
 			}
 			else{
 				while (locationIds.size() > 0){
 	    			dbAdapter.deleteLocation(locationIds.remove(0));
 	    		}
 			}
	         return null;
	     }

	     protected void onPostExecute(Void result) {
	    	 HomeTabbed.this.onUploadFinished();
	     }
	 }
}
