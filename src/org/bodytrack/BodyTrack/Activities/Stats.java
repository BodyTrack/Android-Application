package org.bodytrack.BodyTrack.Activities;

import org.bodytrack.BodyTrack.BTStatisticTracker;
import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class Stats extends Activity implements OnClickListener{
	
	private BTStatisticTracker btStats;
	
	private TextView timeJSON, timeInDB, timeOutDB, timeUpload, timeDelete, timeTotal, consoleText,
				dbSize, dbWrites, dataUp, overheadUp;
	
	private Button scrollStart, scrollEnd, refresh;
	
	private ScrollView scroller;
	
	private DbAdapter dbAdapter;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);
		
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
		
		scrollStart = (Button) findViewById(R.id.scrollStart);
		scrollEnd = (Button) findViewById(R.id.scrollEnd);
		refresh = (Button) findViewById(R.id.refreshStats);
		
		scrollStart.setOnClickListener(this);
		scrollEnd.setOnClickListener(this);
		refresh.setOnClickListener(this);
		
		scroller = (ScrollView) findViewById(R.id.consoleScroller);
		
		btStats = BTStatisticTracker.getInstance();
		dbAdapter = DbAdapter.getDbAdapter(getApplicationContext());
		
		updateLabels();
	}
	
	public void onResume(){
		super.onResume();
		updateLabels();
	}
	
	
	private void updateLabels(){
		timeJSON.setText("JSON: " + (btStats.getTimeSpentConvertingToJSON() / 1000.0) + " s");
		timeInDB.setText("Write DB: " + (btStats.getTimeSpentPushingIntoDB() / 1000.0) + " s");
		timeOutDB.setText("Get and Join: " + (btStats.getTimeSpentGatheringAndJoining() / 1000.0) + " s");
		timeUpload.setText("Upload: " + (btStats.getTimeSpentUploadingData() / 1000.0) + " s");
		timeDelete.setText("Delete: " + (btStats.getTimeSpentDeletingData() / 1000.0) + " s");
		timeTotal.setText("Uptime: " + (btStats.getTotalUptime() / 1000.0) + " s");
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
		
		consoleText.setText(btStats.getConsoleText());
	}

	@Override
	public void onClick(View v) {
		if (v == scrollStart){
			scroller.fullScroll(ScrollView.FOCUS_UP);
		}
		else if (v == scrollEnd){
			scroller.fullScroll(ScrollView.FOCUS_DOWN);
		}
		else if (v == refresh){
			updateLabels();
		}
	}
}
