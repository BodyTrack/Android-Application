package org.bodytrack.BodyTrack.Activities;



import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.bodytrack.BodyTrack.AccelerometerService;
import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.GpsService;
import org.bodytrack.BodyTrack.IAccSvcRPC;
import org.bodytrack.BodyTrack.IGPSSvcRPC;
import org.bodytrack.BodyTrack.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Accelerometer extends Activity implements OnClickListener{
	//This is the manager that gets instantiated inside the on create method.
	
	private IAccSvcRPC accBinder;
	
	protected DbAdapter dbAdapter; 
	
	private Button startAccService, stopAccService, showData, dumpData;
	
	private TextView outputArea;
	
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accel);
		dbAdapter = new DbAdapter(this).open();
		
		startAccService = (Button)findViewById(R.id.StartAcc);
		startAccService.setEnabled(false);
		stopAccService = (Button)findViewById(R.id.stopAcc);
		stopAccService.setEnabled(false);
		showData = (Button)findViewById(R.id.dumpAccData);
		dumpData = (Button)findViewById(R.id.dump_acc);
		
		outputArea = (TextView)findViewById(R.id.accData);
		
		startAccService.setOnClickListener(this);
		stopAccService.setOnClickListener(this);
		showData.setOnClickListener(this);
		dumpData.setOnClickListener(this);
		
		Context ctx = getApplicationContext();
    	Intent intent = new Intent(ctx, AccelerometerService.class);
    	ctx.startService(intent);
    	ctx.bindService(intent, sc, 0);
    }


	@Override
	public void onClick(View v) {
		if (v == startAccService){
			try {
				accBinder.startLogging();
				startAccService.setEnabled(false);
				stopAccService.setEnabled(true);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (v == stopAccService){
			try {
				accBinder.stopLogging();
				startAccService.setEnabled(true);
				stopAccService.setEnabled(false);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (v == showData){
			outputArea.setText("");
	    	Cursor c = dbAdapter.fetchAllAccelerations();
	    	c.moveToFirst();
	    	while (c.isAfterLast() == false) {
	    		float x = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_X));
	    		float y = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_Y));
	    		float z = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_Z));
	    		double magnitude = Math.sqrt(x*x+y*y+z*z);
	    		outputArea.append("\n" + x + "," + y + "," + z + "," + magnitude);
	    		c.moveToNext();
	    	}
	    	c.close();
		}
		else if (v == dumpData){
			try{
				FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/acc.csv");
				OutputStreamWriter writer = new OutputStreamWriter(fos);
				writer.write("X,Y,Z,magnitude,timestamp");
				Cursor c = dbAdapter.fetchAllAccelerations();
				c.moveToFirst();
				while (!c.isAfterLast()){
					float x = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_X));
		    		float y = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_Y));
		    		float z = c.getFloat(c.getColumnIndex(DbAdapter.ACCEL_KEY_Z));
		    		double magnitude = Math.sqrt(x*x+y*y+z*z);
		    		long time = c.getLong(c.getColumnIndex(DbAdapter.ACCEL_KEY_TIME));
					writer.write("\n" + x + "," + y + "," + z + "," + magnitude + "," + time);
					c.moveToNext();
				}
				c.close();
				writer.close();
				Toast.makeText(Accelerometer.this, "acc.csv created", Toast.LENGTH_LONG).show();
			}
			catch (Exception e){
				Toast.makeText(Accelerometer.this, "failed to write acc.csv: " + e.toString(), Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private ServiceConnection sc = new ServiceConnection(){
    	
    	@Override
		public void onServiceConnected(ComponentName svc, IBinder binder) {
    		accBinder = IAccSvcRPC.Stub.asInterface(binder);
    		serviceBound(accBinder);
		}

    	@Override
		public void onServiceDisconnected(ComponentName name) {
		}
    };


	protected void serviceBound(IAccSvcRPC binder) {
		accBinder = binder;
    	try {
	        startAccService.setEnabled(!binder.isLogging());
	        stopAccService.setEnabled(binder.isLogging());
        //TODO catch
    	} catch (Exception e){}
	}
	
}
