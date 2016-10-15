/*******************************************************************************
 * ΔΙΑΛΟΓΟΣ ΡΥΘΜΙΣΕΩΝ ΥΠΗΡΕΣΙΑΣ GOOGLE DRIVE                                   *
 *******************************************************************************/
package com.example.notification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class GoogleDrvDialog implements 
	DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
	DialogInterface.OnShowListener, android.widget.RadioGroup.OnCheckedChangeListener,
	TextWatcher {
	/*
	 * Οι τύποι ονομασίας των αρχείων ενημερώσεων:
	 * 	FILE_TITLE_NOTIF_TITLE = Το αρχείο λαμβάνει ως ονομασία τον σύντομο τίτλο 
	 * 							 της ειδοποίησης
	 *  FILE_TITLE_APP_PACKAGE = Το αρχείο λαμβάνει ως ονομασία το πακέτο της
	 *  						 εφαρμογής που ανάρτησε την ειδοποίηση
	 *  FILE_TITLE_UNKNOWN     = Επιστρέφεται μόνο όταν ο χρήστης ακυρώσει τον
	 *  						 διάλογο
	 */
	public static final int FILE_TITLE_NOTIF_TITLE = 1,
							FILE_TITLE_APP_PACKAGE = 2,
							FILE_TITLE_UNKNOWN     = 0;
	
	// Η βάση του διαλόγου
	protected AlertDialog dialog;
	// Ο Listener του διαλόγου	
	protected GoogleDrvDialogListener listener;	
	// Το UI (layout) του διαλόγου
	protected View view;
	// Το control ονομασίας του φακέλου αποθήκευσης των ειδοποιήσεων στο GoogleDrive
	protected EditText edFolderTitle;
	// Τα controls ορισμού τύπου ονομασίας των αρχείων 
	protected RadioButton rbFnTitle,
						  rbFnAppPackage;
	// Το group με τα δυο παραπάνω RadioButtons
	protected RadioGroup rgFn;
	
	// Η πιο πρόσφατη ονομασία του φακέλου αποθήκευσης ειδοποιήσεων
	protected String folderTitle;
	// Ο πιο πρόσφατος τύπος ονομασίας αρχείων
	protected int fileTitle;
	// Η κατάσταση σύνδεσης του χρήστη στο Google Drive Service
	protected boolean gdConnected;

	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  Εδώ ορίζουμε το Context του προγράμματος, την ονομασία του φακέλου που *
	 *   θα αποθηκεύονται οι ειδοποιήσεις (folderTitle), την μορφή του τίτλου  *
	 *  τους (fileTitle) και το GoogleDrvDialogListener στο οποίο θα αποσταλεί *
	 *   η απόκριση του χρήστη στον διάλογο.                                   *
	 ***************************************************************************/
	public GoogleDrvDialog(final Context context,
			final String folderTitle,
			final int fileTitle,
			final GoogleDrvDialogListener listener) {
		this.listener    = listener;
		this.folderTitle = folderTitle;
		this.fileTitle   = fileTitle;
		// Κατασκευή ενός νέου AlertDialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setIcon(R.drawable.ic_action_gd);
		builder.setTitle(R.string.button_gd);
		builder.setView(view = View.inflate(context, R.layout.dialog_gd_settings, null));
		builder.setPositiveButton(R.string.button_save, this);
		builder.setNegativeButton(R.string.button_cancel, this);
		builder.setOnCancelListener(this);
		
		/* 
		 * Η ετικέτα του ουδέτερου (μεσαίου) πλήκτρου του διαλόγου εξαρτάται από
		 *  από το εάν ο χρήστης είχε συνδεθεί επιτυχώς κάποια στιγμή στο παρελθόν
		 * σε λογαριασμό του Google Drive, οπότε σε αυτή την περίπτωση ισούται με
		 *  "Σύνδεση" αλλιώς με "Αποσύνδεση".     
		 */
		builder.setNeutralButton((gdConnected = !GoogleDrv.isUserAuthorized())
				? R.string.button_gd_connect: R.string.button_gd_disconnect, 
						this);
		
		/*
		 * Κατασκευή του διαλόγου και ρύθμιση του ώστε να μην κλείνει αν ο χρήστης
		 *  πατήσει εκτός του πλαισίου του
		 */
		dialog = builder.create();
		dialog.setOnShowListener(this);
		dialog.setCanceledOnTouchOutside(false);		
	}
	
	/***************************************************************************
	 * Κατασκευή & παρουσίαση διαλόγου στην οθόνη                              *
	 ***************************************************************************/
	public static GoogleDrvDialog show(final Context context,
			final String folderTitle,
			final int fileTitle,
			final GoogleDrvDialogListener listener) { 
		return new GoogleDrvDialog(context, folderTitle, fileTitle, listener).show();
	}
	
	/***************************************************************************
	 * Παρουσίαση διαλόγου στην οθόνη                                          *
	 *                                                                         *
	 * Η συνάρτηση επιστρέφει το GoogleDrvDialog που παρουσιάζει               *
	 ***************************************************************************/
	public GoogleDrvDialog show() { 
		dialog.show();		
		return this;
	}
		
	/***************************************************************************
	 * DialogInterface.ShowListener      Παρουσίαση διαλόγου, προετοιμασία UI  *
	 ***************************************************************************/
	@Override
	public void onShow(DialogInterface dialog) {
		edFolderTitle  = (EditText)view.findViewById(R.id.editTextGdFolder);
		edFolderTitle.setText(folderTitle);
		edFolderTitle.setSelection(edFolderTitle.length());
		edFolderTitle.addTextChangedListener(this);		
		
		rgFn           = (RadioGroup)view.findViewById(R.id.radioGroupGdFn);
		rbFnTitle      = (RadioButton)view.findViewById(R.id.radioGdFnTitle);		
		rbFnAppPackage = (RadioButton)view.findViewById(R.id.radioGdFnAppPackage);
		
		if(fileTitle == FILE_TITLE_NOTIF_TITLE) 
			rbFnTitle.setChecked(true);
		else
			rbFnAppPackage.setChecked(true);
		
		rgFn.setOnCheckedChangeListener(this);
		
		this.dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);		
	}
	
	/***************************************************************************
	 * DialogInterface.OnClickListener   Διαχείριση των πλήκτρων του διαλόγου  *
	 ***************************************************************************/	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(listener != null) {
			if(which != DialogInterface.BUTTON_NEUTRAL) {			
				listener.OnGoogleDrvSettings(edFolderTitle.getText().toString(),
						rgFn.getCheckedRadioButtonId() == R.id.radioGdFnTitle ? 
								FILE_TITLE_NOTIF_TITLE: FILE_TITLE_APP_PACKAGE,
								which);
			}
			else { 
				/*
				 * Αν ο χρήστης είναι συνδεδεμένος στο GoogleDrive τότε το πάτημα
				 *  του πλήκτρου σημαίνει "Αποσύνδεση" (->connect = false) αλλιώς  
				 * "Σύνδεση" (->connect = true). 
				 */
				listener.onGoogleDrvAccountSetting(!gdConnected);
			}
		}
	}
	
	/***************************************************************************
	 * OnCancelListener                  Διαχείριση της ακύρωσης του διαλόγου  *
	 ***************************************************************************/	
	@Override
	public void onCancel(DialogInterface dialog) {
		if(listener != null)
			listener.OnGoogleDrvSettings(null, FILE_TITLE_UNKNOWN, DialogInterface.BUTTON_NEGATIVE);
	}

	/***************************************************************************
	 * TextWatcher                       Παρακολούθηση εισόδου Τίτλου φακέλου  *
	 ***************************************************************************/
	@Override
	public void afterTextChanged(Editable s) {
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(s.length() > 0);		
	}
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// Δεν χρησιμοποιείται..		
	}
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// Δεν χρησιμοποιείται..		
	}
	
	/***************************************************************************
	 * RadioGroupCheckedChanged			Παρακολούθηση των επιλογών του RG      *
	 ***************************************************************************/
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(edFolderTitle.length() > 0);
	}	

	/***************************************************************************
	 * YesNoListener                    Το GoogleDrvDialogListener Interface   *
	 ***************************************************************************/	
	public interface GoogleDrvDialogListener { 
		public void OnGoogleDrvSettings(String folderTitle, int fileTitle, int which);
		public void onGoogleDrvAccountSetting(final boolean connect);
	}
}
