/*******************************************************************************
 * ������������� ������������ - �������� ACTIVITY                              *
 *******************************************************************************/
package com.example.notification;

import com.example.notification.GoogleDrvDialog.GoogleDrvDialogListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity 
	implements OnClickListener, OnLongClickListener, 
	ConnectionCallbacks, OnConnectionFailedListener, 
	GoogleDrvDialogListener {
	/*
	 * �� logcat Tag ��� ����� ������������� �� ������������ ��������� ���
	 * �������� ��� ������ 
	 */
	private static final String LOG_TAG = "NOTIF";
	
	// ������� ���� ��������� ��� ������������
	public static final String  ACTIVITY_SETS = "ASET",
	// � ������ ��� ������� ����������� ��� ������������ ��� GoogleDrive
								ACTIVITY_SET_GD_FOLDER_TITLE = "ASETGDFLDRT",
	// � ����� ��������� ��� ������� ��� ������������� ��� GoogleDrive
								ACTIVITY_SET_GD_FILE_TITLE   = "ASETGDFNT",
	// ������� ���� �������� Google Drive
								ACTIVITY_SET_GD_CONNECT		 = "GDCONNECT";
	
	/*
	 * � ��������� (RequestCode) �� ��� ����� ���������� �� ������������ ���
	 *  ��� Activity � ���������� GoogleDrv.resolveConnectionFailure   
	 */
	private static final int GDRIVE_REQ_CODE = 1010;
	// ������� ���� �������� Google Drive (��. ����� GoogleDrv) 
	GoogleApiClient gClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		// ������������ ��� UI
		setContentView(R.layout.activity_main);
		
		Button btnFilter,
			   btnHistory,
			   btnSettings,
			   btnGoogleDrive;
		
		btnFilter = (Button)findViewById(R.id.buttonMainFilter);
		btnFilter.setOnClickListener(this);
		btnFilter.setOnLongClickListener(this);
		
		btnHistory = (Button)findViewById(R.id.ButtonMainHistory);
		btnHistory.setOnClickListener(this);
		btnHistory.setOnLongClickListener(this);
		
		btnSettings = (Button)findViewById(R.id.ButtonMainSettings);
		btnSettings.setOnClickListener(this);
		btnSettings.setOnLongClickListener(this);
		
		btnGoogleDrive = (Button)findViewById(R.id.ButtonMainGD);
		btnGoogleDrive.setOnClickListener(this);
		btnGoogleDrive.setOnLongClickListener(this);		
		
		/*
		 * ������� �� �� Google Drive API
		 * 
		 * � ��������� ����������� ��� ��� ���������� ������������ (OAuth2) ���   
		 *  ������ ���� �������� Google Drive �� ���� ������� ���� (getUserConnectionSetting).
		 */
		try { 
			gClient = GoogleDrv.initConnection(this, this, this);
			
			if(GoogleDrv.getUserConnectionSetting(this))
				gClient.connect();
		} catch(Exception e) { 
			e.printStackTrace();
		}
		
		// ������������ ��� ��
		try { 
			DbFilterManager.getInstance(this);			
		} catch(SQLException e) { 
			CenterToast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
		}
	}
	
	/***************************************************************************
	 * OnResume,	�� activity ���������� ��� ���������, �� ���� ������� �    *
	 * 				������� ��� ������ ��� GoogleDrive, ������ �������� ��� �� *
	 ***************************************************************************/
	@Override
	public void onResume() { 
		super.onResume();
				
		if(GoogleDrv.getUserConnectionSetting(this))
			GoogleDrv.gClient.connect();
	}	
	

	/***************************************************************************
	 * OnClickListener, ���������� ��� �������� ��� Activity                   *
	 **************************************************************************/
	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		// ������� ����������� ������������� �������
		case R.id.buttonMainFilter:
			startActivity(new Intent(this, FiltersActivity.class));
			return;
		// ������� ������ ������������� ������������ (��������)
		case R.id.ButtonMainHistory:			
			startActivity(new Intent(this, HistoryActivity.class));
			return;
		// ���������� ��� "��������� �������������� ������������" ��� ��
		case R.id.ButtonMainSettings:
		{
			Intent inSettings = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
			// ��������� �� �� ����� ��� ���������;
			if(inSettings.resolveActivity(getPackageManager()) != null)			
				startActivity(inSettings);
			else {
				// ���!
				CenterToast.makeText(this, R.string.msg_service_not_ready, 
						Toast.LENGTH_SHORT).show();
			}
		}		
			return;
		// ���������� ��� "��������� ��� Google Drive"
		case R.id.ButtonMainGD:
		{
			/*
			 * �������� ��� ��������� �������� ��� ������, �� �������� ������ 
			 *  ����������� ��� default!
			 */
			final SharedPreferences pref = getSharedPreferences(ACTIVITY_SETS, 
					Context.MODE_PRIVATE);			
			GoogleDrvDialog.show(this, 
				pref.getString(ACTIVITY_SET_GD_FOLDER_TITLE, GoogleDrv.GD_FOLDER),
				pref.getInt(ACTIVITY_SET_GD_FILE_TITLE, GoogleDrvDialog.FILE_TITLE_NOTIF_TITLE),
				this);
		}
			return;
		default: return;
		}
	}

	/***************************************************************************
	 * OnLongClickListener, ���������� ������������� ��������� �������� ���    *
	 *                      Activity                                           *
	 ***************************************************************************/
	@Override
	public boolean onLongClick(View view) {
		/*
		 * ������� �������� ���������� ��� �������������� ��� ��������.
		 * 
		 * � ��������� ����� ������������ ���� �������� tag ��� ���� Button
		 *  ��� �������� ��� activity_main.xml ��� �����. 
		 */
		if(view.getTag() != null)
			CenterToast.makeText(this, (String)view.getTag(), Toast.LENGTH_LONG).show();
		
		return true;
	}

	/***************************************************************************
	 * ���������� ��������� ��� ����������� GoogleDrv.resolveConnectionFailure * 
	 ***************************************************************************/
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) { 
		if(requestCode == GDRIVE_REQ_CODE) {
			Log.i(LOG_TAG, "onActivityResult ReqCode = " + requestCode + "\n" +
						                    "ResCode = " + resultCode);
					
			if(resultCode == Activity.RESULT_OK) { 		
				GoogleDrv.setUserConnectionSetting(this, true);
				gClient.connect();			
			}
			else { 
				GoogleDrv.setUserConnectionSetting(this, false);
				CenterToast.makeText(this, "H ������� ���� �������� Google Drive �������", 
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
	/***************************************************************************
	 * ���������� ������������ � ������������ �������� �� �� Google Drive      *
	 ***************************************************************************/
	@Override
	public void onConnected(Bundle bundle) {
		Log.i(LOG_TAG, "onConnected");
		GoogleDrv.setUserConnectionSetting(this, true);
		CenterToast.makeText(this, R.string.msg_gd_connected, Toast.LENGTH_SHORT).show();		
	}
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(LOG_TAG, "onConnectionFailed->GoogleDrv.resolveConnectionFailure");		
		
		GoogleDrv.setUserConnectionSetting(this, 
				GoogleDrv.resolveConnectionFailure(this, connectionResult, GDRIVE_REQ_CODE));
	}
	@Override
	public void onConnectionSuspended(int reason) {
		// ��� ���������������..		
	}	

	/***************************************************************************
	 * ���������� ��� �������� ��������� ��� Google Drive, GoogleDrvDialog     * 
	 ***************************************************************************/
	@Override
	public void OnGoogleDrvSettings(String folderTitle, int fileTitle, int which) {
		if(which == DialogInterface.BUTTON_POSITIVE) { 
			// ���������� ��� ��������� ��� ������
			getSharedPreferences(ACTIVITY_SETS, Context.MODE_PRIVATE)
			.edit()
			.putString(ACTIVITY_SET_GD_FOLDER_TITLE, folderTitle)
			.putInt(ACTIVITY_SET_GD_FILE_TITLE, fileTitle)
			.commit();
		}
	}
	@Override
	public void onGoogleDrvAccountSetting(boolean connect) {
		// � ������� ������ ������� � ���������� ��� �� Google Drive
		if(!connect) {							// �������
			GoogleDrv.gClient.connect();
		}
		else {									// ����������			
			GoogleDrv.setUserConnectionSetting(this, false);			
			GoogleDrv.gClient.clearDefaultAccountAndReconnect();
		}
	}	
}
