/******************************************************************************* 
 * ����� ��� �������� �� ������ ��� ������������                               *
 *******************************************************************************/
package com.example.notification;

import android.content.Context;

public class FilterItem {
							 // �� �������� ����
	public static final byte UNDEFINED = 0,
							 
							 // ��������� �������� ���� ������ ��� �����������
							 MATCHING_TYPE_START = 1,
							 // ��������� �������� ���������� ���� �����������
							 MATCHING_TYPE_END   = 2,
							 // ��������� �������� ���� ���� ��� �����������
							 MATCHING_TYPE_ANY   = 3;
		
							 // �������� �����������	   ** bitwise **
	public static final int  PAYLOAD_REMOVE      		   = 0x01,
							 // ��������� ����������� ���� ������ �� ��� ������������
							 PAYLOAD_COPY_TO_DB	 		   = 0x02,
							 // ��������� ����������� ��� Google Drive ���� ������� ������� WiFi
							 PAYLOAD_COPY_TO_GD_OVER_WIFI  = 0x04,
							 // ��������� ����������� ��� Google Drive ���� ������� ������� GSM/3G
							 PAYLOAD_COPY_TO_GD_OVER_GSM   = 0x08,
							 // ��������� ����������� ��� Google Drive ���� ������� ������� WiFi � GSM/3G
							 PAYLOAD_COPY_TO_GD            = 0x10;							 
	
	// �� ���� ��������� �������
	public String token;					 	
	// ��� ���� ������� ��� �������� ��� ����������� ��� ����������� 
	public byte matchingType;
	// �� ���������� ���� ������ �� �������
	public int payload;
	
	// �� true �� ������ ����� ������ ��� ������ �� ���������� ��� �� NotificationMonitor
	public boolean active,
	// �� true �� ������ ����� ������������
				   mark;
	
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 * ��� �������� �� Context ��� ������������, �� ���� ������ ������� ����   *
	 *  ����������, ��� ���� ������� ��� �������� ���� ����������(matchingType)*
	 * ��� �� ���������� ���� ������ �� �������                                *
	 ***************************************************************************/
	public FilterItem(final String token, 
			final byte matchingType, 
			final int payload) {		
		this.token = token;
		this.matchingType = matchingType;
		this.payload = payload;
	}
	// ctor ���� �������� ���� �� ���� ������ �������
	public FilterItem(final String token) { 
		this(token, UNDEFINED, UNDEFINED);
	}
	// ctor ����� FilterItem
	public FilterItem() { 
		this("", UNDEFINED, UNDEFINED);
	}
	
	/***************************************************************************
	 * ��������� �� ������������� ����� ��� ��������� matchingType             *
	 ***************************************************************************/
	public CharSequence getMatchingTypeStr(final Context context) {
		switch(matchingType) { 
		case MATCHING_TYPE_START:
			return context.getText(R.string.mt_start);
		case MATCHING_TYPE_END:
			return context.getText(R.string.mt_end);
		case MATCHING_TYPE_ANY:
			return context.getText(R.string.mt_any);
		default:
			return context.getText(R.string.unknown);
		}
	}
}
