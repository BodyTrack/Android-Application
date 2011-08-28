package org.bodytrack.BodyTrack;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.bodytrack.BodyTrack.Activities.HomeTabbed;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;

/**
 * This class wraps database operations.
 */

public class DbAdapter {
	public static final String TAG = "DbAdapter";
	public static enum sqlTypes {
		INTEGER, REAL, TEXT
	}
	
	private static final String DB_NAME = "BodytrackDB";
	private static final String EXTERNAL_DB_NAME = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DB_NAME;
	private static final int DB_VERSION = 6;
	
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
    
    //fields of photo table
	public static final String PIX_TABLE = "pix";
	public static final String	PIX_KEY_ID = "_id";
	public static final String	PIX_KEY_TIME = "time";
	public static final String PIX_KEY_PIC = "pic";
	public static final String PIX_KEY_UPLOAD_STATE = "uploadstate";
	public static final String PIX_KEY_COMMENT = "comment";
	   
	
	private static final String PIX_TABLE_CREATE =
        "create table " + PIX_TABLE + " (" + 
        PIX_KEY_ID + " integer primary key autoincrement, " +
         PIX_KEY_TIME + " integer not null, " +
         PIX_KEY_PIC + " string not null, " + 
         PIX_KEY_UPLOAD_STATE + " integer not null, " + 
         PIX_KEY_COMMENT + " string not null default '');";
	
	private static final String PIX_TABLE_ADD_COMMENT_FIELD = "alter table " + PIX_TABLE + " add " + PIX_KEY_COMMENT + " string not null default '';";
	   
	 //generic key names
	   public static final String KEY_TIME = "time";
	   public static final String KEY_DATA = "data";
	   
	 //new location table
	   public static final String NEW_LOC_TABLE = "newlocs";
	   
	   private static final String NEW_LOC_TABLE_CREATE = "create table " + NEW_LOC_TABLE + " (" +
	   					KEY_TIME + " integer primary key, " +
	   					KEY_DATA + " String not null);";
	   
	 //new acceleration table
	   public static final String NEW_ACC_TABLE = "newacc";
	   
	   
	   private static final String NEW_ACC_TABLE_CREATE = "create table " + NEW_ACC_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
				KEY_DATA + " String not null);";
	   
	   public static final String GRAV_ACC_TABLE = "gravacc";
	   
	   private static final String GRAV_ACC_TABLE_CREATE = "create table " + GRAV_ACC_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
	   KEY_DATA + " String not null);";
	   
	   public static final String LINE_ACC_TABLE = "lineacc";
	   
	   private static final String LINE_ACC_TABLE_CREATE = "create table " + LINE_ACC_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
	   KEY_DATA + " String not null);";
	   
	 //new gyroscope table
	   public static final String NEW_GYRO_TABLE = "newgyro";
	   
	   private static final String NEW_GYRO_TABLE_CREATE = "create table " + NEW_GYRO_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
						KEY_DATA + " String not null);";
	   
	 //new wifi table
	   public static final String NEW_WIFI_TABLE = "newwifi";
	   
	   private static final String NEW_WIFI_TABLE_CREATE = "create table " + NEW_WIFI_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
		KEY_DATA + " String not null);";
	   
	 //new light table
	   public static final String NEW_LIGHT_TABLE = "newlight";
	   
	   private static final String NEW_LIGHT_TABLE_CREATE = "create table " + NEW_LIGHT_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
		KEY_DATA + " String not null);";
	   
	 //new temp table
	   public static final String NEW_TEMP_TABLE = "newtemp";
	   
	   private static final String NEW_TEMP_TABLE_CREATE = "create table " + NEW_TEMP_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
		KEY_DATA + " String not null);";
	   
	 //new orientation table
	   public static final String NEW_ORNT_TABLE = "newornt";
	   
	   private static final String NEW_ORNT_TABLE_CREATE = "create table " + NEW_ORNT_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
		KEY_DATA + " String not null);";
	   
	 //pressure table
	   public static final String PRESS_TABLE = "pressure";
	   
	   private static final String PRESS_TABLE_CREATE = "create table " + PRESS_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
		KEY_DATA + " String not null);";
	   
	 //new barcode table
	   public static final String NEW_BC_TABLE = "newbc";
	   
	   private static final String NEW_BC_TABLE_CREATE = "create table " + NEW_BC_TABLE + " (" +
	   KEY_TIME + " integer primary key, " + 
	   	KEY_DATA + " String not null);";
	   	
	   	public static final String LOG_COMMENT_TABLE = "comment";
	   
	   private static final String LOG_COMMENT_TABLE_CREATE ="create table " + LOG_COMMENT_TABLE + " (" +
	   KEY_TIME + " integer primary key, " +
	   KEY_DATA + " String not null);";
	   
	   public static final String QUICK_COMMENT_TABLE = "qcomment";
	   
