/*******************************************************************************
 * ΔΙΑΧΕΙΡΙΣΗ ΤΗΣ ΒΔ ΤΩΝ ΦΙΛΤΡΩΝ ΤΟΥ ΠΡΟΓΡΑΜΜΑΤΟΣ                              *
 *******************************************************************************/
package com.example.notification;

import java.io.File;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbFilterManager extends SQLiteOpenHelper {
	// Η ονομασία του αρχείο της ΒΔ του προγράμματος 
	public static final String DB_FILENAME = "app.db";
	// Η ονομασία του πίνακα αποθήκευσης των φίλτρων
	private static final String DB_TABLE_FILTER = "FLTR",
	// Η ονομασία του πίνακα αποθήκευσης των Notification
								DB_TABLE_HISTORY= "HIST";
	// Η έκδοση της ΒΔ (χρησιμοποιείται από το SQLiteOpenHelper)
	private static final int DB_VERSION = 1;
	
	// Στατική αναφορά στην μηχανή SQLite που διαχειρίζεται την ΒΔ του προγράμματος 
	private static SQLiteDatabase sqlDb = null;
	/*****************************************************************************
	 * Στατική αναφορά (Singleton) στην παρούσα κλάση, ώστε όλες οι αναφορές του *
	 *  του προγράμματος στο DbFilterManager να οδηγούν σε ένα κοινό αντικείμενο.*
	 *                                                                           *
	 * Η SQLiteDatabase με βάση την επίσημη τεκμηρίωση ορίζεται ως thread-safe.  *
	 *****************************************************************************/
	private static DbFilterManager self = null;
	
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  Εδώ ορίζουμε το Context του προγράμματος και την ονομασία της ΒΔ που   *
	 *   θα διαχειριστεί η κλάση DbFilterManager.                              *
	 ***************************************************************************/
	public DbFilterManager(Context context, String fileName) throws SQLiteException {
		super(context, fileName, null, DB_VERSION);
		
		sqlDb = getWritableDatabase();
	}

	/****************************************************************************
	 * Στατική συνάρτηση επιστροφής αναφοράς σε μια ΒΔ στο κατάλογο εγκατάστασης*
	 *  του προγράμματος.                                                       *
	 *                                                                          *
	 * βλ. ctor                                                                 *
	 ****************************************************************************/
	public static DbFilterManager getInstance(
			final Context context, 
			final String fileName) {
		if(self == null)		
			return self = new DbFilterManager(context, 
					new File(context.getFilesDir(), fileName).getAbsolutePath());
		else
			return self;
	}
	/***************************************************************************
	 * Στατική συνάρτηση επιστροφής αναφοράς σε μια νέας ΒΔ στο κατάλογο       *
	 *  εγκατάστασης του προγράμματος δίχως χειροκίνητο ορισμό ονομασίας ΒΔ    *
	 *                                                                         *
	 * βλ. getInstance(context, fileName) και ctor                             *
	 ***************************************************************************/
	public static DbFilterManager getInstance(final Context context) { 
		return getInstance(context, DB_FILENAME);
	}
	
	/***************************************************************************
	 * Προσθήκη νέου φίλτρου (filter) στην ΒΔ.                                 *
	 *                                                                         *
	 * Σε περίπτωση επιτυχίας η συνάρτηση επιστρέφει true αλλιώς false         *
	 ***************************************************************************/
	public boolean insertFilter(final FilterItem filter) {
		final ContentValues values = new ContentValues();
		values.put("FTOKEN", filter.token);
		values.put("TOKENPOS", filter.matchingType);
		values.put("PAYLOAD" , filter.payload);
		values.put("ACTIVE"  , filter.active);
		
		return sqlDb.insert(DB_TABLE_FILTER, null, values) != -1;
	}
	/***************************************************************************
	 * Αντικατάσταση ήδη υπάρχοντος φίλτρου (oldFilter) με ένα νέο φίλτρο      * 
	 *  (newFilter).                                                           *
	 *                                                                         *
	 * Σε περίπτωση επιτυχίας η συνάρτηση επιστρέφει true αλλιώς false         *
	 ***************************************************************************/
	public boolean replaceFilter(final FilterItem oldFilter, 
			final FilterItem newFilter) {
		final boolean dbg = deleteFilter(oldFilter);
		Log.i("PRG", " REPLACE-FOUND OLD = " + dbg);
		
		return dbg ? insertFilter(newFilter): false;		
	}	
	/***************************************************************************
	 * Ενεργοποίηση ή απενεργοποίηση (enable) του φίλτρου filter της ΒΔ        *
	 *                                                                         *
	 * Σε περίπτωση επιτυχίας η συνάρτηση επιστρέφει true αλλιώς false         *
	 ***************************************************************************/
	public boolean setFilterActive(final FilterItem filter, final boolean enable) { 	
		if(findFilterByToken(filter.token)) {
			final ContentValues values = new ContentValues();
			values.put("ACTIVE", enable);
			
			return sqlDb.update(DB_TABLE_FILTER, 
					values, 
					"FTOKEN=?", 
					new String[] { filter.token }) > 0;
		}
		return false;
	}
	/***************************************************************************
	 * Έλεγχος ύπαρξης φίλτρου με κείμενο (token) ισότιμο του "token"          *
	 *                                                                         *
	 * Σε περίπτωση που υπάρχει καταχωρημένο φίλτρο με κείμενο "token"         *
	 *  (ανεξαρτήτως πεζών ή κεφαλαίων γραμμάτων) η συνάρτηση επιστρέφει       *
	 * true αλλιώς false                                                       *
	 ***************************************************************************/
	public boolean findFilterByToken(final String token) {
		Cursor cr = null;
		
		try { 
			cr = sqlDb.query(DB_TABLE_FILTER, 
					new String[] { "FTOKEN" } , 
					null, 
					null, 
					null, 
					null, 
					null);
			
			while(cr.moveToNext()) { 
				if(token.equalsIgnoreCase(cr.getString(0)))
					return true;
			}			
		} catch(SQLException e) { 
			e.printStackTrace();
			return false;
		} finally { 
			if(cr != null)
				cr.close();
		}
		
		return false;
	}	
	
	/***************************************************************************
	 * Διαγραφή φίλτρου (filter) από την ΒΔ                                    *
	 *                                                                         *
	 * Σε περίπτωση επιτυχίας η συνάρτηση επιστρέφει true αλλιώς false         *
	 ***************************************************************************/
	public boolean deleteFilter(final FilterItem filter) { 
		return sqlDb.delete(DB_TABLE_FILTER, "FTOKEN=?", new String[] { filter.token }) > 0; 
	}
	/***************************************************************************
	 * Επιστροφή όλων των φίλτρων της ΒΔ                                       *
	 *                                                                         *
	 * Η συνάρτηση επιστρέφει ένα ArrayList<FilterItem> με όλα τα φίλτρα ή αν  *
	 *  δεν υπάρχει κανένα, το ArrayList επιστρέφεται κενό                     *
	 ***************************************************************************/
	public ArrayList<FilterItem> listFilters(final Context context) { 
		ArrayList<FilterItem> fList = new ArrayList<FilterItem>();
		
		Cursor cr = null;
		
		try { 
			cr = sqlDb.query(DB_TABLE_FILTER, null, null, null, null, null, "rowid DESC");						
			
			while(cr.moveToNext()) { 
				FilterItem filter = new FilterItem(cr.getString(0),
						(byte)cr.getInt(1),
						cr.getInt(2));
				filter.active = cr.getInt(3) > 0 ? true: false;
				fList.add(filter);
			}			
		} finally { 
			if(cr != null)
				cr.close();
		}
		
		return fList;
	}
		
	
	/***************************************************************************
	 * Προσθήκη ειδοποίησης (notification) στην ΒΔ.                            *
	 *                                                                         *
	 * Σε περίπτωση επιτυχίας η συνάρτηση επιστρέφει true αλλιώς false         * 
	 ***************************************************************************/
	public boolean insertNotification(final NotificationItem notification) { 
		final ContentValues values = new ContentValues();
		/* 
		 * PK είναι πρωτεύον κλειδί κατηγορίας AUTOINCREMENT άρα 
		 *  αποδίδεται αυτόματα από την ΒΔ 
		 */ 
		values.put("APP"     , notification.app);
		values.put("TITLE"   , notification.title);
		values.put("TITLEBIG", notification.titleBig);
		values.put("MSG"     , notification.message);
		values.put("TICKER"  , notification.ticker);
		values.put("MCLOCK"  , notification.clock);
		
		return sqlDb.insert(DB_TABLE_HISTORY, null, values) != -1;		
	}
	/***************************************************************************
	 * Διαγραφή ειδοποίησης (notification) από την ΒΔ.                         *
	 *                                                                         *
	 * Σε περίπτωση επιτυχίας η συνάρτηση επιστρέφει true αλλιώς false         *
	 ***************************************************************************/	
	public boolean deleteNotification(final NotificationItem notification) { 
		return sqlDb.delete(DB_TABLE_HISTORY, 
				"PK=?", 
				new String[] { notification.pkStr }) > 0;
	}
	/***************************************************************************
	 * Επιστροφή όλων των ειδοποιήσεων της ΒΔ                                  *
	 *                                                                         *
	 * Η συνάρτηση επιστρέφει ένα ArrayList<NotificationItem> με όλες τις      * 
	 *  ειδοποιήσεις ή αν δεν υπάρχει καμιά το ArrayList επιστρέφεται κενό     *
	 ***************************************************************************/
	public ArrayList<NotificationItem> listNotifications(final Context context) { 
		ArrayList<NotificationItem> nList = new ArrayList<NotificationItem>();
		
		Cursor cr = null;
		
		try { 
			cr = sqlDb.query(DB_TABLE_HISTORY, null, null, null, null, null, "MCLOCK DESC");						
			
			while(cr.moveToNext()) {
				nList.add(new NotificationItem(cr.getInt(0),	/* 0-PK      */ 						
						cr.getString(1),						/* 1-APP     */
						cr.getString(2),						/* 2-TITLE   */
						cr.getString(3),						/* 3-TITLEBIG*/
						cr.getString(4),						/* 4-MSG     */
						cr.getString(5),						/* 5-TICKER  */
						cr.getLong(6)));						/* 3-MCLOCK  */
			}			
		} finally { 
			if(cr != null)
				cr.close();
		}
		
		return nList;
	}	
	
	
	/***************************************************************************
	 * Κατασκευή μιας νέας, κενής ΒΔ για την αποθήκευση των φίλτρων και των    * 
	 *  ειδοποιήσεων του προγράμματος (βλ. SQLiteOpenHelper)                   *
	 ***************************************************************************/
	@Override
	public void onCreate(SQLiteDatabase db) throws SQLException {			
		// Πίνακας φίλτρων [FLTR]
		db.execSQL("CREATE TABLE FLTR(" + 
				"FTOKEN CHAR(20) PRIMARY KEY NOT NULL COLLATE NOCASE," + /* 0 */ 
				"TOKENPOS INT NOT NULL,"+								 /* 1 */
				"PAYLOAD INT NOT NULL," +								 /* 2 */
				"ACTIVE INT NOT NULL)");								 /* 3 */
		
		// Πίνακας ειδοποιήσεων [HIST]
		db.execSQL("CREATE TABLE HIST("   + 
				"PK INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +	/* 0 */
				"APP TEXT NOT NULL,"      +							/* 1 */
				"TITLE TEXT NOT NULL,"    + 						/* 2 */
				"TITLEBIG TEXT NOT NULL," +							/* 3 */
				"MSG TEXT NOT NULL,"      + 						/* 4 */
				"TICKER TEXT NOT NULL,"   +							/* 5 */
				"MCLOCK NUMERIC NOT NULL)");						/* 6 */
	}

	/***************************************************************************
	 * Καλείται από την SQLiteOpenHelper όταν η ΒΔ πρέπει να αναβαθμιστεί      *
	 ***************************************************************************/
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Δεν χρησιμοποιείται από την κλάση
	}	
}
