package org.bodytrack.BodyTrack;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
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
import org.bodytrack.BodyTrack.Activities.CameraReview;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Environment;

/**
 * This class wraps database operations.
 */

public class DbAdapter {
	public static final String TAG = "DbAdapter";
	public static enum sqlTypes {
		INTEGER, REAL, TEXT
	}
	
	private static String DB_NAME = "BodytrackDB";
	private static int DB_VERSION = 1;
	
	public static final int PIC_NOT_UPLOADED = 0;
	public static final int PIC_PENDING_UPLOAD = 1;
	public static final int PIC_UPLOADED = 2;


    //Barcode table creation SQL
    private static final String BARCODE_TABLE_CREATE =
        "create table barcode (_id integer primary key autoincrement, "
                + "time integer not null, barcode integer not null);";
    //fields of barcode table
	public static final String BARCODE_TABLE = "barcode";
	public static final String BC_KEY_ID = "_id";
	public static final String BC_KEY_TIME = "time";
	public static final String BC_KEY_BARCODE = "barcode";
	
    //Photo table creation SQL
    private static final String PIX_TABLE_CREATE =
        "create table pix (_id integer primary key autoincrement, "
                + "time integer not null, pic string not null, uploadstate integer not null);";
    //fields of photo table
	public static final String PIX_TABLE = "pix";
	public static final String	PIX_KEY_ID = "_id";
	public static final String	PIX_KEY_TIME = "time";
	public static final String PIX_KEY_PIC = "pic";
	public static final String PIX_KEY_UPLOAD_STATE = "uploadstate";
	   
	   
	 //generic key names
	   public static final String KEY_ID = "_id";
	   public static final String KEY_DATA = "data";
	   
	 //new location table
	   public static final String NEW_LOC_TABLE = "newlocs";
	   
	   private static final String NEW_LOC_TABLE_CREATE = "create table " + NEW_LOC_TABLE + " (" +
	   					KEY_ID + " integer primary key autoincrement, " +
	   					KEY_DATA + " String not null);";
	   
	 //new acceleration table
	   public static final String NEW_ACC_TABLE = "newacc";
	   
	   
	   private static final String NEW_ACC_TABLE_CREATE = "create table " + NEW_ACC_TABLE + " (" +
			KEY_ID + " integer primary key autoincrement, " +
				KEY_DATA + " String not null);";
	   
	 //new gyroscope table
	   public static final String NEW_GYRO_TABLE = "newgyro";
	   
	   private static final String NEW_GYRO_TABLE_CREATE = "create table " + NEW_GYRO_TABLE + " (" +
					   KEY_ID + " integer primary key autoincrement, " +
						KEY_DATA + " String not null);";
	   
	 //new wifi table
	   public static final String NEW_WIFI_TABLE = "newwifi";
	   
	   private static final String NEW_WIFI_TABLE_CREATE = "create table " + NEW_WIFI_TABLE + " (" +
	   KEY_ID + " integer primary key autoincrement, " +
		KEY_DATA + " String not null);";
	   
	 //new light table
	   public static final String NEW_LIGHT_TABLE = "newlight";
	   
	   private static final String NEW_LIGHT_TABLE_CREATE = "create table " + NEW_LIGHT_TABLE + " (" +
	   KEY_ID + " integer primary key autoincrement, " +
		KEY_DATA + " String not null);";
	   
	 //new temp table
	   public static final String NEW_TEMP_TABLE = "newtemp";
	   
	   private static final String NEW_TEMP_TABLE_CREATE = "create table " + NEW_TEMP_TABLE + " (" +
	   KEY_ID + " integer primary key autoincrement, " +
		KEY_DATA + " String not null);";
	   
	 //new orientation table
	   public static final String NEW_ORNT_TABLE = "newornt";
	   
	   private static final String NEW_ORNT_TABLE_CREATE = "create table " + NEW_ORNT_TABLE + " (" +
	   KEY_ID + " integer primary key autoincrement, " +
		KEY_DATA + " String not null);";
	   
    private DatabaseHelper mDbHelper;
    private Context mCtx;
    private SQLiteDatabase mDb;
    
