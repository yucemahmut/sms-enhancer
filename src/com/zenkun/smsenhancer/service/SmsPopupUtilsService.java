package com.zenkun.smsenhancer.service;

import java.util.ArrayList;

import com.zenkun.smsenhancer.BuildConfig;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.zenkun.smsenhancer.provider.SmsMmsMessage;
import com.zenkun.smsenhancer.provider.SmsPopupContract.ContactNotifications;
import com.zenkun.smsenhancer.util.Log;
import com.zenkun.smsenhancer.util.ManageNotification;
import com.zenkun.smsenhancer.util.SmsPopupUtils;
import com.zenkun.smsenhancer.util.SmsPopupUtils.ContactIdentification;

public class SmsPopupUtilsService extends WakefulIntentService {
    private static final String TAG = SmsPopupUtilsService.class.getName();

    public static final String ACTION_MARK_THREAD_READ =
            "com.zenkun.smsenhancer.ACTION_MARK_THREAD_READ";
    public static final String ACTION_MARK_MESSAGE_READ =
            "com.zenkun.smsenhancer.ACTION_MARK_MESSAGE_READ";
    public static final String ACTION_DELETE_MESSAGE =
            "com.zenkun.smsenhancer.ACTION_DELETE_MESSAGE";
    public static final String ACTION_UPDATE_NOTIFICATION =
            "com.zenkun.smsenhancer.ACTION_UPDATE_NOTIFICATION";
    public static final String ACTION_QUICKREPLY =
            "com.zenkun.smsenhancer.ACTION_QUICKREPLY";
    public static final String ACTION_SYNC_CONTACT_NAMES =
            "com.zenkun.smsenhancer.ACTION_SYNC_CONTACT_NAMES";

    public SmsPopupUtilsService() {
        super(TAG);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.commonsware.cwac.wakeful.WakefulIntentService#doWakefulWork(android
     * .content.Intent)
     */
    @Override
    protected void doWakefulWork(Intent intent) {
        if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: doWakefulWork()");

        final String action = intent.getAction();

        if (ACTION_MARK_THREAD_READ.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Marking thread read");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.setThreadRead();
        } else if (ACTION_MARK_MESSAGE_READ.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Marking message read");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.setMessageRead();
        } else if (ACTION_DELETE_MESSAGE.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Deleting message");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.delete();
        } else if (ACTION_QUICKREPLY.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Quick Reply to message");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.replyToMessage(intent.getStringExtra(SmsMmsMessage.EXTRAS_QUICKREPLY));
        } else if (ACTION_UPDATE_NOTIFICATION.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Updating notification");
            updateNotification(intent);
        } else if (ACTION_SYNC_CONTACT_NAMES.equals(action)) {
        	if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Sync'ing contact names");
        	syncContactNames(this);
        }
    }

    /**
     * Any custom contact notifications are stored in a local database, including the contact names
     * so we can quickly display them on the configuration screens. This function will loop through
     * the locally stored contacts and check to see if the system contact name has changed at all
     * (from either a manual edit or some sort of sync event). If so, it will update the local
     * database with the new name.
     * @param context Context.
     * @return The number of rows updated with a new name.
     */
    private int syncContactNames(Context context) {

    	final ContentResolver contentResolver = context.getContentResolver();
        final Cursor cursor = contentResolver.query(
                ContactNotifications.CONTENT_URI, null, null, null, null);

        if (cursor == null) {
        	return 0;
        }

        if (cursor.getCount() == 0) {
        	return 0;
        }

        int count = 0;
        int updatedCount = 0;
        String id;
        String contactName;
        String contactLookup;
        String contactId;

        // loop through the local sms popup contact notifications table
        while (cursor.moveToNext()) {
            count++;

            id = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications._ID));
            contactName = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_NAME));
            contactId = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_ID));
            contactLookup = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_LOOKUPKEY));

            ContactIdentification contactInfo =
                    SmsPopupUtils.getPersonNameByLookup(context, contactLookup, contactId);

            if (contactInfo != null) {
                boolean runUpdate = false;
            	ContentValues vals = new ContentValues();

            	if (contactName == null || !contactName.equals(contactInfo.contactName)) {
            	    vals.put(ContactNotifications.CONTACT_NAME, contactInfo.contactName);
            	    runUpdate = true;
            	}

            	if (contactId == null || !contactId.equals(contactInfo.contactId)) {
            	    vals.put(ContactNotifications.CONTACT_ID, contactInfo.contactId);
            	    runUpdate = true;
            	}

            	if (contactLookup == null || !contactLookup.equals(contactInfo.contactLookup)) {
            	    vals.put(ContactNotifications.CONTACT_LOOKUPKEY, contactInfo.contactLookup);
            	    runUpdate = true;
            	}

            	if (runUpdate && 1 == contentResolver.update(
            			ContactNotifications.buildContactUri(id), vals, null, null)) {
            		updatedCount++;
            	}
            }
        }

        if (cursor != null) {
        	cursor.close();
        }

        if (BuildConfig.DEBUG)
        	Log.v("Sync Contacts: " + updatedCount + " / " + count);

        return updatedCount;
    }

    public static void startSyncContactNames(Context context) {
        Intent i = new Intent(context, SmsPopupUtilsService.class);
        i.setAction(SmsPopupUtilsService.ACTION_SYNC_CONTACT_NAMES);
        WakefulIntentService.sendWakefulWork(context, i);
    }

    private void updateNotification(Intent intent) {
        // In the case the user is "replying" to the message (ie. starting an
        // external intent) we need to ignore all messages in the thread when
        // calculating the unread messages to show in the status notification
        boolean ignoreThread = intent.getBooleanExtra(SmsMmsMessage.EXTRAS_REPLYING, false);

        long threadId = 0;
        if (ignoreThread) {
            // If ignoring messages from the thread, pass the full message over
            final SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            threadId = message.getThreadId();
        }

        // Get the most recent message + total message counts
        final ArrayList<SmsMmsMessage> messages = SmsPopupUtils.getUnreadMessages(this);

        if (messages != null) {
            if (threadId > 0) {
                for (int i=0; i<messages.size(); i++) {
                    if (messages.get(i).getThreadId() == threadId) {
                        messages.remove(i);
                    }
                }
            }
            final int numMessages = messages.size();

            if (numMessages > 0) {
            // Update the notification in the status bar
            ManageNotification.update(this, messages.get(numMessages - 1), numMessages);
            } else {
                ManageNotification.clearAll(this);
            }
        } else {
            ManageNotification.clearAll(this);
        }
    }

}
