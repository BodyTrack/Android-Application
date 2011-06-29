package org.bodytrack.BodyTrack.Activities;


import org.bodytrack.BodyTrack.BTStatisticTracker;
import org.bodytrack.BodyTrack.BodyTrackExceptionHandler;
import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.PreferencesAdapter;
import org.bodytrack.BodyTrack.R;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
	
	private BTStatisticTracker btStats;

	public void onCreate(Bundle savedInstanceState) {
		
		btStats = BTStatisticTracker.getInstance();
		
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
        
        dbAdapter.getSize();
	}

		
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
	
	public void onUserIDUpdated(boolean networkForceDisabled){
		try{
			if (networkForceDisabled){
				 new AlertDialog.Builder(this)
			        	.setCancelable(false)
			            .setTitle(R.string.pref_config)
			            .setMessage(R.string.network_force_disabled)
			            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int id) {
				                dialog.dismiss();
			                }
			            }).create().show();
			}
			else if (!prefAdapter.prefsAreGood()){
	    		prefConfigDialog.setMessage(getString(R.string.pref_invalid));
	    		prefConfigDialog.show();
	    	}
		}
		catch (Exception e){
			Toast.makeText(this, "Exception occured in onUserIDUpdated. exception logged.", Toast.LENGTH_SHORT).show();
			btStats.out.println("Exception in onUserIDUpdated:");
			e.printStackTrace(btStats.out);
		}
	}
}
