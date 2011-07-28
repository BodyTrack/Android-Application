package org.bodytrack.BodyTrack.Activities;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.Format;

import org.bodytrack.BodyTrack.BTStatisticTracker;
import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.R;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class Stats extends Activity implements OnClickListener{
	
	private BTStatisticTracker btStats;
	
	private TextView timeJSON, timeInDB, timeOutDB, timeUpload, timeDelete, timeTotal, consoleText,
				dbSize, dbWrites, dataUp, overheadUp, cpuUsage;
	
	private Button scrollStart, scrollEnd;
	
	private ScrollView scroller;
	
	private DbAdapter dbAdapter;
	
	private Format usageFormat;
	
	private Handler updateHandler = new Handler();
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);
		
		usageFormat = new DecimalFormat("##.00%");
		
		timeJSON = (TextView) findViewById(R.id.timeJSON);
		timeInDB = (TextView) findViewById(R.id.timeInDB);
		timeOutDB = (TextView) findViewById(R.id.timeOutDB);
		timeUpload = (TextView) findViewById(R.id.timeUpload);
		timeDelete = (TextView) findViewById(R.id.timeDelete);
		consoleText = (TextView) findViewById(R.id.consoleText);
		timeTotal = (TextView) findViewById(R.id.timeTotal);
		dbSize = (TextView) findViewById(R.id.dbSize);
		dbWrites = (TextView) findViewById(R.id.dbWrites);
		dataUp = (TextView) findViewById(R.id.dataUp);
		overheadUp = (TextView) findViewById(R.id.overheadUp);
		cpuUsage = (TextView) findViewById(R.id.cpuUsage);
		
		scrollStart = (Button) findViewById(R.id.scrollStart);
		scrollEnd = (Button) findViewById(R.id.scrollEnd);
		
		scrollStart.setOnClickListener(this);
		scrollEnd.setOnClickListener(this);
		
		scroller = (ScrollView) findViewById(R.id.consoleScroller);
		
		btStats = BTStatisticTracker.getInstance();
		dbAdapter = DbAdapter.getDbAdapter(getApplicationContext());
		updateLabels();
	}
	
	public void onDestroy(){
		updateHandler.removeCallbacks(updater);
		super.onDestroy();
	}
	
	public void onPause(){
		updateHandler.removeCallbacks(updater);
		super.onPause();
	}
	
	
	private void updateLabels(){
		timeJSON.setText("JSON: " + (btStats.getTimeSpentConvertingToJSON() / 1000.0) + " s");
		timeInDB.setText("Write DB: " + (btStats.getTimeSpentPushingIntoDB() / 1000.0) + " s");
		timeOutDB.setText("Get and Join: " + (btStats.getTimeSpentGatheringAndJoining() / 1000.0) + " s");
		timeUpload.setText("Upload: " + (btStats.getTimeSpentUploadingData() / 1000.0) + " s");
		timeDelete.setText("Delete: " + (btStats.getTimeSpentDeletingData() / 1000.0) + " s");
		timeTotal.setText("Uptime: " + (btStats.getTotalUptime() / 1000.0) + " s");
		//timeTotal.refreshDrawableState();
		String sizeUnits = "B";
		long size = dbAdapter.getSize();
		if (size >= 1048576){
			size /= 1048576;
			sizeUnits = "MB";
		}
		else if (size >= 1024){
			size /= 1024;
			sizeUnits = "KB";
		}
		dbSize.setText("DB Size: " + size + " " + sizeUnits);
		dbWrites.setText("DB Writes: " + btStats.getDbWrites());
		
		size = btStats.getTotalDataBytes();
		if (size >= 1073741824){
			size /= 1073741824;
			sizeUnits = "GB";
		}
		if (size >= 1048576){
			size /= 1048576;
			sizeUnits = "MB";
		}
		else if (size >= 1024){
			size /= 1024;
			sizeUnits = "KB";
		}
		else{
			sizeUnits = "B";
		}
		
		dataUp.setText("Data: " + size + " " + sizeUnits);
		
		size = btStats.getTotalOverheadBytes();
		if (size >= 1073741824){
			size /= 1073741824;
			sizeUnits = "GB";
		}
		if (size >= 1048576){
			size /= 1048576;
			sizeUnits = "MB";
		}
		else if (size >= 1024){
			size /= 1024;
			sizeUnits = "KB";
		}
		else{
			sizeUnits = "B";
		}
		
		overheadUp.setText("Overhead: " + size + " " + sizeUnits);
		
		cpuUsage.setText("CPU: " + usageFormat.format(readUsage()));
		
		consoleText.setText(btStats.getConsoleText());
		updateHandler.removeCallbacks(updater);
		updateHandler.postDelayed(updater, 5000);
	}

	@Override
	public void onClick(View v) {
		if (v == scrollStart){
			scroller.fullScroll(ScrollView.FOCUS_UP);
		}
		else if (v == scrollEnd){
			scroller.fullScroll(ScrollView.FOCUS_DOWN);
		}
	}
	
	private long lastIdle = 0;
	private long lastCPU = 0;
	
	private float readUsage() {
	    try {
	        RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
	        String load = reader.readLine();

	        String[] toks = load.split(" ");

	        long idle1 = Long.parseLong(toks[5]);
	        long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	              + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
	        long idle2, cpu2;
		    if (lastCPU + lastIdle == 0){
		        try {
		            Thread.sleep(360);
		        } catch (Exception e) {}
	
		        reader.seek(0);
		        load = reader.readLine();
		        reader.close();
	
		        toks = load.split(" ");
	
		        idle2 = Long.parseLong(toks[5]);
		        cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
		            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
		    }
		    else{
		    	reader.close();
		    	idle2 = idle1;
		    	cpu2 = cpu1;
		    	idle1 = lastIdle;
		    	cpu1 = lastCPU;
		    }
		    
		    lastIdle = idle2;
		    lastCPU = cpu2;

	        return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

	    } catch (IOException ex) {
	        ex.printStackTrace(btStats.out);
	    }

	    return 0;
	} 
	
	public Runnable updater = new Runnable(){
		public void run(){
			updateLabels();
		}
	};
}
