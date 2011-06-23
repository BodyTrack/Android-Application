package org.bodytrack.BodyTrack.Activities;



import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.BTService;
import org.bodytrack.BodyTrack.IBTSvcRPC;
import org.bodytrack.BodyTrack.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.wifi.WifiManager;
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

public class Sensors extends Activity implements OnClickListener{
	//This is the manager that gets instantiated inside the on create method.
	
	private IBTSvcRPC btBinder;
	
	protected DbAdapter dbAdapter; 
	
	private Button toggleAcc, toggleGyro, toggleWifi;
	
	private TextView outputArea;
	
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensors);
		dbAdapter = new DbAdapter(this).open();
		
		toggleAcc = (Button)findViewById(R.id.toggleAcc);
		toggleGyro = (Button)findViewById(R.id.toggleGyro);
		toggleWifi = (Button)findViewById(R.id.toggleWifi);
		toggleAcc.setEnabled(false);
		toggleGyro.setEnabled(false);
		toggleWifi.setEnabled(false);
		
		toggleAcc.setOnClickListener(this);
		toggleGyro.setOnClickListener(this);
		toggleWifi.setOnClickListener(this);
		
		Context ctx = getApplicationContext();
    	Intent intent = new Intent(ctx, BTService.class);
    	ctx.startService(intent);
    	ctx.bindService(intent, sc, 0);
    }


	@Override
	public void onClick(View v) {
		if (v == toggleAcc){
			try {
				if (btBinder.isLogging(BTService.ACC_LOGGING)){
					btBinder.stopLogging(BTService.ACC_LOGGING);
					toggleAcc.setText(R.string.start_acc);
				}
				else{
					btBinder.startLogging(BTService.ACC_LOGGING);
					toggleAcc.setText(R.string.stop_acc);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (v == toggleGyro){
			try {
				if (btBinder.isLogging(BTService.GYRO_LOGGING)){
					btBinder.stopLogging(BTService.GYRO_LOGGING);
					toggleGyro.setText(R.string.startGyro);
				}
				else{
					btBinder.startLogging(BTService.GYRO_LOGGING);
					toggleGyro.setText(R.string.stopGyro);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (v == toggleWifi){
			try {
				if (btBinder.isLogging(BTService.WIFI_LOGGING)){
					btBinder.stopLogging(BTService.WIFI_LOGGING);
					toggleWifi.setText(R.string.startWifi);
				}
				else{
					btBinder.startLogging(BTService.WIFI_LOGGING);
					toggleWifi.setText(R.string.stopWifi);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void onResume(){
		super.onResume();
		updateButtons();
	}
	
	private ServiceConnection sc = new ServiceConnection(){
    	
    	@Override
		public void onServiceConnected(ComponentName svc, IBinder binder) {
    		btBinder = IBTSvcRPC.Stub.asInterface(binder);
    		serviceBound(btBinder);
		}

    	@Override
		public void onServiceDisconnected(ComponentName name) {
		}
    };
    
    protected void updateButtons(){
    	if (btBinder == null){
    		if (toggleAcc != null)
    			toggleAcc.setEnabled(false);
    		if (toggleGyro != null)
    			toggleGyro.setEnabled(false);
    		if (toggleWifi != null)
    			toggleWifi.setEnabled(false);
			return;
    	}
    	try{
			toggleAcc.setText(btBinder.isLogging(BTService.ACC_LOGGING) ? R.string.stop_acc : R.string.start_acc);
			toggleGyro.setText(btBinder.isLogging(BTService.GYRO_LOGGING) ? R.string.stopGyro : R.string.startGyro);
			toggleWifi.setText(btBinder.isLogging(BTService.WIFI_LOGGING) ? R.string.stopWifi : R.string.startWifi);
			
			toggleAcc.setEnabled(btBinder.canLog(BTService.ACC_LOGGING));
			toggleGyro.setEnabled(btBinder.canLog(BTService.GYRO_LOGGING));
			toggleWifi.setEnabled(btBinder.canLog(BTService.WIFI_LOGGING));
    	}
    	catch (Exception e){}
    }


	protected void serviceBound(IBTSvcRPC binder) {
		btBinder = binder;
		updateButtons();
	}
	
}
