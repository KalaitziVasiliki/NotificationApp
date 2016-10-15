/*******************************************************************************
 * ������� ����� ����������� ��� ��������� GOOGLE DRIVE                        *
 *******************************************************************************/
package com.example.notification;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

public class GoogleDrv {	
	/*
	 * � default �������� ��� ������� ���� ����� ������������� �� ������������
	 * ��� ������������
	 */
	public static final String GD_FOLDER = "�������� ������������";
	/*
	 * �� ������� ���� (�� ������������) ��� ��������� �� ��������� �������� ���
	 * �� GoogleDrive
	 */
	public static final int TIMEOUT_SEC = 30;
	/*
	 * �� logcat Tag ��� ����� ������������� �� ������������ ��������� ���
	 * �������� ��� ������ 
	 */
	private static final String LOG_TAG = "NOTIF";
	/*
	 * �� GoogleApiClient ��� ����������� ���� ��� ������������ �������� ��� 
	 * ������ �� �� GoogleDrive  
	 */
	public static GoogleApiClient gClient = null;
	
	/***************************************************************************
	 * ������� ��� Context ��� ������������ �� ��� �������� Google Drive.      *
	 *                                                                         *
	 * � �������� �������� ��� ������������ �� ��� �������� ������������ ���   *
	 *  cbListener event ��� � �������� ��� cfListener.                        *
	 *                                                                         *
	 * �� ��������� ���������, �� ��������� ������ �� ����������� ���          *
	 *  ���������� �������� ��� ��������� � Google.                            *
	 *                                                                         *
	 * � ��������� ���������� ����� �� ������� GoogleApiClient ��� ��������    *
	 *  ��� ����������� �� ��� �������� Google Drive.                          * 
	 ***************************************************************************/
	public static GoogleApiClient initConnection(final Context context, 
			final ConnectionCallbacks cbListener,
			final OnConnectionFailedListener cfListener) { 
		if(gClient == null) { 		
			gClient = new GoogleApiClient.Builder(context)
				.addApi(Drive.API)
				.addScope(Drive.SCOPE_FILE)
				.addConnectionCallbacks(cbListener)
				.addOnConnectionFailedListener(cfListener)
				.build();
		}
		return gClient;
	}
	
	/***************************************************************************
	 * ���������� �������� ��� ��������� (r) ��� �������� ���� ��� ������� ��� *
	 *  Activity (activity) �� ��� �������� GoogleDrive.                      *
	 *                                                                         *
	 * �� � ��������� ���������� true ���� �� Activity �� ������ �� �������    *
	 *  ���� ��� ��������� initConnection �� �������� ��� requestCode ���      *
	 * �� ����� ��� onActivityResult event ���!                                *
	 ***************************************************************************/
	public static boolean resolveConnectionFailure(final Activity activity, 
			final ConnectionResult r, 
			final int requestCode) { 
		if (r.hasResolution()) {
	        try {
	            r.startResolutionForResult(activity, requestCode);
	            return true;
	        } catch (IntentSender.SendIntentException e) {
	        	Log.e(LOG_TAG, "resolveConnectionFailure->SendIntentException");
	        	e.printStackTrace();	        		        
	        }
	    } else {
	        GooglePlayServicesUtil
	        	.getErrorDialog(r.getErrorCode(), activity, 0)
	        	.show();	       
	    }	
		return false;
	}	
	
