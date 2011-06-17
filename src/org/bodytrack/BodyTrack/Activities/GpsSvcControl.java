package org.bodytrack.BodyTrack.Activities;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.GpsService;
import org.bodytrack.BodyTrack.IGPSSvcRPC;
import org.bodytrack.BodyTrack.R;
import org.json.JSONArray;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class defines an activity that provides the user control over the GPS
 * service. 
 */
//https://bodytrack.org/users/UID/upload

public class GpsSvcControl extends Activity{
	public static final String TAG = "GpsSvcControl";
			
	private Button gpsSvcStartButton;
	private Button gpsSvcStopButton;
	private Button gpsShowButton;
	
	private TextView Outbox;
	protected DbAdapter dbAdapter;
	
	protected SharedPreferences prefs;
	
	private IGPSSvcRPC gpsBinder;
	
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.v(TAG, "Starting GpsSvcControl activity");
        setContentView(R.layout.gpscontrol);
                
        //Set up buttons
        gpsSvcStartButton = (Button)findViewById(R.id.gpsSvcStartButton);
        gpsSvcStartButton.setEnabled(false);
        gpsSvcStopButton = (Button)findViewById(R.id.gpsSvcStopButton);
        gpsSvcStopButton.setEnabled(false);
        gpsSvcStartButton.setOnClickListener(mStartSvc);
        gpsSvcStopButton.setOnClickListener(mStopSvc);
        gpsShowButton = (Button)findViewById(R.id.gpsShowButton);
        gpsShowButton.setOnClickListener(showData);
       // gpsDumpButton.setOnClickListener(dumpData);
        Outbox = (TextView)findViewById(R.id.Outbox);
        
        //Open/connect to the GPS logging service & check its state
    	Context ctx = getApplicationContext();
    	Intent intent = new Intent(ctx, GpsService.class);
    	ctx.startService(intent);
    	Boolean bindSuccess = ctx.bindService(intent, sc, 0);
        
        //connect to database
		dbAdapter = new DbAdapter(this).open();
		
