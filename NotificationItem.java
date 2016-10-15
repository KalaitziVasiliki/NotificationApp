/******************************************************************************* 
 * ΚΛΑΣΗ ΠΟΥ ΥΛΟΠΟΙΕΙ ΤΙΣ ΚΑΤΑΧΩΡΗΣΕΙΣ ΤΟΥ ΙΣΤΟΡΙΚΟΥ ΤΟΥ ΠΡΟΓΡΑΜΜΑΤΟΣ          *
 *******************************************************************************/ 
package com.example.notification;

import java.text.DateFormat;
import java.util.Date;

import android.app.Notification;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Pair;

public class NotificationItem {
	// Το πρωτεύον κλειδί δεν υπάρχει 
	public static final int UNDEFINED = -1;	
	// Το πρωτεύον κλειδί της ειδοποίησης στην ΒΔ μας 
	final public int pk;
	// Το παραπάνω κλειδί σε αλφαριθμητική τιμή (για άμεση χρήση σε SQLite Queries)
	final public String pkStr,
	// Η εφαρμογή που ανάρτησε την ειδοποίηση
						app,
	// Ο τίτλος της ειδοποίησης
						title,
	// Ο επεκταμένος τίτλος της ειδοποίησης
						titleBig,
	// Το περιεχόμενο της ειδοποίησης
						message,
	// Το βοηθητικό περιεχόμενο της ειδοποίησης για ΑΜΕΑ 
						ticker;
	// Η ημερομηνία & ώρα που ελήφθη η ειδοποίηση από την υπηρεσία NotificationMonitor
	final public long clock;
	
	// Αν true το φίλτρο είναι μαρκαρισμένο
	public boolean mark = false;
	
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  Εδώ ορίζουμε το πρωτεύον κλειδί της ειδοποίησης (pk) στην ΒΔ μας,      *
	 *   την εφαρμογή που ανάρτησε την ειδοποίηση (app), το περιεχόμενο της    *
	 *  ειδοποίησης (message) και ημερομηνία & ώρα που ελήφθη από το πρόγραμμα.*
	 ***************************************************************************/
	public NotificationItem(final int pk, 
			final String app,
			final String title,
			final String titleBig,
			final String message,
			final String ticker,
			final long clock) { 
		this.pk = pk;
		this.pkStr = String.valueOf(pk);
		this.app = app;
		this.title = title;
		this.titleBig = titleBig;
		this.message = message;
		this.ticker = ticker;
		this.clock = clock;
	}
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  Όπως ο παραπάνω ctor με την διαφορά ότι ως πρωτεύον κλειδί (pk)        *
	 *   ορίζεται η τιμή UNDEFINED.                                            *
	 ***************************************************************************/
	public NotificationItem(final String app,
			final String title,
			final String titleBig,
			final String message,
			final String ticker,
			final long clock) { 
		this(UNDEFINED, app, title, titleBig, message, ticker, clock);
	}
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  Οι τιμές των μεταβλητών του NotificationItem ορίζονται από το          *
	 *   περιεχόμενο της κλάσης εισόδου "StatusBarNotification"                *
	 ***************************************************************************/
	public NotificationItem(final StatusBarNotification sbn) { 
		final Notification notif = sbn.getNotification();
		final Bundle extras = sbn.getNotification().extras;
		
		pk       = UNDEFINED;
		pkStr	 = String.valueOf(pk);
		app      = sbn.getPackageName();
		title    = extras.getString(Notification.EXTRA_TITLE, "");
		titleBig = extras.getString(Notification.EXTRA_TITLE_BIG, "");
		message  = extras.getString(Notification.EXTRA_TEXT, "");
		ticker   = notif.tickerText != null ? notif.tickerText.toString(): "";
		clock    = sbn.getPostTime();
	}

	/***************************************************************************
	 * Επιστροφή του τίτλου της ειδοποίησης.                                   *
	 *                                                                         *
	 * Αν η ειδοποίηση έχει επεκταμένο αλλά και σύντομο τίτλο, τότε η συνάρτηση*
	 *  επιστρέφει τον επεκταμένο τίτλο.                                       *
	 ***************************************************************************/
	public String getTitle() { 
		return !titleBig.isEmpty() ? titleBig: title; 
	}
	/***************************************************************************
	 * Επιστροφή της ονομασίας και του εικονιδίου της εφαρμογής που ανάρτησε   *
	 *  την ειδοποίηση.                                                        *
	 *                                                                         *
	 * Αν το OS δεν επιστρέψει πληροφορίες ή ονομασία εφαρμογής, τότε ως       *
	 *  ονομασία η συνάρτηση επιστρέφει την εσωτερική ονομασία του πακέτου στο *
	 * οποίο ανήκει η ειδοποίηση (πχ. com.foo.foo_application αντί Foo-App)    *
	 *                                                                         *
	 * Το ίδιο ισχύει και για το εικονίδιο, αν δεν υπάρχει επιστρέφεται null   *
	 ***************************************************************************/
	public Pair<String, Drawable> getAppTitleIcon(final PackageManager packMan) { 
		try { 
			final String sAppLabel = packMan.getApplicationLabel(
					packMan.getApplicationInfo(app, 0))
					.toString();
			
			return Pair.create(sAppLabel != null ? sAppLabel: app, 
					packMan.getApplicationIcon(app));
		} catch(PackageManager.NameNotFoundException e) { 
			e.printStackTrace();			
		}
		return Pair.create(app, null);
	}
	
	/***************************************************************************
	 * Επιστροφή της ημερομηνίας & ώρας ανάρτησης της ειδοποίησης σε           * 
	 *  αλφαριθμητική μορφή.                                                   *
	 ***************************************************************************/
	public String getClockStr() { 
		return DateFormat.getDateTimeInstance().format(new Date(clock));
	}
}
