/*
 * SERVICE ΠΑΡΑΚΟΛΟΥΘΗΣΗΣ ΤΩΝ ΕΙΔΟΠΟΙΗΣΕΩΝ ΤΟΥ ΛΣ
 * 
 * ΑΥΤΟ ΤΟ SERVICE ΔΕΝ ΠΡΕΠΕΙ ΝΑ ΔΙΑΚΟΠΕΙ ΟΣΟ ΕΙΝΑΙ ΣΥΝΔΕΔΕΜΕΝΟ ΜΕ ΤΗΝ 
 * ΥΠΗΡΕΣΙΑ NOTIFICATION-LISTENER-SERVICE ΔΙΟΤΙ ΣΕ ΟΡΙΣΜΕΝΕΣ ΕΚΔΟΣΕΙΣ ΤΟΥ ΛΣ
 * (ΠΧ. KITKAT ή ANDROID 6+) ΜΠΛΟΚΑΡΕΙ ΤΟ NOTIFICATION-LISTENER-SERVICE ΤΗΣ
 * ΣΥΣΚΕΗΣ ΚΑΙ ΧΡΕΙΑΖΕΤΑΙ REBOOT ΓΙΑ ΝΑ ΕΠΑΝΕΛΘΕΙ Η ΟΜΑΛΗ ΛΕΙΤΟΥΡΓΙΑ ΤΟΥ API.
 * 
 * ΤΟ ΠΡΟΒΛΗΜΑ ΕΙΝΑΙ ΓΝΩΣΤΟ
 * 	ISSUE #62811: https://code.google.com/p/android/issues/detail?id=62811
 * 
 * ΟΠΩΣ ΚΑΙ ΑΛΛΑ:
 *  ISSUE #200490, #65419, #203733
 */
package com.example.notification;

import java.util.ArrayList;

import com.example.notification.GoogleDrv.CopyNotificationToGDListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

