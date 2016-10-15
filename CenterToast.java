/*******************************************************************************
 * ��� Toast ��� ����������� �� ����������� ��� �������������                  *
 *******************************************************************************/
package com.example.notification;

import android.content.Context;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.widget.Toast;

public class CenterToast {
	// ctor �������� Toast ��� �� string resources ��� ������������
	public static Toast makeText(final Context context, final int msgId, final int duration) { 
		return makeText(context, context.getText(msgId), duration);
	}
	// ctor �������� Toast ��� CharSequence 
	public static Toast makeText(final Context context, final CharSequence msg, final int duration) { 
		return Toast.makeText(context, cntrStr(msg), duration);
	}
	
	/***************************************************************************
	 * ��������� ���� CharSequence (cStr) �� �������������� SpannableString    *
	 ***************************************************************************/
	private static SpannableString cntrStr(final CharSequence cStr) { 		
		SpannableString spStr = new SpannableString(cStr);
		spStr.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 
				0, cStr.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spStr;
	}
}
