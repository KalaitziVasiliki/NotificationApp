/*******************************************************************************
 * ΠΡΟΒΟΛΗ / ΔΙΑΓΡΑΦΗ ΙΣΤΟΡΙΚΟΥ                                                *
 *******************************************************************************/
package com.example.notification;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.example.notification.YesNoDialog.YesNoListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends Activity implements 
	OnClickListener, YesNoListener {	
	
	// Η ταυτότητα του διαλόγου επιβεβαίωσης διαγραφής μαρκαρισμένων ειδοποιήσεων
	private final static int YESNO_ID_DELETE = 1;	
	
	// Διάλογος προόδου για χρονοβόρες εντολές
	private ProgressDialog pDlg = null;
	// Subclass του ArrayAdapter για την προβολή των ειδοποιήσεων στο ListView του Activity
	private NotifAdapter nAdapter;
	// Ο διαχειριστής της ΒΔ
	private DbFilterManager dbManager;
	// Αναφορά στον διαχειριστή εγκαταστημένων εφαρμογών του OS (PackageManager)
	private PackageManager packMan; 
	
	/*
	 * Η ημερομηνία & ώρα που φορτώθηκαν οι ειδοποιήσεις στο ListView του Activity
	 *  από την ΒΔ του προγράμματος.
	 *  
	 * (Χρησιμοποιείται από την συνάρτηση DateUtils.formatSameDayTime της
	 *  NotifAdapter.getView)
	 */
	private long lastRTC = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		// Προετοιμασία του UI
		setContentView(R.layout.activity_history);
		
		getActionBar().show();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		/* 
		 * Προετοιμασία NotifAdapter για την προβολή των ειδοποιήσεων της ΒΔ 
		 *  στο ListView
		 */
		nAdapter = new NotifAdapter((ListView)findViewById(R.id.listViewHistory), this);
		
		// Πρόσβαση στον διαχειριστή εγκαταστημένων εφαρμογών του OS (PackageManager)
		packMan = getPackageManager(); 		
		
		// Προετοιμασία ΒΔ και ανάγνωση των καταχωρημένων ειδοποιήσεων της
		try { 
			dbManager = DbFilterManager.getInstance(this);
			listNotifications();
		} catch(SQLException e) { 
			CenterToast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			finish();
		}		
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
		 * Επειδή δανείζομαι το μενού από το Filters Activity, φροντίζω το item
		 *  "εισαγωγής φίλτρων" να είναι πάντα αόρατο!
		 */
		menu.findItem(R.id.itemFilterAdd).setVisible(false);
		/*
		 * Αντίθετα, το πλήκτρο "επικαιροποίησης ιστορικού" είναι πάντα ορατό
		 *  εκτός και αν είμαστε σε κατάσταση mark mode 
		 */
		menu.findItem(R.id.itemHistoryRefresh).setVisible(!nAdapter.getMarkMode());
		/*
		 * Αν σε Mark mode (ή αν κενό ListView) τότε απόκρυψη του item έναρξης  
		 *  mark mode
		 */			
		menu.findItem(R.id.itemFilterMarkMode).setVisible(!nAdapter.getMarkMode() && !nAdapter.isEmpty());
		/*
		 * Αντίστροφα αν σε Mark mode τότε εμφάνιση του πλήκτρου Επιλογή ή 
		 *  "Από επιλογή" όλων και των πλήκτρων "Επιβεβαίωση διαγραφής" & "Ακύρωση"
		 * (το πρώτο μόνο όταν υπάρχει έστω και μια επιλεγμένη ειδοποίηση)
		 */
		menu.findItem(R.id.itemFilterDoRemove)
			.setVisible(nAdapter.getMarkMode() && nAdapter.getMarkedCount() > 0); 
		menu.findItem(R.id.itemFilterSelect).setVisible(nAdapter.getMarkMode());
		menu.findItem(R.id.itemFilterCancel).setVisible(nAdapter.getMarkMode()); 
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
		// Ο χρήστης ζήτησε επικαιροποίηση (refresh) του ιστορικού 
		case R.id.itemHistoryRefresh:
			listNotifications();
			return true;
		// Ο χρήστης επίλεξε την έναρξη του mark mode
		case R.id.itemFilterMarkMode:
			nAdapter.setMarkMode(true);
			invalidateOptionsMenu();
			return true;
		// Επιβεβαίωση διαγραφής επιλεγμένων ειδοποιήσεων
		case R.id.itemFilterDoRemove:			
			YesNoDialog.show(this, 
					YESNO_ID_DELETE, 
					getText(R.string.title_confirmation), 
					getResources().getQuantityString(R.plurals.notifications_to_delete, 
							nAdapter.getMarkedCount(), 
							nAdapter.getMarkedCount()), this);
			return true;
		/*
		 * Ο χρήστης επιλέγει ή από επιλέγει όλες τις ειδοποιήσεις
		 * 
		 * Αν υπάρχει έστω μία επιλεγμένη ειδοποίηση τότε από επιλέγεται αλλιώς 
		 * επιλέγονται όλες οι ειδοποιήσεις.
		 */
		case R.id.itemFilterSelect:
			nAdapter.setMarkAll(nAdapter.getMarkedCount() == 0);
			invalidateOptionsMenu();
			return true;
		// Ο χρήστης ζήτησε ακύρωση mark mode
		case R.id.itemFilterCancel:
			nAdapter.setMarkMode(false);
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
		if(nAdapter.getMarkMode()) {
			nAdapter.setMarkMode(false);
			invalidateOptionsMenu();
		}
		else
			super.onBackPressed();
	}
	
	/***************************************************************************
	 * ClickListener 				Διαχείριση των πλήκτρων του Activity       *
	 ***************************************************************************/		  
	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		// Μαρκάρισμα ή ξεμαρκάρισμα διαγραφής ειδοποίησης
		case R.id.imageViewHistoryMarked:
			nAdapter.setNotificationMark((Integer)view.getTag(), 
					!nAdapter.isNotificationMarked((Integer)view.getTag()));
			invalidateOptionsMenu();
			return;
		}
	}	

	/***************************************************************************
	 * Επιστροφή όλων των καταχωρημένων ειδοποιήσεων της ΒΔ στο ListView του   *
	 *  Activity.                                                              *
	 *                                                                         *
	 * Η ενημέρωση του ListView γίνεται ασύγχρονα σε ξεχωριστό Thread ώστε να  *
	 * μην καθυστερεί την εκτέλεση του UI.                                     *
	 ***************************************************************************/	
	private void listNotifications() {
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
				// Επιστροφή όλων των ειδοποιήσεων της ΒΔ
				return dbManager.listNotifications(context);
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
					lastRTC = Calendar.getInstance().getTimeInMillis();
					
					// Ενημέρωση του ArrayAdapter με τις καταχωρημένες ειδοποιήσεις 
					nAdapter.setNotificationList((ArrayList<NotificationItem>)r);
					
					// Εμφάνιση ή απόκρυψη της ένδειξης "κενού ListView"
					((TextView)findViewById(R.id.textViewHistoryEmpty))
						.setVisibility(nAdapter.isEmpty() ? View.VISIBLE: View.GONE);					
					// Ενημέρωση της ένδειξης αριθμού καταχωρημένων ειδοποιήσεων
					((TextView)findViewById(R.id.textViewNotificationCounter))
						.setText(!nAdapter.isEmpty() ? 
								getResources().getQuantityString(R.plurals.notification_counter, 
										nAdapter.getCount(), nAdapter.getCount()): "");
					
					// Τέλος του mark mode
					nAdapter.setMarkMode(false);					
					// Ενημέρωση του Menu του Activity
					invalidateOptionsMenu();
				}
			}
		}.execute();
	}	
	
	/***************************************************************************
	 * Διαγραφή από την ΒΔ όλων των μαρκαρισμένων ειδοποιήσεων                 *
	 *                                                                         *
	 * Η διαγραφή γίνεται ασύγχρονα σε ξεχωριστό Thread ώστε να μην καθυστερεί *
	 * την εκτέλεση του UI.                                                    *
	 ***************************************************************************/
	private void deleteNotifications() { 
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
				// Διαγραφή όλων των μαρκαρισμένων ειδοποιήσεων του ListView
				try { 
					for(int position = 0; position < nAdapter.getCount(); position++) { 
						final NotificationItem item = nAdapter.getItem(position);
						
						if(item.mark) // μαρκαρισμένη;
							if(!dbManager.deleteNotification(item))
								throw new Exception(context.getString(R.string.error_notification_mass_delete) + item.title);						
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
				listNotifications();
			}
		}.execute();
	}	
		
	/***************************************************************************
	 * YesNoListener 			Επιβεβαίωση ή απόρριψη ερωτήματος Activity	   *
	 ***************************************************************************/
	@Override
	public void OnYesNoListener(int id, int which) {
		switch(id) { 
		case YESNO_ID_DELETE:	// Διαγραφή επιλεγμένων ειδοποιήσεων;
			if(which == DialogInterface.BUTTON_POSITIVE)
				deleteNotifications();
			return;
		default: return;
		}
	}		
	
	/***************************************************************************
	 * NotifAdapter   		 Subclass του ArrayAdapter ώστε να διαχειρίζεται   * 	
	 *                		 ειδοποιήσει κατηγορίας NotificationItem           *
	 ***************************************************************************/
	private class NotifAdapter extends ArrayAdapter<NotificationItem> {		
		private final OnClickListener externClickListener;
		
		private boolean markMode = false;
		private int markedCount = 0;
		
		public NotifAdapter(final ListView owner,
				final OnClickListener externClickListener) {
			super(owner.getContext(), R.layout.item_history);
						
			owner.setAdapter(this);
			this.externClickListener = externClickListener;
		}
		
		// Αύξηση του αριθμού των μαρκαρισμένων ειδοποιήσεων κατά 1
		private void increaseMarkedCount() { 
			if(++markedCount > getCount())
				markedCount = getCount();
		}
		// Μείωση του αριθμού των μαρκαρισμένων ειδοποιήσεων κατά 1
		private void decreaseMarkedCount() { 
			if(--markedCount < 0)
				markedCount = 0;
		}
		// Επιστροφή του αριθμού των μαρκαρισμένων ειδοποιήσεων
		public int getMarkedCount() { 
			return markedCount;
		}
		
		/*
		 * Αν mark ίσο με true τότε μαρκάρισμα όλων των ειδοποιήσεων αλλιώς
		 *  ξεμαρκάρισμα τους.
		 * 
		 * Αν το FilterAdapter είναι σε edit mode τότε η συνάρτηση επιστρέφει
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
		 * Φόρτωμα των ειδοποιήσεων της ArrayList<NotificationItem> στο ListView.
		 * 
		 * Αν το list είναι null τότε το περιεχόμενο του ListView διαγράφεται.
		 */
		public void setNotificationList(final ArrayList<NotificationItem> list) {			
			clear();
			
			if(list != null)			
				addAll(list);
			
			if(isEmpty())
				setMarkMode(false);
		}		
		
		/*
		 * Ενεργοποίηση ή απενεργοποίηση του ρυθμού μαρκαρίσματος (mark mode)
		 *  του NotifAdapter.
		 * 
		 * Όταν το mark mode είναι ενεργό, ο χρήστης μπορεί να μαρκάρει ή να 
		 *  ξεμαρκάρει ειδοποιήσεις του ListView.
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
		 * Επιστροφή true αν η ειδοποίηση στην θέση position του ListView είναι
		 *  μαρκαρισμένη αλλιώς false.
		 */
		public boolean isNotificationMarked(final int position) { 
			return getItem(position).mark;
		}		
		/*
		 * Μαρκάρισμα ή ξεμαρκάρισμα (mark) της ειδοποίησης στην θέση position 
		 *  του ListView.
		 * 
		 * Η συνάρτηση επιστρέφει την είσοδο της (mark).
		 */
		public boolean setNotificationMark(final int position, final boolean mark) { 
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
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) { 
			final View view;
			
			final ImageView imgAppIcon, 
							imgMark;
			
			final TextView txtApp,
						   txtTitle,
						   txtMsg,
						   txtTicker,
						   txtClock;
			
			if(convertView == null) { 
				view = View.inflate(getContext(), R.layout.item_history, null);
				
				imgAppIcon = (ImageView)view.findViewById(R.id.imageViewHistoryAppIcon);
				
				imgMark    = (ImageView)view.findViewById(R.id.imageViewHistoryMarked);
				imgMark.setOnClickListener(externClickListener);
				
				txtApp      = (TextView)view.findViewById(R.id.textViewHistoryApp);
				txtTitle    = (TextView)view.findViewById(R.id.textViewHistoryTitle);
				txtMsg      = (TextView)view.findViewById(R.id.textViewHistoryBody);
				txtTicker   = (TextView)view.findViewById(R.id.textViewHistoryTicker);
				txtClock    = (TextView)view.findViewById(R.id.textViewHistoryClock);
				
				view.setTag(R.id.imageViewHistoryAppIcon,imgAppIcon);
				view.setTag(R.id.imageViewHistoryMarked, imgMark);
				
				view.setTag(R.id.textViewHistoryApp   , txtApp);
				view.setTag(R.id.textViewHistoryTitle , txtTitle);
				view.setTag(R.id.textViewHistoryBody  , txtMsg);
				view.setTag(R.id.textViewHistoryTicker, txtTicker);
				view.setTag(R.id.textViewHistoryClock , txtClock);
			}
			else { 
				view = convertView;
				
				imgAppIcon = (ImageView)view.getTag(R.id.imageViewHistoryAppIcon); 
				imgMark    = (ImageView)view.getTag(R.id.imageViewHistoryMarked);
				
				txtApp   = (TextView)view.getTag(R.id.textViewHistoryApp);
				txtTitle = (TextView)view.getTag(R.id.textViewHistoryTitle);
				txtMsg   = (TextView)view.getTag(R.id.textViewHistoryBody);
				txtTicker= (TextView)view.getTag(R.id.textViewHistoryTicker);
				txtClock = (TextView)view.getTag(R.id.textViewHistoryClock);
			}
			
			// Ανάκτηση προβαλλόμενης ειδοποίησης
			final NotificationItem item = getItem(position);						
			
			// Πρόκειται για μαρκαρισμένη ειδοποίηση ή όχι; 
			imgMark.setImageResource(item.mark ? 
					android.R.drawable.checkbox_on_background:
						android.R.drawable.checkbox_off_background);
			imgMark.setTag(position);
			
			// Αν σε κατάσταση mark τότε εμφάνιση της ένδειξης μαρκαρισμένης ειδοποίησης
			imgMark.setVisibility(markMode ? View.VISIBLE: View.INVISIBLE);			
			
			// Προβολή της εφαρμογής που ανάρτησε την ειδοποίηση
			final Pair<String, Drawable> appInf = item.getAppTitleIcon(packMan);			
			txtApp.setText(appInf.first);
			imgAppIcon.setImageDrawable(appInf.second);
			
			// Προβολή τίτλου, περιεχομένου και βοηθητικού κειμένου του Notification
			txtTitle.setText(item.getTitle());
			txtMsg.setText(item.message);
			txtTicker.setText(item.ticker);
			// Προβολή του χρόνου ανάρτησης του Notification
			txtClock.setText(DateUtils.formatSameDayTime(item.clock,
					lastRTC, 
					DateFormat.DEFAULT,
					DateFormat.DEFAULT));
			
			return view;
		}		
	}
}