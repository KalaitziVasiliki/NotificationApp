/*******************************************************************************
 * ������� / �������� ���������                                                *
 *******************************************************************************/
package com.example.notification;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.example.notification.YesNoDialog.YesNoListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends Activity implements 
	OnClickListener, YesNoListener {	
	
	// � ��������� ��� �������� ������������ ��������� ������������� ������������
	private final static int YESNO_ID_DELETE = 1;	
	
	// �������� ������� ��� ���������� �������
	private ProgressDialog pDlg = null;
	// Subclass ��� ArrayAdapter ��� ��� ������� ��� ������������ ��� ListView ��� Activity
	private NotifAdapter nAdapter;
	// � ������������ ��� ��
	private DbFilterManager dbManager;
	// ������� ���� ����������� �������������� ��������� ��� OS (PackageManager)
	private PackageManager packMan; 
	
	/*
	 * � ���������� & ��� ��� ���������� �� ������������ ��� ListView ��� Activity
	 *  ��� ��� �� ��� ������������.
	 *  
	 * (��������������� ��� ��� ��������� DateUtils.formatSameDayTime ���
	 *  NotifAdapter.getView)
	 */
	private long lastRTC = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		// ������������ ��� UI
		setContentView(R.layout.activity_history);
		
		getActionBar().show();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		/* 
		 * ������������ NotifAdapter ��� ��� ������� ��� ������������ ��� �� 
		 *  ��� ListView
		 */
		nAdapter = new NotifAdapter((ListView)findViewById(R.id.listViewHistory), this);
		
		// �������� ���� ����������� �������������� ��������� ��� OS (PackageManager)
		packMan = getPackageManager(); 		
		
		// ������������ �� ��� �������� ��� ������������� ������������ ���
		try { 
			dbManager = DbFilterManager.getInstance(this);
			listNotifications();
		} catch(SQLException e) { 
			CenterToast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			finish();
		}		
	}
	
	/***************************************************************************
	 * ������� ��� ����� Activity ��� ������� ��� ���������� ���� ���          *
	 * �������� ���.                                                           *
	 *                                                                         *
	 * �� items ��� ����� ������������ ��� ��������� ��� ActionBar ��� Activity* 
	 ***************************************************************************/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		/*
		 * ������ ���������� �� ����� ��� �� Filters Activity, �������� �� item
		 *  "��������� �������" �� ����� ����� ������!
		 */
		menu.findItem(R.id.itemFilterAdd).setVisible(false);
		/*
		 * ��������, �� ������� "��������������� ���������" ����� ����� �����
		 *  ����� ��� �� ������� �� ��������� mark mode 
		 */
		menu.findItem(R.id.itemHistoryRefresh).setVisible(!nAdapter.getMarkMode());
		/*
		 * �� �� Mark mode (� �� ���� ListView) ���� �������� ��� item �������  
		 *  mark mode
		 */			
		menu.findItem(R.id.itemFilterMarkMode).setVisible(!nAdapter.getMarkMode() && !nAdapter.isEmpty());
		/*
		 * ���������� �� �� Mark mode ���� �������� ��� �������� ������� � 
		 *  "��� �������" ���� ��� ��� �������� "����������� ���������" & "�������"
		 * (�� ����� ���� ���� ������� ���� ��� ��� ���������� ����������)
		 */
		menu.findItem(R.id.itemFilterDoRemove)
			.setVisible(nAdapter.getMarkMode() && nAdapter.getMarkedCount() > 0); 
		menu.findItem(R.id.itemFilterSelect).setVisible(nAdapter.getMarkMode());
		menu.findItem(R.id.itemFilterCancel).setVisible(nAdapter.getMarkMode()); 
		return true;
	}	
	
	
	/***************************************************************************
	 * � ������� ������� ������ item ��� �� ����� ��� Activity                 *
	 ***************************************************************************/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) { 
		switch(item.getItemId()) { 
		// � ������� ������� ����� ��� �� Activity
		case android.R.id.home:
			finish();
			return true;
		// � ������� ������ �������������� (refresh) ��� ��������� 
		case R.id.itemHistoryRefresh:
			listNotifications();
			return true;
		// � ������� ������� ��� ������ ��� mark mode
		case R.id.itemFilterMarkMode:
			nAdapter.setMarkMode(true);
			invalidateOptionsMenu();
			return true;
		// ����������� ��������� ����������� ������������
		case R.id.itemFilterDoRemove:			
			YesNoDialog.show(this, 
					YESNO_ID_DELETE, 
					getText(R.string.title_confirmation), 
					getResources().getQuantityString(R.plurals.notifications_to_delete, 
							nAdapter.getMarkedCount(), 
							nAdapter.getMarkedCount()), this);
			return true;
		/*
		 * � ������� �������� � ��� �������� ���� ��� ������������
		 * 
		 * �� ������� ���� ��� ���������� ���������� ���� ��� ���������� ������ 
		 * ����������� ���� �� ������������.
		 */
		case R.id.itemFilterSelect:
			nAdapter.setMarkAll(nAdapter.getMarkedCount() == 0);
			invalidateOptionsMenu();
			return true;
		// � ������� ������ ������� mark mode
		case R.id.itemFilterCancel:
			nAdapter.setMarkMode(false);
			invalidateOptionsMenu();
			return true;			
		default: 
			return super.onOptionsItemSelected(item);
		}
	}
	
	/*************************************************************************** 
	 * � ������� ������ ��� ����� ��� ��� �� Activity �������� �� ������� BACK.*
	 *                                                                         *
	 * �� �� mark mode ����� ������, ���� ���� ������ ��� �� Activity, ������� *
	 *  ��� mark mode.                                                         *
	 ***************************************************************************/
	@Override
	public void onBackPressed() {
		if(nAdapter.getMarkMode()) {
			nAdapter.setMarkMode(false);
			invalidateOptionsMenu();
		}
		else
			super.onBackPressed();
	}
	
	/***************************************************************************
	 * ClickListener 				���������� ��� �������� ��� Activity       *
	 ***************************************************************************/		  
	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		// ���������� � ������������ ��������� �����������
		case R.id.imageViewHistoryMarked:
			nAdapter.setNotificationMark((Integer)view.getTag(), 
					!nAdapter.isNotificationMarked((Integer)view.getTag()));
			invalidateOptionsMenu();
			return;
		}
	}	

	/***************************************************************************
	 * ��������� ���� ��� ������������� ������������ ��� �� ��� ListView ���   *
	 *  Activity.                                                              *
	 *                                                                         *
	 * � ��������� ��� ListView ������� ��������� �� ��������� Thread ���� ��  *
	 * ��� ���������� ��� �������� ��� UI.                                     *
	 ***************************************************************************/	
	private void listNotifications() {
		final Context context = this;
		
		new AsyncTask<Void, Void, Object>() {
			@Override
			protected void onPreExecute() { 
				if(pDlg != null) {
					pDlg.dismiss();
					pDlg = null;
				}				
				pDlg = ProgressDialog.show(context, "", context.getText(R.string.msg_wait));
			}
			@Override
			protected Object doInBackground(Void... args) {
				// ��������� ���� ��� ������������ ��� ��
				return dbManager.listNotifications(context);
			} 
			@SuppressWarnings("unchecked")
			@Override
			protected void onPostExecute(Object r) { 
				if(pDlg != null) {
					pDlg.dismiss();
					pDlg = null;
				}
				
				// ������� ���������;
				if(r instanceof Exception) { 
					CenterToast.makeText(context, ((Exception)r).toString(), 
							Toast.LENGTH_LONG).show();
				}
				else { 
					lastRTC = Calendar.getInstance().getTimeInMillis();
					
					// ��������� ��� ArrayAdapter �� ��� ������������� ������������ 
					nAdapter.setNotificationList((ArrayList<NotificationItem>)r);
					
					// �������� � �������� ��� �������� "����� ListView"
					((TextView)findViewById(R.id.textViewHistoryEmpty))
						.setVisibility(nAdapter.isEmpty() ? View.VISIBLE: View.GONE);					
					// ��������� ��� �������� ������� ������������� ������������
					((TextView)findViewById(R.id.textViewNotificationCounter))
						.setText(!nAdapter.isEmpty() ? 
								getResources().getQuantityString(R.plurals.notification_counter, 
										nAdapter.getCount(), nAdapter.getCount()): "");
					
					// ����� ��� mark mode
					nAdapter.setMarkMode(false);					
					// ��������� ��� Menu ��� Activity
					invalidateOptionsMenu();
				}
			}
		}.execute();
	}	
	
	/***************************************************************************
	 * �������� ��� ��� �� ���� ��� ������������� ������������                 *
	 *                                                                         *
	 * � �������� ������� ��������� �� ��������� Thread ���� �� ��� ���������� *
	 * ��� �������� ��� UI.                                                    *
	 ***************************************************************************/
	private void deleteNotifications() { 
		final Context context = this;
		
		new AsyncTask<Void, Void, Object>() {
			@Override
			protected void onPreExecute() {
				if(pDlg != null) { 
					pDlg.dismiss();
					pDlg = null;
				}				
				pDlg = ProgressDialog.show(context, "", context.getText(R.string.msg_wait));
			}			
			@Override
			protected Object doInBackground(Void... args) {
				// �������� ���� ��� ������������� ������������ ��� ListView
				try { 
					for(int position = 0; position < nAdapter.getCount(); position++) { 
						final NotificationItem item = nAdapter.getItem(position);
						
						if(item.mark) // ������������;
							if(!dbManager.deleteNotification(item))
								throw new Exception(context.getString(R.string.error_notification_mass_delete) + item.title);						
					}
					
					return null;
				} catch(Exception e) { 
					e.printStackTrace();
					return e;
				}
			}
			@Override
			protected void onPostExecute(Object r) { 
				if(pDlg != null) {
					pDlg.dismiss();
					pDlg = null;
				}
				
				// ������� ���������;
				if(r instanceof Exception) {
					final Exception e = (Exception)r;
					
					CenterToast.makeText(context, 
							e.getMessage() != null && !e.getMessage().isEmpty() ? 
									e.getMessage(): e.toString(), 
							Toast.LENGTH_LONG).show();
				}				
				// ����������� ListView
				listNotifications();
			}
		}.execute();
	}	
		
	/***************************************************************************
	 * YesNoListener 			����������� � �������� ���������� Activity	   *
	 ***************************************************************************/
	@Override
	public void OnYesNoListener(int id, int which) {
		switch(id) { 
		case YESNO_ID_DELETE:	// �������� ����������� ������������;
			if(which == DialogInterface.BUTTON_POSITIVE)
				deleteNotifications();
			return;
		default: return;
		}
	}		
	
	/***************************************************************************
	 * NotifAdapter   		 Subclass ��� ArrayAdapter ���� �� �������������   * 	
	 *                		 ����������� ���������� NotificationItem           *
	 ***************************************************************************/
	private class NotifAdapter extends ArrayAdapter<NotificationItem> {		
		private final OnClickListener externClickListener;
		
		private boolean markMode = false;
		private int markedCount = 0;
		
		public NotifAdapter(final ListView owner,
				final OnClickListener externClickListener) {
			super(owner.getContext(), R.layout.item_history);
						
			owner.setAdapter(this);
			this.externClickListener = externClickListener;
		}
		
		// ������ ��� ������� ��� ������������� ������������ ���� 1
		private void increaseMarkedCount() { 
			if(++markedCount > getCount())
				markedCount = getCount();
		}
		// ������ ��� ������� ��� ������������� ������������ ���� 1
		private void decreaseMarkedCount() { 
			if(--markedCount < 0)
				markedCount = 0;
		}
		// ��������� ��� ������� ��� ������������� ������������
		public int getMarkedCount() { 
			return markedCount;
		}
		
		/*
		 * �� mark ��� �� true ���� ���������� ���� ��� ������������ ������
		 *  ������������ ����.
		 * 
		 * �� �� FilterAdapter ����� �� edit mode ���� � ��������� ����������
		 *  false ������ true.
		 */
		public boolean setMarkAll(final boolean mark) {
			if(markMode) {
				for(int position = 0; position < getCount(); position++)
					getItem(position).mark = mark;
				
				markedCount = mark ? getCount(): 0;
				
				notifyDataSetChanged();
				return true;
			}
			return false;
		}		
		
		/* 
		 * ������� ��� ������������ ��� ArrayList<NotificationItem> ��� ListView.
		 * 
		 * �� �� list ����� null ���� �� ����������� ��� ListView �����������.
		 */
		public void setNotificationList(final ArrayList<NotificationItem> list) {			
			clear();
			
			if(list != null)			
				addAll(list);
			
			if(isEmpty())
				setMarkMode(false);
		}		
		
		/*
		 * ������������ � �������������� ��� ������ ������������� (mark mode)
		 *  ��� NotifAdapter.
		 * 
		 * ���� �� mark mode ����� ������, � ������� ������ �� �������� � �� 
		 *  ���������� ������������ ��� ListView.
		 * 
		 * � ��������� ���������� ��� ������ ��� (markMode).
		 */
		public boolean setMarkMode(final boolean markMode) {
			this.markMode = markMode;

			markedCount = 0;
			
			for(int item = 0; item < getCount(); item++)
				getItem(item).mark = false;			
			notifyDataSetChanged();
			
			return markMode;
		}
		/*
		 * ��������� true �� �� mark mode ��� FilterAdapter ����� ������ 
		 *   ������ false.
		 */
		public boolean getMarkMode() { 
			return markMode;
		}		
		/*
		 * ��������� true �� � ���������� ���� ���� position ��� ListView �����
		 *  ������������ ������ false.
		 */
		public boolean isNotificationMarked(final int position) { 
			return getItem(position).mark;
		}		
		/*
		 * ���������� � ������������ (mark) ��� ����������� ���� ���� position 
		 *  ��� ListView.
		 * 
		 * � ��������� ���������� ��� ������ ��� (mark).
		 */
		public boolean setNotificationMark(final int position, final boolean mark) { 
			if(getItem(position).mark = mark)
				increaseMarkedCount();
			else
				decreaseMarkedCount();
			
			notifyDataSetChanged();
			return mark;
		}		
		
		/* 
		 * ������� ��� ���� (view) ���� item ��� ListView
		 * 
		 * ��. https://developer.android.com/reference/android/widget/ArrayAdapter.html#getView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) { 
			final View view;
			
			final ImageView imgAppIcon, 
							imgMark;
			
			final TextView txtApp,
						   txtTitle,
						   txtMsg,
						   txtTicker,
						   txtClock;
			
			if(convertView == null) { 
				view = View.inflate(getContext(), R.layout.item_history, null);
				
				imgAppIcon = (ImageView)view.findViewById(R.id.imageViewHistoryAppIcon);
				
				imgMark    = (ImageView)view.findViewById(R.id.imageViewHistoryMarked);
				imgMark.setOnClickListener(externClickListener);
				
				txtApp      = (TextView)view.findViewById(R.id.textViewHistoryApp);
				txtTitle    = (TextView)view.findViewById(R.id.textViewHistoryTitle);
				txtMsg      = (TextView)view.findViewById(R.id.textViewHistoryBody);
				txtTicker   = (TextView)view.findViewById(R.id.textViewHistoryTicker);
				txtClock    = (TextView)view.findViewById(R.id.textViewHistoryClock);
				
				view.setTag(R.id.imageViewHistoryAppIcon,imgAppIcon);
				view.setTag(R.id.imageViewHistoryMarked, imgMark);
				
				view.setTag(R.id.textViewHistoryApp   , txtApp);
				view.setTag(R.id.textViewHistoryTitle , txtTitle);
				view.setTag(R.id.textViewHistoryBody  , txtMsg);
				view.setTag(R.id.textViewHistoryTicker, txtTicker);
				view.setTag(R.id.textViewHistoryClock , txtClock);
			}
			else { 
				view = convertView;
				
				imgAppIcon = (ImageView)view.getTag(R.id.imageViewHistoryAppIcon); 
				imgMark    = (ImageView)view.getTag(R.id.imageViewHistoryMarked);
				
				txtApp   = (TextView)view.getTag(R.id.textViewHistoryApp);
				txtTitle = (TextView)view.getTag(R.id.textViewHistoryTitle);
				txtMsg   = (TextView)view.getTag(R.id.textViewHistoryBody);
				txtTicker= (TextView)view.getTag(R.id.textViewHistoryTicker);
				txtClock = (TextView)view.getTag(R.id.textViewHistoryClock);
			}
			
			// �������� ������������� �����������
			final NotificationItem item = getItem(position);						
			
			// ��������� ��� ������������ ���������� � ���; 
			imgMark.setImageResource(item.mark ? 
					android.R.drawable.checkbox_on_background:
						android.R.drawable.checkbox_off_background);
			imgMark.setTag(position);
			
			// �� �� ��������� mark ���� �������� ��� �������� ������������� �����������
			imgMark.setVisibility(markMode ? View.VISIBLE: View.INVISIBLE);			
			
			// ������� ��� ��������� ��� �������� ��� ����������
			final Pair<String, Drawable> appInf = item.getAppTitleIcon(packMan);			
			txtApp.setText(appInf.first);
			imgAppIcon.setImageDrawable(appInf.second);
			
			// ������� ������, ������������ ��� ���������� �������� ��� Notification
			txtTitle.setText(item.getTitle());
			txtMsg.setText(item.message);
			txtTicker.setText(item.ticker);
			// ������� ��� ������ ��������� ��� Notification
			txtClock.setText(DateUtils.formatSameDayTime(item.clock,
					lastRTC, 
					DateFormat.DEFAULT,
					DateFormat.DEFAULT));
			
			return view;
		}		
	}
}