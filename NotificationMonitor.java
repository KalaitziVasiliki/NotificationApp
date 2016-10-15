/*
 * SERVICE �������������� ��� ������������ ��� ��
 * 
 * ���� �� SERVICE ��� ������ �� �������� ��� ����� ����������� �� ��� 
 * �������� NOTIFICATION-LISTENER-SERVICE ����� �� ��������� �������� ��� ��
 * (��. KITKAT � ANDROID 6+) ��������� �� NOTIFICATION-LISTENER-SERVICE ���
 * ������� ��� ���������� REBOOT ��� �� ��������� � ����� ���������� ��� API.
 * 
 * �� �������� ����� ������
 * 	ISSUE #62811: https://code.google.com/p/android/issues/detail?id=62811
 * 
 * ���� ��� ����:
 *  ISSUE #200490, #65419, #203733
 */
package com.example.notification;

import java.util.ArrayList;

import com.example.notification.GoogleDrv.CopyNotificationToGDListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

@SuppressLint("DefaultLocale")
public class NotificationMonitor extends NotificationListenerService 
	implements ConnectionCallbacks, OnConnectionFailedListener {
	/*
	 * �� logcat Tag ��� ����� ������������� �� ������������ ��������� ���
	 * �������� ��� ������ 
	 */
	private static final String LOG_TAG = "NOTIF";	
	
	private DbFilterManager dbManager = null;
	private ConnectivityManager cnManager = null;
	private SharedPreferences pref;
	private static Object NO_ERROR = null;
	
	@Override
	public void onCreate() { 
		super.onCreate();		
		Log.i(LOG_TAG, "NotificationMonitor::onCreate()");
		
		// �������� �������� ���� ��� ������ �� ��� ������������
		try { 
			dbManager = DbFilterManager.getInstance(this);						
		} catch(SQLException e) {
			Log.e(LOG_TAG, "NotificationMonitor::onCreate().DbFilterManager->SQLException");
			e.printStackTrace();			
		} catch(Exception e) { 
			Log.e(LOG_TAG, "NotificationMonitor::onCreate().DbFilterManager->Exception");
			e.printStackTrace();
		}
		
		// �������� ��������� ���� �������� GoogleDrive.
		try { 
			GoogleDrv.initConnection(this, this, this);
			
			if(GoogleDrv.getUserConnectionSetting(this))
				GoogleDrv.gClient.connect();
		} catch(Exception e) { 
			Log.e(LOG_TAG, "NotificationMonitor::onCreate().initConnection->Exception");
		}
		
		// �������� ��������� ���� ��������� ��������� ��� ��������
		cnManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		// �������� ��������� ���� ��������� ��� ���������
		pref = getSharedPreferences(MainActivity.ACTIVITY_SETS, Context.MODE_PRIVATE);
	}
	
	/***************************************************************************
	 * ����������� �� Android 6+ (;)                                           *
	 ***************************************************************************/
	@Override
	public IBinder onBind(Intent intent) { 
		return super.onBind(intent);
	}
	@Override
	public boolean onUnbind(Intent intent) { 
		return super.onUnbind(intent);
	}
	
	/***************************************************************************
	 * ���������� ����������� ���� ������������                                *
	 *                                                                         *
	 * � ���������� ������� ���������, �� ��� ��������� thread ��� ������ ���  *
	 * NotificationListenerService                                             *
	 ***************************************************************************/
	@Override
	public void onNotificationPosted(StatusBarNotification sbn) { 
		Log.i(LOG_TAG, "NotificationMonitor::onNotificationPosted(sbn)");
		
		final Context context = this;
		try { 
			new AsyncTask<StatusBarNotification, Void, Object>() {				
				@Override
				protected Object doInBackground(StatusBarNotification... args) {					
					try {
						final Notification notif = args[0].getNotification();
						final Bundle notifExtras = notif.extras;
						
						// � ������ ��� �����������
						final String notifTitle    = notifExtras
								.getString(Notification.EXTRA_TITLE, ""),
						// � ����������� (�� �������) ������ ��� �����������
									 notifBigTitle = notifExtras
								.getString(Notification.EXTRA_TITLE_BIG, ""),
						// �� ������� ����������� ��� �����������
									 notifText     = notifExtras
								.getString(Notification.EXTRA_TEXT),
						// �������������� ������� ��� ������� ��� �������� Accessibility (���������� ��� ����) ��� OS  
									 notifTicker   = notif.tickerText != null ? notif.tickerText.toString(): "";
						
						Log.i(LOG_TAG, " -TITLE     = \"" + notifTitle    + "\"");
						Log.i(LOG_TAG, " -BIG TITLE = \"" + notifBigTitle + "\"");
						Log.i(LOG_TAG, " -TEXT      = \"" + notifText     + "\"");
						Log.i(LOG_TAG, " -TICKER    = \"" + notifTicker   + "\"");
						
						// �� ������ ��� ������ 
						final ArrayList<FilterItem> filterList = 
								dbManager.listFilters(context);						
						
						/*
						 * ��������� ��� �������� ���� ������� �)���� �����,
						 *  �)���� ���������� �����, �)��� ����������� ��� 
						 * ����������� ��� �)��� ��������� ������� (ticker) ���
						 *  ��� ��������� ����������� ����.
						 */						
						for(FilterItem filter: filterList) { 
							// ������� ���� ������� �������
							if(!filter.active)
								continue;
							
							if(matchStr(filter.matchingType, notifTitle   , filter.token)
							|| matchStr(filter.matchingType, notifBigTitle, filter.token)
							|| matchStr(filter.matchingType, notifText    , filter.token)
							|| matchStr(filter.matchingType, notifTicker  , filter.token)) {
								// �������� ��� Payload ���� �������
								Log.i(LOG_TAG, "  -APPLY FILTER PAYLOAD -> " + filter.token);
								
								applyPayload(filter, args[0]);
							}														
						}
						return NO_ERROR;
					} catch(Exception e) { 
						e.printStackTrace();
						
						Log.e(LOG_TAG, "NotificationMonitor::OnNotificationPosted->Exception!");
						return e;
					}
				}
				@Override
				protected void onPostExecute(Object r) { 
					if(r instanceof Exception)
						return;
				}
			}.execute(sbn);
		} catch(Exception e) { 
			e.printStackTrace();
			
			Log.e(LOG_TAG, "NotificationMonitor::onNotificationPosted(sbn)->Exception");
		}	
	}
	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) { 
		// ��� ���������������
	}
	
	/***************************************************************************
	 * ������� ��������� �������������� token ��� ������������� ����������� ���*
	 *  str.                                                                   *
	 *                                                                         *
	 * � ��������� ��� token ����� ��� str ������ �� ����� (matchType) ����     *
	 *  ������ (MATCHING_TYPE_START), ���� ���� (MATCHING_TYPE_END) �          *
	 * ���������� (MATCHING_TYPE_ANY).                                         *
	 *                                                                         *
	 * �� �� token ������ ��� ������ matchType ��� str ���� � ���������        *
	 *  ���������� true ������ false.                                          *
	 ***************************************************************************/
	private boolean matchStr(final int matchType, String str, String token) {
		if(str != null && !str.isEmpty() && token != null && !token.isEmpty()) { 
			str = str.toUpperCase();
			token = token.toUpperCase();
			
			switch(matchType) { 
			case FilterItem.MATCHING_TYPE_START:
				return str.startsWith(token);
			case FilterItem.MATCHING_TYPE_END:
				return str.endsWith(token);
			case FilterItem.MATCHING_TYPE_ANY:
				return str.indexOf(token) != -1; 
			default: break;
			}
		}
		return false;
	}
	
	/***************************************************************************
	 * �������� ��� ������������� ������ / ������������� (payload) ��� ������� *
	 *  (filter) ���� ���������� (StatusBarNotification).                      *
	 ***************************************************************************/
	private void applyPayload(final FilterItem filter, final StatusBarNotification sbn) {			
		// ��������� ��� ����������� ���� ������ �� ��� ������������
		if(0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_DB)) { 
			Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->COPY_TO_DB");
			
			dbManager.insertNotification(new NotificationItem(sbn));
		}
		
		// �������� ��� ����������� ��� �� Notification Tray ��� OS
		if(0 != (filter.payload & FilterItem.PAYLOAD_REMOVE)) { 
			Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->PAYLOAD_REMOVE");
			
			/*
			 * ���������� �� ���� ��� �������� Android
			 *  (KitKat+) ��� �� minSdkVersion ��� targetSdkVersion
			 * ��� AndroidManifest.xml ����� 19.
			 * 
			 * �� ����� 19+ ���� ������ �� �������������� ��� ��� 
			 *  ��� ���������� cancelNotification(String key).
			 *  
			 * �� �� sbn ���������� �� ��� ���������� � ����� ��� ������ �� 
			 *  ���������, ��� ���������� ��� ���������� ����� �� ������� (FLAG_ONGOING_EVENT), 
			 * � ��� ���������� ��� ��� ����������� (FLAG_NO_CLEAR), ����    
			 *  �� PAYLOAD_REMOVE ���������.
			 */
			if(0 == (sbn.getNotification().flags & Notification.FLAG_NO_CLEAR) 
			&& 0 == (sbn.getNotification().flags & Notification.FLAG_ONGOING_EVENT))
				cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());
			else
				Log.i(LOG_TAG, "  -NOTIFICATION SKIPPED->FLAG_NO_CLEAR || FLAG_ONGOING_EVENT ");
		}
			
		// ��������� ��� ����������� ��� Google Drive ��� ������
		if(0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD)
		|| 0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM)
		|| 0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI)) { 
			if(0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM))
				Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->PAYLOAD_COPY_TO_GD_GSM");
			else
				if(0 != (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI))
					Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->PAYLOAD_COPY_TO_GD_WIFI");
				else
					Log.i(LOG_TAG, " NotificationMonitor::applyPayLoad->PAYLOAD_COPY_TO_GD");
			
			if(GoogleDrv.getUserConnectionSetting(this) && GoogleDrv.isUserAuthorized())
				GoogleDrv.gClient.connect();
			
			// �� ��� ������� ������, ���� �� PAYLOAD ����������
			final NetworkInfo netInf;
			
			if( cnManager == null 
			|| (netInf = cnManager.getActiveNetworkInfo()) == null 
			|| !netInf.isConnected()) { 
				Log.e(LOG_TAG, "  -NOTIFICATION SKIPPED->NO DATA CONNECTION");
				return;
			}			
			
			// �� ������ ��������� ����������� ��������� ��������;
			if(0 == (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD)) { 			
				// �� ������ ������� ������� ���������� GSM-GPRS/EDGE � 3G/4G
				if(netInf.getType() == ConnectivityManager.TYPE_MOBILE 
				&& (0 == (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM))) {
					Log.e(LOG_TAG, "  -NOTIFICATION SKIPPED->NO WIFI/WIMAX/BT DATA CONNECTION");
					return;
				}
				// �� ������ ������� �������� ���������� WIFI/WIMAX � BLUETOOTH
				if((netInf.getType() == ConnectivityManager.TYPE_WIFI
				||  netInf.getType() == ConnectivityManager.TYPE_WIMAX
				||  netInf.getType() == ConnectivityManager.TYPE_BLUETOOTH)
				&& (0 == (filter.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI))) { 
					Log.e(LOG_TAG, "  -NOTIFICATION SKIPPED->NO GSM/3G/4G DATA CONNECTION");
					return;
				}
			}
						
			GoogleDrv.CopyNotificationToGD(
					// �������� ��������� ������������ ��� ��� ��������� ��� ������������ ��� GoogleDrive 
					pref.getString(MainActivity.ACTIVITY_SET_GD_FOLDER_TITLE, GoogleDrv.GD_FOLDER),
					pref.getInt(MainActivity.ACTIVITY_SET_GD_FILE_TITLE, GoogleDrvDialog.FILE_TITLE_NOTIF_TITLE),					
					new NotificationItem(sbn), 
					new CopyNotificationToGDListener() {						
						@Override
						public void onCopyNotificationGD(String folderTitle, String fileTitle, boolean r) {
							if(!r) {
								Log.e("PRG", "NotificationMonitor::onCopyNotificationGD\n" + 
										" folderTitle = " + folderTitle + "\n" + 
										" fileTitle   = " + fileTitle + "\n" + 
										" Failure!");
							}
						}
					});  		
		}
	}

	/***************************************************************************
	 * ������������ �������� �� ��� ��������� ��������� ��� Google             *
	 ***************************************************************************/
	@Override
	public void onConnected(Bundle bundle) {
		// � ������� �� ��� �������� GoogleDrive ����������
		Log.i(LOG_TAG, "NotificationMonitor::onConnected->GoogleDriveClient");
	}
	@Override
	public void onConnectionFailed(ConnectionResult r) {
		Log.e(LOG_TAG, "NotificationMonitor::onConnectionFailed->GoogleDriveClient");
		
		if(!GoogleDrv.attemptReconnect())
			Log.e(LOG_TAG, "NotificationMonitor::OnConnectionFailed->attemptReconnect failure!");
	}
	@Override
	public void onConnectionSuspended(int arg0) {
		Log.i(LOG_TAG, "NotifictionMonitor::onConnectionSuspended->GoogleDriveClient");
	}
}
