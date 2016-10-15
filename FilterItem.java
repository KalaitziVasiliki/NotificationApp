/******************************************************************************* 
 * ΚΛΑΣΗ ΠΟΥ ΥΛΟΠΟΙΕΙ ΤΑ ΦΙΛΤΡΑ ΤΟΥ ΠΡΟΓΡΑΜΜΑΤΟΣ                               *
 *******************************************************************************/
package com.example.notification;

import android.content.Context;

public class FilterItem {
							 // Μη ορισμένη τιμή
	public static final byte UNDEFINED = 0,
							 
							 // Ταίριασμα κειμένου στην έναρξη της ειδοποίησης
							 MATCHING_TYPE_START = 1,
							 // Ταίριασμα κειμένου οπουδήποτε στην ειδοποίησης
							 MATCHING_TYPE_END   = 2,
							 // Ταίριασμα κειμένου στην λήξη της ειδοποίησης
							 MATCHING_TYPE_ANY   = 3;
		
							 // Αφαίρεση ειδοποίησης	   ** bitwise **
	public static final int  PAYLOAD_REMOVE      		   = 0x01,
							 // Αντιγραφή ειδοποίησης στην τοπική ΒΔ του προγράμματος
							 PAYLOAD_COPY_TO_DB	 		   = 0x02,
							 // Αντιγραφή ειδοποίησης στο Google Drive όταν υπάρχει σύνδεση WiFi
							 PAYLOAD_COPY_TO_GD_OVER_WIFI  = 0x04,
							 // Αντιγραφή ειδοποίησης στο Google Drive όταν υπάρχει σύνδεση GSM/3G
							 PAYLOAD_COPY_TO_GD_OVER_GSM   = 0x08,
							 // Αντιγραφή ειδοποίησης στο Google Drive όταν υπάρχει σύνδεση WiFi ή GSM/3G
							 PAYLOAD_COPY_TO_GD            = 0x10;							 
	
	// Το προς αναζήτηση κείμενο
	public String token;					 	
	// Την θέση εύρεσης του κειμένου στο περιεχόμενο της ειδοποίησης 
	public byte matchingType;
	// Το αποτέλεσμα όταν βρεθεί το κείμενο
	public int payload;
	
	// Αν true το φίλτρο είναι ενεργό και πρέπει να εφαρμοστεί από το NotificationMonitor
	public boolean active,
	// Αν true το φίλτρο είναι μαρκαρισμένο
				   mark;
	
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 * Εδώ ορίζουμε το Context του προγράμματος, το προς εύρεση κειμένο στην   *
	 *  ειδοποίηση, την θέση εύρεσης του κειμένου στην ειδοποίηση(matchingType)*
	 * και το αποτέλεσμα όταν βρεθεί το κείμενο                                *
	 ***************************************************************************/
	public FilterItem(final String token, 
			final byte matchingType, 
			final int payload) {		
		this.token = token;
		this.matchingType = matchingType;
		this.payload = payload;
	}
	// ctor όπου ορίζεται μόνο το προς εύρεση κείμενο
	public FilterItem(final String token) { 
		this(token, UNDEFINED, UNDEFINED);
	}
	// ctor κενού FilterItem
	public FilterItem() { 
		this("", UNDEFINED, UNDEFINED);
	}
	
	/***************************************************************************
	 * Επιστροφή σε αλφαριθμητική μορφή του ορισμένου matchingType             *
	 ***************************************************************************/
	public CharSequence getMatchingTypeStr(final Context context) {
		switch(matchingType) { 
		case MATCHING_TYPE_START:
			return context.getText(R.string.mt_start);
		case MATCHING_TYPE_END:
			return context.getText(R.string.mt_end);
		case MATCHING_TYPE_ANY:
			return context.getText(R.string.mt_any);
		default:
			return context.getText(R.string.unknown);
		}
	}
}
