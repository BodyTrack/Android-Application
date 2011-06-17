package org.bodytrack.BodyTrack.Activities;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.PreferencesAdapter;
import org.bodytrack.BodyTrack.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;


/**
 * This class defines an activity that allows the user to review
 * previously taken (meal) photos and has a button to take new
 * ones.
 */
public class CameraReview extends Activity {
	public static final String TAG = "cameraReview";
	
	private Button takePic, uploadPics, deletePics;

	private DbAdapter dbAdapter;
	
	private Gallery picGallery;
	
	private ProgressDialog uploadDialog;
	
	public static final int ACTIVITY_NEW_PICTURE = 1;
	
	public static final int DELETE_ID = 0,
							UPLOAD_ID = 1;
	
	private AlertDialog uploadCompleteDialog;
	
	private PreferencesAdapter prefAdapter;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_review);
		
		prefAdapter = new PreferencesAdapter(this);
		
		dbAdapter = new DbAdapter(this).open();
		
		//Set up button to go to camera activity
		takePic = (Button)findViewById(R.id.takePic);
		takePic.setOnClickListener(mTakePic);
		
		uploadPics = (Button) findViewById(R.id.upload_all_pics);
		uploadPics.setOnClickListener(mUploadAll);
		
		deletePics = (Button) findViewById(R.id.delete_all_uploaded_pics);
		deletePics.setOnClickListener(mDeleteAllUploaded);
		
		picGallery = (Gallery) findViewById(R.id.reviewGallery);
		picGallery.setAdapter(new ImageAdapter(this));
		
		registerForContextMenu(picGallery);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
            .setTitle(R.string.upload_complete)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                }
            });
        uploadCompleteDialog = builder.create();
	}
	
	private Button.OnClickListener mTakePic = new Button.OnClickListener(){
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
			startActivityForResult(intent, ACTIVITY_NEW_PICTURE);
		}
	};
	
	private Button.OnClickListener mUploadAll = new Button.OnClickListener(){
		public void onClick(View v) {
	    	Cursor c = dbAdapter.fetchAllUnuploadedPics();
	    	Long[] ids = new Long[c.getCount()];
	    	c.moveToFirst();
	    	for (int i = 0; i < ids.length; i++){
	    		ids[i] = c.getLong(c.getColumnIndex(DbAdapter.PIX_KEY_ID));
	    		c.moveToNext();
	    	}
	    	c.close();
	    	if (ids.length > 0){
	    		uploadPhotos(ids);
	    	}
	    	else{
	    		uploadCompleteDialog.setMessage(getString(R.string.no_images_to_upload));
	    		uploadCompleteDialog.show();
	    	}
		}
	};
	
	private Button.OnClickListener mDeleteAllUploaded = new Button.OnClickListener(){

		@Override
		public void onClick(View v) {
			dbAdapter.deleteUploadedPictures();
			((ImageAdapter) picGallery.getAdapter()).notifyDataSetChanged();
		}
		
	};
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    menu.add(0, DELETE_ID, 0, R.string.picture_delete);
	    boolean enabled;
		
	    if (!prefAdapter.isNetworkEnabled()){
	    	enabled = false;
	    	menu.add(0, UPLOAD_ID, 0, R.string.network_disabled);
	    }
	    else{
	    	Cursor c = dbAdapter.fetchPicture(((AdapterContextMenuInfo)menuInfo).id);
	    	enabled = c.getInt(c.getColumnIndex(DbAdapter.PIX_KEY_UPLOADED)) == 0;
	    	c.close();
	    	menu.add(0, UPLOAD_ID, 0, enabled ? R.string.picture_upload : R.string.picture_already_uploaded);
	    }
	    
		menu.getItem(UPLOAD_ID).setEnabled(enabled);
		
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch(item.getItemId()) {
		    case DELETE_ID:
		    {
			     dbAdapter.deletePicture(info.id);
			     ((ImageAdapter) picGallery.getAdapter()).notifyDataSetChanged();
		        return true;
		    }
		    case UPLOAD_ID:
		    {
		    	uploadPhotos(info.id);
		    	return true;
		    }
	    }
	    return super.onContextItemSelected(item);
	}
	
	public void uploadPhotos(Long... ids){
		uploadDialog = new ProgressDialog(this);
    	uploadDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

    	    @Override
    	    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
    	        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
    	            return true; // Pretend we processed it
    	        }
    	        return false; // Any other keys are still processed as normal
    	    }
    	});
    	uploadDialog.setCancelable(false);
    	uploadDialog.show();
    	new UploadImageTask().execute(ids);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        switch(requestCode) {
	        case ACTIVITY_NEW_PICTURE:
	        	if (resultCode == RESULT_OK){
	        		((ImageAdapter) picGallery.getAdapter()).notifyDataSetChanged();
	        		if (prefAdapter.autoUploadPhotosEnabled()){
	        			Cursor c = dbAdapter.fetchLastPicture();
	        			long id = c.getLong(c.getColumnIndex(DbAdapter.LOC_KEY_ID));
	        			c.close();
	        			uploadPhotos(id);
	        		}
	        	}
	            break;
        }
        
    }
	
	protected void onResume(){
		super.onResume();
		uploadPics.setEnabled(prefAdapter.isNetworkEnabled());
	}
	
	public class ImageAdapter extends BaseAdapter {
	    int mGalleryItemBackground;
	    private Context mContext;
	    
	    private ArrayList<Long> pictureIDs = new ArrayList<Long>();
	    private ArrayList<Bitmap> bms = new ArrayList<Bitmap>();
	    
	    public ImageAdapter(Context c) {
	        mContext = c;
	        TypedArray a = obtainStyledAttributes(R.styleable.PictureGallery);
	        mGalleryItemBackground = a.getResourceId(
	                R.styleable.PictureGallery_android_galleryItemBackground, 0);
	        a.recycle();
	    }
	    
	    private Bitmap getBitmap(long imageId) throws IOException{
	    	int index = pictureIDs.indexOf(imageId);
	    	if (index == -1){
		        
		        Cursor c = dbAdapter.fetchPicture(imageId);
		        String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
		        c.close();
		        
		        FileInputStream fis = openFileInput(picFileName);
		        
		        BitmapFactory.Options opts = new BitmapFactory.Options();
		        
		        opts.inSampleSize = 8;
		        
		        Bitmap bm = BitmapFactory.decodeStream(fis,null,opts);
		        fis.close();
		        
		        bms.add(bm);
		        return bm;
	        }
	    	else{
	    		return bms.get(index);
	    	}
	    }
	    
	    private void purgeBmCache(){
	    	for (int i = 0; i < pictureIDs.size(); i++){
	    		Cursor c = dbAdapter.fetchPicture(pictureIDs.get(i));
	    		if (c != null){
	    			c.close();
	    		}
	    		else{
	    			pictureIDs.remove(i);
	    			bms.remove(i);
	    			i--;
	    		}
	    	}
	    }
	    
	    public void notifyDataSetChanged(){
	    	purgeBmCache();
	    	super.notifyDataSetChanged();
	    }

	    public int getCount() {
	    	Cursor c = dbAdapter.fetchAllPics();
	    	int count = c.getCount();
	    	c.close();
	        return count;
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	    	Cursor c = dbAdapter.fetchAllPics();
	    	c.moveToFirst();
	    	for (int i = 0; i < position; i++)
	    		c.moveToNext();
	    	long id = c.getLong(c.getColumnIndex(DbAdapter.PIX_KEY_ID));
	    	c.close();
	        return id;
	    }
	    
	    

	    public View getView(int position, View convertView, ViewGroup parent) {
	    	try{
		        ImageView i = new ImageView(mContext);
		        
		        i.setImageBitmap(getBitmap(getItemId(position)));
		        
		       // i.setLayoutParams(new Gallery.LayoutParams(150, 100));
		        i.setScaleType(ImageView.ScaleType.FIT_XY);
		        i.setBackgroundResource(mGalleryItemBackground);
		        //i.setBackgroundColor(Color.GREEN);
	
		        return i;
	    	}
	    	catch (Exception e){
	    		Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
	    		return new ImageView(mContext);
	    	}
	    }
	}
	
	private class UploadImageTask extends AsyncTask<Long, Integer, Long> {
		
		private int numUploaded = 0;
		private int totalToUpload;
		
	     protected Long doInBackground(Long... ids) {
	    	totalToUpload = ids.length;
	    	HttpClient mHttpClient = new DefaultHttpClient();
			HttpPost postToServer = new HttpPost(prefAdapter.getUploadAddress());
			WifiManager wifiManager = (WifiManager) CameraReview.this.getSystemService(Context.WIFI_SERVICE);
			WifiInfo address = wifiManager.getConnectionInfo();
			for (int i = 0; i < totalToUpload; i++) {
				publishProgress(i);
				try {
					
					Cursor c = dbAdapter.fetchPicture(ids[i]);
					String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
				    c.close();
				    
				    FileInputStream fis = openFileInput(picFileName);
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
					reqEntity.addPart("photo",bin);
					postToServer.setEntity(reqEntity);
					HttpResponse response = mHttpClient.execute(postToServer);
					StatusLine status = response.getStatusLine();
					if (status.getStatusCode() >= 200 && status.getStatusCode() < 300){
						dbAdapter.setPictureUploaded(ids[i],true);
						numUploaded++;
						if (prefAdapter.autoDeletePhotosEnabled()){
							dbAdapter.deletePicture(ids[i]);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
	         return 0L;
	     }

	     protected void onProgressUpdate(Integer... progress) {
	    	 if (totalToUpload == 1){
					uploadDialog.setMessage("Uploading Image");
				}
				else{
					uploadDialog.setMessage("Uploading Image " + (progress[0] + 1) + " of " + totalToUpload);
				}
	     }

	     protected void onPostExecute(Long result) {
	    	 if (prefAdapter.autoDeletePhotosEnabled() && numUploaded > 0){
	    		 ((ImageAdapter) picGallery.getAdapter()).notifyDataSetChanged();
	    	 }
	    	 uploadDialog.dismiss();
	    	 if (totalToUpload == 1){
	    		 if (numUploaded == 1){
	    			 uploadCompleteDialog.setMessage("Image successfully uploaded");
	    		 }
	    		 else{
	    			 uploadCompleteDialog.setMessage("Failed to upload image");
	    		 }
	    	 }
	    	 else{
	    		 uploadCompleteDialog.setMessage("Successfully uploaded " + numUploaded + " of " + totalToUpload + " Image" + (totalToUpload != 0 ? "s" : ""));
	    	 }
	    	 uploadCompleteDialog.show();
	     }
	 }

}