	/***************************************************************************
	 * ������ ������������� ��� ������������ ���� �������� GoogleDrive.        *
	 *                                                                         *
	 * � ��������� ���������� false �� ������� ������ �������� � ���� ��       *
	 *  gClient ����� ��� ����������� � ��������� �� GoogleDrive.              *
	 ***************************************************************************/
	public static boolean attemptReconnect() { 
		if(gClient != null) { 
			try { 
				if(!gClient.isConnected() && !gClient.isConnecting()) {
					gClient.connect();
					return true;
				}
			} catch(Exception e) { 
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/***************************************************************************
	 * ��������� ��� �� � ������� ����� ������������ ���� �������� GoogleDrive *
	 *                                                                         * 
	 * true �� � ����������� ��� ������ ��������� ������ ������ ��� ��������   *
	 *  �������� �� �� GoogleDrive ������ false.                               *
	 ***************************************************************************/
	public static boolean getUserConnectionSetting(final Context context) { 
		return context.getSharedPreferences(MainActivity.ACTIVITY_SETS, Context.MODE_PRIVATE)
				.getBoolean(MainActivity.ACTIVITY_SET_GD_CONNECT, false);
	}
	/***************************************************************************
	 * ���������� ���������� �������� ��� ������ ���� �������� GoogleDrive     *
	 *                                                                         *
	 * true �� � ���������� ��� ���������� ���� �������� ������ false.         *
	 ***************************************************************************/
	public static boolean setUserConnectionSetting(final Context context, 
			final boolean connected) { 
		return context.getSharedPreferences(MainActivity.ACTIVITY_SETS, Context.MODE_PRIVATE)
				.edit()
				.putBoolean(MainActivity.ACTIVITY_SET_GD_CONNECT, connected)
				.commit();
	}
	
	/***************************************************************************
	 * ��������� true �� � ����������� ������ ���� ����� ������������� ���     *
	 *  �������� ���� �������� Google Drive ������ false.                      *  
	 ***************************************************************************/
	public static boolean isUserAuthorized() { 
		return gClient != null ? gClient.hasConnectedApi(Drive.API): false; 
	}
	
	/***************************************************************************
	 * ��������� ����������� (notification) �� ������ (folderTitle) ���        *
	 *  Google Drive ��� �� ������������ ����� ��� ��� ������ ��� �������.     *
	 *                                                                         *  
	 * �� ��������� ��������� � ��������� ���������� true ������ false         *
	 *                                                                         *
	 * -� ��������� ������� �� ������������� ����� (await) ������� ������ ��   *
	 *   ������ ����� ���� ���������� Thread (��. AsyncTas�) ������ ������ ��  *
	 *  �������� �� ANR �� ���������!!                                         *
	 *                                                                         *
	 * -�� ��� ����������� ���� (��. �������� ��������� ��������� ��� �� GD)   * 
	 *   ������� ������� ������������ ������ ��� GDAA (GoogleDrive Android API)*
	 *  ��� ��� Server ��� ��������� GoogleDrive �� ������ �� ������           *
	 *   (������� ��� ����) � ������:                                          *
	 *                                                                         *
	 *  Drive.DriveApi.requestSync(gClient);                                   *
	 *                                                                         *
	 *  � ����� ��� requestSync ������ �� ������� ��� �� ������� �������� �����*
	 *   �������� �� ������ ��� GoogleDrive Server.                            *
	 *                                                                         *
	 *  ����������� �������� �� �������� �� GDAA �� ����������� �������� ����  *
	 *   ��� ��� ������������ ������� �������� (��. ��� 5' �� 30'+) ��� �������*
	 *  ��� ����������� �� ��� ��������� �������� ��� ���� �������� folderTitle*
	 *   ��� ������������� � ��������!                                         *
	 ***************************************************************************/
	private static boolean syncCopyNotificationToGD(final String folderTitle,
			final int fileTitle,
			final NotificationItem notification) { 
		// ��������� ��� ������� �� ����� 'folderTitle' ��� GoogleDrive 
		final MetadataBufferResult qFolder = Drive.DriveApi.query(gClient, 
				new Query.Builder()
					.addFilter(Filters.eq(SearchableField.TITLE, 
							folderTitle))
					.addFilter(Filters.eq(SearchableField.MIME_TYPE, 
							"application/vnd.google-apps.folder"))
					.addFilter(Filters.eq(SearchableField.TRASHED, 
							false))
					.build())
				.await(TIMEOUT_SEC, TimeUnit.SECONDS);
		// ����� � ����� ���� �������� GoogleDrive Query �������; 
		if(!qFolder.getStatus().isSuccess()) {
			Log.e(LOG_TAG, "syncCopyNotificationToGD::Query failure!");
			return false;
		}
		
		/*
		 * � ��������� ��� ������� ��� GoogleDrive
		 * 
		 * -�� GoogleDrive ����������� ����������� ������ ������� ��� �������
		 *   �� ��� ���� ��������, ���� �� ��������� DriveId ��� ���� ��� ���
		 *  ����! 
		 */
		DriveId folderId;		
		
		/*
		 * �� �� Query ���������� �������� ������ ��������� ���� � �������
		 *  �� ����� 'folderTitle' ��� ������� ����� ������ �� ��� �����������.
		 */
		if(qFolder.getMetadataBuffer().getCount() == 0) {
			DriveFolderResult rFolder = Drive.DriveApi.getRootFolder(gClient)
				.createFolder(gClient, 
						new MetadataChangeSet.Builder()
							.setTitle(folderTitle)						
							.build())
				.await(TIMEOUT_SEC, TimeUnit.SECONDS);
			// ����� � ����� ���� �������� GoogleDrive createFolder �������;
			if(!rFolder.getStatus().isSuccess()) {
				Log.e(LOG_TAG, "syncCopyNotification::createFolder failure!");
				return false;
			}
			folderId = rFolder.getDriveFolder().getDriveId();
		}
		else			
			folderId = qFolder.getMetadataBuffer().get(0).getDriveId();
		
		/*
		 * ���������� ���� ���� ������� (���������� text/html) ���� ������ �� 
		 *  ��������� 'folderId'
		 */
		final DriveFileResult rFile = Drive.DriveApi.getFolder(gClient, folderId)
				.createFile(gClient, 
						new MetadataChangeSet.Builder()
							.setTitle(fileTitle == GoogleDrvDialog.FILE_TITLE_NOTIF_TITLE 
							&& !notification.title.isEmpty() ? 
									notification.title: notification.app)
							.setMimeType("text/html")
						.build(),
						null)
				.await(TIMEOUT_SEC, TimeUnit.SECONDS);
		// ����� � ����� ���� �������� GoogleDrive createFile �������;
		if(!rFile.getStatus().isSuccess()) { 
			Log.e(LOG_TAG, "syncCopyNotification::createFile failure!");
			return false;
		}
		
		// ������� ������� ���� ��� ������� (WRITE_ONLY)
		final DriveContentsResult rFileIO = rFile.getDriveFile()
				.open(gClient, DriveFile.MODE_WRITE_ONLY, null)
				.await(TIMEOUT_SEC, TimeUnit.SECONDS);
		// ����� � ����� ���� �������� GoogleDrive open (MODE_WRITE_ONLY) �������;
		if(!rFileIO.getStatus().isSuccess()) { 
			Log.e(LOG_TAG, "syncCopyNotification::open(file) failure!");
			return false;
		}		
		
		// �������� ��� ����������� ��� ������� - ������ I/O..
		final DriveContents fileIO = rFileIO.getDriveContents();
		
		boolean fileCommited = false;
		
		/*
		 * � ������� ��� ������ ������� �� ��� ������� ��� PrintStream ��� 
		 *  ��������� ��� ������� �������� ������-������.
		 * 
		 * �� ����������� ��� ������� �������� �� ����� 'UTF-16', ��� ��� �����
		 *  ������� ��� ������������ ��� ������ �� �������������� ������� ���������
		 * (editor). 
		 */
		PrintStream pOS = null;		
		try { 			 
			pOS = new PrintStream(fileIO.getOutputStream(), false, "UTF-16");
			pOS.println(notification.app);
			pOS.println(notification.getClockStr());
			pOS.println(notification.getTitle());
			pOS.println(notification.message);
			pOS.println(notification.ticker);
			pOS.flush();
		} catch(UnsupportedEncodingException e) { 
			e.printStackTrace();
		} finally { 
			pOS.close();			
			/*
			 * �� � ����������� ��� PrintSteram ������, ������� ��� ������������ ���
			 *  ��� GoogleDrive (commit) ������ ��� (discard)..
			 */
			if(!pOS.checkError())			
				fileCommited = fileIO.commit(gClient, null)
					.await(TIMEOUT_SEC, TimeUnit.SECONDS)
					.getStatus().isSuccess();
			else {
				fileIO.discard(gClient);
				fileCommited = false;
			}
		}
		
		return fileCommited;
	}
	
	/***************************************************************************
	 * ��������� ��������� ����������� (notification) �� ������ (folderTitle)  *
	 *  ��� GoogleDrive ��� �� ������������ ����� ��� ��� ������ ��� �������.  *
	 *                                                                         *
	 * � ���������� ���������� ��� ��� AsyncTask �� ����� ����������� ���      *
	 *  �������� ��� syncCopyNotificationToGD ��� ��� ��������� ���            *
	 * ������������� ��������� ��� ��� ��������� ���� ���� onCopyNotification- *
	 *  ToGD listener                                                          *
	 ***************************************************************************/
	public static void CopyNotificationToGD(final String folderTitle,
			final int fileTitle,
			final NotificationItem notification, 
			final CopyNotificationToGDListener listener) { 
		new AsyncTask<NotificationItem, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(NotificationItem... args) {
				try { 
					return syncCopyNotificationToGD(folderTitle, fileTitle, args[0]);
				} catch(Exception e) { 
					e.printStackTrace();
					return false;
				}
			}
			@Override
			protected void onPostExecute(Boolean r) { 
				Log.i(LOG_TAG, " asyncCopyNotificationToGD = " + (r ? "SUCCEEDED": "FAILED"));
				
				if(listener != null)
					listener.onCopyNotificationGD(folderTitle, 
							fileTitle == GoogleDrvDialog.FILE_TITLE_NOTIF_TITLE ? 
									notification.title: notification.app, r);
			}
		}.execute(notification);
	}
	public interface CopyNotificationToGDListener { 
		public void onCopyNotificationGD(final String folderTitle, 
				final String fileTitle, 
				final boolean r);
	}
}