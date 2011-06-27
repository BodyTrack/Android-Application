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
	
	private Button toggleAcc, toggleGyro, toggleWifi, toggleLight, toggleTemp, toggleOrnt, toggleGPS;
	
	private TextView outputArea;
	
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensors);
		dbAdapter = DbAdapter.getDbAdapter(getApplicationContext());
		
		toggleAcc = (Button)findViewById(R.id.toggleAcc);
		toggleGyro = (Button)findViewById(R.id.toggleGyro);
		toggleWifi = (Button)findViewById(R.id.toggleWifi);
		toggleLight = (Button)findViewById(R.id.toggleLight);
		toggleTemp = (Button)findViewById(R.id.toggleTemp);
		toggleOrnt = (Button)findViewById(R.id.toggleOrnt);
		toggleGPS = (Button)findViewById(R.id.toggleGPS);
		toggleAcc.setEnabled(false);
		toggleGyro.setEnabled(false);
		toggleWifi.setEnabled(false);
		toggleLight.setEnabled(false);
		toggleTemp.setEnabled(false);
		toggleOrnt.setEnabled(false);
		toggleGPS.setEnabled(false);
		
		toggleAcc.setOnClickListener(this);
		toggleGyro.setOnClickListener(this);
		toggleWifi.setOnClickListener(this);
		toggleLight.setOnClickListener(this);
		toggleTemp.setOnClickListener(this);
		toggleOrnt.setOnClickListener(this);
		toggleGPS.setOnClickListener(this);
		
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
		else if (v == toggleLight){
			try {
				if (btBinder.isLogging(BTService.LIGHT_LOGGING)){
					btBinder.stopLogging(BTService.LIGHT_LOGGING);
					toggleLight.setText(R.string.startLight);
				}
				else{
					btBinder.startLogging(BTService.LIGHT_LOGGING);
					toggleLight.setText(R.string.stopLight);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (v == toggleTemp){
			try {
				if (btBinder.isLogging(BTService.TEMP_LOGGING)){
					btBinder.stopLogging(BTService.TEMP_LOGGING);
					toggleTemp.setText(R.string.startTemp);
				}
				else{
					btBinder.startLogging(BTService.TEMP_LOGGING);
					toggleTemp.setText(R.string.stopTemp);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (v == toggleOrnt){
			try {
				if (btBinder.isLogging(BTService.ORNT_LOGGING)){
					btBinder.stopLogging(BTService.ORNT_LOGGING);
					toggleOrnt.setText(R.string.startOrnt);
				}
				else{
					btBinder.startLogging(BTService.ORNT_LOGGING);
					toggleOrnt.setText(R.string.stopOrnt);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (v == toggleGPS){
			try {
				if (btBinder.isLogging(BTService.GPS_LOGGING)){
					btBinder.stopLogging(BTService.GPS_LOGGING);
					toggleGPS.setText(R.string.startGPS);
				}
				else{
					btBinder.startLogging(BTService.GPS_LOGGING);
					toggleGPS.setText(R.string.stopGPS);
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
    	/*if (btBinder == null){
    		if (toggleAcc != null)
    			toggleAcc.setEnabled(false);
    		if (toggleGyro != null)
    			toggleGyro.setEnabled(false);
    		if (toggleWifi != null)
    			toggleWifi.setEnabled(false);
    		if (toggleLight != null)
    			toggleLight.set
			return;
    	}*/
    	try{
			toggleAcc.setText(btBinder.isLogging(BTService.ACC_LOGGING) ? R.string.stop_acc : R.string.start_acc);
			toggleGyro.setText(btBinder.isLogging(BTService.GYRO_LOGGING) ? R.string.stopGyro : R.string.startGyro);
			toggleWifi.setText(btBinder.isLogging(BTService.WIFI_LOGGING) ? R.string.stopWifi : R.string.startWifi);
			toggleLight.setText(btBinder.isLogging(BTService.LIGHT_LOGGING) ? R.string.stopLight : R.string.startLight);
			toggleTemp.setText(btBinder.isLogging(BTService.TEMP_LOGGING) ? R.string.stopTemp : R.string.startTemp);
			toggleOrnt.setText(btBinder.isLogging(BTService.ORNT_LOGGING) ? R.string.stopOrnt : R.string.startOrnt);
			toggleGPS.setText(btBinder.isLogging(BTService.GPS_LOGGING) ? R.string.stopGPS : R.string.startGPS);
			
			toggleAcc.setEnabled(btBinder.canLog(BTService.ACC_LOGGING));
			toggleGyro.setEnabled(btBinder.canLog(BTService.GYRO_LOGGING));
			toggleWifi.setEnabled(btBinder.canLog(BTService.WIFI_LOGGING));
			toggleLight.setEnabled(btBinder.canLog(BTService.LIGHT_LOGGING));
			toggleTemp.setEnabled(btBinder.canLog(BTService.TEMP_LOGGING));
			toggleOrnt.setEnabled(btBinder.canLog(BTService.ORNT_LOGGING));
			toggleGPS.setEnabled(btBinder.canLog(BTService.GPS_LOGGING));
    	}
    	catch (Exception e){}
    }


	protected void serviceBound(IBTSvcRPC binder) {
		btBinder = binder;
		updateButtons();
	}
	
}
