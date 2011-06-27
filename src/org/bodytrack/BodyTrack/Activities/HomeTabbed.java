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
import org.bodytrack.BodyTrack.BodyTrackExceptionHandler;
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
	private static final long UPLOAD_RATE = 1; //milliseconds between upload checks
	
	private PreferencesAdapter prefAdapter;
	private AlertDialog prefConfigDialog;
	
	private Menu mMenu;
	
	private boolean uploading = false;
	
	private WifiManager wifiManager;

	public void onCreate(Bundle savedInstanceState) {
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.tabbed_home);
	    dbAdapter = DbAdapter.getDbAdapter(getApplicationContext());
	    prefAdapter = new PreferencesAdapter(this);
	    //Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, Sensors.class);
	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("sensors").setIndicator("Sensors")
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
	    
	    intent = new Intent().setClass(this, Stats.class);
	    spec = tabHost.newTabSpec("stats").setIndicator("Stats").setContent(intent);
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
}
