/*******************************************************************************
 * ΕΝΑ Toast ΠΟΥ ΠΑΡΟΥΣΙΑΖΕΙ ΤΑ ΠΕΡΙΕΧΟΜΕΝΑ ΤΟΥ ΚΕΝΤΡΑΡΙΣΜΕΝΑ                  *
 *******************************************************************************/
package com.example.notification;

import android.content.Context;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.widget.Toast;

public class CenterToast {
	// ctor προβολής Toast από τα string resources του προγράμματος
	public static Toast makeText(final Context context, final int msgId, final int duration) { 
		return makeText(context, context.getText(msgId), duration);
	}
	// ctor προβολής Toast από CharSequence 
	public static Toast makeText(final Context context, final CharSequence msg, final int duration) { 
		return Toast.makeText(context, cntrStr(msg), duration);
	}
	
	/***************************************************************************
	 * Επιστροφή ενός CharSequence (cStr) ως κεντραρισμένου SpannableString    *
	 ***************************************************************************/
	private static SpannableString cntrStr(final CharSequence cStr) { 		
		SpannableString spStr = new SpannableString(cStr);
		spStr.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 
				0, cStr.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spStr;
	}
}
