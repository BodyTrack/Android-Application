package org.bodytrack.BodyTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

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
	
	
	//Location table creation SQL
	
    private static final String LOCATION_TABLE_CREATE =
        "create table location (_id integer primary key autoincrement, "
                + "latitude real not null, longitude real not null, time real not null,"
                + "accuracy real, altitude real, bearing real, provider text,"
                + "speed real);";
	//fields of location table    
	public static final String LOCATION_TABLE = "location";
	public static final String LOC_KEY_ID = "_id";
	public static final String LOC_KEY_TIME = "time";
	public static final String LOC_KEY_LATITUDE = "latitude";
	public static final String LOC_KEY_LONGITUDE = "longitude";
	public static final String LOC_KEY_ACCURACY = "accuracy";
	public static final String LOC_KEY_ALTITUDE = "altitude";
	public static final String LOC_KEY_BEARING = "bearing";
	public static final String LOC_KEY_PROVIDER = "provider";
	public static final String LOC_KEY_SPEED = "speed";


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
	

	//Accelerometer table creation
	   private static final String ACCEL_TABLE_CREATE =
	        "create table accel (_id integer primary key autoincrement, "
	                + "time integer not null, xvalue real not null, yvalue real not null, " +
	                		"zvalue real not null);";
	//fields of Accelerometer
	   public static final String ACCEL_TABLE = "accel";
	   public static final String ACCEL_KEY_ID = "_id";
	   public static final String ACCEL_KEY_TIME = "time";
	   public static final String ACCEL_KEY_X = "xvalue";
	   public static final String ACCEL_KEY_Y = "yvalue";
	   public static final String ACCEL_KEY_Z = "zvalue";
	   
	 //fields of wifi
	   public static final String WIFI_TABLE = "wifi";
	   public static final String WIFI_KEY_ID = "_id";
	   public static final String WIFI_KEY_TIME = "time";
	   public static final String WIFI_KEY_SSID = "ssid";
	   public static final String WIFI_KEY_BSSID = "bssid";
	   
	 //wifi table creation
	   private static final String WIFI_TABLE_CREATE = "create table " + WIFI_TABLE + " (" +
	   			WIFI_KEY_ID + " integer primary key autoincrement, " +
	   			WIFI_KEY_TIME + " integer not null, " +
	   			WIFI_KEY_SSID + " string not null," +
	   			WIFI_KEY_BSSID + " string nto null);";
	   
	   
	 //fields of gyroscope
	   public static final String GYRO_TABLE = "gyro";
	   public static final String GYRO_KEY_ID = "_id";
	   public static final String GYRO_KEY_TIME = "time";
	   public static final String GYRO_KEY_X = "xvalue";
	   public static final String GYRO_KEY_Y = "yvalue";
	   public static final String GYRO_KEY_Z = "zvalue";
	   
	 //gyroscope table creation
	   private static final String GYRO_TABLE_CREATE = "create table " + GYRO_TABLE + " (" +
	   				GYRO_KEY_ID + " integer primary key autoincrement, " +
	   				GYRO_KEY_TIME + " integer not null, " +
	   				GYRO_KEY_X + " real not null," +
	   				GYRO_KEY_Y + " real not null," +
	   				GYRO_KEY_Z + " real not null);";
    
    private DatabaseHelper mDbHelper;
    private Context mCtx;
    private SQLiteDatabase mDb;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DbAdapter.DB_NAME, null, DbAdapter.DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			//create the 3 database tables
			
			db.execSQL(LOCATION_TABLE_CREATE);
			db.execSQL(BARCODE_TABLE_CREATE);
			db.execSQL(PIX_TABLE_CREATE);
			db.execSQL(ACCEL_TABLE_CREATE);
			db.execSQL(WIFI_TABLE_CREATE);
			db.execSQL(GYRO_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
		}	
	}
	
	public DbAdapter(Context ctx) {
		this.mCtx = ctx;
	}
	
	public DbAdapter open() throws SQLException{
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}
	
	public long writeAcceleration(long timestamp, float[] values){
		ContentValues accToPut = new ContentValues();
		accToPut.put(ACCEL_KEY_TIME, timestamp);
		accToPut.put(ACCEL_KEY_X, values[0]);
		accToPut.put(ACCEL_KEY_Y, values[1]);
		accToPut.put(ACCEL_KEY_Z, values[2]);
		return mDb.insert(ACCEL_TABLE, null, accToPut);
	}
	
	public Cursor fetchAllAccelerations(){
		return fetchAccelerations(null);
	}
	
	public Cursor fetchAccelerations(long limit){
		return fetchAccelerations("" + limit);
	}
	
	private Cursor fetchAccelerations(String limit){
		return mDb.query(ACCEL_TABLE, new String[] {ACCEL_KEY_ID,ACCEL_KEY_TIME,ACCEL_KEY_X,
				ACCEL_KEY_Y, ACCEL_KEY_Z},
                null, null, null, null, ACCEL_KEY_TIME, limit);
	}
	
	public long writeLocation(Location loc)
	{
		ContentValues locToPut = new ContentValues();
		locToPut.put(LOC_KEY_TIME, loc.getTime());
		locToPut.put(LOC_KEY_LATITUDE, loc.getLatitude());
		locToPut.put(LOC_KEY_LONGITUDE, loc.getLongitude());
		locToPut.put(LOC_KEY_ACCURACY, loc.getAccuracy());
		locToPut.put(LOC_KEY_ALTITUDE, loc.getAltitude());
		locToPut.put(LOC_KEY_BEARING, loc.getBearing());
		locToPut.put(LOC_KEY_PROVIDER, loc.getProvider());
		locToPut.put(LOC_KEY_SPEED, loc.getSpeed());
		
		return mDb.insert(LOCATION_TABLE, null, locToPut);
	}
	
	public Location getLocationData(Cursor c){
		Location loc = new Location(c.getString(c.getColumnIndex(LOC_KEY_PROVIDER)));
		loc.setAccuracy(c.getFloat(c.getColumnIndex(LOC_KEY_ACCURACY)));
		loc.setAltitude(c.getDouble(c.getColumnIndex(LOC_KEY_ALTITUDE)));
		loc.setBearing(c.getFloat(c.getColumnIndex(LOC_KEY_BEARING)));
		loc.setLatitude(c.getDouble(c.getColumnIndex(LOC_KEY_LATITUDE)));
		loc.setLongitude(c.getDouble(c.getColumnIndex(LOC_KEY_LONGITUDE)));
		loc.setSpeed(c.getFloat(c.getColumnIndex(LOC_KEY_SPEED)));
		loc.setTime(c.getLong(c.getColumnIndex(LOC_KEY_TIME)));
		return loc;
	}
	
	public long getLocationId(Cursor c){
		return c.getLong(c.getColumnIndex(LOC_KEY_ID));
	}
	
	public long deleteLocation(long id){
		return mDb.delete(LOCATION_TABLE, LOC_KEY_ID + "=" + id, null);
	}
	
	public long deleteAcceleration(long id) {
		return mDb.delete(ACCEL_TABLE, ACCEL_KEY_ID + "=" + id, null);
	}
	
	
	//WARNING: TIME MUST BE FIRST COLUMN IN QUERIES. UPLOADER CODE DEPENDS ON THIS
    public Cursor fetchAllLocations() {
        return fetchLocations(null);
    }
    
    public Cursor fetchLocations(long limit){
    	return fetchLocations("" + limit);
    }
    
    private Cursor fetchLocations(String limit){
    	return mDb.query(LOCATION_TABLE, new String[] {LOC_KEY_ID, LOC_KEY_TIME, LOC_KEY_LATITUDE, 
        		LOC_KEY_LONGITUDE, LOC_KEY_ACCURACY, LOC_KEY_ALTITUDE,
        		LOC_KEY_BEARING, LOC_KEY_PROVIDER, LOC_KEY_SPEED},
                null, null, null, null, LOC_KEY_TIME, limit);
    }
    
    public Cursor fetchAllPics() {
    	return fetchPics(null);
    }
    
    public Cursor fetchPics(long limit){
    	return fetchPics("" + limit);
    }
    
    private Cursor fetchPics(String limit){
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, null, null, null, null, LOC_KEY_TIME, limit);
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
    	updateUploaded.put(PIX_KEY_UPLOAD_STATE, state);
    	return mDb.update(PIX_TABLE, updateUploaded, PIX_KEY_ID + "=" + id, null);
    }
    
    public long deletePicture(long id){
    	Cursor c = fetchPicture(id);
    	if (c.getCount() > 0){
	    	String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
	    	new File(picFileName).delete();
    	}
        c.close();
        
       
        
        return mDb.delete(PIX_TABLE, PIX_KEY_ID + "=" + id, null);
    }
    
    public long deleteUploadedPictures(){
    	Cursor c = mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE}, PIX_KEY_UPLOAD_STATE + " = 1", null, null, null, LOC_KEY_TIME);
    	c.moveToFirst();
    	while (!c.isAfterLast()){
    		String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
    		 mCtx.deleteFile(picFileName);
    		c.moveToNext();
    	}
    	
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

		return mDb.insert(BARCODE_TABLE, null, codeToPut);
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
		if (result == 0){
			mCtx.deleteFile(picFileName);
		}
		return result;
	}
	
	public long writeWifi(long timeFound, String ssid, String bssid){
		ContentValues wifiToPut = new ContentValues();
		wifiToPut.put(WIFI_KEY_TIME, timeFound);
		wifiToPut.put(WIFI_KEY_SSID, ssid);
		wifiToPut.put(WIFI_KEY_BSSID, bssid);
		
		return mDb.insert(WIFI_TABLE, null, wifiToPut);
	}
	
	public long deleteWifi(long id){
		return mDb.delete(WIFI_TABLE, WIFI_KEY_ID + "=" + id, null);
	}
	
	public Cursor fetchAllWifis(){
		return fetchWifis(null);
	}
	
	public Cursor fetchWifis(long limit){
		return fetchWifis("" + limit);
	}
	
	private Cursor fetchWifis(String limit){
		return mDb.query(WIFI_TABLE, new String[] {WIFI_KEY_ID, WIFI_KEY_TIME, WIFI_KEY_SSID,
				WIFI_KEY_BSSID},
                null, null, null, null, WIFI_KEY_TIME, limit);
	}
	
	public long writeGyro(long timestamp, float[] values){
		ContentValues gyroToPut = new ContentValues();
		gyroToPut.put(GYRO_KEY_TIME, timestamp);
		gyroToPut.put(GYRO_KEY_X, values[0]);
		gyroToPut.put(GYRO_KEY_Y, values[1]);
		gyroToPut.put(GYRO_KEY_Z, values[2]);
		
		return mDb.insert(GYRO_TABLE, null, gyroToPut);
	}
	
	public long deleteGyro(long id){
		return mDb.delete(GYRO_TABLE, GYRO_KEY_ID + "=" + id, null);
	}
}
