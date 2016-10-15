/******************************************************************************* 
 * ����� ��� �������� ��� ������������ ��� ��������� ��� ������������          *
 *******************************************************************************/ 
package com.example.notification;

import java.text.DateFormat;
import java.util.Date;

import android.app.Notification;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Pair;

public class NotificationItem {
	// �� �������� ������ ��� ������� 
	public static final int UNDEFINED = -1;	
	// �� �������� ������ ��� ����������� ���� �� ��� 
	final public int pk;
	// �� �������� ������ �� ������������� ���� (��� ����� ����� �� SQLite Queries)
	final public String pkStr,
	// � �������� ��� �������� ��� ����������
						app,
	// � ������ ��� �����������
						title,
	// � ����������� ������ ��� �����������
						titleBig,
	// �� ����������� ��� �����������
						message,
	// �� ��������� ����������� ��� ����������� ��� ���� 
						ticker;
	// � ���������� & ��� ��� ������ � ���������� ��� ��� �������� NotificationMonitor
	final public long clock;
	
	// �� true �� ������ ����� ������������
	public boolean mark = false;
	
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  ��� �������� �� �������� ������ ��� ����������� (pk) ���� �� ���,      *
	 *   ��� �������� ��� �������� ��� ���������� (app), �� ����������� ���    *
	 *  ����������� (message) ��� ���������� & ��� ��� ������ ��� �� ���������.*
	 ***************************************************************************/
	public NotificationItem(final int pk, 
			final String app,
			final String title,
			final String titleBig,
			final String message,
			final String ticker,
			final long clock) { 
		this.pk = pk;
		this.pkStr = String.valueOf(pk);
		this.app = app;
		this.title = title;
		this.titleBig = titleBig;
		this.message = message;
		this.ticker = ticker;
		this.clock = clock;
	}
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  ���� � �������� ctor �� ��� ������� ��� �� �������� ������ (pk)        *
	 *   �������� � ���� UNDEFINED.                                            *
	 ***************************************************************************/
	public NotificationItem(final String app,
			final String title,
			final String titleBig,
			final String message,
			final String ticker,
			final long clock) { 
		this(UNDEFINED, app, title, titleBig, message, ticker, clock);
	}
	/***************************************************************************
	 * ctor                                                                    *
	 *                                                                         *
	 *  �� ����� ��� ���������� ��� NotificationItem ��������� ��� ��          *
	 *   ����������� ��� ������ ������� "StatusBarNotification"                *
	 ***************************************************************************/
	public NotificationItem(final StatusBarNotification sbn) { 
		final Notification notif = sbn.getNotification();
		final Bundle extras = sbn.getNotification().extras;
		
		pk       = UNDEFINED;
		pkStr	 = String.valueOf(pk);
		app      = sbn.getPackageName();
		title    = extras.getString(Notification.EXTRA_TITLE, "");
		titleBig = extras.getString(Notification.EXTRA_TITLE_BIG, "");
		message  = extras.getString(Notification.EXTRA_TEXT, "");
		ticker   = notif.tickerText != null ? notif.tickerText.toString(): "";
		clock    = sbn.getPostTime();
	}

	/***************************************************************************
	 * ��������� ��� ������ ��� �����������.                                   *
	 *                                                                         *
	 * �� � ���������� ���� ���������� ���� ��� ������� �����, ���� � ���������*
	 *  ���������� ��� ���������� �����.                                       *
	 ***************************************************************************/
	public String getTitle() { 
		return !titleBig.isEmpty() ? titleBig: title; 
	}
	/***************************************************************************
	 * ��������� ��� ��������� ��� ��� ���������� ��� ��������� ��� ��������   *
	 *  ��� ����������.                                                        *
	 *                                                                         *
	 * �� �� OS ��� ���������� ����������� � �������� ���������, ���� ��       *
	 *  �������� � ��������� ���������� ��� ��������� �������� ��� ������� ��� *
	 * ����� ������ � ���������� (��. com.foo.foo_application ���� Foo-App)    *
	 *                                                                         *
	 * �� ���� ������ ��� ��� �� ���������, �� ��� ������� ������������ null   *
	 ***************************************************************************/
	public Pair<String, Drawable> getAppTitleIcon(final PackageManager packMan) { 
		try { 
			final String sAppLabel = packMan.getApplicationLabel(
					packMan.getApplicationInfo(app, 0))
					.toString();
			
			return Pair.create(sAppLabel != null ? sAppLabel: app, 
					packMan.getApplicationIcon(app));
		} catch(PackageManager.NameNotFoundException e) { 
			e.printStackTrace();			
		}
		return Pair.create(app, null);
	}
	
	/***************************************************************************
	 * ��������� ��� ����������� & ���� ��������� ��� ����������� ��           * 
	 *  ������������� �����.                                                   *
	 ***************************************************************************/
	public String getClockStr() { 
		return DateFormat.getDateTimeInstance().format(new Date(clock));
	}
}
