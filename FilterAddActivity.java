/*******************************************************************************
 * ΠΡΟΣΘΗΚΗ ή ΕΠΕΞΕΡΓΑΣΙΑ ΦΙΛΤΡΟΥ                                              *
 *******************************************************************************/
package com.example.notification;

import com.example.notification.YesNoDialog.YesNoListener;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class FilterAddActivity extends Activity implements 
	TextWatcher, OnClickListener, OnCheckedChangeListener,
	YesNoListener {
	public static final int REQ_CODE = 1234;
	
	public static final String EXTRA_TOKEN = "TOKEN",
							   EXTRA_TOKENPOS = "TOKENPOS",
							   EXTRA_PAYLOAD  = "PAYLOAD",
							   EXTRA_ACTIVE	  = "ACTIVE",
							   EXTRA_EDIT     = "EDIT";
	
	private static final int YESNO_ID_CLOSE = 1;
	
	private EditText edToken;
	private RadioButton rbStart, 
						rbAny, 
						rbEnd,		
						
						rbDriveGsmWifi,
						rbDriveGsm,
						rbDriveWifi;
	private Button btnOk, 
				   btnCancel;
	private CheckBox cbRemove,
					 cbCopy,
					 cbDrive;
	private Switch swActive;
	
	private boolean edit = false,
					contentChanges = false;
	private FilterItem oldFilter = null;
	
	private DbFilterManager dbManager = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
				
		// Προετιμασία του UI
		setContentView(R.layout.activity_add_filter);
		
		edToken = (EditText)findViewById(R.id.editTextAFToken);
		edToken.addTextChangedListener(this);
		
		swActive = (Switch)findViewById(R.id.switchAFActive);
		swActive.setOnCheckedChangeListener(this);
				
		rbStart = (RadioButton)findViewById(R.id.radioAFStart);
		rbStart.setOnClickListener(this);
		rbAny = (RadioButton)findViewById(R.id.radioAFAny);
		rbAny.setOnClickListener(this);
		rbEnd = (RadioButton)findViewById(R.id.radioAFEnd);
		rbEnd.setOnClickListener(this);
		
		cbRemove = (CheckBox)findViewById(R.id.checkBoxAFPayloadDelete);
		cbRemove.setOnCheckedChangeListener(this);
		cbCopy = (CheckBox)findViewById(R.id.checkBoxAFPayloadStore);
		cbCopy.setOnCheckedChangeListener(this);
		cbDrive= (CheckBox)findViewById(R.id.checkBoxAFPayloadDrive);
		cbDrive.setOnCheckedChangeListener(this);		
		
		rbDriveGsmWifi = (RadioButton)findViewById(R.id.radioAFPayloadDriveGsmWifi);
		rbDriveGsmWifi.setOnClickListener(this);
		rbDriveGsm = (RadioButton)findViewById(R.id.radioAFPayloadDriveGsm);
		rbDriveGsm.setOnClickListener(this);
		rbDriveWifi= (RadioButton)findViewById(R.id.radioAFPayloadDriveWifi);
		rbDriveWifi.setOnClickListener(this);
		
		btnOk = (Button)findViewById(R.id.buttonAFOK);
		btnOk.setOnClickListener(this);
		btnCancel = (Button)findViewById(R.id.buttonAFCancel);
		btnCancel.setOnClickListener(this);
		
		/*
		 * Αν ο χρήστης δεν πατήσει το πλήκτρο OK, το Activity θα επιστρέψει
		 *  RESULT_CANCELED στο πατρογονικό του Activity! 
		 */
		setResult(Activity.RESULT_CANCELED);
		
		// Ανάγνωση παραμέτρων έναρξης Activity
		if(getIntent() != null && getIntent().getExtras() != null) { 
			final Bundle args = getIntent().getExtras();
			
			// Έναρξη σε κατάσταση Επεξεργασίας φίλτρου;
			if(edit = args.getBoolean(EXTRA_EDIT, false)) { 
				// Ναι!
				setTitle(R.string.title_edit_filter);				
				
				oldFilter = new FilterItem(args.getString(EXTRA_TOKEN, ""));
				oldFilter.matchingType = args.getByte(EXTRA_TOKENPOS, (byte)0);
				oldFilter.payload = args.getInt(EXTRA_PAYLOAD, 0);
				oldFilter.active = args.getBoolean(EXTRA_ACTIVE, false);
				
				// Φόρτωμα των παραμέτρων του φίλτρου στο UI του Activity..
				edToken.setText(oldFilter.token);
				edToken.setSelection(edToken.length());
				
				swActive.setChecked(oldFilter.active);
				
				switch(oldFilter.matchingType) { 
				case FilterItem.MATCHING_TYPE_START:
					rbStart.setChecked(true);
					break;
				case FilterItem.MATCHING_TYPE_ANY:
					rbAny.setChecked(true);
					break;
				case FilterItem.MATCHING_TYPE_END:
					rbEnd.setChecked(true);
					break;
				}
				
				final int payload = oldFilter.payload;
				cbRemove.setChecked(0 != (payload & FilterItem.PAYLOAD_REMOVE));
				cbCopy.setChecked(0 != (payload & FilterItem.PAYLOAD_COPY_TO_DB));
				
				cbDrive.setChecked(0 != (payload & FilterItem.PAYLOAD_COPY_TO_GD)
								|| 0 != (payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM)
								|| 0 != (payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI));
				if(cbDrive.isChecked()) { 									
					rbDriveGsmWifi.setChecked(0 != (payload & FilterItem.PAYLOAD_COPY_TO_GD));					
					rbDriveWifi.setChecked(0 != (payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI));
					rbDriveGsm.setChecked(0 != (payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM));
				}
				
				// Στην πραγματικότητα δεν υπάρχουν αλλαγές!
				setContentChanged(false);		
			}
		}			
		
		// Πρόσβαση στην ΒΔ του προγράμματος
		try { 
			dbManager = DbFilterManager.getInstance(this);
		} catch(SQLException e) { 
			CenterToast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	/***************************************************************************
	 * Ο χρήστης άλλαξε ή δεν άλλαξε (contentChanges) το περιεχομένο του Activity;
	 ***************************************************************************/
	private boolean setContentChanged(final boolean changes) {
		if(contentChanges = changes)
			btnOk.setEnabled(edToken.length() > 0);
		else
			btnOk.setEnabled(false);
		
		return changes;
	}
	
	/***************************************************************************
	 * Έξοδος από το Activity;                                                 *
	 *                                                                         *
	 * Η συνάρτηση επιστρέφει false όταν ο χρήστης πρέπει να δώσει την         *
	 *  συγκατάθεση του για να κλείσει το Activity αλλιώς true.                *
	 ***************************************************************************/
	private boolean leaveActivity() { 
		if(contentChanges) {
			YesNoDialog.show(this, 
					YESNO_ID_CLOSE, R.string.title_confirmation, 
					R.string.msg_filter_lost, this);
			return false;
		}
		
		finish();
		return true;
	}
	
	// Ο χρήστης ζήτησε την έξοδο του από το Activity πατώντας το πλήκτρο BACK
	@Override
	public void onBackPressed() {
		leaveActivity();
	}

	/***************************************************************************
	 * TextWatcher Listener                                                    *
 	 ***************************************************************************/
	@Override
	public void afterTextChanged(Editable editable) {
		// Ο χρήστης τροποποίησε την δήλωση αλφαριθμητικού
		setContentChanged(true);
	}
	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// Δεν χρησιμοποιείται
	}	
	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// Δεν χρησιμοποιείται
	}
	
	/***************************************************************************
	 * OnClickListener                                                         *
	 ***************************************************************************/
	@Override
	public void onClick(View view) {
		switch(view.getId()) { 
		case R.id.radioAFStart:
		case R.id.radioAFAny:
		case R.id.radioAFEnd:
		case R.id.radioAFPayloadDriveGsmWifi:
		case R.id.radioAFPayloadDriveWifi:
		case R.id.radioAFPayloadDriveGsm:		
			setContentChanged(true);
			return;
		// Κατασκευή νέου φίλτρου
		case R.id.buttonAFOK:
		{
			FilterItem filter = new FilterItem(edToken.getText().toString());			
			filter.active = swActive.isChecked();
			
			// Θέση κειμένου στην ειδοποίηση..
			if(rbStart.isChecked())
				filter.matchingType = FilterItem.MATCHING_TYPE_START;
			if(rbAny.isChecked())
				filter.matchingType = FilterItem.MATCHING_TYPE_ANY;
			if(rbEnd.isChecked())
				filter.matchingType = FilterItem.MATCHING_TYPE_END;
			
			// Αποτέλεσμα φίλτρου..
			if(cbDrive.isChecked()) { 
				if(rbDriveGsmWifi.isChecked())
					filter.payload = FilterItem.PAYLOAD_COPY_TO_GD;
				if(rbDriveGsm.isChecked())
					filter.payload = FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM;
				if(rbDriveWifi.isChecked())
					filter.payload = FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI;
			}
			
			if(cbRemove.isChecked())
				filter.payload |= FilterItem.PAYLOAD_REMOVE;
			if(cbCopy.isChecked())
				filter.payload |= FilterItem.PAYLOAD_COPY_TO_DB;
			
			// Υπάρχει ήδη κάποιο καταχωρημένο φίλτρο με το ίδιο κλειδί (Token);
			if(dbManager.findFilterByToken(edToken.getText().toString()) && !edit) {
				CenterToast.makeText(this, R.string.msg_filter_not_unique, Toast.LENGTH_SHORT)
					 .show();
			}
			else {
				// Εισαγωγή ή Αντικατάσταση φίλτρου στην ΒΔ
				if(!edit) {	// Εισαγωγή φίλτρου..					
					if(!dbManager.insertFilter(filter)) {
						CenterToast.makeText(this, R.string.msg_filter_not_added, Toast.LENGTH_SHORT)
							 .show();
						return;
					}
				}
				else {		// Αντικατάσταση φίλτρου.. 					
					if(!dbManager.replaceFilter(oldFilter, filter)) { 
						CenterToast.makeText(this, R.string.msg_filter_not_updated, Toast.LENGTH_SHORT)
						 	 .show();
						return;
					}
				}
				
				// Μετάδοση του κλειδιού του φίλτρου στο πατρογονικό Activity
				setResult(Activity.RESULT_OK, 
						new Intent().putExtra(EXTRA_TOKEN, filter.token));				
				
				setContentChanged(false);
				leaveActivity();				
			}
		}
			return;
		case R.id.buttonAFCancel:
			leaveActivity();
			return;
		default: return;
		}
	}
	
	/***************************************************************************
	 * CheckedChangedListener   Ο χρήστης μάρκαρε/ξεμάρκαρε την υπ. GoogleDrive*
	 ***************************************************************************/
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		setContentChanged(true);
		
		switch(buttonView.getId()) { 
		case R.id.checkBoxAFPayloadDrive:
			rbDriveGsmWifi.setEnabled(isChecked);			
			rbDriveGsm.setEnabled(isChecked);	
			rbDriveWifi.setEnabled(isChecked);
			return;
		default: return;
		}
	}

	/***************************************************************************
	 * YesNoListener 			Επιβεβαίωση ή απόρριψη ερωτήματος Activity     *	  
	 ***************************************************************************/	
	@Override
	public void OnYesNoListener(int id, int which) {
		switch(id) { 
		case YESNO_ID_CLOSE:
			if(which == DialogInterface.BUTTON_POSITIVE) {				
				setContentChanged(false);
				leaveActivity();
			}
			return;
		default: return;
		}
	}	
}
