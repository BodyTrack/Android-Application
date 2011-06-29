package org.bodytrack.BodyTrack;

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

public class PreferencesAdapter {
	public static final int INVALID_USER_ID = -1;
	public static final int DEFAULT_GPS_DELAY = 100;
	public static final String INVALID_USER_NAME = "NO_USER_NAME";
	public static final String INVALID_PASSWORD = "NO_PASSWORD";
	public static final String loginAddress = "http://bodytrack.org/login.json";
	
	private Context ctx;
	
	private ProgressDialog obtainingUserIDDialog;
	
	private SharedPreferences prefs;
	
	private BTStatisticTracker btStats = BTStatisticTracker.getInstance();
	
	public PreferencesAdapter(Context context){
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
	
	public void setGPSDelay(int index){
		prefs.edit().putInt("gpsDelay", index).commit();
	}
	
	public String getUploadAddress(){
		return "http://bodytrack.org/users/" + getUserID() + "/upload";
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
	
	public boolean autoDeletePhotosEnabled(){
		return prefs.getBoolean("photoAutodelete", false);
	}
	
	public boolean autoUploadPhotosEnabled(){
		return prefs.getBoolean("photoAutoupload", false);
	}
	
	public boolean isNetworkEnabled(){
		return prefs.getBoolean("networkEnabled", true);
	}
	
	public void setNetworkEnabled(boolean enabled){
		prefs.edit().putBoolean("networkEnabled", enabled).commit();
	}
	
	public boolean isPhotoBGUploadEnabled(){
		return prefs.getBoolean("photoUploadBG", true);
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
		obtainingUserIDDialog = new ProgressDialog(home);
		obtainingUserIDDialog.setMessage("Obtaining user id");
		obtainingUserIDDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

    	    @Override
    	    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
    	        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
    	            return true; // Pretend we processed it
    	        }
    	        return false; // Any other keys are still processed as normal
    	    }
    	});
		obtainingUserIDDialog.setCancelable(false);
		obtainingUserIDDialog.show();
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
				if (isNetworkEnabled()){
					if (hasWebAccess()){
					try {
						HttpClient mHttpClient = new DefaultHttpClient();
						HttpPost postToServer = new HttpPost(loginAddress);
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
				}
			}
			prefs.edit().putInt("userID", uid).commit();
			return null;
		}
		
		protected void onPostExecute(Void result) {
			try{
				obtainingUserIDDialog.dismiss();
			}
			catch (Exception e){
			}
			boolean networkSwitchedOff = false;
			if (isNetworkEnabled() && getUserID() == INVALID_USER_ID){
				btStats.out.println("Disabling network access.");
				setNetworkEnabled(false);
				networkSwitchedOff = true;
			}
			obtainingUserIDDialog = null;
			caller.onUserIDUpdated(networkSwitchedOff);
		}
		
	}
}
