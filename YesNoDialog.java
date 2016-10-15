/*******************************************************************************
 * ΔΙΑΛΟΓΟΣ ΑΠΑΝΤΗΣΗΣ ΧΡΗΣΤΗ ΣΕ ΕΡΩΤΗΜΑ ΤΟΥ ΠΡΟΓΡΑΜΜΑΤΟΣ                       *
 *******************************************************************************/
package com.example.notification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Layout.Alignment;
import android.text.style.AlignmentSpan;

public class YesNoDialog implements DialogInterface.OnClickListener, 
	OnCancelListener{
	// Η βάση του διαλόγου
	protected AlertDialog dialog;
	// Η ταυτότητα του διαλόγου
	protected int id;
	// Ο Listener του διαλόγου
	protected YesNoListener listener;
	
	/*******************************************************************************
	 * ctor                                                                        *
	 *                                                                             *
	 *  Εδώ ορίζουμε το Context του προγράμματος, την ταυτότητα του διαλόγου (id), *
	 *   τον τίτλο του (csTitle), το ερώτημα / μήνυμα (csBody) προς τον χρήστη και *
	 *  το YesNoListener στο οποίο θα αποσταλεί η απόκριση του χρήστη στον διάλογο.*
	 *******************************************************************************/
	public YesNoDialog(final Context context,
			final int id,
			final CharSequence csTitle, 
			final CharSequence csBody,
			final YesNoListener listener) {
		this.id = id;
		this.listener = listener;
		
		// Κατασκευή ενός νέου AlertDialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(csTitle);
		builder.setPositiveButton(R.string.yes, this);
		builder.setNegativeButton(R.string.no, this);
		builder.setOnCancelListener(this);
		// Το μήνυμα εμφανίζεται στο κέντρο του διαλόγου
		final SpannableString spBody = new SpannableString(csBody);
		spBody.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 
				0, csBody.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);		
		builder.setMessage(spBody);
		
		/*
		 * Κατασκευή του διαλόγου και ρύθμιση του ώστε να μην κλείνει αν ο χρήστης
		 *  πατήσει εκτός του πλαισίου του
		 */
		dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
	}
	
	/***************************************************************************
	 * Στατική διαδικασία παρουσίασης ενός διαλόγου.                           *
	 *                                                                         *
	 * βλ. ctor                                                                *
	 ***************************************************************************/
	public static void show(final Context context, 
			final int id, 
			final CharSequence csTitle,
			final CharSequence csBody,
			final YesNoListener listener) { 
		new YesNoDialog(context, id, csTitle, csBody, listener).show();
	}
	/***************************************************************************
	 * Στατική διαδικασία παρουσίασης ενός διαλόγου με τον τίτλο και το        *
	 *  περιεχόμενο του να ορίζονται από τα string resources του προγράμματος  *
	 *                                                                         *
	 * βλ. ctor                                                                *
	 ***************************************************************************/
	public static void show(final Context context,
			final int id,
			final int titleId,
			final int bodyId,
			final YesNoListener listener) { 
		show(context, id, context.getText(titleId), context.getText(bodyId), listener);
	}

	/***************************************************************************
	 * Παρουσίαση διαλόγου στην οθόνη                                          *
	 ***************************************************************************/
	public void show() { 
		dialog.show();
	}
	
	/***************************************************************************
	 * DialogInterface.OnClickListener   Διαχείριση των πλήκτρων του διαλόγου  *
	 ***************************************************************************/	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(listener != null)
			listener.OnYesNoListener(id, which);
	}
	
	/***************************************************************************
	 * OnCancelListener                  Διαχείριση της ακύρωσης του διαλόγου  *
	 ***************************************************************************/	
	@Override
	public void onCancel(DialogInterface dialog) {
		if(listener != null)
			listener.OnYesNoListener(id, DialogInterface.BUTTON_NEGATIVE);
	}		
	
	/***************************************************************************
	 * YesNoListener                    Το YesNoListener Interface             *
	 ***************************************************************************/
	public interface YesNoListener { 
		public void OnYesNoListener(final int id, final int which);
	}

}
