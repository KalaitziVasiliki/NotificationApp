/*******************************************************************************
 * �������� ��������� ������ �� ������� ��� ������������                       *
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
	// � ���� ��� ��������
	protected AlertDialog dialog;
	// � ��������� ��� ��������
	protected int id;
	// � Listener ��� ��������
	protected YesNoListener listener;
	
	/*******************************************************************************
	 * ctor                                                                        *
	 *                                                                             *
	 *  ��� �������� �� Context ��� ������������, ��� ��������� ��� �������� (id), *
	 *   ��� ����� ��� (csTitle), �� ������� / ������ (csBody) ���� ��� ������ ��� *
	 *  �� YesNoListener ��� ����� �� ��������� � �������� ��� ������ ���� �������.*
	 *******************************************************************************/
	public YesNoDialog(final Context context,
			final int id,
			final CharSequence csTitle, 
			final CharSequence csBody,
			final YesNoListener listener) {
		this.id = id;
		this.listener = listener;
		
		// ��������� ���� ���� AlertDialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(csTitle);
		builder.setPositiveButton(R.string.yes, this);
		builder.setNegativeButton(R.string.no, this);
		builder.setOnCancelListener(this);
		// �� ������ ����������� ��� ������ ��� ��������
		final SpannableString spBody = new SpannableString(csBody);
		spBody.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 
				0, csBody.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);		
		builder.setMessage(spBody);
		
		/*
		 * ��������� ��� �������� ��� ������� ��� ���� �� ��� ������� �� � �������
		 *  ������� ����� ��� �������� ���
		 */
		dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
	}
	
	/***************************************************************************
	 * ������� ���������� ����������� ���� ��������.                           *
	 *                                                                         *
	 * ��. ctor                                                                *
	 ***************************************************************************/
	public static void show(final Context context, 
			final int id, 
			final CharSequence csTitle,
			final CharSequence csBody,
			final YesNoListener listener) { 
		new YesNoDialog(context, id, csTitle, csBody, listener).show();
	}
	/***************************************************************************
	 * ������� ���������� ����������� ���� �������� �� ��� ����� ��� ��        *
	 *  ����������� ��� �� ��������� ��� �� string resources ��� ������������  *
	 *                                                                         *
	 * ��. ctor                                                                *
	 ***************************************************************************/
	public static void show(final Context context,
			final int id,
			final int titleId,
			final int bodyId,
			final YesNoListener listener) { 
		show(context, id, context.getText(titleId), context.getText(bodyId), listener);
	}

	/***************************************************************************
	 * ���������� �������� ���� �����                                          *
	 ***************************************************************************/
	public void show() { 
		dialog.show();
	}
	
	/***************************************************************************
	 * DialogInterface.OnClickListener   ���������� ��� �������� ��� ��������  *
	 ***************************************************************************/	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(listener != null)
			listener.OnYesNoListener(id, which);
	}
	
	/***************************************************************************
	 * OnCancelListener                  ���������� ��� �������� ��� ��������  *
	 ***************************************************************************/	
	@Override
	public void onCancel(DialogInterface dialog) {
		if(listener != null)
			listener.OnYesNoListener(id, DialogInterface.BUTTON_NEGATIVE);
	}		
	
	/***************************************************************************
	 * YesNoListener                    �� YesNoListener Interface             *
	 ***************************************************************************/
	public interface YesNoListener { 
		public void OnYesNoListener(final int id, final int which);
	}

}