		Button dumpButton = (Button)findViewById(R.id.dump_gps);
		dumpButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				try{
					FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gps.csv");
					OutputStreamWriter writer = new OutputStreamWriter(fos);
					writer.write("Latitude,Longitude,Altitude,Accuracy,Bearing,Speed,Provider,Timestamp");
					Cursor c = dbAdapter.fetchAllLocations();
					c.moveToFirst();
					while (!c.isAfterLast()){
						Location loc = dbAdapter.getLocationData(c);
						writer.write("\n" + loc.getLatitude() + "," + loc.getLongitude() + "," + loc.getAltitude() + "," + 
								loc.getAccuracy() + "," + loc.getBearing() + "," + loc.getSpeed() + "," + loc.getProvider() + "," + loc.getTime());
						c.moveToNext();
					}
					c.close();
					writer.close();
					Toast.makeText(GpsSvcControl.this, "gps.csv created", Toast.LENGTH_LONG).show();
				}
				catch (Exception e){
					Toast.makeText(GpsSvcControl.this, "failed to write gps.csv: " + e.toString(), Toast.LENGTH_LONG).show();
				}
			}
			
		});
		
    }
    
    protected void serviceBound(IGPSSvcRPC binder) {
        //Log.v(TAG, "Telling GPS service to start. Success? " + bindSuccess);
    	this.gpsBinder = binder;
    	try {
        	gpsSvcStartButton.setEnabled(!binder.isLogging());
        	gpsSvcStopButton.setEnabled(binder.isLogging());
        //TODO catch
    	} catch (Exception e){}
	
    }
    
    private void startGps() {
        gpsSvcStartButton.setEnabled(false);
        gpsSvcStopButton.setEnabled(true);
        
        try {
        	gpsBinder.startLogging();
        } catch(Exception e) {
        	Log.e(TAG, "Failed to start logging in GPS Service; exception: " + e);
        }
    }
    
    private void stopGps() {
        Log.v(TAG, "Telling GPS service to stop");

    	try {
            gpsSvcStartButton.setEnabled(true);
            gpsSvcStopButton.setEnabled(false);
            gpsBinder.stopLogging();
    	} catch (Exception e) {
            Log.e(TAG, "Attempting to stop GPS service which wasn't running; exception:" + e.toString());
    	}
    }
    
    private Button.OnClickListener mStartSvc = new Button.OnClickListener(){
	    public void onClick(View v) {
	    	startGps();
	    }
    };
 
    //TODO: replace with a listview in bottom half of view (maybe move svc
    //buttons to menu?)
    private Button.OnClickListener showData = new Button.OnClickListener(){
	    public void onClick(View v) {
	    	Outbox.setText("");
	    	Cursor geodata = dbAdapter.fetchAllLocations();
	    	geodata.moveToFirst();
	    	while (geodata.isAfterLast() == false) {
	    		Outbox.append("\n" + geodata.getString(geodata.getColumnIndex("latitude")) + ", " 
	    			+ geodata.getString(geodata.getColumnIndex("longitude")) 
	    			+ "; acc:" + geodata.getString(geodata.getColumnIndex("accuracy")));
	    		geodata.moveToNext();
	    	}
	    	geodata.close();
	    }
    };
    
  /**  //TODO: moved all uploading to DB adapter? sure shouldn't be here
    private Button.OnClickListener dumpData = new Button.OnClickListener(){
	    public void onClick(View v) {
	    	//grab location data cursor from database
	    	Cursor geodata = dbAdapter.fetchAllLocations();
	    	
	    	//create a json array to put it in
	    	JSONArray data = new JSONArray();
	    	
	    	//reset database cursor
	    	geodata.moveToFirst();
	    	
	    	//grab column names as a List for iterating
	    	String[] channelNamesArr = geodata.getColumnNames();
	    	List <String> channelNames = Arrays.asList(channelNamesArr);

	    	while (geodata.isAfterLast() == false) {
	    		ArrayList<String> fields= new ArrayList<String>();
		    	for (String name : channelNames) {
		    		fields.add(geodata.getString(geodata.getColumnIndex(name)));
		    	}
		    	JSONArray jsonFields = new JSONArray(fields);
		    	data.put(jsonFields);
		    	Toast.makeText(GpsSvcControl.this, "This is the response: " + data.toString(),
	    				Toast.LENGTH_SHORT).show();	 
	    		geodata.moveToNext();
	    	}
	    	
	    	//get list of column names ready to send as upload: format & make JSON
	    	//remove "time" column from list since BT protocol assumes time in first column
	    	List <String> channelNamesNoTime = channelNames.subList(1, channelNames.size());
	    	JSONArray channelNamesJson = new JSONArray(channelNamesNoTime);
	    	//make an http request
	    	HttpClient mHttpClient = new DefaultHttpClient();
	    	HttpPost postToServer = new HttpPost(dumpAddress);
	    	try {
	    		List<NameValuePair> postRequest = new ArrayList<NameValuePair>();
		    	postRequest.add(new BasicNameValuePair("device_class","droid-phone"));//TODO: get name
		    	postRequest.add(new BasicNameValuePair("source_class", "location"));
		    	postRequest.add(new BasicNameValuePair("device_id", "00:00:00:00:00"));//TODO: make real
		    	postRequest.add(new BasicNameValuePair("source_id", "00:00:00:00:00/location"));
		    	postRequest.add(new BasicNameValuePair("sensor_nickname", "phone location"));
		    	postRequest.add(new BasicNameValuePair("timezone", "utc"));//TODO:: make real
		    	postRequest.add(new BasicNameValuePair("channel_names", channelNamesJson.toString())); 
		    	postRequest.add(new BasicNameValuePair("data", data.toString()));	
	    		postToServer.setEntity(new UrlEncodedFormEntity(postRequest));
	    		HttpResponse response = mHttpClient.execute(postToServer);
	    		Toast.makeText(GpsSvcControl.this, "This is the response: " + EntityUtils.toString(response.getEntity()),
	    				Toast.LENGTH_SHORT).show();	  
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	geodata.close();
	    }
    };**/
    
    private ServiceConnection sc = new ServiceConnection(){
    	/*GpsSvcControl parentAct;
    	
    	public ServiceConnection(GpsSvcControl parent)
    	{
    		parentAct = parent;
    	}*/
    	
    	@Override
		public void onServiceConnected(ComponentName svc, IBinder binder) {
    		gpsBinder = IGPSSvcRPC.Stub.asInterface(binder);
    		serviceBound(gpsBinder);
			Log.v(TAG, "Service connected");
		}

    	@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.v(TAG, "Service disconnected");
		}
    };
    
    private Button.OnClickListener mStopSvc = new Button.OnClickListener(){
	    public void onClick(View v) {
	    	stopGps();
	    }
    };
   
    
}