    private BTStatisticTracker btStats;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		public DatabaseHelper(Context context) {
			super(context, DbAdapter.DB_NAME, null, DbAdapter.DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			//create the 3 database tables
			
			db.execSQL(BARCODE_TABLE_CREATE);
			db.execSQL(PIX_TABLE_CREATE);
			
			db.execSQL(NEW_LOC_TABLE_CREATE);
			db.execSQL(NEW_ACC_TABLE_CREATE);
			db.execSQL(NEW_GYRO_TABLE_CREATE);
			db.execSQL(NEW_WIFI_TABLE_CREATE);
			db.execSQL(NEW_LIGHT_TABLE_CREATE);
			db.execSQL(NEW_TEMP_TABLE_CREATE);
			db.execSQL(NEW_ORNT_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
		}	
	}
	
	private static DbAdapter instance = null;
	
	public static DbAdapter getDbAdapter(Context context){
		if (instance == null)
			instance = new DbAdapter(context).open();
		return instance;
	}
	
	private DbAdapter(Context ctx) {
		btStats = BTStatisticTracker.getInstance();
		this.mCtx = ctx;
	}
	
	public DbAdapter open() throws SQLException{
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		mDb.setLockingEnabled(true);
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}
	
	public long writeLocations(List<Object[]> data){
		if (data.size() == 0)
			return 0;
		long start = System.currentTimeMillis();
		JSONArray dataArray = new JSONArray();
		for (Iterator<Object[]> iter = data.iterator(); iter.hasNext();){
			Object[] curDat = iter.next();
			Location loc = (Location) curDat[0];
			JSONArray locData = new JSONArray();
			try{
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
			catch (Exception e){
			}
		}
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		long retVal = mDb.insert(NEW_LOC_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite();
		return retVal;
	}
	
	public long writeAccelerations(List<Object[]> data){
		if (data.size() == 0)
			return 0;
		long start = System.currentTimeMillis();
		JSONArray dataArray = new JSONArray();
		for (Iterator<Object[]> iter = data.iterator(); iter.hasNext();){
			Object[] curDat = iter.next();
			JSONArray locData = new JSONArray();
			try{
				locData.put(((Long) curDat[0]) / 1000.0);
				for (int i = 0; i < 3; i++)
					locData.put((Float) curDat[1+i]);
				dataArray.put(locData);
			}
			catch (Exception e){
			}
		}
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues accToPut = new ContentValues();
		accToPut.put(KEY_DATA, jsonData);
		long retVal = mDb.insert(NEW_ACC_TABLE, null, accToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite();
		return retVal;
	}
	
	public long writeWifis(List<Object[]> data){
		if (data.size() == 0)
			return 0;
		long start = System.currentTimeMillis();
		JSONArray dataArray = new JSONArray();
		for (Iterator<Object[]> iter = data.iterator(); iter.hasNext();){
			Object[] curDat = iter.next();
			JSONArray locData = new JSONArray();
			try{
				locData.put(((Long) curDat[0]) / 1000.0);
				for (int i = 0; i < 2; i++)
					locData.put((String) curDat[1+i]);
				dataArray.put(locData);
			}
			catch (Exception e){
			}
		}
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		long retVal = mDb.insert(NEW_WIFI_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite();
		return retVal;
	}
	
	public long writeGyros(List<Object[]> data){
		if (data.size() == 0)
			return 0;
		long start = System.currentTimeMillis();
		JSONArray dataArray = new JSONArray();
		for (Iterator<Object[]> iter = data.iterator(); iter.hasNext();){
			Object[] curDat = iter.next();
			JSONArray locData = new JSONArray();
			try{
				locData.put(((Long) curDat[0]) / 1000.0);
				for (int i = 0; i < 3; i++)
					locData.put((Float) curDat[1+i]);
				dataArray.put(locData);
			}
			catch (Exception e){
			}
		}
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		long retVal = mDb.insert(NEW_GYRO_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite();
		return retVal;
	}
	
	public long writeOrientations(List<Object[]> data){
		if (data.size() == 0)
			return 0;
		long start = System.currentTimeMillis();
		JSONArray dataArray = new JSONArray();
		for (Iterator<Object[]> iter = data.iterator(); iter.hasNext();){
			Object[] curDat = iter.next();
			JSONArray locData = new JSONArray();
			try{
				locData.put(((Long) curDat[0]) / 1000.0);
				for (int i = 0; i < 3; i++)
					locData.put((Float) curDat[1+i]);
				dataArray.put(locData);
			}
			catch (Exception e){
			}
		}
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		long retVal = mDb.insert(NEW_ORNT_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite();
		return retVal;
	}
	
	public long writeLights(List<Object[]> data){
		if (data.size() == 0)
			return 0;
		long start = System.currentTimeMillis();
		JSONArray dataArray = new JSONArray();
		for (Iterator<Object[]> iter = data.iterator(); iter.hasNext();){
			Object[] curDat = iter.next();
			JSONArray locData = new JSONArray();
			try{
				locData.put(((Long) curDat[0]) / 1000.0);
				locData.put((Float) curDat[1]);
				dataArray.put(locData);
			}
			catch (Exception e){
			}
		}
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		long retVal = mDb.insert(NEW_LIGHT_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite();
		return retVal;
	}
	
	public long writeTemps(List<Object[]> data){
		if (data.size() == 0)
			return 0;
		long start = System.currentTimeMillis();
		JSONArray dataArray = new JSONArray();
		for (Iterator<Object[]> iter = data.iterator(); iter.hasNext();){
			Object[] curDat = iter.next();
			JSONArray locData = new JSONArray();
			try{
				locData.put(((Long) curDat[0]) / 1000.0);
				locData.put((Float) curDat[1]);
				dataArray.put(locData);
			}
			catch (Exception e){
			}
		}
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		long retVal = mDb.insert(NEW_TEMP_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite();
		return retVal;
	}
    
    public Cursor fetchAllPics() {
    	return fetchPics(null);
    }
    
    public Cursor fetchPics(long limit){
    	return fetchPics("" + limit);
    }
    
    private Cursor fetchPics(String limit){
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, null, null, null, null, PIX_KEY_ID, limit);
    }
    
    public Cursor fetchAllUnuploadedPics() {
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, PIX_KEY_UPLOAD_STATE + " = " + PIC_NOT_UPLOADED, null, null, null, PIX_KEY_ID);
    }
    
    public Cursor fetchAllPendingUploadPics(){
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, PIX_KEY_UPLOAD_STATE + " = " + PIC_PENDING_UPLOAD, null, null, null, PIX_KEY_ID);
    }
    
    public Cursor fetchFirstPendingUploadPic(){
    	Cursor c = mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, PIX_KEY_UPLOAD_STATE + " = " + PIC_PENDING_UPLOAD, null, null, null, PIX_KEY_ID, "1");
    	if (c != null){
    		c.moveToFirst();
    	}
    	return c;
    }
    
    public Cursor fetchPicture(long id) {

        Cursor mCursor =

            mDb.query(true, PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, PIX_KEY_ID + "=" + id, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    public Cursor fetchLastPicture(){
    	Cursor mCursor =

            mDb.query(true, PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, "1", null,
                    null, null, PIX_KEY_ID + " desc", "1");
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    public long setPictureUploadState(long id, int state){
    	ContentValues updateUploaded = new ContentValues();
    	switch (state){
    		case PIC_PENDING_UPLOAD:
    			btStats.out.println("Pic " + id + " marked for upload");
    			break;
    		case PIC_UPLOADED:
    			btStats.out.println("Pic " + id + " uploaded");
    			break;
    	}
    	updateUploaded.put(PIX_KEY_UPLOAD_STATE, state);
    	return mDb.update(PIX_TABLE, updateUploaded, PIX_KEY_ID + "=" + id, null);
    }
    
    public long deletePicture(long id){
    	Cursor c = fetchPicture(id);
    	if (c.getCount() > 0){
	    	String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
	    	new File(picFileName).delete();
	    	btStats.out.println("Pic " + id + " deleted");
    	}
        c.close();
        
       
        
        return mDb.delete(PIX_TABLE, PIX_KEY_ID + "=" + id, null);
    }
    
    public long deleteUploadedPictures(){
    	Cursor c = mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, PIX_KEY_UPLOAD_STATE + " = 1", null, null, null, PIX_KEY_ID);
    	c.moveToFirst();
    	while (!c.isAfterLast()){
    		String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
    		 mCtx.deleteFile(picFileName);
    		c.moveToNext();
    	}
    	c.close();
    	
        return mDb.delete(PIX_TABLE, PIX_KEY_UPLOAD_STATE + "=" + PIC_UPLOADED, null);
    }
    
	//WARNING: TIME MUST BE FIRST COLUMN IN QUERIES. UPLOADER CODE DEPENDS ON THIS
    public Cursor fetchAllBarcodes() {
        return mDb.query(BARCODE_TABLE, new String[] {BC_KEY_TIME, BC_KEY_ID, BC_KEY_BARCODE},
                null, null, null, null, BC_KEY_TIME);
    }
    
	public long writeBarcode(long barcode)
	{
		ContentValues codeToPut = new ContentValues();
		codeToPut.put(BC_KEY_BARCODE, barcode);
		codeToPut.put(BC_KEY_TIME, System.currentTimeMillis());
		
		btStats.addDbWrite();
		
		return mDb.insert(BARCODE_TABLE, null, codeToPut);
	}
	
	public long getSize(){
		Cursor c = mDb.rawQuery("pragma page_size;", null);
		c.moveToFirst();
		long size = c.getLong(0);
		c.close();
		c = mDb.rawQuery("pragma page_count;", null);
		c.moveToFirst();
		size *= c.getLong(0);
		c.close();
		return size;
	}
	
	public long writePicture(byte[] picture) throws IOException {
		ContentValues picToPut = new ContentValues();
		long currentTime = System.currentTimeMillis();
		String picFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/pic_" + currentTime + ".jpg";
		
		FileOutputStream fos;
		try{
			fos = new FileOutputStream(picFileName);
		}
		catch (IOException e){
			picFileName = mCtx.getFilesDir().getAbsolutePath() + "/pic_" + currentTime + ".jpg";
			fos = new FileOutputStream(picFileName);
		}
		fos.write(picture);
		fos.close();
		
		picToPut.put(PIX_KEY_PIC, picFileName);
		picToPut.put(PIX_KEY_TIME, System.currentTimeMillis());
		picToPut.put(PIX_KEY_UPLOAD_STATE, 0);
		long result = mDb.insert(PIX_TABLE, null, picToPut);
		btStats.addDbWrite();
		if (result == 0){
			mCtx.deleteFile(picFileName);
		}
		else{
			Cursor c = mDb.query(PIX_TABLE, new String[]{PIX_KEY_ID}, null, null, null, null, PIX_KEY_ID + " DESC", "1");
			if (c.moveToFirst()){
				long id = c.getLong(c.getColumnIndex(PIX_KEY_ID));
				btStats.out.println("Pic " + id + " created");
			}
			c.close();
		}
		return result;
	}
	
	private static JSONArray gpsChannelArray = null;
	private static final String[] gpsChannelArrayElements = {"latitude","longitude","altitude","uncertainty in meters","speed","bearing","provider"};
	
	public void uploadLocations(String macAdd, String uploadAdd, String devNickName){
		
		if (gpsChannelArray == null){
			gpsChannelArray = new JSONArray();
			for (int i = 0; i < gpsChannelArrayElements.length; i++){
				gpsChannelArray.put(gpsChannelArrayElements[i]);
			}
		}
		
		
		Cursor c = mDb.query(NEW_LOC_TABLE, new String[] {KEY_ID, KEY_DATA}, null, null, null, null, KEY_ID, "20");
		if (!c.moveToFirst()){
			c.close();
			return;
		}
		
		long lastId = uploadLogData(macAdd, uploadAdd, devNickName, gpsChannelArray.toString(), c);
		c.close();
		if (lastId >= 0){
			long start = System.currentTimeMillis();
			mDb.delete(NEW_LOC_TABLE, KEY_ID + "<=" + lastId, null);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
	    return;
	}
	
	private static JSONArray accChannelArray = null;
	private static final String[] accChannelArrayElements = {"acceleration_x","acceleration_y","acceleration_z"};
	
	public void uploadAccelerations(String macAdd, String uploadAdd, String devNickName){
		
		if (accChannelArray == null){
			accChannelArray = new JSONArray();
			for (int i = 0; i < accChannelArrayElements.length; i++){
				accChannelArray.put(accChannelArrayElements[i]);
			}
		}
		
		Cursor c = mDb.query(NEW_ACC_TABLE, new String[] {KEY_ID, KEY_DATA}, null, null, null, null, KEY_ID, "20");
		if (!c.moveToFirst()){
			c.close();
			return;
		}
		
		long lastId = uploadLogData(macAdd, uploadAdd, devNickName, accChannelArray.toString(), c);
		c.close();
		if (lastId >= 0){
			long start = System.currentTimeMillis();
			mDb.delete(NEW_ACC_TABLE, KEY_ID + "<=" + lastId, null);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
	    return;
	}
	
	private static JSONArray gyroChannelArray = null;
	private static final String[] gyroChannelArrayElements = {"angular_speed_x","angular_speed_y","angular_speed_z"};
	
	public void uploadGyros(String macAdd, String uploadAdd, String devNickName){
		
		if (gyroChannelArray == null){
			gyroChannelArray = new JSONArray();
			for (int i = 0; i < gyroChannelArrayElements.length; i++){
				gyroChannelArray.put(gyroChannelArrayElements[i]);
			}
		}
		
		
		Cursor c = mDb.query(NEW_GYRO_TABLE, new String[] {KEY_ID, KEY_DATA}, null, null, null, null, KEY_ID, "20");
		if (!c.moveToFirst()){
			c.close();
			return;
		}
		
		long lastId = uploadLogData(macAdd, uploadAdd, devNickName, gyroChannelArray.toString(), c);
		c.close();
		if (lastId >= 0){
			long start = System.currentTimeMillis();
			mDb.delete(NEW_GYRO_TABLE, KEY_ID + "<=" + lastId, null);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
	    return;
	}
	
	private static JSONArray ornChannelArray = null;
	private static final String[] ornChannelArrayElements = {"azimuth","pitch","roll"};
	
	public void uploadOrientations(String macAdd, String uploadAdd, String devNickName){
		
		if (ornChannelArray == null){
			ornChannelArray = new JSONArray();
			for (int i = 0; i < ornChannelArrayElements.length; i++){
				ornChannelArray.put(ornChannelArrayElements[i]);
			}
		}
		
		
		Cursor c = mDb.query(NEW_ORNT_TABLE, new String[] {KEY_ID, KEY_DATA}, null, null, null, null, KEY_ID, "20");
		if (!c.moveToFirst()){
			c.close();
			return;
		}
		
		long lastId = uploadLogData(macAdd, uploadAdd, devNickName, ornChannelArray.toString(), c);
		c.close();
		if (lastId >= 0){
			long start = System.currentTimeMillis();
			mDb.delete(NEW_ORNT_TABLE, KEY_ID + "<=" + lastId, null);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
	    return;
	}
	
	private static JSONArray lightChannelArray = null;
	private static final String[] lightChannelArrayElements = {"illuminance"};
	
	public void uploadIlluminances(String macAdd, String uploadAdd, String devNickName){
		
		if (lightChannelArray == null){
			lightChannelArray = new JSONArray();
			for (int i = 0; i < lightChannelArrayElements.length; i++){
				lightChannelArray.put(lightChannelArrayElements[i]);
			}
		}
		
		
		Cursor c = mDb.query(NEW_LIGHT_TABLE, new String[] {KEY_ID, KEY_DATA}, null, null, null, null, KEY_ID, "20");
		if (!c.moveToFirst()){
			c.close();
			return;
		}
		
		long lastId = uploadLogData(macAdd, uploadAdd, devNickName, lightChannelArray.toString(), c);
		c.close();
		if (lastId >= 0){
			long start = System.currentTimeMillis();
			mDb.delete(NEW_LIGHT_TABLE, KEY_ID + "<=" + lastId, null);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
	    return;
	}
	
	private static JSONArray tempChannelArray = null;
	private static final String[] tempChannelArrayElements = {"temperature"};
	
	public void uploadTemperatures(String macAdd, String uploadAdd, String devNickName){
		
		if (tempChannelArray == null){
			tempChannelArray = new JSONArray();
			for (int i = 0; i < tempChannelArrayElements.length; i++){
				tempChannelArray.put(tempChannelArrayElements[i]);
			}
		}
		
		
		Cursor c = mDb.query(NEW_TEMP_TABLE, new String[] {KEY_ID, KEY_DATA}, null, null, null, null, KEY_ID, "20");
		if (!c.moveToFirst()){
			c.close();
			return;
		}
		
		long lastId = uploadLogData(macAdd, uploadAdd, devNickName, tempChannelArray.toString(), c);
		c.close();
		if (lastId >= 0){
			long start = System.currentTimeMillis();
			mDb.delete(NEW_TEMP_TABLE, KEY_ID + "<=" + lastId, null);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
	    return;
	}
	
	private static JSONArray wifiChannelArray = null;
	private static JSONObject wifiSpecs = null;
	private static final String[] wifiChannelArrayElements = {"SSID", "BSSID"};
	
	public void uploadWifis(String macAdd, String uploadAdd, String devNickName){
		
		if (wifiChannelArray == null){
			wifiChannelArray = new JSONArray();
			for (int i = 0; i < wifiChannelArrayElements.length; i++){
				wifiChannelArray.put(wifiChannelArrayElements[i]);
			}
		}
		
		if (wifiSpecs == null){
			wifiSpecs = new JSONObject();
		 	try {
		 		JSONObject ssidInfo = new JSONObject();
			 	ssidInfo.put("type", "String");
				wifiSpecs.put("SSID", ssidInfo);
				wifiSpecs.put("BSSID", ssidInfo);
			} catch (JSONException e) {
			}
		 	
		}
		
		
		Cursor c = mDb.query(NEW_WIFI_TABLE, new String[] {KEY_ID, KEY_DATA}, null, null, null, null, KEY_ID, "20");
		if (!c.moveToFirst()){
			c.close();
			return;
		}
		
		long lastId = uploadLogData(macAdd, uploadAdd, devNickName, wifiChannelArray.toString(), wifiSpecs.toString(), c);
		c.close();
		if (lastId >= 0){
			long start = System.currentTimeMillis();
			mDb.delete(NEW_WIFI_TABLE, KEY_ID + "<=" + lastId, null);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
	    return;
	}
	
	public boolean uploadPhoto(String macAdd, String uploadAdd, String devNickName, final CameraReview camRev, final long id){
		Cursor c = fetchPicture(id);
		if (!c.moveToFirst()){
			c.close();
			return false;
		}
		String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
	    c.close();
	    
	    try{
	    
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
		    byte[] photoData = bos.toByteArray();
			ByteArrayBody bin = new ByteArrayBody(photoData, "image/jpeg", picFileName);
			MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			reqEntity.addPart("device_id",new StringBody(macAdd));
			reqEntity.addPart("device_class",new StringBody(android.os.Build.MODEL));
			reqEntity.addPart("dev_nickname",new StringBody(devNickName));
			reqEntity.addPart("photo",bin);
			
			HttpClient mHttpClient = new DefaultHttpClient();
	    	HttpPost postToServer = new HttpPost(uploadAdd);
			
			postToServer.setEntity(reqEntity);
			HttpResponse response = mHttpClient.execute(postToServer);
			StatusLine status = response.getStatusLine();
			long bytes = photoData.length;
			long overhead = reqEntity.getContentLength() - bytes;
			btStats.logBytesUploaded(bytes, overhead, status.getStatusCode());
			if (status.getStatusCode() >= 200 && status.getStatusCode() < 300){
				setPictureUploadState(id,DbAdapter.PIC_UPLOADED);
				if (camRev != null){
					camRev.runOnUiThread(new Runnable(){
						public void run(){
							camRev.onImageUploaded(id);
						}
					});
				}
				return true;
			}
	    }
	    catch (Exception e){
	    }
	    return false;
	}
	
	private long uploadLogData(String macAdd, String uploadAdd, String devNickName, String channels, Cursor dataCursor){
		return uploadLogData(macAdd, uploadAdd, devNickName, channels, null, dataCursor);
	}
	
	private long uploadLogData(String macAdd, String uploadAdd, String devNickName, String channels, String channelSpecs, Cursor dataCursor){
		StringBuffer dataToUpload = new StringBuffer();
		boolean first = true;
		
		long lastId = 0;
		
		long start = System.currentTimeMillis();
		
		while (!dataCursor.isAfterLast()){
			String curData = dataCursor.getString(dataCursor.getColumnIndex(KEY_DATA));
			lastId = dataCursor.getLong(dataCursor.getColumnIndex(KEY_ID));
			if (first){
				dataToUpload.append(curData);
				first = false;
			}
			else{
				dataToUpload.deleteCharAt(dataToUpload.length() - 1);
				dataToUpload.append(",");
				dataToUpload.append(curData.substring(1));
			}
			dataCursor.moveToNext();
		}
		String dataString = dataToUpload.toString();
		btStats.addTimeSpentGatheringAndJoining(System.currentTimeMillis() - start);
		if (uploadData(uploadAdd, macAdd, devNickName, channels, channelSpecs, dataString))
			return lastId;
		return -1;
	}
	
	private boolean uploadData(String uploadAdd, String macAdd, String devNickName, String channels, String channelSpecs, String data){
		long start = System.currentTimeMillis();
		try {
			HttpEntity reqEntity = null;
			if (data.length() > 1024 * 5){ //multipart seems to be preferable in majority of situations
	    		MultipartEntity mPartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
	    		mPartEntity.addPart("device_id", new StringBody(macAdd));
	    		mPartEntity.addPart("timezone", new StringBody("UTC"));
	    		mPartEntity.addPart("device_class", new StringBody(android.os.Build.MODEL));
	    		mPartEntity.addPart("dev_nickname", new StringBody(devNickName));
	    		mPartEntity.addPart("channel_names", new StringBody(channels));
		    	if (channelSpecs != null){
		    		mPartEntity.addPart("channel_specs",new StringBody(channelSpecs));
		    	}
		    	mPartEntity.addPart("data", new StringBody(data));
		    	reqEntity = mPartEntity;
			}
			else{
				List<NameValuePair> postParams = new LinkedList<NameValuePair>();
				postParams.add(new BasicNameValuePair("device_id",macAdd));
				postParams.add(new BasicNameValuePair("timezone","UTC"));
				postParams.add(new BasicNameValuePair("device_class",android.os.Build.MODEL));
				postParams.add(new BasicNameValuePair("dev_nickname", devNickName));
				postParams.add(new BasicNameValuePair("channel_names", channels));
				if (channelSpecs != null){
					postParams.add(new BasicNameValuePair("channel_specs", channelSpecs));
				}
				postParams.add(new BasicNameValuePair("data", data));
				reqEntity = new UrlEncodedFormEntity(postParams);
			}
	    	HttpClient mHttpClient = new DefaultHttpClient();
	    	HttpPost postToServer = new HttpPost(uploadAdd);
    		postToServer.setEntity(reqEntity);
    		HttpResponse response = mHttpClient.execute(postToServer);
    		int statusCode = response.getStatusLine().getStatusCode();
    		long bytes = data.length();
    		long overhead = reqEntity.getContentLength() - bytes;
    		btStats.logBytesUploaded(bytes, overhead, statusCode);
    		if (statusCode >= 200 && statusCode < 300){
	    		return true;
    		}
    	} catch (Exception e) {
    	}
    	finally{
    		btStats.addTimeSpentUploadingData(System.currentTimeMillis() - start);
    	}
    	return false;
	}
}
