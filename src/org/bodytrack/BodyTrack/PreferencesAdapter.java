package org.bodytrack.BodyTrack;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bodytrack.BodyTrack.Activities.HomeTabbed;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class PreferencesAdapter implements PreferencesChangeListener {
	public static final int INVALID_USER_ID = -1;
	public static final int DEFAULT_GPS_DELAY = 0;
	public static final int DEFAULT_SENSOR_DELAY = 1;
	public static final String INVALID_USER_NAME = "NO_USER_NAME";
	public static final String INVALID_PASSWORD = "NO_PASSWORD";
	
	private Context ctx;
	
	private ProgressDialog obtainingUserIDDialog;
	
	private SharedPreferences prefs;
	
	private BTStatisticTracker btStats = BTStatisticTracker.getInstance();
	
	private static PreferencesAdapter instance = null;
	
	private List<PreferencesChangeListener> listeners = new ArrayList<PreferencesChangeListener>();
	
	public static PreferencesAdapter getInstance(Context context){
		if (instance == null)
			instance = new PreferencesAdapter(context);
		return instance;
	}
	
	private PreferencesAdapter(Context context){
		ctx = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public int getUserID(){
		int id;
		try{
			id = prefs.getInt("userID", INVALID_USER_ID);
		}
		catch (Exception e){
			id = INVALID_USER_ID;
		}
		return id < 0 ? INVALID_USER_ID : id;
	}
	
	public int getGPSDelay(){
		int index;
		try{
			index = prefs.getInt("gpsDelay", DEFAULT_GPS_DELAY);
		}
		catch (Exception e){
			index = DEFAULT_GPS_DELAY;
		}
		return index;
	}
	
	public int getSensorDelay(){
		int index;
		try{
			index = prefs.getInt("sensorDelay", DEFAULT_SENSOR_DELAY);
		}
		catch (Exception e){
			index = DEFAULT_SENSOR_DELAY;
		}
		return index;
	}
	
	public void setSensorDelay(int index){
		prefs.edit().putInt("sensorDelay", index).commit();
	}
	
	public void setGPSDelay(int index){
		prefs.edit().putInt("gpsDelay", index).commit();
	}
	
	public String getHost(){
		try{
			return prefs.getString("serverHost", "www.bodytrack.org") + ":" + Integer.parseInt(prefs.getString("serverPort", "80"));
		}
		catch (Exception e){
			return "www.bodytrack.org";
		}
	}
	
	public String getUploadAddress(){
		return "http://" + getHost() + "/users/" + getUserID() + "/upload";
	}
	
	public String getUserName(){
		return prefs.getString("userName", INVALID_USER_NAME);
	}
	
	public String getPassword(){
		return prefs.getString("password", INVALID_PASSWORD);
	}
	
	public String getNickName(){
		return prefs.getString("deviceNickName", "");
	}
	
	public void setNickName(String nickname){
		prefs.edit().putString("deviceNickName", nickname).commit();
	}
	
	public boolean isNetworkEnabled(){
		return prefs.getBoolean("networkEnabled", true);
	}
	
	public void setNetworkEnabled(boolean enabled){
		prefs.edit().putBoolean("networkEnabled", enabled).commit();
	}
	
	public boolean canUse3G(){
		return !prefs.getBoolean("onlyUseWifi", false);
	}
	
	public boolean noTrackingOnLowBat(){
		return prefs.getBoolean("turnOffTrackingOnLowBat", false);
	}
	
	public boolean noTrackingOnLowStorage(){
		return prefs.getBoolean("turnOffTrackingOnLowStore", true);
	}
	
	public boolean noUploadingOnLowBat(){
		return prefs.getBoolean("turnOffUploadingOnLowBat", false);
	}
	
	public boolean uploadRequiresExternalPower(){
		return prefs.getBoolean("uploadOnlyOnExternalPower", false);
	}
	
	public boolean uploadRequiresACPower(){
		return prefs.getBoolean("uploadOnlyOnAC", false);
	}
	
	public boolean dbStoredExternally(){
		return prefs.getBoolean("dbOnExternalStorage", false);
	}
	
	public void setDbStoredExternally(boolean external){
		prefs.edit().putBoolean("dbOnExternalStorage", external).commit();
	}
	
	public boolean noCommentsOnPhotos(){
		//return prefs.getBoolean("noCommentsOnPhotos", false);
		return true;
	}
	
	public int getSensorSettings(){
		return prefs.getInt("sensorsOn",0);
	}
	
	public void setSensorSettings(int value){
		prefs.edit().putInt("sensorsOn", value).commit();
	}
	
	private void fixUpNickName(){
		String nickname = getNickName();
		StringBuffer processedNickName = new StringBuffer();
		boolean nickChanged = false;
		if (!nickname.equals(""))
			for (int i = 0; i < nickname.length(); i++){
				char curChar = nickname.charAt(i);
				if (!((curChar >= 'a' && curChar <= 'z') || 
						(curChar >= 'A' && curChar <= 'Z') || 
						(curChar >= '0' && curChar <= '9') ||
						curChar == '_')){
					processedNickName.append('_');
					nickChanged = true;
				}
				else
					processedNickName.append(curChar);
			}
		if (nickChanged){
			setNickName(processedNickName.toString());
			btStats.out.println("Nickname contained invalid characters; replaced with underscores.");
		}
		return;	
	}
	
	public boolean prefsAreGood(){
		fixUpNickName();
		return (!isNetworkEnabled() || getUserID() != PreferencesAdapter.INVALID_USER_ID);
	}
	
	public void obtainUserID(HomeTabbed home){
		new UpdateUserIDTask().execute(home);
	}
	
	private boolean hasWebAccess(){
		try{
			ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		 	return cm.getActiveNetworkInfo().isConnected();
		}
		catch (Exception e){
			return false;
		}
	}
	
	private class UpdateUserIDTask extends AsyncTask<HomeTabbed, Void, Void> {

		private HomeTabbed caller;
		
		@Override
		protected Void doInBackground(HomeTabbed... objs) {
			caller = objs[0];
			int uid = INVALID_USER_ID;
			String oldUserName = prefs.getString("oldUserName", INVALID_USER_NAME);
			String oldPassword = prefs.getString("oldPassword", INVALID_PASSWORD);
			if (oldUserName.equals(getUserName()) && oldPassword.equals(getPassword()))
				return null;
			if (isNetworkEnabled()){
				if (hasWebAccess()){
					caller.runOnUiThread(new Runnable(){
						public void run(){
							obtainingUserIDDialog = new ProgressDialog(caller);
							obtainingUserIDDialog.setMessage("Obtaining user id");
							obtainingUserIDDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

					    	    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					    	        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
					    	            return true; // Pretend we processed it
					    	        }
					    	        	return false; // Any other keys are still processed as normal
						    	    }
							});
							obtainingUserIDDialog.setCancelable(false);
							obtainingUserIDDialog.show();
						}
					});
					try {
						HttpClient mHttpClient = new DefaultHttpClient();
						HttpPost postToServer = new HttpPost("http://" + getHost() + "/login.json");
						List<NameValuePair> postRequest = new ArrayList<NameValuePair>();
				    	postRequest.add(new BasicNameValuePair("login", getUserName()));
				    	postRequest.add(new BasicNameValuePair("password", getPassword()));
						postToServer.setEntity(new UrlEncodedFormEntity(postRequest));
						HttpResponse response = mHttpClient.execute(postToServer);
						int statusCode = response.getStatusLine().getStatusCode();
						if (statusCode >= 200 && statusCode < 300){
							String text = EntityUtils.toString(response.getEntity());
							JSONObject jObject = new JSONObject(text);
							uid = Integer.parseInt(jObject.getString("user_id"));
							btStats.out.println("Successfully obtained user_id: " + uid);
						}
					} catch (Exception e){
						btStats.out.println("Failed to obtain user_id: " + e);
					}
				}
				else{
					btStats.out.println("Failed to obtain user_id: no web access found.");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
			else{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			prefs.edit().putInt("userID", uid).commit();
			if (uid == INVALID_USER_ID){
				prefs.edit().putString("oldUserName", INVALID_USER_NAME).commit();
				prefs.edit().putString("oldPassword", INVALID_PASSWORD).commit();
			}
			else{
				prefs.edit().putString("oldUserName", getUserName()).commit();
				prefs.edit().putString("oldPassword", getPassword()).commit();
			}
			return null;
		}
		
		protected void onPostExecute(Void result) {
			caller.runOnUiThread(new Runnable(){
				public void run(){
					try{
						obtainingUserIDDialog.dismiss();
					}
					catch (Exception e){
					}
					obtainingUserIDDialog = null;
				}
			});
			
			boolean networkSwitchedOff = false;
			if (isNetworkEnabled() && getUserID() == INVALID_USER_ID){
				btStats.out.println("Disabling network access.");
				setNetworkEnabled(false);
				networkSwitchedOff = true;
			}
			caller.onUserIDUpdated(networkSwitchedOff);
		}
		
	}
	
	public void addPreferencesChangeListener(PreferencesChangeListener listener){
		listeners.add(listener);
	}
	
	public void removePreferencesChangeListener(PreferencesChangeListener listener){
		listeners.remove(listener);
	}

	@Override
	public void onPreferencesChanged() {
		PreferencesChangeListener[] listenerArray = listeners.toArray(new PreferencesChangeListener[]{});
		for (PreferencesChangeListener listener : listenerArray){
			listener.onPreferencesChanged();
		}
	}
}
