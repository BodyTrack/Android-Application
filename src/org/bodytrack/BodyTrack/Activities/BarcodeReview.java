package org.bodytrack.BodyTrack.Activities;

import org.bodytrack.BodyTrack.DbAdapter;
import org.bodytrack.BodyTrack.R;
import org.bodytrack.BodyTrack.R.id;
import org.bodytrack.BodyTrack.R.layout;
import org.bodytrack.BodyTrack.R.string;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;


/**
 * This class defines an activity which allows the user to review
 *  previously captured  barcodes and has a button to allow the
 *   capture of new ones.
 */
public class BarcodeReview extends ListActivity {
	public static final String TAG = "BarcodeReview";
	
	private Button getBarcode;
	private DbAdapter dbAdapter;
	
	private AlertDialog noZxingDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.barcode_review);
		
		Context ctx = getApplicationContext();
		
		//Set up button to go to camera activity
		getBarcode = (Button)findViewById(R.id.getBarcode);
		getBarcode.setOnClickListener(mGetBarcode);
		
        //connect to database
		dbAdapter = DbAdapter.getDbAdapter(ctx);
		
		Log.v(TAG, "Got DB adapter");
		
		//set list contents
		fillData();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
        	.setTitle(R.string.no_zxing_title)
            .setMessage(R.string.no_zxing)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                }
            });
        noZxingDialog = builder.create();
	}
	
    /**Handles the barcode button: Requests the ZXing app scan a barcode*/
	private Button.OnClickListener mGetBarcode = new Button.OnClickListener(){
	    public void onClick(View v) {
	    	Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	    	intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
	    	try {
	    		startActivityForResult(intent,0);
	    	} catch (ActivityNotFoundException e) {
	    		noZxingDialog.show();
			}
	    	
	    }
    };
    
    /**Handles the barcode data (a long) returned by the ZXing app*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			try {
				long code = Long.parseLong((intent.getStringExtra("SCAN_RESULT")));
				dbAdapter.writeBarcode(code);
				fillData();
			}
			catch (NumberFormatException e) {
				Toast.makeText(this, R.string.barcodeFail, Toast.LENGTH_LONG);
			}

		} else {
		}
    }
    
    private void fillData() {
        // Get all of the notes from the database and create the item list
        Cursor c = dbAdapter.fetchAllBarcodes();
        startManagingCursor(c);

        String[] from = new String[] { DbAdapter.BC_KEY_BARCODE };
        int[] to = new int[] { android.R.id.text1 };
        
        // Now create an array adapter and set it to display using our row
        SimpleCursorAdapter barcodes =
            new SimpleCursorAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, c, from, to);
        setListAdapter(barcodes);
    }

}
