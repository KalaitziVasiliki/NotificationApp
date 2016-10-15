/*
 * ΠΡΟΒΟΛΗ, ΔΗΛΩΣΗ ΚΑΙ ΔΙΑΓΡΑΦΗ ΦΙΛΤΡΩΝ
 */
package com.example.notification;

import java.util.ArrayList;

import com.example.notification.YesNoDialog.YesNoListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FiltersActivity extends Activity implements 
	OnClickListener, OnItemClickListener, YesNoListener {

	// Η ταυτότητα του διαλόγου επιβεβαίωσης διαγραφής μαρκαρισμένων φίλτρων
	private final static int YESNO_ID_DELETE = 1;
	
	// Διάλογος προόδου για χρονοβόρες εντολές
	private ProgressDialog pDlg = null;
	// Subclass του ArrayAdapter για την προβολή των φίλτρων στο ListView του Activity 
	private FilterAdapter fAdapter;
	// Ο διαχειριστής της ΒΔ
	private DbFilterManager dbManager;
	
	/***************************************************************************
	 * Κατασκευή Activity                                                      *
	 ***************************************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		// Προετιμασία του UI
		setContentView(R.layout.activity_filters);
		
		getActionBar().show();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		
		// Προετοιμασία ListView για την προβολή των φίλτρων της ΒΔ
		ListView listView = (ListView)findViewById(R.id.listViewFilters);
		listView.setOnItemClickListener(this);
		// Προετοιμασία FilterAdapter για την προβολή των φίλτρων της ΒΔ στο ListView
		fAdapter = new FilterAdapter(listView, this);
		
		// Προετοιμασία ΒΔ και ανάγνωση των καταχωρημένων φίλτρων της
		try { 
			dbManager = DbFilterManager.getInstance(this);
			listFilters();
		} catch(SQLException e) { 
			CenterToast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	/***************************************************************************
	 * Έλεγχος αποτελέσματος Activity "Εισαγωγής ή Επεξεργασίας φίλτρου"       *
	 *                                                                         *
	 * Αν η "Εισαγωγή ή Επεξεργασία" φίλτρου επέστρεψε RESULT_OK τότε ενημέρωση*
	 * των περιεχομένων του ListView και σκρολάρισμα στο Item με κείμενο ίσο με*
	 * EXTRA_TOKEN                                                             *
	 ***************************************************************************/
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == FilterAddActivity.REQ_CODE && resultCode == Activity.RESULT_OK 
		&& data != null && data.getExtras() != null  && 
		   data.hasExtra(FilterAddActivity.EXTRA_TOKEN))
			listFilters(data.getExtras().getString(FilterAddActivity.EXTRA_TOKEN));
	}
	
	/***************************************************************************
	 * Φόρτωμα του μενού Activity και ορισμός της κατάστασης όλων των          *
	 * επιλογών του.                                                           *
	 *                                                                         *
	 * Τα items του μενού εμφανίζονται σαν εικονίδια στο ActionBar του Activity* 
	 ***************************************************************************/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);		
		/*
		 * Αν σε Mark mode (ή αν κενό ListView) τότε απόκρυψη του item έναρξης  
		 *  mark mode και της εισαγωγής νέου φίλτρου
		 */
		menu.findItem(R.id.itemFilterAdd).setVisible(!fAdapter.getMarkMode());		
		menu.findItem(R.id.itemFilterMarkMode).setVisible(!fAdapter.getMarkMode() && !fAdapter.isEmpty());
		/*
		 * Αντίστροφα αν σε Mark mode τότε εμφάνιση του πλήκτρου Επιλογή ή 
		 *  "Από επιλογή" όλων και των πλήκτρων "Επιβεβαίωση διαγραφής" & "Ακύρωση"
		 * (το πρώτο μόνο όταν υπάρχει έστω και ένα επιλεγμένο φίλτρο)  
		 */
		menu.findItem(R.id.itemFilterDoRemove)
			.setVisible(fAdapter.getMarkMode() && fAdapter.getMarkedCount() > 0); 
		menu.findItem(R.id.itemFilterSelect).setVisible(fAdapter.getMarkMode());
		menu.findItem(R.id.itemFilterCancel).setVisible(fAdapter.getMarkMode()); 
		return true;
	}
	
	/***************************************************************************
	 * Ο χρήστης επίλεξε κάποιο item από το μενού του Activity                 *
	 ***************************************************************************/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) { 
		switch(item.getItemId()) { 
		// Ο χρήστης επίλεξε έξοδο από το Activity
		case android.R.id.home:
			finish();
			return true;		
		// Ο χρήστης επίλεξε την προσθήκη νέου φίλτρου
		case R.id.itemFilterAdd:
			startActivityForResult(new Intent(this, FilterAddActivity.class), 
					FilterAddActivity.REQ_CODE);
			return true;
		// Ο χρήστης επίλεξε την έναρξη του mark mode
		case R.id.itemFilterMarkMode:
			fAdapter.setMarkMode(true);
			invalidateOptionsMenu();
			return true;
		// Επιβεβαίωση διαγραφής επιλεγμένων φίλτρων
		case R.id.itemFilterDoRemove:
			YesNoDialog.show(this, 
					YESNO_ID_DELETE, 
					getText(R.string.title_confirmation), 
					getResources().getQuantityString(R.plurals.filters_to_delete, 
							fAdapter.getMarkedCount(), 
							fAdapter.getMarkedCount()), this);
			return true;
		/*
		 * Ο χρήστης επιλέγει ή από επιλέγει όλα τα φίλτρα
		 * 
		 * Αν υπάρχει έστω ένα επιλεγμένο φίλτρο τότε από επιλέγεται αλλιώς 
		 * επιλέγονται όλα τα φίλτρα.
		 */
		case R.id.itemFilterSelect:
			fAdapter.setMarkAll(fAdapter.getMarkedCount() == 0);
			invalidateOptionsMenu();
			return true;
		// Ο χρήστης ζήτησε ακύρωση mark mode
		case R.id.itemFilterCancel:
			fAdapter.setMarkMode(false);
			invalidateOptionsMenu();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/*************************************************************************** 
	 * Ο χρήστης ζήτησε την έξοδο του από το Activity πατώντας το πλήκτρο BACK.*
	 *                                                                         *
	 * Αν το mark mode είναι ενεργό, τότε αντί εξόδου από το Activity, ακύρωση *
	 *  του mark mode.                                                         *
	 ***************************************************************************/
	@Override
	public void onBackPressed() {
		if(fAdapter.getMarkMode()) {
			fAdapter.setMarkMode(false);
			invalidateOptionsMenu();
		}
		else
			super.onBackPressed();
	}	
	
	/***************************************************************************
	 * Επιστροφή όλων των καταχωρημένων φίλτρων της ΒΔ στο ListView του        *
	 *  Activity.                                                              *
	 *                                                                         *
	 * Αν το keyFocus δεν είναι null τότε μετακίνηση του ListView στο          *
	 *  πρώτο Item του οποίου το κείμενο (κλειδί) ισούται με το αλφαριθμητικό  *
	 * keyFocus.                                                               *
	 *                                                                         *
	 * Η ενημέρωση του ListView γίνεται ασύγχρονα σε ξεχωριστό Thread ώστε να  *
	 * μην καθυστερεί την εκτέλεση του UI.                                     *
	 ***************************************************************************/
	private void listFilters(final String keyFocus) {
		final Context context = this;
		
		new AsyncTask<Void, Void, Object>() {
			@Override
			protected void onPreExecute() { 
				if(pDlg != null) {
					pDlg.dismiss();
					pDlg = null;
				}				
				pDlg = ProgressDialog.show(context, "", context.getText(R.string.msg_wait));
			}
			@Override
			protected Object doInBackground(Void... args) {
				// Επιστροφή όλων των φίλτρων της ΒΔ
				return dbManager.listFilters(context);
			} 
			@SuppressWarnings("unchecked")
			@Override
			protected void onPostExecute(Object r) { 
				if(pDlg != null) {
					pDlg.dismiss();
					pDlg = null;
				}
				
				// Προβολή σφάλματος;
				if(r instanceof Exception) { 
					CenterToast.makeText(context, ((Exception)r).toString(), 
							Toast.LENGTH_LONG).show();
				}
				else { 
					/* 
					 * Ενημέρωση του ArrayAdapter με τα καταχωρημένα φίλτρα και
					 *  σκρολάρισμα στο φίλτρο με κείμενο (κλειδί) το keyFocus
					 */
					fAdapter.setFilterList((ArrayList<FilterItem>)r, keyFocus);
					
					// Εμφάνιση ή απόκρυψη της ένδειξης "κενού ListView"
					((TextView)findViewById(R.id.textViewFiltersEmpty))
						.setVisibility(fAdapter.isEmpty() ? View.VISIBLE: View.GONE);					
					// Ενημέρωση της ένδειξης αριθμού καταχωρημένων φίλτρων
					((TextView)findViewById(R.id.textViewFilterCounter))
						.setText(!fAdapter.isEmpty() ? 
								getResources().getQuantityString(R.plurals.filter_counter, 
										fAdapter.getCount(), fAdapter.getCount()): "");
					// Τέλος του mark mode
					fAdapter.setMarkMode(false);
					// Ενημέρωση του Menu του Activity
					invalidateOptionsMenu();
				}
			}
		}.execute();
	}
	/***************************************************************************
	 * Η έκδοση της παραπάνω διαδικασίας δίχως σκρολάρισμα σε κάποιο φίλτρο    *
	 ***************************************************************************/
	private void listFilters() { 
		listFilters(null);
	}
	
	/***************************************************************************
	 * Διαγραφή από την ΒΔ όλων των μαρκαρισμένων φίλτρων                      *
	 *                                                                         *
	 * Η διαγραφή γίνεται ασύγχρονα σε ξεχωριστό Thread ώστε να μην καθυστερεί *
	 * την εκτέλεση του UI.                                                    *
	 ***************************************************************************/
	private void deleteFilters() { 
		final Context context = this;
		
		new AsyncTask<Void, Void, Object>() {
			@Override
			protected void onPreExecute() {
				if(pDlg != null) { 
					pDlg.dismiss();
					pDlg = null;
				}				
				pDlg = ProgressDialog.show(context, "", context.getText(R.string.msg_wait));
			}			
			@Override
			protected Object doInBackground(Void... args) {
				// Διαγραφή όλων των μαρκαρισμένων φίλτρων του ListView
				try { 
					for(int position = 0; position < fAdapter.getCount(); position++) { 
						final FilterItem item = fAdapter.getItem(position);
						
						if(item.mark) // μαρκαρισμένο;
							if(!dbManager.deleteFilter(item))
								throw new Exception(context.getString(R.string.error_filter_mass_delete) + item.token);						
					}
					
					return null;
				} catch(Exception e) { 
					e.printStackTrace();
					return e;
				}
			}
			@Override
			protected void onPostExecute(Object r) { 
				if(pDlg != null) {
					pDlg.dismiss();
					pDlg = null;
				}
				
				// Προβολή σφάλματος;
				if(r instanceof Exception) {
					final Exception e = (Exception)r;
					
					CenterToast.makeText(context, 
							e.getMessage() != null && !e.getMessage().isEmpty() ? 
									e.getMessage(): e.toString(), 
							Toast.LENGTH_LONG).show();
				}				
				// Φρεσκάρισμα ListView
				listFilters();
			}
		}.execute();
	}
	
	/***************************************************************************
	 * Ενεργοποίηση ή απενεργοποίηση (enable) του φίλτρου της ΒΔ στην θέση     * 
	 *  position                                                               *
	 *                                                                         *
	 * Η ενεργοποίηση ή απενεργοποίηση γίνεται ασύγχρονα σε ξεχωριστό Thread   *
	 * ώστε να μην καθυστερεί την εκτέλεση του UI.                             * 
	 ***************************************************************************/
	public void enableFilter(final int position, final boolean enable) {
		final Context context = this;
		
		new AsyncTask<Void, Void, Object>() { 
			@Override
			protected void onPreExecute() { 
				if(pDlg != null) { 
					pDlg.dismiss();
					pDlg = null;
				}
				pDlg = ProgressDialog.show(context, "", getText(R.string.msg_wait));
			}
			@Override
			protected Object doInBackground(Void... args) {
				try { 					
					if(!dbManager.setFilterActive(fAdapter.getItem(position), enable))
						throw new Exception();
					return null;
				} catch(Exception e) { 
					e.printStackTrace();
					return e;
				}
			}
			@Override
			protected void onPostExecute(Object r) { 
				if(pDlg != null) { 
					pDlg.dismiss();
					pDlg = null;
				}
				
				if(r instanceof Exception) { 
					CenterToast.makeText(context, 
							enable ? R.string.error_filter_set_enable: R.string.error_filter_set_disable, 
							Toast.LENGTH_SHORT).show();
				}
				else { 
					// Ενημέρωση κατάστασης φίλτρου
					fAdapter.setFilterEnable(position, enable);
				}
			}
		}.execute();
	}
	
	/***************************************************************************
	 * ClickListener 				Διαχείριση των πλήκτρων του Activity       *
	 ***************************************************************************/		  
	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		/*
		 * Ενεργοποίηση ή απενεργοποίηση φίλτρου, η θέση του φίλτρου στο 
		 *  ListView (& fAdapter) δίδεται από το tag του View σε μορφή Integer.
		 */
		case R.id.imageViewFilterActive:
			enableFilter((Integer)view.getTag(), 
					!fAdapter.isFilterEnabled((Integer)view.getTag()));
			return;
		// Μαρκάρισμα ή ξεμαρκάρισμα διαγραφής φίλτρου
		case R.id.ImageViewFilterMark:
			fAdapter.setFilterMark((Integer)view.getTag(), 
					!fAdapter.isFilterMarked((Integer)view.getTag()));
			invalidateOptionsMenu();
			return;
		}
	}	
	
	/***************************************************************************
	 * Ο χρήστης πάτησε κάποιο φίλτρο, έναρξη επεξεργασίας του εκτός και αν    *
	 *  και τρέχουμε σε edit mode!                                             *
	 ***************************************************************************/
	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		if(!fAdapter.getMarkMode()) { 		
			final FilterItem fItem = fAdapter.getItem(position);
			
			startActivityForResult(new Intent(this, FilterAddActivity.class)
				.putExtra(FilterAddActivity.EXTRA_EDIT    , true)
				.putExtra(FilterAddActivity.EXTRA_TOKEN   , fItem.token)
				.putExtra(FilterAddActivity.EXTRA_TOKENPOS, fItem.matchingType)
				.putExtra(FilterAddActivity.EXTRA_PAYLOAD , fItem.payload)
				.putExtra(FilterAddActivity.EXTRA_ACTIVE  , fItem.active),				
				FilterAddActivity.REQ_CODE);
		}
	}
	

	/***************************************************************************
	 * YesNoListener 			Επιβεβαίωση ή απόρριψη ερωτήματος Activity	   *
	 ***************************************************************************/
	@Override
	public void OnYesNoListener(int id, int which) {
		switch(id) { 
		case YESNO_ID_DELETE:	// Διαγραφή επιλεγμένων φίλτρων;
			if(which == DialogInterface.BUTTON_POSITIVE)
				deleteFilters();
			return;
		default: return;
		}
	}	
	
	/***************************************************************************
	 *FilterAdapter   Subclass του ArrayAdapter ώστε να διαχειρίζεται φίλτρα   * 	
	 *                κατηγορίας FilterItem                                    *
	 ***************************************************************************/
	private class FilterAdapter extends ArrayAdapter<FilterItem> {
		private final ListView owner;
		private final OnClickListener externClickListener;
		
		private boolean markMode = false;
		private int markedCount = 0;
		
		/*
		 * ctor
		 * 	Εδώ ορίζουμε το Context του προγράμματος, το ListView (owner) στο 
		 *   οποίο θα προβάλλει τα αποτελέσματα του ο ArrayAdapter και ένα    
		 *  OnClickListener το οποίο θα ενημερώνεται όταν ο χρήστης μαρκάρει 
		 *   - ξεμαρκάρει ή ενεργοποιεί - απενεργοποιεί κάποιο φίλτρο. 
		 */
		public FilterAdapter(final ListView owner, 
				final OnClickListener externClickListener) {
			super(owner.getContext(), R.layout.item_filter);
			
			(this.owner = owner).setAdapter(this);
			this.externClickListener = externClickListener;
		}
		
		// Αύξηση του αριθμού των μαρκαρισμένων φίλτρων κατά 1
		private void increaseMarkedCount() { 
			if(++markedCount > getCount())
				markedCount = getCount();
		}
		// Μείωση του αριθμού των μαρκαρισμένων φίλτρων κατά 1
		private void decreaseMarkedCount() { 
			if(--markedCount < 0)
				markedCount = 0;
		}
		// Επιστροφή του αριθμού των μαρκαρισμένων φίλτρων
		public int getMarkedCount() { 
			return markedCount;
		}
		
		/*
		 * Αν mark ίσο με true τότε μαρκάρισμα όλων των φίλτρων αλλιώς
		 *  ξεμαρκάρισμα τους.
		 * 
		 * Αν το FilterAdapter είναι σε mark mode τότε η συνάρτηση επιστρέφει
		 *  false αλλιώς true.
		 */
		public boolean setMarkAll(final boolean mark) {
			if(markMode) {
				for(int position = 0; position < getCount(); position++)
					getItem(position).mark = mark;
				
				markedCount = mark ? getCount(): 0;
				
				notifyDataSetChanged();
				return true;
			}
			return false;
		}
		
		/* 
		 * Φόρτωμα των φίλτρων της ArrayList<FilterItem> στο ListView και 
		 *  σκρολάρισμα στο φίλτρο με κείμενο (κλειδί) ίσο με keyFocus εκτός
		 * αν το keyFocus ισούται με null οπότε αγνοείται.
		 * 
		 * Αν το list είναι null τότε το περιεχόμενο του ListView απλά 
		 *  διαγράφεται.
		 */
		public void setFilterList(final ArrayList<FilterItem> list, 
				final String keyFocus) {			
			clear();
			
			if(list != null)			
				addAll(list);
			
			if(isEmpty())
				setMarkMode(false);
			
			if(keyFocus != null) { 
				for(int position = 0; position < getCount(); position++) {
					if(keyFocus.equalsIgnoreCase(getItem(position).token)) {						
						owner.setSelection(position);
						break;
					}
				}
			}
		}
		
		/*
		 * Επιστροφή true αν το φίλτρο στην θέση position του ListView είναι
		 *  ενεργοποιημένο αλλιώς false.
		 */
		public boolean isFilterEnabled(final int position) { 
			return getItem(position).active;
		}
		/*
		 * Ενεργοποίηση ή απενεργοποίηση (enable) του φίλτρου στην θέση position 
		 *  του ListView.
		 * 
		 * Η συνάρτηση επιστρέφει την είσοδο της (enable).
		 */
		public boolean setFilterEnable(final int position, final boolean enable) { 
			getItem(position).active = enable;
			notifyDataSetChanged();
			return enable;
		}
		
		/*
		 * Ενεργοποίηση ή απενεργοποίηση του ρυθμού μαρκαρίσματος (mark mode)
		 *  του FilterAdapter.
		 * 
		 * Όταν το mark mode είναι ενεργό, ο χρήστης μπορεί να μαρκάρει ή να 
		 *  ξεμαρκάρει φίλτρα του ListView ενώ το control ενεργοποίησης ή 
		 * απενεργοποίησης φίλτρων αποκρύπτεται.
		 * 
		 * Η συνάρτηση επιστρέφει την είσοδο της (markMode).
		 */
		public boolean setMarkMode(final boolean markMode) {
			this.markMode = markMode;

			markedCount = 0;
			
			for(int item = 0; item < getCount(); item++)
				getItem(item).mark = false;			
			notifyDataSetChanged();
			
			return markMode;
		}
		/*
		 * Επιστροφή true αν το mark mode του FilterAdapter είναι ενεργό 
		 *   αλλιώς false.
		 */
		public boolean getMarkMode() { 
			return markMode;
		}
		/*
		 * Επιστροφή true αν το φίλτρο στην θέση position του ListView είναι
		 *  μαρκαρισμένο αλλιώς false.
		 */
		public boolean isFilterMarked(final int position) { 
			return getItem(position).mark;
		}
		/*
		 * Μαρκάρισμα ή ξεμαρκάρισμα (mark) του φίλτρου στην θέση position 
		 *  του ListView.
		 * 
		 * Η συνάρτηση επιστρέφει την είσοδο της (mark).
		 */
		public boolean setFilterMark(final int position, final boolean mark) { 
			if(getItem(position).mark = mark)
				increaseMarkedCount();
			else
				decreaseMarkedCount();
			
			notifyDataSetChanged();
			return mark;
		}
		
		/* 
		 * Φόρτωμα της όψης (view) κάθε item του ListView
		 * 
		 * βλ. https://developer.android.com/reference/android/widget/ArrayAdapter.html#getView(int, android.view.View, android.view.ViewGroup)
		 * και https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) { 
			final View view;
			
			final ImageView imgState,
							imgMark;
			
			final TextView txtToken,
						   txtScanner,
						   txtPayloadRemove,
						   txtPayloadStore,
						   txtPayloadDrive,
						   txtPayloadNothing;
			
			/* 
			 * Απλοποιημένο σχήμα View-Holder για αύξηση επιδόσεων του ListView
			 * 
			 * βλ. https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
			 */
			if(convertView == null) { 
				view = View.inflate(getContext(), R.layout.item_filter, null);
				
				imgState   = (ImageView)view.findViewById(R.id.imageViewFilterActive);
				imgState.setOnClickListener(externClickListener);
				imgMark  = (ImageView)view.findViewById(R.id.ImageViewFilterMark);
				imgMark.setOnClickListener(externClickListener);
				
				txtToken   = (TextView)view.findViewById(R.id.textViewFilterToken);
				
				txtScanner = (TextView)view.findViewById(R.id.textViewFilterScanner);				
				txtPayloadRemove = (TextView)view.findViewById(R.id.textViewPLRemove);
				txtPayloadStore  = (TextView)view.findViewById(R.id.textViewPLStore);
				txtPayloadDrive  = (TextView)view.findViewById(R.id.TextViewPLDrive);
				
				txtPayloadNothing = (TextView)view.findViewById(R.id.TextViewPLNothing);
				
				view.setTag(R.id.imageViewFilterActive, imgState);
				view.setTag(R.id.ImageViewFilterMark  , imgMark);
				
				view.setTag(R.id.textViewFilterToken  , txtToken);
				view.setTag(R.id.textViewFilterScanner, txtScanner);
				view.setTag(R.id.textViewPLRemove , txtPayloadRemove);
				view.setTag(R.id.textViewPLStore  , txtPayloadStore);
				view.setTag(R.id.TextViewPLDrive  , txtPayloadDrive);				
				view.setTag(R.id.TextViewPLNothing, txtPayloadNothing);
			}
			else { 
				view = convertView;
				
				imgState   = (ImageView)view.getTag(R.id.imageViewFilterActive);
				imgMark  = (ImageView)view.getTag(R.id.ImageViewFilterMark);
				
				txtToken   = (TextView)view.getTag(R.id.textViewFilterToken);
				txtScanner = (TextView)view.getTag(R.id.textViewFilterScanner);
				txtPayloadRemove = (TextView)view.getTag(R.id.textViewPLRemove);
				txtPayloadStore	 = (TextView)view.getTag(R.id.textViewPLStore);
				txtPayloadDrive  = (TextView)view.getTag(R.id.TextViewPLDrive);				
				txtPayloadNothing= (TextView)view.getTag(R.id.TextViewPLNothing);
			}
			
			// Ανάκτηση προς προβολή φίλτρου 
			final FilterItem item = getItem(position);
			
			// Πρόκειται για ενεργό ή ανενεργό φίλτρο;
			imgState.setImageResource(item.active ? 
					android.R.drawable.checkbox_on_background:
						android.R.drawable.checkbox_off_background);
			imgState.setTag(position);			
			
			// Πρόκειται για μαρκαρισμένο φίλτρο ή όχι; 
			imgMark.setImageResource(item.mark ? 
					android.R.drawable.checkbox_on_background:
						android.R.drawable.checkbox_off_background);
			imgMark.setTag(position);
			
			/*
			 * Αν σε κατάσταση mark τότε απόκρυψη της ένδειξης ενεργού φίλτρυ
			 *  και εμφάνιση της ένδειξης μαρκαρισμένου φίλτρου
			 */
			if(markMode) { 
				imgMark.setVisibility(View.VISIBLE);
				imgState.setVisibility(View.INVISIBLE);
			}
			else { 
				imgMark.setVisibility(View.INVISIBLE);
				imgState.setVisibility(View.VISIBLE);				
			}
			
			// Προβολή του ζητούμενου αλφαριθμητικού
			txtToken.setText(item.token);
			// Προβολή της θέσης του ζητούμενου αλφαριθμητικού
			txtScanner.setText(item.getMatchingTypeStr(getContext()));
						
			/*
			 * Προβολή των δράσεων στις οποίες θα προβεί το φίλτρο όταν η 
			 *  ειδοποίηση συμμορφώνεται στους κανόνες 
			 */
			txtPayloadRemove.setVisibility(
					0 != (item.payload & FilterItem.PAYLOAD_REMOVE) ? 
							View.VISIBLE: View.GONE);
			txtPayloadStore.setVisibility(
					0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_DB) ? 
							View.VISIBLE: View.GONE);
			
			if(0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD) 
			|| 0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM)
			|| 0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI)) { 
				txtPayloadDrive.setVisibility(View.VISIBLE);
				
				// Αντιγραφή στο Google Drive αν υπάρχει οποιαδήποτε υπηρεσία δεδομένων
				if(0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD))
					txtPayloadDrive.setText(R.string.pl_copy_gd_wifi_gsm);
				// Αντιγραφή στο Google Drive αν υπάρχει υπηρεσία δεδομένων δικτύου (πχ. GSM,3G κλπ)
				if(0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM))
					txtPayloadDrive.setText(R.string.pl_copy_gd_gsm);
				// Αντιγραφή στο Google Drive αν υπάρχει υπηρεσία δεδομένων 802.11 (WiFi)
				if(0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI))
					txtPayloadDrive.setText(R.string.pl_copy_gd_wifi);
			}
			else// Ο χρήστης δεν έχει επιλέξει χρήση του Google Drive
				txtPayloadDrive.setVisibility(View.GONE);
			
			// Ένδειξη ότι δεν υπάρχει κανένα Payload για το φίλτρο!
			txtPayloadNothing.setVisibility(item.payload == 0 ? View.VISIBLE: View.GONE);
			
			// Η τελική όψη (μορφή) του φίλτρου
			return view;
		}
	}
}
