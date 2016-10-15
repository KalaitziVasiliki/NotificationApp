/*******************************************************************************
 * �������� ��������� ��������� GOOGLE DRIVE                                   *
 *******************************************************************************/
package com.example.notification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class GoogleDrvDialog implements 
	DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
	DialogInterface.OnShowListener, android.widget.RadioGroup.OnCheckedChangeListener,
	TextWatcher {
	/*
	 * �� ����� ��������� ��� ������� �����������:
	 * 	FILE_TITLE_NOTIF_TITLE = �� ������ �������� �� �������� ��� ������� ����� 
	 * 							 ��� �����������
	 *  FILE_TITLE_APP_PACKAGE = �� ������ �������� �� �������� �� ������ ���
	 *  						 ��������� ��� �������� ��� ����������
	 *  FILE_TITLE_UNKNOWN     = ������������ ���� ���� � ������� �������� ���
	 *  						 �������
	 */
	public static final int FILE_TITLE_NOTIF_TITLE = 1,
							FILE_TITLE_APP_PACKAGE = 2,
							FILE_TITLE_UNKNOWN     = 0;
	
	// � ���� ��� ��������
	protected AlertDialog dialog;
	// � Listener ��� ��������	
	protected GoogleDrvDialogListener listener;	
	// �� UI (layout) ��� ��������
	protected View view;
	// �� control ��������� ��� ������� ����������� ��� ������������ ��� GoogleDrive
	protected EditText edFolderTitle;
	// �� controls ������� ����� ��������� ��� ������� 
	protected RadioButton rbFnTitle,
						  rbFnAppPackage;
	// �� group �� �� ��� �������� RadioButtons
	protected RadioGroup rgFn;
	
	// � ��� �������� �������� ��� ������� ����������� ������������
	protected String folderTitle;
	// � ��� ��������� ����� ��������� �������
	protected int fileTitle;
	// � ��������� �������� ��� ������ ��� Google Drive Service
	protected boolean gdConnected;

	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  ��� �������� �� Context ��� ������������, ��� �������� ��� ������� ��� *
	 *   �� ������������� �� ������������ (folderTitle), ��� ����� ��� ������  *
	 *  ���� (fileTitle) ��� �� GoogleDrvDialogListener ��� ����� �� ��������� *
	 *   � �������� ��� ������ ���� �������.                                   *
	 ***************************************************************************/
	public GoogleDrvDialog(final Context context,
			final String folderTitle,
			final int fileTitle,
			final GoogleDrvDialogListener listener) {
		this.listener    = listener;
		this.folderTitle = folderTitle;
		this.fileTitle   = fileTitle;
		// ��������� ���� ���� AlertDialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setIcon(R.drawable.ic_action_gd);
		builder.setTitle(R.string.button_gd);
		builder.setView(view = View.inflate(context, R.layout.dialog_gd_settings, null));
		builder.setPositiveButton(R.string.button_save, this);
		builder.setNegativeButton(R.string.button_cancel, this);
		builder.setOnCancelListener(this);
		
		/* 
		 * � ������� ��� ��������� (�������) �������� ��� �������� ��������� ���
		 *  ��� �� ��� � ������� ���� �������� �������� ������ ������ ��� ��������
		 * �� ���������� ��� Google Drive, ����� �� ���� ��� ��������� ������� ��
		 *  "�������" ������ �� "����������".     
		 */
		builder.setNeutralButton((gdConnected = !GoogleDrv.isUserAuthorized())
				? R.string.button_gd_connect: R.string.button_gd_disconnect, 
						this);
		
		/*
		 * ��������� ��� �������� ��� ������� ��� ���� �� ��� ������� �� � �������
		 *  ������� ����� ��� �������� ���
		 */
		dialog = builder.create();
		dialog.setOnShowListener(this);
		dialog.setCanceledOnTouchOutside(false);		
	}
	
	/***************************************************************************
	 * ��������� & ���������� �������� ���� �����                              *
	 ***************************************************************************/
	public static GoogleDrvDialog show(final Context context,
			final String folderTitle,
			final int fileTitle,
			final GoogleDrvDialogListener listener) { 
		return new GoogleDrvDialog(context, folderTitle, fileTitle, listener).show();
	}
	
	/***************************************************************************
	 * ���������� �������� ���� �����                                          *
	 *                                                                         *
	 * � ��������� ���������� �� GoogleDrvDialog ��� �����������               *
	 ***************************************************************************/
	public GoogleDrvDialog show() { 
		dialog.show();		
		return this;
	}
		
	/***************************************************************************
	 * DialogInterface.ShowListener      ���������� ��������, ������������ UI  *
	 ***************************************************************************/
	@Override
	public void onShow(DialogInterface dialog) {
		edFolderTitle  = (EditText)view.findViewById(R.id.editTextGdFolder);
		edFolderTitle.setText(folderTitle);
		edFolderTitle.setSelection(edFolderTitle.length());
		edFolderTitle.addTextChangedListener(this);		
		
		rgFn           = (RadioGroup)view.findViewById(R.id.radioGroupGdFn);
		rbFnTitle      = (RadioButton)view.findViewById(R.id.radioGdFnTitle);		
		rbFnAppPackage = (RadioButton)view.findViewById(R.id.radioGdFnAppPackage);
		
		if(fileTitle == FILE_TITLE_NOTIF_TITLE) 
			rbFnTitle.setChecked(true);
		else
			rbFnAppPackage.setChecked(true);
		
		rgFn.setOnCheckedChangeListener(this);
		
		this.dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);		
	}
	
	/***************************************************************************
	 * DialogInterface.OnClickListener   ���������� ��� �������� ��� ��������  *
	 ***************************************************************************/	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(listener != null) {
			if(which != DialogInterface.BUTTON_NEUTRAL) {			
				listener.OnGoogleDrvSettings(edFolderTitle.getText().toString(),
						rgFn.getCheckedRadioButtonId() == R.id.radioGdFnTitle ? 
								FILE_TITLE_NOTIF_TITLE: FILE_TITLE_APP_PACKAGE,
								which);
			}
			else { 
				/*
				 * �� � ������� ����� ������������ ��� GoogleDrive ���� �� ������
				 *  ��� �������� �������� "����������" (->connect = false) ������  
				 * "�������" (->connect = true). 
				 */
				listener.onGoogleDrvAccountSetting(!gdConnected);
			}
		}
	}
	
	/***************************************************************************
	 * OnCancelListener                  ���������� ��� �������� ��� ��������  *
	 ***************************************************************************/	
	@Override
	public void onCancel(DialogInterface dialog) {
		if(listener != null)
			listener.OnGoogleDrvSettings(null, FILE_TITLE_UNKNOWN, DialogInterface.BUTTON_NEGATIVE);
	}

	/***************************************************************************
	 * TextWatcher                       ������������� ������� ������ �������  *
	 ***************************************************************************/
	@Override
	public void afterTextChanged(Editable s) {
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(s.length() > 0);		
	}
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// ��� ���������������..		
	}
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// ��� ���������������..		
	}
	
	/***************************************************************************
	 * RadioGroupCheckedChanged			������������� ��� �������� ��� RG      *
	 ***************************************************************************/
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(edFolderTitle.length() > 0);
	}	

	/***************************************************************************
	 * YesNoListener                    �� GoogleDrvDialogListener Interface   *
	 ***************************************************************************/	
	public interface GoogleDrvDialogListener { 
		public void OnGoogleDrvSettings(String folderTitle, int fileTitle, int which);
		public void onGoogleDrvAccountSetting(final boolean connect);
	}
}