	   public static final String QCOMMENT_KEY_ID = "_id";
	   public static final String QCOMMENT_KEY_NAME = "name";
	   public static final String QCOMMENT_KEY_COMMENT = "comment";
	   
	   private static final String QUICK_COMMENT_TABLE_CREATE = "create table " + QUICK_COMMENT_TABLE + " ("+
	   QCOMMENT_KEY_ID + " integer primary key autoincrement, " +
	   QCOMMENT_KEY_NAME + " String not null, " +
	   QCOMMENT_KEY_COMMENT + " String not null);";
	   
    private DatabaseHelper mDbHelper;
    private Context mCtx;
    private SQLiteDatabase mDb;
    
    private HttpClient mHttpClient = new DefaultHttpClient();
    
    private BTStatisticTracker btStats;
    private PreferencesAdapter prefAdapter;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		 private BTStatisticTracker btStats;
		 private boolean external;
		
		private DatabaseHelper(Context context, boolean external) {
			super(context, external ? EXTERNAL_DB_NAME : DB_NAME, null, DB_VERSION);
			btStats = BTStatisticTracker.getInstance();
			this.external = external;
		}
		
		public boolean isExternal(){
			return external;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			//create the 3 database tables
			db.execSQL(PIX_TABLE_CREATE);
			
			db.execSQL(NEW_LOC_TABLE_CREATE);
			db.execSQL(NEW_ACC_TABLE_CREATE);
			db.execSQL(NEW_GYRO_TABLE_CREATE);
			db.execSQL(NEW_WIFI_TABLE_CREATE);
			db.execSQL(NEW_LIGHT_TABLE_CREATE);
			db.execSQL(NEW_TEMP_TABLE_CREATE);
			db.execSQL(NEW_ORNT_TABLE_CREATE);
			db.execSQL(NEW_BC_TABLE_CREATE);
			db.execSQL(LOG_COMMENT_TABLE_CREATE);
			db.execSQL(QUICK_COMMENT_TABLE_CREATE);
			db.execSQL(GRAV_ACC_TABLE_CREATE);
			db.execSQL(LINE_ACC_TABLE_CREATE);
			db.execSQL(PRESS_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			switch (oldVersion){
				case 1://log comment table missing
					db.execSQL(LOG_COMMENT_TABLE_CREATE);
				case 2://comment field missing from pic table
					db.execSQL(PIX_TABLE_ADD_COMMENT_FIELD);
				case 3://quick comment table missing
					db.execSQL(QUICK_COMMENT_TABLE_CREATE);
				case 4://linear and gravity acceleration tables missing
					db.execSQL(GRAV_ACC_TABLE_CREATE);
					db.execSQL(LINE_ACC_TABLE_CREATE);
				case 5://pressure table missing
					db.execSQL(PRESS_TABLE_CREATE);
				default:
					btStats.out.println("Upgraded database from " + oldVersion + " to " + newVersion);
					break;
			}
		}	
	}
	
	private static DbAdapter instance = null;
	
	public static DbAdapter getDbAdapter(Context context){
		if (instance == null)
			instance = new DbAdapter(context).open();
		return instance;
	}
	
	private DbAdapter(Context ctx) {
		prefAdapter = PreferencesAdapter.getInstance(ctx);
		btStats = BTStatisticTracker.getInstance();
		this.mCtx = ctx;
	}
	
	public DbAdapter open() throws SQLException{
		boolean external = prefAdapter.dbStoredExternally();
		while (true){
			try{
				mDbHelper = new DatabaseHelper(mCtx,external);
				mDb = mDbHelper.getWritableDatabase();
				mDb.setLockingEnabled(true);
				break;
			} catch (RuntimeException e){
				mDbHelper.close();
				if (!external)
					throw e;
				external = false;
				btStats.out.println("failed to open external db. opening db on internal memory!");
			}
		}
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}
	
	public void verifyDBLocation(Context ctxForProgress){
		boolean isExternal = mDbHelper.isExternal();
		boolean shouldBeExternal = prefAdapter.dbStoredExternally();
		if (isExternal != shouldBeExternal){
			SQLiteDatabase oldDatabase = mDb;
			DatabaseHelper oldHelper = mDbHelper;
			open();
			if (mDbHelper.isExternal() != isExternal){
				new MoveDatabaseTask().execute(ctxForProgress,oldDatabase,oldHelper);
			}
			else{
				oldDatabase.close();
				oldHelper.close();
			}
		}
	}
	
