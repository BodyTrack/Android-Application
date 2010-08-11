package org.bodytrack.BodyTrack;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/*This class creates an activity to modify preferences.
 * Preferences set are defined in res/xml/prefs.xml
 * 
 */

public class BTPrefs extends PreferenceActivity {
	public static final String TAG = "BTPrefs";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Load preferences from XML
		addPreferencesFromResource(R.xml.prefs);
	}

}
