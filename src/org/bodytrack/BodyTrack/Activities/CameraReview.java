package org.bodytrack.BodyTrack.Activities;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
	
	private static final String temporaryStorage = "temppic.jpg";
	
	private Button takePic, uploadPics, deletePics;
	
	private static final int PIC_CACHE_LIMIT = 10; //limit of images to cache in ram for display

	private DbAdapter dbAdapter;
	
	private Gallery picGallery;
	
	private ProgressDialog uploadDialog;
	
	public static final int ACTIVITY_NEW_PICTURE = 1;
	
	public static final int DELETE_ID = 0,
							UPLOAD_ID = 1;
	
	public static CameraReview activeInstance = null; //a poor way to get a pointer to the activity
	
	private AlertDialog uploadCompleteDialog;
	
	private PreferencesAdapter prefAdapter;
	
	private long currentContextMenuId = -1;
	
	private Uri imageUri;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_review);
		
		activeInstance = this;
		
		prefAdapter = new PreferencesAdapter(this);
		
		dbAdapter = DbAdapter.getDbAdapter(getApplicationContext());
		
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
			try{
				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				ContentValues values = new ContentValues();
				values.put(MediaStore.Images.Media.TITLE, temporaryStorage);
				values.put(MediaStore.Images.Media.DESCRIPTION, "Image taken by BodyTrack");
				imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(cameraIntent, ACTIVITY_NEW_PICTURE);
			}
			catch (Exception e){
				Toast.makeText(CameraReview.this, "Please insert an SD card before using the camera.", Toast.LENGTH_SHORT).show();
			}
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
			refreshGallery();
		}
		
	};
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    currentContextMenuId = ((AdapterContextMenuInfo)menuInfo).id;
	    menu.add(0, DELETE_ID, 0, R.string.picture_delete);
	    boolean enabled;
		
	    if (!prefAdapter.isNetworkEnabled()){
	    	enabled = false;
	    	menu.add(0, UPLOAD_ID, 0, R.string.network_disabled);
	    }
	    else{
	    	Cursor c = dbAdapter.fetchPicture(currentContextMenuId);
	    	int state = c.getInt(c.getColumnIndex(DbAdapter.PIX_KEY_UPLOAD_STATE));
	    	enabled = state == DbAdapter.PIC_NOT_UPLOADED;
	    	int titleRes;
	    	c.close();
	    	switch (state){
		    	default:
		    		titleRes = R.string.picture_upload;
		    		break;
		    	case DbAdapter.PIC_PENDING_UPLOAD:
		    		titleRes = R.string.picture_pending_upload;
		    		break;
		    	case DbAdapter.PIC_UPLOADED:
		    		titleRes = R.string.picture_already_uploaded;
		    		break;
	    	}
	    	menu.add(0, UPLOAD_ID, 0, titleRes);
	    }
	    
		menu.getItem(UPLOAD_ID).setEnabled(enabled);
		
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		 currentContextMenuId = -1;
	    switch(item.getItemId()) {
		    case DELETE_ID:
		    {
			     dbAdapter.deletePicture(info.id);
			     refreshGallery();
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
		if (prefAdapter.isPhotoBGUploadEnabled()){
			for (int i = 0; i < ids.length; i++){
				dbAdapter.setPictureUploadState(ids[i], DbAdapter.PIC_PENDING_UPLOAD);
			}
		}
		else{
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
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        switch(requestCode) {
	        case ACTIVITY_NEW_PICTURE:
	        	if (resultCode == RESULT_OK){
	        		try {
	        			File tempFile = convertImageUriToFile(imageUri,this);
						FileInputStream fis = new FileInputStream(tempFile);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] buffer = new byte[1024];
						int read;
						do{
							read = fis.read(buffer);
							if (read > 0){
								bos.write(buffer,0,read);
							}
						} while(read >= 0);
						fis.close();
						tempFile.delete();
						dbAdapter.writePicture(bos.toByteArray());
						refreshGallery();
		        		if (prefAdapter.autoUploadPhotosEnabled()){
		        			Cursor c = dbAdapter.fetchLastPicture();
		        			long id = c.getLong(c.getColumnIndex(DbAdapter.PIX_KEY_ID));
		        			c.close();
		        			uploadPhotos(id);
		        		}
					} catch (Exception e){
						
					}
	        		
	        		
	        	}
	            break;
        }
        
    }
	
	public static File convertImageUriToFile (Uri imageUri, Activity activity)  {
		Cursor cursor = null;
		try {
		    String [] proj={MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID, MediaStore.Images.ImageColumns.ORIENTATION};
		    cursor = activity.managedQuery( imageUri,
		            proj, // Which columns to return
		            null,       // WHERE clause; which rows to return (all rows)
		            null,       // WHERE clause selection arguments (none)
		            null); // Order-by clause (ascending by name)
		    int file_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		    int orientation_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION);
		    if (cursor.moveToFirst()) {
		        String orientation =  cursor.getString(orientation_ColumnIndex);
		        return new File(cursor.getString(file_ColumnIndex));
		    }
		    return null;
		} catch (Exception e){
			return null;
		} finally {
		    if (cursor != null) {
		        cursor.close();
		    }
		}
		}
	
	protected void onResume(){
		super.onResume();
		uploadPics.setEnabled(prefAdapter.isNetworkEnabled());
	}
	
	protected void refreshGallery(){
		if (currentContextMenuId != -1){
			closeContextMenu();
		}
		((ImageAdapter) picGallery.getAdapter()).notifyDataSetChanged();
	}
	
	public void onImageUploaded(long id){
		if (prefAdapter.autoDeletePhotosEnabled()){
			dbAdapter.deletePicture(id);
			refreshGallery();
		}
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
	    
	    private void putBitmapInCache(long imageId, Bitmap bm){
	    	pictureIDs.add(imageId);
	        bms.add(bm);
	        if (pictureIDs.size() > PIC_CACHE_LIMIT){
	        	int minIndex = PIC_CACHE_LIMIT;
	        	long min = imageId;
	        	int maxIndex = PIC_CACHE_LIMIT;
	        	long max = imageId;
	        	for (int i = 0; i < PIC_CACHE_LIMIT; i++){
	        		long curId = pictureIDs.get(i);
	        		if (curId < min){
	        			min = curId;
	        			minIndex = i;
	        		}
	        		else if (curId > max){
	        			max = curId;
	        			maxIndex = i;
	        		}
	        	}
	        	if (min != imageId){
	        		pictureIDs.remove(minIndex);
	        		bms.remove(minIndex);
	        	}
	        	else if (max != imageId){
	        		pictureIDs.remove(maxIndex);
	        		bms.remove(maxIndex);
	        	}
	        }
	    }
	    
	    private Bitmap getBitmap(long imageId) throws IOException{
	    	int index = pictureIDs.indexOf(imageId);
	    	if (index == -1){
		        
		        Cursor c = dbAdapter.fetchPicture(imageId);
		        String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
		        c.close();
		        
		        FileInputStream fis = new FileInputStream(picFileName);
		        
		        BitmapFactory.Options opts = new BitmapFactory.Options();
		        
		        opts.inSampleSize = 8;
		        
		        Bitmap bm = BitmapFactory.decodeStream(fis,null,opts);
		        fis.close();
		        
		        putBitmapInCache(imageId, bm);
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
	    	long id = 0;
		    if (c.getCount() > position){
		    	c.moveToFirst();
		    	for (int i = 0; i < position; i++)
		    		c.moveToNext();
		    	id = c.getLong(c.getColumnIndex(DbAdapter.PIX_KEY_ID));
		    }
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
				if (dbAdapter.uploadPhoto(address.getMacAddress(), prefAdapter.getUploadAddress(), prefAdapter.getNickName(), CameraReview.this, ids[i])){
					numUploaded++;
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


