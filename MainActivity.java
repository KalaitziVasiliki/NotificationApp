/*******************************************************************************
 * ΠΑΡΑΚΟΛΟΥΘΗΣΗ ΕΙΔΟΠΟΙΗΣΕΩΝ - ΚΕΝΤΡΙΚΟ ACTIVITY                              *
 *******************************************************************************/
package com.example.notification;

import com.example.notification.GoogleDrvDialog.GoogleDrvDialogListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity 
	implements OnClickListener, OnLongClickListener, 
	ConnectionCallbacks, OnConnectionFailedListener, 
	GoogleDrvDialogListener {
	/*
	 * Το logcat Tag στο οποίο δημοσιεύονται τα αποτελέσματα εκτέλεσης των
	 * ρουτινών της κλάσης 
	 */
	private static final String LOG_TAG = "NOTIF";
	
	// Αναφορά στις ρυθμίσεις του προγράμματος
	public static final String  ACTIVITY_SETS = "ASET",
	// Ο τίτλος του φακέλου αποθήκευσης των ειδοποιήσεων στο GoogleDrive
								ACTIVITY_SET_GD_FOLDER_TITLE = "ASETGDFLDRT",
	// Ο τύπος ονομασίας των αρχείων που αποθηκεύονται στο GoogleDrive
								ACTIVITY_SET_GD_FILE_TITLE   = "ASETGDFNT",
	// Σύνδεση στην υπηρεσία Google Drive
								ACTIVITY_SET_GD_CONNECT		 = "GDCONNECT";
	
	/*
	 * Η ταυτότητα (RequestCode) με την οποία επιστρέφει τα αποτελέσματα της
	 *  στο Activity η διαδικασία GoogleDrv.resolveConnectionFailure   
	 */
	private static final int GDRIVE_REQ_CODE = 1010;
	// Αναφορά στην υπηρεσία Google Drive (βλ. κλάση GoogleDrv) 
	GoogleApiClient gClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		// Προετοιμασία του UI
		setContentView(R.layout.activity_main);
		
		Button btnFilter,
			   btnHistory,
			   btnSettings,
			   btnGoogleDrive;
		
		btnFilter = (Button)findViewById(R.id.buttonMainFilter);
		btnFilter.setOnClickListener(this);
		btnFilter.setOnLongClickListener(this);
		
		btnHistory = (Button)findViewById(R.id.ButtonMainHistory);
		btnHistory.setOnClickListener(this);
		btnHistory.setOnLongClickListener(this);
		
		btnSettings = (Button)findViewById(R.id.ButtonMainSettings);
		btnSettings.setOnClickListener(this);
		btnSettings.setOnLongClickListener(this);
		
		btnGoogleDrive = (Button)findViewById(R.id.ButtonMainGD);
		btnGoogleDrive.setOnClickListener(this);
		btnGoogleDrive.setOnLongClickListener(this);		
		
		/*
		 * Σύνδεση με το Google Drive API
		 * 
		 * Η συνάρτηση αναλαμβάνει και την διαδικασία πιστοποίησης (OAuth2) του   
		 *  χρήστη στην υπηρεσία Google Drive αν έχει ζητηθεί αυτό (getUserConnectionSetting).
		 */
		try { 
			gClient = GoogleDrv.initConnection(this, this, this);
			
			if(GoogleDrv.getUserConnectionSetting(this))
				gClient.connect();
		} catch(Exception e) { 
			e.printStackTrace();
		}
		
		// Προετοιμασία της ΒΔ
		try { 
			DbFilterManager.getInstance(this);			
		} catch(SQLException e) { 
			CenterToast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
		}
	}
	
	/***************************************************************************
	 * OnResume,	Το activity επιστρέφει στο προσκήνιο, αν έχει ζητηθεί η    *
	 * 				σύνδεση του χρήστη στο GoogleDrive, έναρξη σύνδεσης από δω *
	 ***************************************************************************/
	@Override
	public void onResume() { 
		super.onResume();
				
		if(GoogleDrv.getUserConnectionSetting(this))
			GoogleDrv.gClient.connect();
	}	
	

	/***************************************************************************
	 * OnClickListener, διαχείριση των πλήκτρων του Activity                   *
	 **************************************************************************/
	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		// Φόρτωμα διαχειριστή καταχωρημένων φίλτρων
		case R.id.buttonMainFilter:
			startActivity(new Intent(this, FiltersActivity.class));
			return;
		// Προβολή τοπικά αποθηκευμένων ειδοποιήσεων (ιστορικό)
		case R.id.ButtonMainHistory:			
			startActivity(new Intent(this, HistoryActivity.class));
			return;
		// Παρουσίαση των "Ρυθμίσεων Παρακολούθησης Ειδοποιήσεων" του ΛΣ
		case R.id.ButtonMainSettings:
		{
			Intent inSettings = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
			// Προσφέρει το ΛΣ αυτές τις ρυθμίσεις;
			if(inSettings.resolveActivity(getPackageManager()) != null)			
				startActivity(inSettings);
			else {
				// Όχι!
				CenterToast.makeText(this, R.string.msg_service_not_ready, 
						Toast.LENGTH_SHORT).show();
			}
		}		
			return;
		// Παρουσίαση των "Ρυθμίσεων του Google Drive"
		case R.id.ButtonMainGD:
		{
			/*
			 * Ανάγνωση των πρόσφατων επιλογών του χρήστη, αν υπάρχουν αλλιώς 
			 *  χρησιμοποιώ τις default!
			 */
			final SharedPreferences pref = getSharedPreferences(ACTIVITY_SETS, 
					Context.MODE_PRIVATE);			
			GoogleDrvDialog.show(this, 
				pref.getString(ACTIVITY_SET_GD_FOLDER_TITLE, GoogleDrv.GD_FOLDER),
				pref.getInt(ACTIVITY_SET_GD_FILE_TITLE, GoogleDrvDialog.FILE_TITLE_NOTIF_TITLE),
				this);
		}
			return;
		default: return;
		}
	}

	/***************************************************************************
	 * OnLongClickListener, διαχείριση παρατεταμένου πατήματος πλήκτρων του    *
	 *                      Activity                                           *
	 ***************************************************************************/
	@Override
	public boolean onLongClick(View view) {
		/*
		 * Προβολή σύντομης επεξήγησης της δραστηριότητας του πλήκτρου.
		 * 
		 * Η επεξήγηση είναι αποθηκευμένη στην ιδιότητα tag του κάθε Button
		 *  και δηλωμένη στο activity_main.xml του έργου. 
		 */
		if(view.getTag() != null)
			CenterToast.makeText(this, (String)view.getTag(), Toast.LENGTH_LONG).show();
		
		return true;
	}

	/***************************************************************************
	 * Διαχείριση απόκρισης της διαδικασίας GoogleDrv.resolveConnectionFailure * 
	 ***************************************************************************/
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) { 
		if(requestCode == GDRIVE_REQ_CODE) {
			Log.i(LOG_TAG, "onActivityResult ReqCode = " + requestCode + "\n" +
						                    "ResCode = " + resultCode);
					
			if(resultCode == Activity.RESULT_OK) { 		
				GoogleDrv.setUserConnectionSetting(this, true);
				gClient.connect();			
			}
			else { 
				GoogleDrv.setUserConnectionSetting(this, false);
				CenterToast.makeText(this, "H σύνδεση στην υπηρεσία Google Drive απέτυχε", 
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
	/***************************************************************************
	 * Διαχείριση επιτυχημένης ή αποτυχημένης σύνδεσης με το Google Drive      *
	 ***************************************************************************/
	@Override
	public void onConnected(Bundle bundle) {
		Log.i(LOG_TAG, "onConnected");
		GoogleDrv.setUserConnectionSetting(this, true);
		CenterToast.makeText(this, R.string.msg_gd_connected, Toast.LENGTH_SHORT).show();		
	}
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(LOG_TAG, "onConnectionFailed->GoogleDrv.resolveConnectionFailure");		
		
		GoogleDrv.setUserConnectionSetting(this, 
				GoogleDrv.resolveConnectionFailure(this, connectionResult, GDRIVE_REQ_CODE));
	}
	@Override
	public void onConnectionSuspended(int reason) {
		// Δεν χρησιμοποιείται..		
	}	

	/***************************************************************************
	 * Διαχείριση του διαλόγου ρυθμίσεων του Google Drive, GoogleDrvDialog     * 
	 ***************************************************************************/
	@Override
	public void OnGoogleDrvSettings(String folderTitle, int fileTitle, int which) {
		if(which == DialogInterface.BUTTON_POSITIVE) { 
			// Αποθήκευση των ρυθμίσεων του χρήστη
			getSharedPreferences(ACTIVITY_SETS, Context.MODE_PRIVATE)
			.edit()
			.putString(ACTIVITY_SET_GD_FOLDER_TITLE, folderTitle)
			.putInt(ACTIVITY_SET_GD_FILE_TITLE, fileTitle)
			.commit();
		}
	}
	@Override
	public void onGoogleDrvAccountSetting(boolean connect) {
		// Ο χρήστης ζήτησε Σύνδεση ή Αποσύνδεση από το Google Drive
		if(!connect) {							// σύνδεση
			GoogleDrv.gClient.connect();
		}
		else {									// αποσύνδεση			
			GoogleDrv.setUserConnectionSetting(this, false);			
			GoogleDrv.gClient.clearDefaultAccountAndReconnect();
		}
	}	
}
