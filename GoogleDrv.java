/*******************************************************************************
 * ΣΤΑΤΙΚΗ ΚΛΑΣΗ ΔΙΑΧΕΙΡΙΣΗΣ ΤΗΣ ΥΠΗΡΕΣΙΑΣ GOOGLE DRIVE                        *
 *******************************************************************************/
package com.example.notification;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

public class GoogleDrv {	
	/*
	 * Η default ονομασία του φακέλου στον οποίο αποθηκεύονται οι ειδοποιήσεις
	 * του προγράμματος
	 */
	public static final String GD_FOLDER = "Ιστορικό Ειδοποιήσεων";
	/*
	 * Το χρονικό όριο (σε δευτερόλεπτα) που περιμένει το πρόγραμμα απόκριση από
	 * το GoogleDrive
	 */
	public static final int TIMEOUT_SEC = 30;
	/*
	 * Το logcat Tag στο οποίο δημοσιεύονται τα αποτελέσματα εκτέλεσης των
	 * ρουτινών της κλάσης 
	 */
	private static final String LOG_TAG = "NOTIF";
	/*
	 * Το GoogleApiClient που αναλαμβάνει όλες τις λεπτομέρειες σύνδεσης της 
	 * κλάσης με το GoogleDrive  
	 */
	public static GoogleApiClient gClient = null;
	
	/***************************************************************************
	 * Σύνδεση του Context του προγράμματος με την υπηρεσία Google Drive.      *
	 *                                                                         *
	 * Η επιτυχής σύνδεσης του προγράμματος με την υπηρεσία δημοσιεύεται στο   *
	 *  cbListener event ενώ η αποτυχία στο cfListener.                        *
	 *                                                                         *
	 * Σε περίπτωση αποτυχίας, το πρόγραμμα πρέπει να ακολουθήσει την          *
	 *  διαδικασία επίλυσης που προτείνει η Google.                            *
	 *                                                                         *
	 * Η συνάρτηση επιστρέφει πάντα το δημόσιο GoogleApiClient που υλοποιεί    *
	 *  την επικοινωνία με την υπηρεσία Google Drive.                          * 
	 ***************************************************************************/
	public static GoogleApiClient initConnection(final Context context, 
			final ConnectionCallbacks cbListener,
			final OnConnectionFailedListener cfListener) { 
		if(gClient == null) { 		
			gClient = new GoogleApiClient.Builder(context)
				.addApi(Drive.API)
				.addScope(Drive.SCOPE_FILE)
				.addConnectionCallbacks(cbListener)
				.addOnConnectionFailedListener(cfListener)
				.build();
		}
		return gClient;
	}
	
	/***************************************************************************
	 * Προσπάθεια επίλυσης του σφάλματος (r) που προέκυψε κατά την σύνδεση του *
	 *  Activity (activity) με την υπηρεσία GoogleDrive.                      *
	 *                                                                         *
	 * Αν η συνάρτηση επιστρέψει true τότε το Activity θα πρέπει να καλέσει    *
	 *  ξανά την συνάρτηση initConnection ως απόκριση στο requestCode που      *
	 * θα λάβει στο onActivityResult event του!                                *
	 ***************************************************************************/
	public static boolean resolveConnectionFailure(final Activity activity, 
			final ConnectionResult r, 
			final int requestCode) { 
		if (r.hasResolution()) {
	        try {
	            r.startResolutionForResult(activity, requestCode);
	            return true;
	        } catch (IntentSender.SendIntentException e) {
	        	Log.e(LOG_TAG, "resolveConnectionFailure->SendIntentException");
	        	e.printStackTrace();	        		        
	        }
	    } else {
	        GooglePlayServicesUtil
	        	.getErrorDialog(r.getErrorCode(), activity, 0)
	        	.show();	       
	    }	
		return false;
	}	
	
