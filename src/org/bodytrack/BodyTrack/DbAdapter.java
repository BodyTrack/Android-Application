package org.bodytrack.BodyTrack;

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
                + "time integer not null, pic string not null, uploaded integer not null);";
    //fields of photo table
	public static final String PIX_TABLE = "pix";
	public static final String	PIX_KEY_ID = "_id";
	public static final String	PIX_KEY_TIME = "time";
	public static final String PIX_KEY_PIC = "pic";
	public static final String PIX_KEY_UPLOADED = "uploaded";
	

	//Accelerometer table creation
	   private static final String ACCEL_TABLE_CREATE =
	        "create table accel (_id integer primary key autoincrement, "
	                + "time integer not null, xvalue integer not null, yvalue integer not null, " +
	                		"zvalue integer not null);";
	//fields of Accelerometer
	   public static final String ACCEL_TABLE = "accel";
	   public static final String ACCEL_KEY_ID = "_id";
	   public static final String ACCEL_KEY_TIME = "time";
	   public static final String ACCEL_KEY_X = "xvalue";
	   public static final String ACCEL_KEY_Y = "yvalue";
	   public static final String ACCEL_KEY_Z = "zvalue";
	   
	   
	   private static final String STACK_TABLE_CREATE =
	        "create table stack (_id integer primary key autoincrement, "
	                + "channel text not null, data text not null);";
		public static final String STACK_TABLE = "stack";
		public static final String STACK_KEY_ID = "_id";
		public static final String STACK_KEY_DATA = "data";
		public static final String STACK_KEY_CHANNEL = "channel";
    
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
			db.execSQL(STACK_TABLE_CREATE);
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
	
	public long writeAcceleration(float[] values){
		ContentValues accToPut = new ContentValues();
		accToPut.put(ACCEL_KEY_TIME, System.currentTimeMillis());
		accToPut.put(ACCEL_KEY_X, values[0]);
		accToPut.put(ACCEL_KEY_Y, values[1]);
		accToPut.put(ACCEL_KEY_Z, values[2]);
		return mDb.insert(ACCEL_TABLE, null, accToPut);
	}
	
	public Cursor fetchAllAccelerations(){
		return mDb.query(ACCEL_TABLE, new String[] {ACCEL_KEY_ID,ACCEL_KEY_TIME,ACCEL_KEY_X,
				ACCEL_KEY_Y, ACCEL_KEY_Z},
                null, null, null, null, ACCEL_KEY_TIME);
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
        return mDb.query(LOCATION_TABLE, new String[] {LOC_KEY_ID, LOC_KEY_TIME, LOC_KEY_LATITUDE, 
        		LOC_KEY_LONGITUDE, LOC_KEY_ACCURACY, LOC_KEY_ALTITUDE,
        		LOC_KEY_BEARING, LOC_KEY_PROVIDER, LOC_KEY_SPEED},
                null, null, null, null, LOC_KEY_TIME);
    }
    
    public Cursor fetchAllPics() {
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOADED}, null, null, null, null, LOC_KEY_TIME);
    }
    
    public Cursor fetchAllUnuploadedPics() {
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOADED}, PIX_KEY_UPLOADED + " = 0", null, null, null, LOC_KEY_TIME);
    }
    
    public Cursor fetchPicture(long id) {

        Cursor mCursor =

            mDb.query(true, PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOADED}, PIX_KEY_ID + "=" + id, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    public Cursor fetchLastPicture(){
    	Cursor mCursor =

            mDb.query(true, PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOADED}, "1", null,
                    null, null, PIX_KEY_ID + " desc", "1");
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    public long setPictureUploaded(long id, boolean uploaded){
    	ContentValues updateUploaded = new ContentValues();
    	updateUploaded.put(PIX_KEY_UPLOADED, uploaded ? 1 : 0);
    	return mDb.update(PIX_TABLE, updateUploaded, PIX_KEY_ID + "=" + id, null);
    }
    
    public long deletePicture(long id){
    	Cursor c = fetchPicture(id);
    	String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
        c.close();
        
        mCtx.deleteFile(picFileName);
        
        return mDb.delete(PIX_TABLE, PIX_KEY_ID + "=" + id, null);
    }
    
    public long deleteUploadedPictures(){
    	Cursor c = mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOADED}, PIX_KEY_UPLOADED + " = 1", null, null, null, LOC_KEY_TIME);
    	c.moveToFirst();
    	while (!c.isAfterLast()){
    		String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
    		 mCtx.deleteFile(picFileName);
    		c.moveToNext();
    	}
    	
        return mDb.delete(PIX_TABLE, PIX_KEY_UPLOADED + "=1", null);
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
		String picFileName = "pic_" + currentTime + ".jpg";
		
		FileOutputStream fos = mCtx.openFileOutput(picFileName, Context.MODE_PRIVATE);
		fos.write(picture);
		fos.close();
		
		picToPut.put(PIX_KEY_PIC, picFileName);
		picToPut.put(PIX_KEY_TIME, System.currentTimeMillis());
		picToPut.put(PIX_KEY_UPLOADED, 0);
		long result = mDb.insert(PIX_TABLE, null, picToPut);
		if (result == 0){
			mCtx.deleteFile(picFileName);
		}
		return result;
	}
	public Cursor fetchAllQueries()
	{
		return mDb.query(STACK_TABLE, new String[]{STACK_KEY_ID,STACK_KEY_CHANNEL,STACK_KEY_DATA},null, null,null,null, STACK_KEY_ID);
	}
	//TODO: Need to parse the string correctly for the database (group timestamp, X,Y,Z)
	public long writeQuery(String channelName, ArrayList<String> values)
	{
		String data = "";
		for(int i=0; i < values.size(); i++)
		{
			if((i+1) == values.size())
			{
				data = data + values.get(i);
			}
			else
			{
				data = data + values.get(i) + ",";
			}
		}
		ContentValues queryToPut = new ContentValues();
		queryToPut.put(STACK_KEY_CHANNEL, channelName);
		queryToPut.put(STACK_KEY_DATA, data);
		return mDb.insert(STACK_TABLE,null, queryToPut);
	}

	public int delete(int stackId) {
		return mDb.delete(STACK_TABLE, "_id=" + stackId,null);
	}
}