@SuppressLint("DefaultLocale")
public class NotificationMonitor extends NotificationListenerService 
	implements ConnectionCallbacks, OnConnectionFailedListener {
	/*
	 * Το logcat Tag στο οποίο δημοσιεύονται τα αποτελέσματα εκτέλεσης των
	 * ρουτινών της κλάσης 
	 */
	private static final String LOG_TAG = "NOTIF";	
	
	private DbFilterManager dbManager = null;
	private ConnectivityManager cnManager = null;
	private SharedPreferences pref;
	private static Object NO_ERROR = null;
	
	@Override
	public void onCreate() { 
		super.onCreate();		
		Log.i(LOG_TAG, "NotificationMonitor::onCreate()");
		
		// Ανάκτηση αναφοράς προς την τοπική ΒΔ του προγράμματος
		try { 
			dbManager = DbFilterManager.getInstance(this);						
		} catch(SQLException e) {
			Log.e(LOG_TAG, "NotificationMonitor::onCreate().DbFilterManager->SQLException");
			e.printStackTrace();			
		} catch(Exception e) { 
			Log.e(LOG_TAG, "NotificationMonitor::onCreate().DbFilterManager->Exception");
			e.printStackTrace();
		}
		
		// Ανάκτηση πρόσβασης στην υπηρεσία GoogleDrive.
		try { 
			GoogleDrv.initConnection(this, this, this);
			
			if(GoogleDrv.getUserConnectionSetting(this))
				GoogleDrv.gClient.connect();
		} catch(Exception e) { 
			Log.e(LOG_TAG, "NotificationMonitor::onCreate().initConnection->Exception");
		}
		
		// Ανάκτηση πρόσβασης στις υπηρεσίες δεδομένων της συσκευής
		cnManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		// Ανάκτηση πρόσβασης στις ρυθμίσεις της εφαρμογής
		pref = getSharedPreferences(MainActivity.ACTIVITY_SETS, Context.MODE_PRIVATE);
	}
	
	/***************************************************************************
	 * Συμβατότητα με Android 6+ (;)                                           *
	 ***************************************************************************/
	@Override
	public IBinder onBind(Intent intent) { 
		return super.onBind(intent);
	}
	@Override
	public boolean onUnbind(Intent intent) { 
		return super.onUnbind(intent);
	}
	
	/***************************************************************************
	 * Διαχείριση παρουσίασης νέων ειδοποιήσεων                                *
	 *                                                                         *
	 * Η διαχείριση γίνεται ασύγχρονα, σε νέο ξεχωριστό thread από εκείνο του  *
	 * NotificationListenerService                                             *
	 ***************************************************************************/
	@Override
	public void onNotificationPosted(StatusBarNotification sbn) { 
		Log.i(LOG_TAG, "NotificationMonitor::onNotificationPosted(sbn)");
		
		final Context context = this;
		try { 
			new AsyncTask<StatusBarNotification, Void, Object>() {				
				@Override
				protected Object doInBackground(StatusBarNotification... args) {					
					try {
						final Notification notif = args[0].getNotification();
						final Bundle notifExtras = notif.extras;
						
						// Ο τίτλος της ειδοποίησης
						final String notifTitle    = notifExtras
								.getString(Notification.EXTRA_TITLE, ""),
						// Ο επεκταμένος (αν υπάρχει) τίτλος της ειδοποίησης
									 notifBigTitle = notifExtras
								.getString(Notification.EXTRA_TITLE_BIG, ""),
						// Το κείμενο περιεχόμενο της ειδοποίησης
									 notifText     = notifExtras
								.getString(Notification.EXTRA_TEXT),
						// Συμπληρωματικό κείμενο που βοηθάει την υπηρεσία Accessibility (υποστήριξη για ΑΜΕΑ) του OS  
									 notifTicker   = notif.tickerText != null ? notif.tickerText.toString(): "";
						
						Log.i(LOG_TAG, " -TITLE     = \"" + notifTitle    + "\"");
						Log.i(LOG_TAG, " -BIG TITLE = \"" + notifBigTitle + "\"");
						Log.i(LOG_TAG, " -TEXT      = \"" + notifText     + "\"");
						Log.i(LOG_TAG, " -TICKER    = \"" + notifTicker   + "\"");
						
						// Τα φίλτρα του χρήστη 
						final ArrayList<FilterItem> filterList = 
								dbManager.listFilters(context);						
						
						/*
						 * Αναζήτηση του κειμένου κάθε φίλτρου α)στον τίτλο,
						 *  β)στον επεκταμένο τίτλο, γ)στο περιεχόμενο της 
						 * ειδοποίησης και δ)στο βοηθητικό κείμενο (ticker) για
						 *  τις εφαρμογές υποστήριξης ΑΜΕΑ.
						 */						
						for(FilterItem filter: filterList) { 
							// Έλεγχος μόνο ενεργών φίλτρων
							if(!filter.active)
								continue;
							
							if(matchStr(filter.matchingType, notifTitle   , filter.token)
							|| matchStr(filter.matchingType, notifBigTitle, filter.token)
							|| matchStr(filter.matchingType, notifText    , filter.token)
							|| matchStr(filter.matchingType, notifTicker  , filter.token)) {
								// Εφαρμογή του Payload κάθε φίλτρου
								Log.i(LOG_TAG, "  -APPLY FILTER PAYLOAD -> " + filter.token);
								
								applyPayload(filter, args[0]);
							}														
						}
						return NO_ERROR;
					} catch(Exception e) { 
						e.printStackTrace();
						
						Log.e(LOG_TAG, "NotificationMonitor::OnNotificationPosted->Exception!");
						return e;
					}
				}
				@Override
				protected void onPostExecute(Object r) { 
					if(r instanceof Exception)
						return;
				}
			}.execute(sbn);
		} catch(Exception e) { 
			e.printStackTrace();
			
			Log.e(LOG_TAG, "NotificationMonitor::onNotificationPosted(sbn)->Exception");
		}	
	}
	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) { 
		// Δεν χρησιμοποιείται
	}
	
	/***************************************************************************
	 * Έλεγχος εμφάνισης αλφαριθμητικού token στο αλφαριθμητικό περιεχόμενο του*
	 *  str.                                                                   *
	 *                                                                         *
	 * Η εμφάνισης του token εντός του str μπορεί να είναι (matchType) στην     *
	 *  έναρξη (MATCHING_TYPE_START), στην λήξη (MATCHING_TYPE_END) ή          *
	 * οπουδήποτε (MATCHING_TYPE_ANY).                                         *
	 *                                                                         *
	 * Αν το token βρεθεί στο σημείο matchType του str τότε η συνάρτηση        *
	 *  επιστρέφει true αλλιώς false.                                          *
	 ***************************************************************************/
	private boolean matchStr(final int matchType, String str, String token) {
		if(str != null && !str.isEmpty() && token != null && !token.isEmpty()) { 
			str = str.toUpperCase();
			token = token.toUpperCase();
			
			switch(matchType) { 
			case FilterItem.MATCHING_TYPE_START:
				return str.startsWith(token);
			case FilterItem.MATCHING_TYPE_END:
				return str.endsWith(token);
			case FilterItem.MATCHING_TYPE_ANY:
				return str.indexOf(token) != -1; 
			default: break;
			}
		}
		return false;
	}
	
	/***************************************************************************
	 * Εκτέλεση της προβλεπομένης δράσης / αποτελέσματος (payload) του φίλτρου *
	 *  (filter) στην Ειδοποίηση (StatusBarNotification).                      *
	 ***************************************************************************/
	private void applyPayload(final FilterItem filter, final StatusBarNotification sbn) {			
		// Αντιγραφή της ειδοποίησης στην τοπική ΒΔ του προγράμματος
		if(0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_DB)) { 
			Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->COPY_TO_DB");
			
			dbManager.insertNotification(new NotificationItem(sbn));
		}
		
		// Αφαίρεση της ειδοποίησης από το Notification Tray του OS
		if(0 != (filter.payload & FilterItem.PAYLOAD_REMOVE)) { 
			Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->PAYLOAD_REMOVE");
			
			/*
			 * Λειτουργεί σε όλες τις εκδόσεις Android
			 *  (KitKat+) όσο το minSdkVersion και targetSdkVersion
			 * στο AndroidManifest.xml είναι 19.
			 * 
			 * Αν είναι 19+ τότε πρέπει να αντικατασταθεί από την 
			 *  νέα διαδικασία cancelNotification(String key).
			 *  
			 * Αν το sbn αναφέρεται σε μια ειδοποίηση η οποία δεν πρέπει να 
			 *  αφαιρεθεί, για παράδειγμα μια τηλεφωνική κλήση σε εξέλιξη (FLAG_ONGOING_EVENT), 
			 * ή μια ειδοποίηση που δεν διαγράφεται (FLAG_NO_CLEAR), τότε    
			 *  το PAYLOAD_REMOVE αγνοείται.
			 */
			if(0 == (sbn.getNotification().flags & Notification.FLAG_NO_CLEAR) 
			&& 0 == (sbn.getNotification().flags & Notification.FLAG_ONGOING_EVENT))
				cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());
			else
				Log.i(LOG_TAG, "  -NOTIFICATION SKIPPED->FLAG_NO_CLEAR || FLAG_ONGOING_EVENT ");
		}
			
		// Αντιγραφή της ειδοποίησης στο Google Drive του χρήστη
		if(0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD)
		|| 0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM)
		|| 0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI)) { 
			if(0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM))
				Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->PAYLOAD_COPY_TO_GD_GSM");
			else
				if(0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI))
					Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->PAYLOAD_COPY_TO_GD_WIFI");
				else
					Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->PAYLOAD_COPY_TO_GD");
			
			if(GoogleDrv.getUserConnectionSetting(this) && GoogleDrv.isUserAuthorized())
				GoogleDrv.gClient.connect();
			
			// Αν δεν υπάρχει δίκτυο, τότε το PAYLOAD ακυρώνεται
			final NetworkInfo netInf;
			
			if( cnManager == null 
			|| (netInf = cnManager.getActiveNetworkInfo()) == null 
			|| !netInf.isConnected()) { 
				Log.e(LOG_TAG, "  -NOTIFICATION SKIPPED->NO DATA CONNECTION");
				return;
			}			
			
			// Το φίλτρο επιτρέπει οποιαδήποτε κατηγορία σύνδεσης;
			if(0 == (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD)) { 			
				// Το φίλτρο απαιτεί σύνδεση κατηγορίας GSM-GPRS/EDGE ή 3G/4G
				if(netInf.getType() == ConnectivityManager.TYPE_MOBILE 
				&& (0 == (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM))) {
					Log.e(LOG_TAG, "  -NOTIFICATION SKIPPED->NO WIFI/WIMAX/BT DATA CONNECTION");
					return;
				}
				// Το φίλτρο απαιτεί σύνδεσης κατηγορίας WIFI/WIMAX ή BLUETOOTH
				if((netInf.getType() == ConnectivityManager.TYPE_WIFI
				||  netInf.getType() == ConnectivityManager.TYPE_WIMAX
				||  netInf.getType() == ConnectivityManager.TYPE_BLUETOOTH)
				&& (0 == (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI))) { 
					Log.e(LOG_TAG, "  -NOTIFICATION SKIPPED->NO GSM/3G/4G DATA CONNECTION");
					return;
				}
			}
						
			GoogleDrv.CopyNotificationToGD(
					// Ανάγνωση ρυθμίσεων προγράμματος για την αντιγραφή των ειδοποιήσεων στο GoogleDrive 
					pref.getString(MainActivity.ACTIVITY_SET_GD_FOLDER_TITLE, GoogleDrv.GD_FOLDER),
					pref.getInt(MainActivity.ACTIVITY_SET_GD_FILE_TITLE, GoogleDrvDialog.FILE_TITLE_NOTIF_TITLE),					
					new NotificationItem(sbn), 
					new CopyNotificationToGDListener() {						
						@Override
						public void onCopyNotificationGD(String folderTitle, String fileTitle, boolean r) {
							if(!r) {
								Log.e("PRG", "NotificationMonitor::onCopyNotificationGD\n" + 
										" folderTitle = " + folderTitle + "\n" + 
										" fileTitle   = " + fileTitle + "\n" + 
										" Failure!");
							}
						}
					});  		
		}
	}

	/***************************************************************************
	 * Διαχειριστής σύνδεσης με τις ασύρματες υπηρεσίες της Google             *
	 ***************************************************************************/
	@Override
	public void onConnected(Bundle bundle) {
		// Η σύνδεση με την υπηρεσία GoogleDrive επετεύχθει
		Log.i(LOG_TAG, "NotificationMonitor::onConnected->GoogleDriveClient");
	}
	@Override
	public void onConnectionFailed(ConnectionResult r) {
		Log.e(LOG_TAG, "NotificationMonitor::onConnectionFailed->GoogleDriveClient");
		
		if(!GoogleDrv.attemptReconnect())
			Log.e(LOG_TAG, "NotificationMonitor::OnConnectionFailed->attemptReconnect failure!");
	}
	@Override
	public void onConnectionSuspended(int arg0) {
		Log.i(LOG_TAG, "NotifictionMonitor::onConnectionSuspended->GoogleDriveClient");
	}
}