	/***************************************************************************
	 * Αίτημα επανασύνδεσης του προγράμματος στην υπηρεσία GoogleDrive.        *
	 *                                                                         *
	 * Η συνάρτηση επιστρέφει false αν υπάρξει σφάλμα σύνδεσης ή όταν το       *
	 *  gClient είναι ήδη συνδεδεμένο ή συνδέεται σο GoogleDrive.              *
	 ***************************************************************************/
	public static boolean attemptReconnect() { 
		if(gClient != null) { 
			try { 
				if(!gClient.isConnected() && !gClient.isConnecting()) {
					gClient.connect();
					return true;
				}
			} catch(Exception e) { 
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/***************************************************************************
	 * Επιστροφή του αν ο χρήστης είναι συνδεδεμένος στην υπηρεσία GoogleDrive *
	 *                                                                         * 
	 * true αν ο λογαριασμός του χρήστη συνδέθηκε κάποια στιγμή στο παρελθόν   *
	 *  επιτυχώς με το GoogleDrive αλλιώς false.                               *
	 ***************************************************************************/
	public static boolean getUserConnectionSetting(final Context context) { 
		return context.getSharedPreferences(MainActivity.ACTIVITY_SETS, Context.MODE_PRIVATE)
				.getBoolean(MainActivity.ACTIVITY_SET_GD_CONNECT, false);
	}
	/***************************************************************************
	 * Αποθήκευση κατάστασης σύνδεσης του χρήστη στην υπηρεσία GoogleDrive     *
	 *                                                                         *
	 * true αν η αποθήκευση της κατάστασης ήταν επιτυχής αλλιώς false.         *
	 ***************************************************************************/
	public static boolean setUserConnectionSetting(final Context context, 
			final boolean connected) { 
		return context.getSharedPreferences(MainActivity.ACTIVITY_SETS, Context.MODE_PRIVATE)
				.edit()
				.putBoolean(MainActivity.ACTIVITY_SET_GD_CONNECT, connected)
				.commit();
	}
	
	/***************************************************************************
	 * Επιστροφή true αν ο λογαριασμός χρήστη έχει λάβει εξουσιοδότηση για     *
	 *  πρόσβαση στην υπηρεσία Google Drive αλλιώς false.                      *  
	 ***************************************************************************/
	public static boolean isUserAuthorized() { 
		return gClient != null ? gClient.hasConnectedApi(Drive.API): false; 
	}
	
	/***************************************************************************
	 * Αντιγραφή ειδοποίησης (notification) σε φάκελο (folderTitle) του        *
	 *  Google Drive και με συγκεκριμένη μορφή για τον τίτλου του αρχείου.     *
	 *                                                                         *  
	 * Σε περίπτωση επιτυχίας η συνάρτηση επιστρέφει true αλλιώς false         *
	 *                                                                         *
	 * -Η αντιγραφή γίνεται με συγχρονισμένο τρόπο (await) συνεπώς πρέπει να   *
	 *   κληθεί εντός ενός ξεχωριστού Thread (πχ. AsyncTasκ) αλλιώς μπορεί να  *
	 *  οδηγήσει σε ANR το πρόγραμμα!!                                         *
	 *                                                                         *
	 * -Αν για οποιοδήποτε λόγο (πχ. διαγραφή καταλόγου απευθείας από το GD)   * 
	 *   υπάρξει αστοχία συγχρονισμού μεταξύ του GDAA (GoogleDrive Android API)*
	 *  και του Server της υπηρεσίας GoogleDrive θα πρέπει να κληθεί           *
	 *   (συνήθως μια φορά) η εντολή:                                          *
	 *                                                                         *
	 *  Drive.DriveApi.requestSync(gClient);                                   *
	 *                                                                         *
	 *  Η χρήση της requestSync πρέπει να γίνεται όσο το δυνατόν λιγότερο καθώς*
	 *   κοστίζει σε πόρους του GoogleDrive Server.                            *
	 *                                                                         *
	 *  Εναλλακτικά μπορούμε να αφήσουμε το GDAA να συγχρονίσει αυτόματα μετά  *
	 *   από ένα συγκεκριμένο χρονικό διάστημα (πχ. από 5' ως 30'+) την διένεξη*
	 *  που προκαλέσαμε με την απευθείας επέμβαση μας στον κατάλογο folderTitle*
	 *   που διαχειρίζεται η εφαρμογή!                                         *
	 ***************************************************************************/
	private static boolean syncCopyNotificationToGD(final String folderTitle,
			final int fileTitle,
			final NotificationItem notification) { 
		// Αναζήτηση του φακέλου με τίτλο 'folderTitle' στο GoogleDrive 
		final MetadataBufferResult qFolder = Drive.DriveApi.query(gClient, 
				new Query.Builder()
					.addFilter(Filters.eq(SearchableField.TITLE, 
							folderTitle))
					.addFilter(Filters.eq(SearchableField.MIME_TYPE, 
							"application/vnd.google-apps.folder"))
					.addFilter(Filters.eq(SearchableField.TRASHED, 
							false))
					.build())
				.await(TIMEOUT_SEC, TimeUnit.SECONDS);
		// Μήπως η κλήση στην υπηρεσία GoogleDrive Query απέτυχε; 
		if(!qFolder.getStatus().isSuccess()) {
			Log.e(LOG_TAG, "syncCopyNotificationToGD::Query failure!");
			return false;
		}
		
		/*
		 * Η ταυτότητα του φακέλου στο GoogleDrive
		 * 
		 * -Το GoogleDrive υποστηρίζει απεριόριστο αριθμό φακέλων και αρχείων
		 *   με την ίδια ονομασία, αλλά με ξεχωριστό DriveId για κάθε ένα από
		 *  αυτά! 
		 */
		DriveId folderId;		
		
		/*
		 * Αν το Query επιστρέψει μηδενικό αριθμό στοιχείων τότε ο φάκελος
		 *  με τίτλο 'folderTitle' δεν βρέθηκε οπότε πρέπει να τον δημιουργήσω.
		 */
		if(qFolder.getMetadataBuffer().getCount() == 0) {
			DriveFolderResult rFolder = Drive.DriveApi.getRootFolder(gClient)
				.createFolder(gClient, 
						new MetadataChangeSet.Builder()
							.setTitle(folderTitle)						
							.build())
				.await(TIMEOUT_SEC, TimeUnit.SECONDS);
			// Μήπως η κλήση στην υπηρεσία GoogleDrive createFolder απέτυχε;
			if(!rFolder.getStatus().isSuccess()) {
				Log.e(LOG_TAG, "syncCopyNotification::createFolder failure!");
				return false;
			}
			folderId = rFolder.getDriveFolder().getDriveId();
		}
		else			
			folderId = qFolder.getMetadataBuffer().get(0).getDriveId();
		
		/*
		 * Δημιουργία ενός νέου αρχείου (κατηγορίας text/html) στον φάκελο με 
		 *  ταυτότητα 'folderId'
		 */
		final DriveFileResult rFile = Drive.DriveApi.getFolder(gClient, folderId)
				.createFile(gClient, 
						new MetadataChangeSet.Builder()
							.setTitle(fileTitle == GoogleDrvDialog.FILE_TITLE_NOTIF_TITLE 
							&& !notification.title.isEmpty() ? 
									notification.title: notification.app)
							.setMimeType("text/html")
						.build(),
						null)
				.await(TIMEOUT_SEC, TimeUnit.SECONDS);
		// Μήπως η κλήση στην υπηρεσία GoogleDrive createFile απέτυχε;
		if(!rFile.getStatus().isSuccess()) { 
			Log.e(LOG_TAG, "syncCopyNotification::createFile failure!");
			return false;
		}
		
		// ’νοιγμα αρχείου μόνο για εγγραφή (WRITE_ONLY)
		final DriveContentsResult rFileIO = rFile.getDriveFile()
				.open(gClient, DriveFile.MODE_WRITE_ONLY, null)
				.await(TIMEOUT_SEC, TimeUnit.SECONDS);
		// Μήπως η κλήση στην υπηρεσία GoogleDrive open (MODE_WRITE_ONLY) απέτυχε;
		if(!rFileIO.getStatus().isSuccess()) { 
			Log.e(LOG_TAG, "syncCopyNotification::open(file) failure!");
			return false;
		}		
		
		// Πρόσβαση στα περιεχόμενα του αρχείου - έναρξη I/O..
		final DriveContents fileIO = rFileIO.getDriveContents();
		
		boolean fileCommited = false;
		
		/*
		 * Η εγγραφή στο αρχείο γίνεται με την βοήθεια του PrintStream που 
		 *  απλοποιεί την εγγραφή κειμένου γραμμή-γραμμή.
		 * 
		 * Το περιεχόμενο του αρχείου ορίζεται σε μορφή 'UTF-16', για την σωστή
		 *  προβολή των περιεχομένων του πρέπει να χρησιμοποιηθεί συμβατό λογισμικό
		 * (editor). 
		 */
		PrintStream pOS = null;		
		try { 			 
			pOS = new PrintStream(fileIO.getOutputStream(), false, "UTF-16");
			pOS.println(notification.app);
			pOS.println(notification.getClockStr());
			pOS.println(notification.getTitle());
			pOS.println(notification.message);
			pOS.println(notification.ticker);
			pOS.flush();
		} catch(UnsupportedEncodingException e) { 
			e.printStackTrace();
		} finally { 
			pOS.close();			
			/*
			 * Αν η αποδέσμευση του PrintSteram πέτυχε, εγγραφή των περιεχομένων του
			 *  στο GoogleDrive (commit) αλλιώς όχι (discard)..
			 */
			if(!pOS.checkError())			
				fileCommited = fileIO.commit(gClient, null)
					.await(TIMEOUT_SEC, TimeUnit.SECONDS)
					.getStatus().isSuccess();
			else {
				fileIO.discard(gClient);
				fileCommited = false;
			}
		}
		
		return fileCommited;
	}
	
	/***************************************************************************
	 * Ασύγχρονη αντιγραφή ειδοποίησης (notification) σε φάκελο (folderTitle)  *
	 *  του GoogleDrive και με συγκεκριμένη μορφή για τον τίτλου του αρχείου.  *
	 *                                                                         *
	 * Η διαδικασία δημιουργεί ένα νέο AsyncTask το οποίο αναλαμβάνει την      *
	 *  εκτέλεση της syncCopyNotificationToGD και την επιστροφή του            *
	 * αποτελέσματος εκτέλεσης της στο πρόγραμμα μέσω ενός onCopyNotification- *
	 *  ToGD listener                                                          *
	 ***************************************************************************/
	public static void CopyNotificationToGD(final String folderTitle,
			final int fileTitle,
			final NotificationItem notification, 
			final CopyNotificationToGDListener listener) { 
		new AsyncTask<NotificationItem, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(NotificationItem... args) {
				try { 
					return syncCopyNotificationToGD(folderTitle, fileTitle, args[0]);
				} catch(Exception e) { 
					e.printStackTrace();
					return false;
				}
			}
			@Override
			protected void onPostExecute(Boolean r) { 
				Log.i(LOG_TAG, " asyncCopyNotificationToGD = " + (r ? "SUCCEEDED": "FAILED"));
				
				if(listener != null)
					listener.onCopyNotificationGD(folderTitle, 
							fileTitle == GoogleDrvDialog.FILE_TITLE_NOTIF_TITLE ? 
									notification.title: notification.app, r);
			}
		}.execute(notification);
	}
	public interface CopyNotificationToGDListener { 
		public void onCopyNotificationGD(final String folderTitle, 
				final String fileTitle, 
				final boolean r);
	}
}