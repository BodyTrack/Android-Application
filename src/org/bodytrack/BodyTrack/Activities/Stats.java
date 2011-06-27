package org.bodytrack.BodyTrack.Activities;

import org.bodytrack.BodyTrack.BTStatisticTracker;
import org.bodytrack.BodyTrack.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Stats extends Activity {
	
	private BTStatisticTracker btStats;
	
	private TextView timeJSON, timeInDB, timeOutDB, timeUpload, timeDelete, timeTotal, consoleText;
	
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
		
		btStats = BTStatisticTracker.getInstance();
		
		updateLabels();
	}
	
	public void onResume(){
		super.onResume();
		updateLabels();
	}
	
	
	private void updateLabels(){
		timeJSON.setText("JSON: " + (btStats.getTimeSpentConvertingToJSON() / 1000.0));
		timeInDB.setText("Write DB: " + (btStats.getTimeSpentPushingIntoDB() / 1000.0));
		timeOutDB.setText("Get and Join: " + (btStats.getTimeSpentGatheringAndJoining() / 1000.0));
		timeUpload.setText("Upload: " + (btStats.getTimeSpentUploadingData() / 1000.0));
		timeDelete.setText("Delete: " + (btStats.getTimeSpentDeletingData() / 1000.0));
		timeTotal.setText("Uptime: " + (btStats.getTotalUptime() / 1000.0));
		consoleText.setText(btStats.getConsoleText());
	}
}