	private class MoveDatabaseTask extends AsyncTask<Object,Void,Void>{
		private ProgressDialog progress;
		@Override
		protected Void doInBackground(final Object... params) {
			((HomeTabbed) params[0]).runOnUiThread(new Runnable(){

				@Override
				public void run() {
					progress = new ProgressDialog((Context) params[0]);
					progress.setMessage("Moving data...");
					progress.setOnKeyListener(new DialogInterface.OnKeyListener() {

			    	    @Override
			    	    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			    	        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
			    	            return true; // Pretend we processed it
			    	        }
			    	        return false; // Any other keys are still processed as normal
			    	    }
			    	});
					progress.setCancelable(false);
					progress.show();
				}
				
			});
			copyDatabaseToCurrentDatabase((SQLiteDatabase) params[1], (DatabaseHelper) params[2]);
			while (progress == null)
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			return null;
		}
		
		protected void onPostExecute(Void result){
			progress.dismiss();
		}
		
	}
	
	private void copyDatabaseToCurrentDatabase(SQLiteDatabase oDb, DatabaseHelper oldHelper){
		long start = System.currentTimeMillis();
		int count = 0;
		count += copyGenericTable(NEW_LOC_TABLE,oDb);//move the generic tables
		count += copyGenericTable(NEW_ACC_TABLE,oDb);
		count += copyGenericTable(NEW_GYRO_TABLE,oDb);
		count += copyGenericTable(NEW_WIFI_TABLE,oDb);
		count += copyGenericTable(NEW_LIGHT_TABLE,oDb);
		count += copyGenericTable(NEW_TEMP_TABLE,oDb);
		count += copyGenericTable(NEW_ORNT_TABLE,oDb);
		count += copyGenericTable(LOG_COMMENT_TABLE,oDb);
		count += copyGenericTable(GRAV_ACC_TABLE,oDb);
		count += copyGenericTable(LINE_ACC_TABLE,oDb);
		count += copyGenericTable(PRESS_TABLE,oDb);
		count += copyPictureTable(oDb); //move picture table
		count += copyQuickCommentTable(oDb); //move quick comment table
		oDb.close();
		oldHelper.close();
		btStats.out.println("Took " + (System.currentTimeMillis() - start) + " ms to transfer " + count + " db entries!");
	}
	
	private long copyPictureTable(SQLiteDatabase oDb){
		Cursor c = oDb.query(PIX_TABLE, new String[] {PIX_KEY_TIME,PIX_KEY_PIC,PIX_KEY_UPLOAD_STATE,PIX_KEY_COMMENT}, null, null, null, null, PIX_KEY_ID);
		if (c == null)
			return 0;
		long count = 0;
		int timePos = c.getColumnIndex(PIX_KEY_TIME);
		int picPos = c.getColumnIndex(PIX_KEY_PIC);
		int statePos = c.getColumnIndex(PIX_KEY_UPLOAD_STATE);
		int cmtPos = c.getColumnIndex(PIX_KEY_COMMENT);
		if (c.moveToFirst()){
			do{
				ContentValues dataToPut = new ContentValues();
				dataToPut.put(PIX_KEY_TIME, c.getLong(timePos));
				dataToPut.put(PIX_KEY_PIC, c.getString(picPos));
				dataToPut.put(PIX_KEY_UPLOAD_STATE, c.getInt(statePos));
				dataToPut.put(PIX_KEY_COMMENT, c.getString(cmtPos));
				if (mDb.insert(PIX_TABLE, null, dataToPut) >= 0)
					count++;
			} while (c.moveToNext());
		}
		c.close();
		oDb.delete(PIX_TABLE, null, null);
		return count;
	}
	private long copyQuickCommentTable(SQLiteDatabase oDb){
		Cursor c = oDb.query(QUICK_COMMENT_TABLE, new String[]{QCOMMENT_KEY_NAME, QCOMMENT_KEY_COMMENT}, null, null, null, null, QCOMMENT_KEY_ID);
		if (c == null)
			return 0;
		long count = 0;
		int namePos = c.getColumnIndex(QCOMMENT_KEY_NAME);
		int cmtPos = c.getColumnIndex(QCOMMENT_KEY_COMMENT);
		if (c.moveToFirst()){
			do{
				ContentValues dataToPut = new ContentValues();
				dataToPut.put(QCOMMENT_KEY_NAME, c.getString(namePos));
				dataToPut.put(QCOMMENT_KEY_COMMENT, c.getString(cmtPos));
				if (mDb.insert(QUICK_COMMENT_TABLE, null, dataToPut) >= 0)
					count++;
			} while (c.moveToNext());
		}
		c.close();
		oDb.delete(QUICK_COMMENT_TABLE, null, null);
		return count;
	}
	
	private long copyGenericTable(String tableName, SQLiteDatabase oDb){
		Cursor c = oDb.query(tableName, new String[]{KEY_TIME,KEY_DATA}, null, null, null, null, KEY_TIME);
		if (c == null)
			return 0;
		long count = 0;
		int timeIndex = c.getColumnIndex(KEY_TIME);
		int dataIndex = c.getColumnIndex(KEY_DATA);
		if (c.moveToFirst()){
			do{
				ContentValues dataToPut = new ContentValues();
				dataToPut.put(KEY_TIME, c.getLong(timeIndex));
				dataToPut.put(KEY_DATA, c.getString(dataIndex));
				if (mDb.insert(tableName, null, dataToPut) >= 0)
					count++;
			} while (c.moveToNext());
		}
		c.close();
		oDb.delete(tableName, null, null);
		return count;
	}
	
	private long writeComment(String comment, long time){
		ContentValues commentToPut = new ContentValues();
		long id = time;
		commentToPut.put(KEY_TIME, id);
		commentToPut.put(KEY_DATA, comment);
		long start = System.currentTimeMillis();
		long retVal = mDb.insert(LOG_COMMENT_TABLE, null, commentToPut);
		btStats.addTimeSpentPushingIntoDB(System.currentTimeMillis() - start);
		btStats.addDbWrite(comment.length());
		if (retVal >= 0)
			btStats.out.println("Comment " + id + " was written to database!");
		else
			btStats.out.println("Comment " + id + " failed to write to database!");
		return retVal;
	}
	
	public long writeComment(String comment){
		return writeComment(comment,System.currentTimeMillis());
	}
	
	public Cursor fetchOldestComment(){
		Cursor c = mDb.query(LOG_COMMENT_TABLE, new String[] {KEY_TIME, KEY_DATA}, null, null, null, null, KEY_TIME, "1");
		if (c != null)
			if (!c.moveToFirst()){
				c.close();
				return null;
			}
		return c;
	}
	
	public Cursor fetchComment(long id){
		Cursor c = mDb.query(LOG_COMMENT_TABLE, new String[]{KEY_TIME,KEY_DATA}, KEY_TIME + "=" + id, null, null, null, KEY_TIME, "1");
		if (c != null)
			if (!c.moveToFirst()){
				c.close();
				return null;
			}
		return c;
	}
	
	public long deleteComment(long id){
		btStats.out.println("Comment " + id + " deleted!");
		return mDb.delete(LOG_COMMENT_TABLE, KEY_TIME + "=" + id, null);
	}
	
	public long deleteOldestComment(){
		Cursor c = fetchOldestComment();
		if (c == null)
			return -1;
		long retVal = deleteComment(c.getLong(0));
		c.close();
		return retVal;
	}
	
	public long writeQuickComment(String name, String comment){
		ContentValues commentToPut = new ContentValues();
		commentToPut.put(QCOMMENT_KEY_NAME, name);
		commentToPut.put(QCOMMENT_KEY_COMMENT, comment);
		long start = System.currentTimeMillis();
		long retVal = mDb.insert(QUICK_COMMENT_TABLE, null, commentToPut);
		btStats.addTimeSpentPushingIntoDB(System.currentTimeMillis() - start);
		btStats.addDbWrite(comment.length());
		if (retVal >= 0)
			btStats.out.println("Quick Comment ("+name+","+comment+") was written to the database!");
		else
			btStats.out.println("Quick Comment ("+name+","+comment+") failed to write to database!");
		return retVal;
	}
	
	public long deleteQuickComment(long id){
		System.out.println("Quick Comment " + id + " deleted!");
		return mDb.delete(QUICK_COMMENT_TABLE, QCOMMENT_KEY_ID + "=" + id, null);
	}
	
	public Cursor fetchQuickComments(){
		Cursor c = mDb.query(QUICK_COMMENT_TABLE, new String[]{QCOMMENT_KEY_ID,QCOMMENT_KEY_NAME,QCOMMENT_KEY_COMMENT}, null, null, null, null, QCOMMENT_KEY_ID);
		if (c != null)
			c.moveToFirst();
		return c;
	}
	
	public Cursor fetchQuickComment(long id){
		Cursor c = mDb.query(QUICK_COMMENT_TABLE, new String[]{QCOMMENT_KEY_ID,QCOMMENT_KEY_NAME,QCOMMENT_KEY_COMMENT}, QCOMMENT_KEY_ID + "=" + id, null, null, null, QCOMMENT_KEY_ID, "1");
		if (c != null)
			if (!c.moveToFirst()){
				c.close();
				return null;
			}
		return c;
	}
	
	public boolean logQuickComment(long id){
		Cursor c = fetchQuickComment(id);
		if (c == null)
			return false;
		return writeComment(c.getString(c.getColumnIndex(QCOMMENT_KEY_COMMENT))) >= 0;
	}
	
	public long writeLocations(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		long time = 0;
		for (Object[] curDat : data){
			Location loc = (Location) curDat[0];
			if (first){
				first = false;
			}
			else{
				dataArray.append(",");
			}
			dataArray.append("[");
			if (time == 0)
				time = loc.getTime();
			dataArray.append(loc.getTime() / 1000.0);
			dataArray.append(",").append(loc.getLatitude());
			dataArray.append(",").append(loc.getLongitude());
			dataArray.append(",").append(loc.getAltitude());
			dataArray.append(",").append(loc.getAccuracy());
			dataArray.append(",").append(loc.getSpeed());
			dataArray.append(",").append(loc.getBearing());
			dataArray.append(",\"").append(loc.getProvider()).append("\"");
			dataArray.append(",").append(loc.getExtras().getInt("satellites",0));
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		locToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(NEW_LOC_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writeAccelerations(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		long time = 0;
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		for (Object[] datum : data){
			if (first)
				first = false;
			else
				dataArray.append(",");
			dataArray.append("[");
			if (time == 0)
				time = (Long) datum[0];
			dataArray.append(((Long) datum[0]) / 1000.0);
			for (int i = 0; i < 3; i++)
				dataArray.append(",").append((Float) datum[1+i]);
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues accToPut = new ContentValues();
		accToPut.put(KEY_DATA, jsonData);
		accToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(NEW_ACC_TABLE, null, accToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writeGravityAccelerations(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		long time = 0;
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		for (Object[] datum : data){
			if (first)
				first = false;
			else
				dataArray.append(",");
			dataArray.append("[");
			if (time == 0)
				time = (Long) datum[0];
			dataArray.append(((Long) datum[0]) / 1000.0);
			for (int i = 0; i < 3; i++)
				dataArray.append(",").append((Float) datum[1+i]);
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues accToPut = new ContentValues();
		accToPut.put(KEY_DATA, jsonData);
		accToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(GRAV_ACC_TABLE, null, accToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writeLinearAccelerations(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		long time = 0;
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		for (Object[] datum : data){
			if (first)
				first = false;
			else
				dataArray.append(",");
			dataArray.append("[");
			if (time == 0)
				time = (Long) datum[0];
			dataArray.append(((Long) datum[0]) / 1000.0);
			for (int i = 0; i < 3; i++)
				dataArray.append(",").append((Float) datum[1+i]);
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues accToPut = new ContentValues();
		accToPut.put(KEY_DATA, jsonData);
		accToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(LINE_ACC_TABLE, null, accToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writeWifis(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		long time = 0;
		for (Object[] dat : data){
			for (Object dat2 : dat){
				Object[] curDat = (Object[]) dat2;
				if (first)
					first = false;
				else
					dataArray.append(",");
				dataArray.append("[");
				if (time == 0)
					time = (Long) curDat[0];
				dataArray.append(((Long) curDat[0]) / 1000.0);
				for (int i = 0; i < 3; i++)
					dataArray.append(",\"").append((String) curDat[1+i]).append("\"");
				for (int i = 0; i < 2; i++)
					dataArray.append(",").append((Integer) curDat[4+i]);
				dataArray.append("]");
			}
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		locToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(NEW_WIFI_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writeGyros(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		long time = 0;
		for (Object[] curDat : data){
			if (first)
				first = false;
			else
				dataArray.append(",");
			dataArray.append("[");
			if (time == 0)
				time = (Long) curDat[0];
			dataArray.append(((Long) curDat[0]) / 1000.0);
			for (int i = 0; i < 3; i++)
				dataArray.append(",").append((Float) curDat[1+i]);
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		locToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(NEW_GYRO_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writeOrientations(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		long time = 0;
		for (Object[] curDat : data){
			if (first)
				first = false;
			else
				dataArray.append(",");
			dataArray.append("[");
			if (time == 0)
				time = (Long) curDat[0];
			dataArray.append(((Long) curDat[0]) / 1000.0);
			for (int i = 0; i < 3; i++)
				dataArray.append(",").append((Float) curDat[1+i]);
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		locToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(NEW_ORNT_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writePressures(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		long time = 0;
		for (Object[] curDat : data){
			if (first)
				first = false;
			else
				dataArray.append(",");
			dataArray.append("[");
			if (time == 0)
				time = (Long) curDat[0];
			dataArray.append(((Long) curDat[0]) / 1000.0);
			dataArray.append(",").append((Float) curDat[1]);
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		locToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(PRESS_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writeLights(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		long time = 0;
		for (Object[] curDat : data){
			if (first)
				first = false;
			else
				dataArray.append(",");
			dataArray.append("[");
			if (time == 0)
				time = (Long) curDat[0];
			dataArray.append(((Long) curDat[0]) / 1000.0);
			dataArray.append(",").append((Float) curDat[1]);
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		locToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(NEW_LIGHT_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long writeTemps(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		StringBuilder dataArray = new StringBuilder("[");
		boolean first = true;
		long time = 0;
		for (Object[] curDat : data){
			if (first)
				first = false;
			else
				dataArray.append(",");
			dataArray.append("[");
			if (time == 0)
				time = (Long) curDat[0];
			dataArray.append(((Long) curDat[0]) / 1000.0);
			dataArray.append(",").append((Float) curDat[1]);
			dataArray.append("]");
		}
		dataArray.append("]");
		String jsonData = dataArray.toString();
		long finish = System.currentTimeMillis();
		btStats.addTimeSpentConvertingToJSON(finish - start);
		start = finish;
		ContentValues locToPut = new ContentValues();
		locToPut.put(KEY_DATA, jsonData);
		locToPut.put(KEY_TIME, time);
		long retVal = mDb.insert(NEW_TEMP_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
    
    public Cursor fetchAllPics() {
    	return fetchPics(null);
    }
    
    public Cursor fetchPics(long limit){
    	return fetchPics("" + limit);
    }
    
    private Cursor fetchPics(String limit){
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE, PIX_KEY_COMMENT}, null, null, null, null, PIX_KEY_ID, limit);
    }
    
    public Cursor fetchAllUnuploadedPics() {
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE, PIX_KEY_COMMENT}, PIX_KEY_UPLOAD_STATE + " = " + PIC_NOT_UPLOADED, null, null, null, PIX_KEY_ID);
    }
    
    public Cursor fetchAllPendingUploadPics(){
    	return mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE, PIX_KEY_COMMENT}, PIX_KEY_UPLOAD_STATE + " = " + PIC_PENDING_UPLOAD, null, null, null, PIX_KEY_ID);
    }
    
    public Cursor fetchFirstPendingUploadPic(){
    	Cursor c = mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE, PIX_KEY_COMMENT}, PIX_KEY_UPLOAD_STATE + " = " + PIC_PENDING_UPLOAD, null, null, null, PIX_KEY_ID, "1");
    	if (c != null){
    		c.moveToFirst();
    	}
    	return c;
    }
    
    public Cursor fetchPicture(long id) {

        Cursor mCursor =

            mDb.query(true, PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE, PIX_KEY_COMMENT}, PIX_KEY_ID + "=" + id, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    public Cursor fetchLastPicture(){
    	Cursor mCursor =

            mDb.query(true, PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE, PIX_KEY_COMMENT}, "1", null,
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
    	Cursor c = mDb.query(PIX_TABLE, new String[] {PIX_KEY_ID, PIX_KEY_TIME, PIX_KEY_PIC, PIX_KEY_UPLOAD_STATE, PIX_KEY_COMMENT}, PIX_KEY_UPLOAD_STATE + " = 1", null, null, null, PIX_KEY_ID);
    	if (c != null){
	    	c.moveToFirst();
	    	while (!c.isAfterLast()){
	    		String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
	    		 mCtx.deleteFile(picFileName);
	    		c.moveToNext();
	    	}
	    	c.close();
    	}
    	
        return mDb.delete(PIX_TABLE, PIX_KEY_UPLOAD_STATE + "=" + PIC_UPLOADED, null);
    }
    
    public long writeBarcodes(Object[][] data){
		if (data.length == 0)
			return 0;
		long start = System.currentTimeMillis();
		JSONArray dataArray = new JSONArray();
		for (Object[] curDat : data){
			JSONArray bcData = new JSONArray();
			try{
				bcData.put(((Long) curDat[0]) / 1000.0);
				bcData.put((Long) curDat[1]);
				dataArray.put(bcData);
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
		long retVal = mDb.insert(NEW_BC_TABLE, null, locToPut);
		finish = System.currentTimeMillis();
		btStats.addTimeSpentPushingIntoDB(finish - start);
		btStats.addDbWrite(jsonData.length());
		return retVal;
	}
	
	public long getSize(){
		Cursor c = mDb.rawQuery("pragma page_size;", null);
		if (c == null)
			return 0;
		c.moveToFirst();
		long size;
		try{
			size = c.getLong(0);
		}
		catch (Exception e){
			size = 0;
		}
		c.close();
		c = mDb.rawQuery("pragma page_count;", null);
		if (c == null)
			return 0;
		c.moveToFirst();
		try{
			size *= c.getLong(0);
		}
		catch (Exception e){
			size = 0;
		}
		c.close();
		return size;
	}
	
	public long writePicture(File picture, String comment){
		if (comment == null)
			comment = "";
		ContentValues picToPut = new ContentValues();
		long currentTime = System.currentTimeMillis();
		String picFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/pic_" + currentTime + ".jpg";
		File picFile = new File(picFileName);
		picture.renameTo(picFile);
		
		picToPut.put(PIX_KEY_PIC, picFileName);
		picToPut.put(PIX_KEY_TIME, System.currentTimeMillis());
		picToPut.put(PIX_KEY_COMMENT, comment);
		picToPut.put(PIX_KEY_UPLOAD_STATE, PIC_PENDING_UPLOAD);
		long result = mDb.insert(PIX_TABLE, null, picToPut);
		btStats.addDbWrite(picFile.length());
		if (result == 0){
			picture.delete();
		}
		else{
			Cursor c = mDb.query(PIX_TABLE, new String[]{PIX_KEY_ID}, null, null, null, null, PIX_KEY_ID + " DESC", "1");
			if (c != null){
				if (c.moveToFirst()){
					long id = c.getLong(c.getColumnIndex(PIX_KEY_ID));
					btStats.out.println("Pic " + id + " created with comment: " + comment);
				}
				c.close();
			}
			else{
				btStats.out.println("Pic created. Failed to obtain id of pic.");
			}
		}
		return result;
	}
	
	private static String gpsChannelArray = "[\"latitude\",\"longitude\",\"altitude\"," +
			"\"uncertainty in meters\",\"speed\",\"bearing\",\"provider\",\"satellites\"]";
	
	public void uploadLocations(String macAdd, String uploadAdd, String devNickName){		
		uploadLogData(macAdd, uploadAdd, devNickName, gpsChannelArray, NEW_LOC_TABLE);
	}
	
	private static String accChannelArray = "[\"acceleration_x\",\"acceleration_y\",\"acceleration_z\"]";
	
	public void uploadAccelerations(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, accChannelArray, NEW_ACC_TABLE);
	}
	
	private static String gravChannelArray = "[\"gravity_acceleration_x\",\"gravity_acceleration_y\",\"gravity_acceleration_z\"]";
	
	public void uploadGravityAccelerations(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, gravChannelArray, GRAV_ACC_TABLE);
	}
	
	private static String lineChannelArray = "[\"linear_acceleration_x\",\"linear_acceleration_y\",\"linear_acceleration_z\"]";
	
	public void uploadLinearAccelerations(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, lineChannelArray, LINE_ACC_TABLE);
	}
	
	private static String gyroChannelArray = "[\"angular_speed_x\",\"angular_speed_y\",\"angular_speed_z\"]";
	
	public void uploadGyros(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, gyroChannelArray, NEW_GYRO_TABLE);
	}
	
	private static String ornChannelArray = "[\"azimuth\",\"pitch\",\"roll\"]";
	
	public void uploadOrientations(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, ornChannelArray, NEW_ORNT_TABLE);
	}
	
	private static String pressureChannelArray = "[\"pressure\"]";
	
	public void uploadPressures(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, pressureChannelArray, PRESS_TABLE);
	}
	
	private static String lightChannelArray = "[\"illuminance\"]";
	
	public void uploadIlluminances(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, lightChannelArray, NEW_LIGHT_TABLE);
	}
	
	private static String tempChannelArray = "[\"temperature\"]";
	
	public void uploadTemperatures(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, tempChannelArray, NEW_TEMP_TABLE);
	}
	
	private static String wifiChannelArray = "[\"SSID\",\"BSSID\",\"capabilities\",\"frequency\",\"level\"]";
	private static String wifiSpecs = "{\"SSID\":{\"type\":\"String\"},\"BSSID\":{\"type\":\"String\"},\"capabilities\":{\"type\":\"String\"}}";
	
	public void uploadWifis(String macAdd, String uploadAdd, String devNickName){		
		uploadLogData(macAdd, uploadAdd, devNickName, wifiChannelArray, wifiSpecs, NEW_WIFI_TABLE);
	}
	
	private static String barcodeChannelArray = "[\"UPC\"]";
	private static String barcodeSpecs = "{\"UPC\":{\"type\":\"String\"}}";
	
	public void uploadBarcodes(String macAdd, String uploadAdd, String devNickName){
		uploadLogData(macAdd, uploadAdd, devNickName, barcodeChannelArray, barcodeSpecs, NEW_BC_TABLE);
	}
	
	public void uploadComment(String macAdd, String uploadAdd, String devNickName, long id){
		Cursor c = fetchComment(id);
		if (c == null)
			return;
		String comment = c.getString(c.getColumnIndex(KEY_DATA));
		String timeRange = "{\"begin\":" + (id / 1000.0) + ",\"end\":" + (id / 1000.0) + "}";
		c.close();
		long start = System.currentTimeMillis();
		if (uploadData(uploadAdd, macAdd, devNickName, null, null, comment, timeRange, null)){
			btStats.addTimeSpentUploadingData(System.currentTimeMillis() - start);
			btStats.out.println("Comment " + id + " uploaded!");
			start = System.currentTimeMillis();
			deleteComment(id);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
		else{
			btStats.addTimeSpentUploadingData(System.currentTimeMillis() - start);
			btStats.out.println("Comment failed to upload!");
		}
	}
	
	public boolean uploadPhoto(String macAdd, String uploadAdd, String devNickName, final long id){
		Cursor c = fetchPicture(id);
		if (!c.moveToFirst()){
			c.close();
			return false;
		}
		String picFileName = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_PIC));
		String comment = c.getString(c.getColumnIndex(DbAdapter.PIX_KEY_COMMENT));
		long timestamp = c.getLong(c.getColumnIndex(DbAdapter.PIX_KEY_TIME));
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
			String timeRange = "{\"begin\":" + (timestamp / 1000.0) + ",\"end\":" + (timestamp / 1000.0) + "}";
			reqEntity.addPart("timerange",new StringBody(timeRange));
			if (comment != null)
				reqEntity.addPart("comment",new StringBody(comment));
			
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
				deletePicture(id);
				return true;
			}
	    }
	    catch (Exception e){
	    }
	    return false;
	}
	
	private void uploadLogData(String macAdd, String uploadAdd, String devNickName, String channels, String dbName){
		uploadLogData(macAdd, uploadAdd, devNickName, channels, null, dbName);
		return;
	}
	
	private void uploadLogData(String macAdd, String uploadAdd, String devNickName, String channels, String channelSpecs, String dbName){
		StringBuilder dataToUpload = new StringBuilder();
		boolean first = true;
		
		long lastId = 0;
		
		long start = System.currentTimeMillis();
		
		Cursor dataCursor = mDb.query(dbName, new String[] {KEY_TIME, KEY_DATA}, null, null, null, null, KEY_TIME, "20");
		if (dataCursor == null){
			btStats.out.println("query of table " + dbName + " returned null!");
			return;
		}
		if (!dataCursor.moveToFirst()){
			dataCursor.close();
			return;
		}
		while (!dataCursor.isAfterLast() && dataToUpload.length() < 500 * 1024){
			String curData = dataCursor.getString(dataCursor.getColumnIndex(KEY_DATA));
			lastId = dataCursor.getLong(dataCursor.getColumnIndex(KEY_TIME));
			if (first){
				dataToUpload.append(curData);
				first = false;
			}
			else{
				dataToUpload.deleteCharAt(dataToUpload.length() - 1);
				dataToUpload.append(",");
				dataToUpload.append(curData,1,curData.length());
			}
			dataCursor.moveToNext();
		}
		dataCursor.close();
		String dataString = dataToUpload.toString();
		btStats.addTimeSpentGatheringAndJoining(System.currentTimeMillis() - start);
		if (lastId >= 0 && uploadData(uploadAdd, macAdd, devNickName, channels, channelSpecs, dataString)){
			start = System.currentTimeMillis();
			mDb.delete(dbName, KEY_TIME + "<=" + lastId, null);
			btStats.addTimeSpentDeletingData(System.currentTimeMillis() - start);
		}
		return;
	}
	
	private static final String[] genericTables = {NEW_LOC_TABLE,NEW_ACC_TABLE,NEW_WIFI_TABLE,NEW_GYRO_TABLE,NEW_LIGHT_TABLE,NEW_TEMP_TABLE,NEW_ORNT_TABLE,GRAV_ACC_TABLE,LINE_ACC_TABLE,PRESS_TABLE}; 
	
	public long getOldestTime(){
		long oldestTime = System.currentTimeMillis();
		for (String table : genericTables){
			Cursor c = mDb.query(table, new String[]{KEY_TIME}, null, null, null, null, KEY_TIME, "1");
			if (c != null){
				if (c.moveToFirst()){
					long curTime = c.getLong(0);
					if (curTime < oldestTime)
						oldestTime = curTime;
				}
				c.close();
			}
		}
		
		Cursor c = mDb.query(PIX_TABLE, new String[]{PIX_KEY_TIME}, null, null, null, null, PIX_KEY_ID,"1");
		if (c != null){
			if (c.moveToFirst()){
				long curTime = c.getLong(0);
				if (curTime < oldestTime)
					oldestTime = curTime;
			}
			c.close();
		}
		
		return oldestTime;
	}
	
	private boolean uploadData(String uploadAdd, String macAdd, String devNickName, String channels, String channelSpecs, String data){
		return uploadData(uploadAdd,macAdd,devNickName,channels,channelSpecs,null,null,data);
	}
	
	private boolean uploadData(String uploadAdd, String macAdd, String devNickName, String channels, String channelSpecs, String comment, String timeRange, String data){
		long start = System.currentTimeMillis();
		try {
			HttpEntity reqEntity = null;
			//if (data.length() > 1024 * 5){ //multipart seems to be preferable in majority of situations
	    		MultipartEntity mPartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
	    		mPartEntity.addPart("device_id", new StringBody(macAdd));
	    		mPartEntity.addPart("timezone", new StringBody("UTC"));
	    		mPartEntity.addPart("device_class", new StringBody(android.os.Build.MODEL));
	    		mPartEntity.addPart("dev_nickname", new StringBody(devNickName));
	    		if (channels != null)
	    			mPartEntity.addPart("channel_names", new StringBody(channels));
		    	if (channelSpecs != null)
		    		mPartEntity.addPart("channel_specs",new StringBody(channelSpecs));
		    	if (data != null)
		    		mPartEntity.addPart("data", new StringBody(data));
		    	if (comment != null)
		    		mPartEntity.addPart("comment", new StringBody(comment));
		    	if (timeRange != null)
		    		mPartEntity.addPart("timerange", new StringBody(timeRange));
		    	reqEntity = mPartEntity;
			/*}
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
			}*/
	    	HttpPost postToServer = new HttpPost(uploadAdd);
    		postToServer.setEntity(reqEntity);
    		HttpResponse response = mHttpClient.execute(postToServer);
    		int statusCode = response.getStatusLine().getStatusCode();
    		long bytes = (data != null ? data.length() : 0) + (comment != null ? comment.length() : 0);
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
