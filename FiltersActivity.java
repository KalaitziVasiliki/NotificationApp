/*
 * �������, ������ ��� �������� �������
 */
package com.example.notification;

import java.util.ArrayList;

import com.example.notification.YesNoDialog.YesNoListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FiltersActivity extends Activity implements 
	OnClickListener, OnItemClickListener, YesNoListener {

	// � ��������� ��� �������� ������������ ��������� ������������� �������
	private final static int YESNO_ID_DELETE = 1;
	
	// �������� ������� ��� ���������� �������
	private ProgressDialog pDlg = null;
	// Subclass ��� ArrayAdapter ��� ��� ������� ��� ������� ��� ListView ��� Activity 
	private FilterAdapter fAdapter;
	// � ������������ ��� ��
	private DbFilterManager dbManager;
	
	/***************************************************************************
	 * ��������� Activity                                                      *
	 ***************************************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		// ����������� ��� UI
		setContentView(R.layout.activity_filters);
		
		getActionBar().show();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		
		// ������������ ListView ��� ��� ������� ��� ������� ��� ��
		ListView listView = (ListView)findViewById(R.id.listViewFilters);
		listView.setOnItemClickListener(this);
		// ������������ FilterAdapter ��� ��� ������� ��� ������� ��� �� ��� ListView
		fAdapter = new FilterAdapter(listView, this);
		
		// ������������ �� ��� �������� ��� ������������� ������� ���
		try { 
			dbManager = DbFilterManager.getInstance(this);
			listFilters();
		} catch(SQLException e) { 
			CenterToast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	/***************************************************************************
	 * ������� ������������� Activity "��������� � ������������ �������"       *
	 *                                                                         *
	 * �� � "�������� � �����������" ������� ��������� RESULT_OK ���� ���������*
	 * ��� ������������ ��� ListView ��� ����������� ��� Item �� ������� ��� ��*
	 * EXTRA_TOKEN                                                             *
	 ***************************************************************************/
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == FilterAddActivity.REQ_CODE && resultCode == Activity.RESULT_OK 
		&& data != null && data.getExtras() != null  && 
		   data.hasExtra(FilterAddActivity.EXTRA_TOKEN))
			listFilters(data.getExtras().getString(FilterAddActivity.EXTRA_TOKEN));
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
		 * �� �� Mark mode (� �� ���� ListView) ���� �������� ��� item �������  
		 *  mark mode ��� ��� ��������� ���� �������
		 */
		menu.findItem(R.id.itemFilterAdd).setVisible(!fAdapter.getMarkMode());		
		menu.findItem(R.id.itemFilterMarkMode).setVisible(!fAdapter.getMarkMode() && !fAdapter.isEmpty());
		/*
		 * ���������� �� �� Mark mode ���� �������� ��� �������� ������� � 
		 *  "��� �������" ���� ��� ��� �������� "����������� ���������" & "�������"
		 * (�� ����� ���� ���� ������� ���� ��� ��� ���������� ������)  
		 */
		menu.findItem(R.id.itemFilterDoRemove)
			.setVisible(fAdapter.getMarkMode() && fAdapter.getMarkedCount() > 0); 
		menu.findItem(R.id.itemFilterSelect).setVisible(fAdapter.getMarkMode());
		menu.findItem(R.id.itemFilterCancel).setVisible(fAdapter.getMarkMode()); 
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
		// � ������� ������� ��� �������� ���� �������
		case R.id.itemFilterAdd:
			startActivityForResult(new Intent(this, FilterAddActivity.class), 
					FilterAddActivity.REQ_CODE);
			return true;
		// � ������� ������� ��� ������ ��� mark mode
		case R.id.itemFilterMarkMode:
			fAdapter.setMarkMode(true);
			invalidateOptionsMenu();
			return true;
		// ����������� ��������� ����������� �������
		case R.id.itemFilterDoRemove:
			YesNoDialog.show(this, 
					YESNO_ID_DELETE, 
					getText(R.string.title_confirmation), 
					getResources().getQuantityString(R.plurals.filters_to_delete, 
							fAdapter.getMarkedCount(), 
							fAdapter.getMarkedCount()), this);
			return true;
		/*
		 * � ������� �������� � ��� �������� ��� �� ������
		 * 
		 * �� ������� ���� ��� ���������� ������ ���� ��� ���������� ������ 
		 * ����������� ��� �� ������.
		 */
		case R.id.itemFilterSelect:
			fAdapter.setMarkAll(fAdapter.getMarkedCount() == 0);
			invalidateOptionsMenu();
			return true;
		// � ������� ������ ������� mark mode
		case R.id.itemFilterCancel:
			fAdapter.setMarkMode(false);
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
		if(fAdapter.getMarkMode()) {
			fAdapter.setMarkMode(false);
			invalidateOptionsMenu();
		}
		else
			super.onBackPressed();
	}	
	
	/***************************************************************************
	 * ��������� ���� ��� ������������� ������� ��� �� ��� ListView ���        *
	 *  Activity.                                                              *
	 *                                                                         *
	 * �� �� keyFocus ��� ����� null ���� ���������� ��� ListView ���          *
	 *  ����� Item ��� ������ �� ������� (������) ������� �� �� �������������  *
	 * keyFocus.                                                               *
	 *                                                                         *
	 * � ��������� ��� ListView ������� ��������� �� ��������� Thread ���� ��  *
	 * ��� ���������� ��� �������� ��� UI.                                     *
	 ***************************************************************************/
	private void listFilters(final String keyFocus) {
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
				// ��������� ���� ��� ������� ��� ��
				return dbManager.listFilters(context);
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
					/* 
					 * ��������� ��� ArrayAdapter �� �� ������������ ������ ���
					 *  ����������� ��� ������ �� ������� (������) �� keyFocus
					 */
					fAdapter.setFilterList((ArrayList<FilterItem>)r, keyFocus);
					
					// �������� � �������� ��� �������� "����� ListView"
					((TextView)findViewById(R.id.textViewFiltersEmpty))
						.setVisibility(fAdapter.isEmpty() ? View.VISIBLE: View.GONE);					
					// ��������� ��� �������� ������� ������������� �������
					((TextView)findViewById(R.id.textViewFilterCounter))
						.setText(!fAdapter.isEmpty() ? 
								getResources().getQuantityString(R.plurals.filter_counter, 
										fAdapter.getCount(), fAdapter.getCount()): "");
					// ����� ��� mark mode
					fAdapter.setMarkMode(false);
					// ��������� ��� Menu ��� Activity
					invalidateOptionsMenu();
				}
			}
		}.execute();
	}
	/***************************************************************************
	 * � ������ ��� �������� ����������� ����� ����������� �� ������ ������    *
	 ***************************************************************************/
	private void listFilters() { 
		listFilters(null);
	}
	
	/***************************************************************************
	 * �������� ��� ��� �� ���� ��� ������������� �������                      *
	 *                                                                         *
	 * � �������� ������� ��������� �� ��������� Thread ���� �� ��� ���������� *
	 * ��� �������� ��� UI.                                                    *
	 ***************************************************************************/
	private void deleteFilters() { 
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
				// �������� ���� ��� ������������� ������� ��� ListView
				try { 
					for(int position = 0; position < fAdapter.getCount(); position++) { 
						final FilterItem item = fAdapter.getItem(position);
						
						if(item.mark) // ������������;
							if(!dbManager.deleteFilter(item))
								throw new Exception(context.getString(R.string.error_filter_mass_delete) + item.token);						
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
				listFilters();
			}
		}.execute();
	}
	
	/***************************************************************************
	 * ������������ � �������������� (enable) ��� ������� ��� �� ���� ����     * 
	 *  position                                                               *
	 *                                                                         *
	 * � ������������ � �������������� ������� ��������� �� ��������� Thread   *
	 * ���� �� ��� ���������� ��� �������� ��� UI.                             * 
	 ***************************************************************************/
	public void enableFilter(final int position, final boolean enable) {
		final Context context = this;
		
		new AsyncTask<Void, Void, Object>() { 
			@Override
			protected void onPreExecute() { 
				if(pDlg != null) { 
					pDlg.dismiss();
					pDlg = null;
				}
				pDlg = ProgressDialog.show(context, "", getText(R.string.msg_wait));
			}
			@Override
			protected Object doInBackground(Void... args) {
				try { 					
					if(!dbManager.setFilterActive(fAdapter.getItem(position), enable))
						throw new Exception();
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
				
				if(r instanceof Exception) { 
					CenterToast.makeText(context, 
							enable ? R.string.error_filter_set_enable: R.string.error_filter_set_disable, 
							Toast.LENGTH_SHORT).show();
				}
				else { 
					// ��������� ���������� �������
					fAdapter.setFilterEnable(position, enable);
				}
			}
		}.execute();
	}
	
	/***************************************************************************
	 * ClickListener 				���������� ��� �������� ��� Activity       *
	 ***************************************************************************/		  
	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		/*
		 * ������������ � �������������� �������, � ���� ��� ������� ��� 
		 *  ListView (& fAdapter) ������� ��� �� tag ��� View �� ����� Integer.
		 */
		case R.id.imageViewFilterActive:
			enableFilter((Integer)view.getTag(), 
					!fAdapter.isFilterEnabled((Integer)view.getTag()));
			return;
		// ���������� � ������������ ��������� �������
		case R.id.ImageViewFilterMark:
			fAdapter.setFilterMark((Integer)view.getTag(), 
					!fAdapter.isFilterMarked((Integer)view.getTag()));
			invalidateOptionsMenu();
			return;
		}
	}	
	
	/***************************************************************************
	 * � ������� ������ ������ ������, ������ ������������ ��� ����� ��� ��    *
	 *  ��� �������� �� edit mode!                                             *
	 ***************************************************************************/
	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		if(!fAdapter.getMarkMode()) { 		
			final FilterItem fItem = fAdapter.getItem(position);
			
			startActivityForResult(new Intent(this, FilterAddActivity.class)
				.putExtra(FilterAddActivity.EXTRA_EDIT    , true)
				.putExtra(FilterAddActivity.EXTRA_TOKEN   , fItem.token)
				.putExtra(FilterAddActivity.EXTRA_TOKENPOS, fItem.matchingType)
				.putExtra(FilterAddActivity.EXTRA_PAYLOAD , fItem.payload)
				.putExtra(FilterAddActivity.EXTRA_ACTIVE  , fItem.active),				
				FilterAddActivity.REQ_CODE);
		}
	}
	

	/***************************************************************************
	 * YesNoListener 			����������� � �������� ���������� Activity	   *
	 ***************************************************************************/
	@Override
	public void OnYesNoListener(int id, int which) {
		switch(id) { 
		case YESNO_ID_DELETE:	// �������� ����������� �������;
			if(which == DialogInterface.BUTTON_POSITIVE)
				deleteFilters();
			return;
		default: return;
		}
	}	
	
	/***************************************************************************
	 *FilterAdapter   Subclass ��� ArrayAdapter ���� �� ������������� ������   * 	
	 *                ���������� FilterItem                                    *
	 ***************************************************************************/
	private class FilterAdapter extends ArrayAdapter<FilterItem> {
		private final ListView owner;
		private final OnClickListener externClickListener;
		
		private boolean markMode = false;
		private int markedCount = 0;
		
		/*
		 * ctor
		 * 	��� �������� �� Context ��� ������������, �� ListView (owner) ��� 
		 *   ����� �� ��������� �� ������������ ��� � ArrayAdapter ��� ���    
		 *  OnClickListener �� ����� �� ������������ ���� � ������� �������� 
		 *   - ���������� � ����������� - ������������� ������ ������. 
		 */
		public FilterAdapter(final ListView owner, 
				final OnClickListener externClickListener) {
			super(owner.getContext(), R.layout.item_filter);
			
			(this.owner = owner).setAdapter(this);
			this.externClickListener = externClickListener;
		}
		
		// ������ ��� ������� ��� ������������� ������� ���� 1
		private void increaseMarkedCount() { 
			if(++markedCount > getCount())
				markedCount = getCount();
		}
		// ������ ��� ������� ��� ������������� ������� ���� 1
		private void decreaseMarkedCount() { 
			if(--markedCount < 0)
				markedCount = 0;
		}
		// ��������� ��� ������� ��� ������������� �������
		public int getMarkedCount() { 
			return markedCount;
		}
		
		/*
		 * �� mark ��� �� true ���� ���������� ���� ��� ������� ������
		 *  ������������ ����.
		 * 
		 * �� �� FilterAdapter ����� �� mark mode ���� � ��������� ����������
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
		 * ������� ��� ������� ��� ArrayList<FilterItem> ��� ListView ��� 
		 *  ����������� ��� ������ �� ������� (������) ��� �� keyFocus �����
		 * �� �� keyFocus ������� �� null ����� ���������.
		 * 
		 * �� �� list ����� null ���� �� ����������� ��� ListView ���� 
		 *  �����������.
		 */
		public void setFilterList(final ArrayList<FilterItem> list, 
				final String keyFocus) {			
			clear();
			
			if(list != null)			
				addAll(list);
			
			if(isEmpty())
				setMarkMode(false);
			
			if(keyFocus != null) { 
				for(int position = 0; position < getCount(); position++) {
					if(keyFocus.equalsIgnoreCase(getItem(position).token)) {						
						owner.setSelection(position);
						break;
					}
				}
			}
		}
		
		/*
		 * ��������� true �� �� ������ ���� ���� position ��� ListView �����
		 *  �������������� ������ false.
		 */
		public boolean isFilterEnabled(final int position) { 
			return getItem(position).active;
		}
		/*
		 * ������������ � �������������� (enable) ��� ������� ���� ���� position 
		 *  ��� ListView.
		 * 
		 * � ��������� ���������� ��� ������ ��� (enable).
		 */
		public boolean setFilterEnable(final int position, final boolean enable) { 
			getItem(position).active = enable;
			notifyDataSetChanged();
			return enable;
		}
		
		/*
		 * ������������ � �������������� ��� ������ ������������� (mark mode)
		 *  ��� FilterAdapter.
		 * 
		 * ���� �� mark mode ����� ������, � ������� ������ �� �������� � �� 
		 *  ���������� ������ ��� ListView ��� �� control ������������� � 
		 * ��������������� ������� ������������.
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
		 * ��������� true �� �� ������ ���� ���� position ��� ListView �����
		 *  ������������ ������ false.
		 */
		public boolean isFilterMarked(final int position) { 
			return getItem(position).mark;
		}
		/*
		 * ���������� � ������������ (mark) ��� ������� ���� ���� position 
		 *  ��� ListView.
		 * 
		 * � ��������� ���������� ��� ������ ��� (mark).
		 */
		public boolean setFilterMark(final int position, final boolean mark) { 
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
		 * ��� https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) { 
			final View view;
			
			final ImageView imgState,
							imgMark;
			
			final TextView txtToken,
						   txtScanner,
						   txtPayloadRemove,
						   txtPayloadStore,
						   txtPayloadDrive,
						   txtPayloadNothing;
			
			/* 
			 * ������������ ����� View-Holder ��� ������ ��������� ��� ListView
			 * 
			 * ��. https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
			 */
			if(convertView == null) { 
				view = View.inflate(getContext(), R.layout.item_filter, null);
				
				imgState   = (ImageView)view.findViewById(R.id.imageViewFilterActive);
				imgState.setOnClickListener(externClickListener);
				imgMark  = (ImageView)view.findViewById(R.id.ImageViewFilterMark);
				imgMark.setOnClickListener(externClickListener);
				
				txtToken   = (TextView)view.findViewById(R.id.textViewFilterToken);
				
				txtScanner = (TextView)view.findViewById(R.id.textViewFilterScanner);				
				txtPayloadRemove = (TextView)view.findViewById(R.id.textViewPLRemove);
				txtPayloadStore  = (TextView)view.findViewById(R.id.textViewPLStore);
				txtPayloadDrive  = (TextView)view.findViewById(R.id.TextViewPLDrive);
				
				txtPayloadNothing = (TextView)view.findViewById(R.id.TextViewPLNothing);
				
				view.setTag(R.id.imageViewFilterActive, imgState);
				view.setTag(R.id.ImageViewFilterMark  , imgMark);
				
				view.setTag(R.id.textViewFilterToken  , txtToken);
				view.setTag(R.id.textViewFilterScanner, txtScanner);
				view.setTag(R.id.textViewPLRemove , txtPayloadRemove);
				view.setTag(R.id.textViewPLStore  , txtPayloadStore);
				view.setTag(R.id.TextViewPLDrive  , txtPayloadDrive);				
				view.setTag(R.id.TextViewPLNothing, txtPayloadNothing);
			}
			else { 
				view = convertView;
				
				imgState   = (ImageView)view.getTag(R.id.imageViewFilterActive);
				imgMark  = (ImageView)view.getTag(R.id.ImageViewFilterMark);
				
				txtToken   = (TextView)view.getTag(R.id.textViewFilterToken);
				txtScanner = (TextView)view.getTag(R.id.textViewFilterScanner);
				txtPayloadRemove = (TextView)view.getTag(R.id.textViewPLRemove);
				txtPayloadStore	 = (TextView)view.getTag(R.id.textViewPLStore);
				txtPayloadDrive  = (TextView)view.getTag(R.id.TextViewPLDrive);				
				txtPayloadNothing= (TextView)view.getTag(R.id.TextViewPLNothing);
			}
			
			// �������� ���� ������� ������� 
			final FilterItem item = getItem(position);
			
			// ��������� ��� ������ � �������� ������;
			imgState.setImageResource(item.active ? 
					android.R.drawable.checkbox_on_background:
						android.R.drawable.checkbox_off_background);
			imgState.setTag(position);			
			
			// ��������� ��� ������������ ������ � ���; 
			imgMark.setImageResource(item.mark ? 
					android.R.drawable.checkbox_on_background:
						android.R.drawable.checkbox_off_background);
			imgMark.setTag(position);
			
			/*
			 * �� �� ��������� mark ���� �������� ��� �������� ������� ������
			 *  ��� �������� ��� �������� ������������� �������
			 */
			if(markMode) { 
				imgMark.setVisibility(View.VISIBLE);
				imgState.setVisibility(View.INVISIBLE);
			}
			else { 
				imgMark.setVisibility(View.INVISIBLE);
				imgState.setVisibility(View.VISIBLE);				
			}
			
			// ������� ��� ���������� ��������������
			txtToken.setText(item.token);
			// ������� ��� ����� ��� ���������� ��������������
			txtScanner.setText(item.getMatchingTypeStr(getContext()));
						
			/*
			 * ������� ��� ������� ���� ������ �� ������ �� ������ ���� � 
			 *  ���������� ������������� ����� ������� 
			 */
			txtPayloadRemove.setVisibility(
					0 != (item.payload & FilterItem.PAYLOAD_REMOVE) ? 
							View.VISIBLE: View.GONE);
			txtPayloadStore.setVisibility(
					0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_DB) ? 
							View.VISIBLE: View.GONE);
			
			if(0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD) 
			|| 0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM)
			|| 0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI)) { 
				txtPayloadDrive.setVisibility(View.VISIBLE);
				
				// ��������� ��� Google Drive �� ������� ����������� �������� ���������
				if(0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD))
					txtPayloadDrive.setText(R.string.pl_copy_gd_wifi_gsm);
				// ��������� ��� Google Drive �� ������� �������� ��������� ������� (��. GSM,3G ���)
				if(0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_GSM))
					txtPayloadDrive.setText(R.string.pl_copy_gd_gsm);
				// ��������� ��� Google Drive �� ������� �������� ��������� 802.11 (WiFi)
				if(0 != (item.payload & FilterItem.PAYLOAD_COPY_TO_GD_OVER_WIFI))
					txtPayloadDrive.setText(R.string.pl_copy_gd_wifi);
			}
			else// � ������� ��� ���� �������� ����� ��� Google Drive
				txtPayloadDrive.setVisibility(View.GONE);
			
			// ������� ��� ��� ������� ������ Payload ��� �� ������!
			txtPayloadNothing.setVisibility(item.payload == 0 ? View.VISIBLE: View.GONE);
			
			// � ������ ��� (�����) ��� �������
			return view;
		}
	}
}
