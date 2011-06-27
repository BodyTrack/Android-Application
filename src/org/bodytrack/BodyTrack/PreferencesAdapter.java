package org.bodytrack.BodyTrack;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bodytrack.BodyTrack.Activities.CameraReview;
import org.bodytrack.BodyTrack.Activities.HomeTabbed;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

public class PreferencesAdapter {
	public static final int INVALID_USER_ID = -1;
	public static final String INVALID_USER_NAME = "NO_USER_NAME";
	public static final String INVALID_PASSWORD = "NO_PASSWORD";
	public static final String loginAddress = "http://bodytrack.org/login.json";
	
	private ProgressDialog obtainingUserIDDialog;
	
	private SharedPreferences prefs;
	
	public PreferencesAdapter(Context context){
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
	
	public boolean autoDeletePhotosEnabled(){
		return prefs.getBoolean("photoAutodelete", false);
	}
	
	public boolean autoUploadPhotosEnabled(){
		return prefs.getBoolean("photoAutoupload", false);
	}
	
	public boolean isNetworkEnabled(){
		return prefs.getBoolean("networkEnabled", true);
	}
	
	public boolean isPhotoBGUploadEnabled(){
		return prefs.getBoolean("photoUploadBG", true);
	}
	
	private boolean isNickNameValid(){
		String nickname = getNickName();
		if (!nickname.equals(""))
			for (int i = 0; i < nickname.length(); i++){
				char curChar = nickname.charAt(i);
				if (!((curChar >= 'a' && curChar <= 'z') || 
						(curChar >= 'A' && curChar <= 'Z') || 
						(curChar >= '0' || curChar <= '9') ||
						curChar == '_'))
						return false;
			}
		return true;	
	}
	
	public boolean prefsAreGood(){
		return (!isNetworkEnabled() || getUserID() != PreferencesAdapter.INVALID_USER_ID) && isNickNameValid();
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
	
	private class UpdateUserIDTask extends AsyncTask<HomeTabbed, Void, Void> {

		private HomeTabbed caller;
		
		@Override
		protected Void doInBackground(HomeTabbed... objs) {
			caller = objs[0];
			int uid = INVALID_USER_ID;
			if (isNetworkEnabled()){
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
					}
				} catch (Exception e){
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
			obtainingUserIDDialog = null;
			caller.onUserIDUpdated();
		}
		
	}
}
