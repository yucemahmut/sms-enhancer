package com.zenkun.smsenhancer.provider;

import java.util.ArrayList;

import com.zenkun.smsenhancer.ui.SmsPopupActivity;
import com.zenkun.smsenhancer.util.Log;
import com.zenkun.smsenhancer.util.SmsMessageSender;
import com.zenkun.smsenhancer.util.SmsPopupUtils;
import com.zenkun.smsenhancer.util.SmsPopupUtils.ContactIdentification;

import com.zenkun.smsenhancer.BuildConfig;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.SmsMessage.MessageClass;
import android.text.format.DateUtils;

public class SmsMmsMessage {
    // Private EXTRAS strings
    private static final String PREFIX = "com.zenkun.smsenhancer.";
    private static final String EXTRAS_FROM_ADDRESS = PREFIX + "EXTRAS_FROM_ADDRESS";
    private static final String EXTRAS_MESSAGE_BODY = PREFIX + "EXTRAS_MESSAGE_BODY";
    private static final String EXTRAS_TIMESTAMP = PREFIX + "EXTRAS_TIMESTAMP";
    private static final String EXTRAS_UNREAD_COUNT = PREFIX + "EXTRAS_UNREAD_COUNT";
    private static final String EXTRAS_THREAD_ID = PREFIX + "EXTRAS_THREAD_ID";
    private static final String EXTRAS_CONTACT_ID = PREFIX + "EXTRAS_CONTACT_ID";
    private static final String EXTRAS_CONTACT_LOOKUP = PREFIX + "EXTRAS_CONTACT_LOOKUP";
    private static final String EXTRAS_CONTACT_NAME = PREFIX + "EXTRAS_CONTACT_NAME";
    private static final String EXTRAS_MESSAGE_TYPE = PREFIX + "EXTRAS_MESSAGE_TYPE";
    private static final String EXTRAS_MESSAGE_ID = PREFIX + "EXTRAS_MESSAGE_ID";
    private static final String EXTRAS_EMAIL_GATEWAY = PREFIX + "EXTRAS_EMAIL_GATEWAY";
    //extra sms enhancer
    private static final String EXTRAS_ENABLECUSTOMMSG  = PREFIX + "EXTRAS_KEYWORDSEXISTS";
    private static final String EXTRAS_HTCSECURE  = PREFIX + "EXTRAS_HTCSECURE";
    private static final String EXTRAS_ENABLEKEYWORDS  = PREFIX + "EXTRAS_ENABLEKEYWORDS";
    private static final String EXTRAS_KEYWORDSLIST  = PREFIX + "EXTRAS_KEYWORDSLIST";
    private static final String EXTRAS_KEYWORDSEXISTS = PREFIX + "EXTRAS_KEYWORDSEXISTS";
    private static final String EXTRAS_KEYWORDSNOTIFICATION = PREFIX + "EXTRAS_KEYWORDSNOTIFICATION";
    private static final String EXTRAS_KEYWORDSPOPUP = PREFIX + "EXTRAS_KEYWORDSPOPUP";
    private static final String EXTRAS_MESSAGEBODYORIGINAL  = PREFIX + "EXTRAS_MESSAGEBODYORIGINAL";
    private static final String EXTRAS_KEYWORDSDEFAULT  = PREFIX + "EXTRAS_KEYWORDSDEFAULT";
    

    // Public EXTRAS strings
    public static final String EXTRAS_NOTIFY = PREFIX + "EXTRAS_NOTIFY";
    public static final String EXTRAS_REMINDER_COUNT = PREFIX + "EXTRAS_REMINDER_COUNT";
    public static final String EXTRAS_REPLYING = PREFIX + "EXTRAS_REPLYING";
    public static final String EXTRAS_QUICKREPLY = PREFIX + "EXTRAS_QUICKREPLY";

    // Message types
    public static final int MESSAGE_TYPE_SMS = 0;
    public static final int MESSAGE_TYPE_MMS = 1;
    public static final int MESSAGE_TYPE_MESSAGE = 2;

    // Timestamp compare buffer for incoming messages
    public static final int MESSAGE_COMPARE_TIME_BUFFER = 5000; // 5 seconds

    // Main message object private vars
    private Context context;
    private String fromAddress = null;
    private String messageBody = null;
    private long timestamp = 0;
    private int unreadCount = 0;
    private long threadId = 0;
    private String contactId = null;
    private String contactLookupKey = null;
    private String contactName = null;
    private int messageType = 0;
    private boolean notify = true;
    private int reminderCount = 0;
    private long messageId = 0;
    private boolean fromEmailGateway = false;
    private MessageClass messageClass = null;
    private String replyText = "";
    private String messageBodyOriginal=null;
    private boolean EnableCustomMsg=false;
    private boolean HtcSecure=false;
    private boolean EnableKeywords=false;
    private String[] Keywordlist=null;
    private boolean KeywordExists=false;
    private boolean KeywordNotification = false;
    private boolean KeywordPopup = false;
    private boolean DefaultKeywords =false;

    // Sprint vars to check for special voicemail messages
    private static final String SPRINT_BRAND = "sprint";
    private static final String SPRINT_VOICEMAIL_PREFIX = "//ANDROID:";

    /**
     * Construct SmsMmsMessage given a raw message (created from pdu), used for
     * when a message is initially received via the network.
     */
    public SmsMmsMessage(Context _context, SmsMessage[] messages, long _timestamp) {
        SmsMessage sms = messages[0];

        context = _context;
        timestamp = _timestamp;
        messageType = MESSAGE_TYPE_SMS;

        /*
         * Fetch data from raw SMS
         */
        fromAddress = sms.getDisplayOriginatingAddress();
        fromEmailGateway = sms.isEmail();
        messageClass = sms.getMessageClass();

        String body = "";

        try {
            if (messages.length == 1 || sms.isReplace()) {
                body = sms.getDisplayMessageBody();
            } else {
                StringBuilder bodyText = new StringBuilder();
                for (int i = 0; i < messages.length; i++) {
                    bodyText.append(messages[i].getMessageBody());
                }
                body = bodyText.toString();
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.v("SmsMmsMessage<init> exception: " + e.toString());
        }
        messageBody = body;

        /*
         * Lookup the rest of the info from the system db
         */

        ContactIdentification contactIdentify = null;

        // If this SMS is from an email gateway then lookup contactId by email
        // address
        if (fromEmailGateway) {
            if (BuildConfig.DEBUG) Log.v("Sms came from email gateway");
            contactIdentify = SmsPopupUtils.getPersonIdFromEmail(context, fromAddress);
            contactName = fromAddress;
        } else { // Else lookup contactId by phone number
            if (BuildConfig.DEBUG) Log.v("Sms did NOT come from email gateway");
            contactIdentify = SmsPopupUtils.getPersonIdFromPhoneNumber(context, fromAddress);
            contactName = PhoneNumberUtils.formatNumber(fromAddress);
        }

        if (contactIdentify != null) {
            contactId = contactIdentify.contactId;
            contactLookupKey = contactIdentify.contactLookup;
            contactName = contactIdentify.contactName;
        }

        final long smscTimestamp = sms.getTimestampMillis();

        SmsPopupUtils.updateSmscTimestampDrift(context, timestamp, smscTimestamp);

        // We need to find the total unread message count here, however we have to deal with a race
        // condition. This message may or may not have already appeared in the system db. First we
        // find all unread messages in the system db, then look to see if this message is in that
        // list. If so, we have all unread messages, if not, we need to add one to the count.
        locateMessageId();
        unreadCount = 1;
        final ArrayList<SmsMmsMessage> unreadMessages = SmsPopupUtils.getUnreadMessages(context);
        if (unreadMessages != null) {
            final int size = unreadMessages.size();
            boolean found = false;
            for (int i=0; i<size; i++) {
                if (unreadMessages.get(i).messageId == messageId) {
                    found = true;
                }
            }
            unreadCount = found ? size : size + 1;
        }
    }

    /**
     * Construct SmsMmsMessage for getMmsDetails() - fetched from the MMS
     * database table
     */
    public SmsMmsMessage(Context _context, long _messageId, long _threadId, long _timestamp,
            String _messageBody, int _unreadCount, int _messageType) {

        context = _context;
        messageId = _messageId;
        threadId = _threadId;
        timestamp = _timestamp;
        messageBody = _messageBody;
        unreadCount = _unreadCount;
        messageType = _messageType;

        fromAddress = SmsPopupUtils.getMmsAddress(context, messageId);
        fromEmailGateway = false;

        contactName = PhoneNumberUtils.formatNumber(fromAddress);

        // Look up by phone number first
        ContactIdentification contactIdentify =
                SmsPopupUtils.getPersonIdFromPhoneNumber(context, fromAddress);

        if (contactIdentify == null) {
            // Lookup by email
            contactIdentify = SmsPopupUtils.getPersonIdFromEmail(context, fromAddress);
            if (contactIdentify != null) {
                // If found then set "from email" flag
                fromEmailGateway = true;
            }
        }

        // If a contact was found then set fields
        if (contactIdentify != null) {
            contactId = contactIdentify.contactId;
            contactLookupKey = contactIdentify.contactLookup;
            contactName = contactIdentify.contactName;
        }
    }

    /**
     * Construct SmsMmsMessage for getSmsDetails() - info fetched from the SMS
     * database table
     */
    public SmsMmsMessage(Context _context, String _fromAddress, String _messageBody,
            long _timestamp, long _threadId, int _unreadCount, long _messageId, int _messageType) {

        context = _context;
        fromAddress = _fromAddress;
        messageBody = _messageBody;
        timestamp = _timestamp;
        messageType = _messageType;

        ContactIdentification contactIdentify = null;

        contactName = PhoneNumberUtils.formatNumber(fromAddress);

        if (SmsPopupUtils.isEmailAddress(fromAddress)) {
            contactIdentify = SmsPopupUtils.getPersonIdFromEmail(context, fromAddress);
            fromEmailGateway = true;
        } else {
            contactIdentify = SmsPopupUtils.getPersonIdFromPhoneNumber(context, fromAddress);
            fromEmailGateway = false;
        }

        if (contactIdentify != null) {
            contactId = contactIdentify.contactId;
            contactLookupKey = contactIdentify.contactLookup;
            contactName = contactIdentify.contactName;
        }

        unreadCount = _unreadCount;
        threadId = _threadId;
        messageId = _messageId;
    }

    /**
     * Construct SmsMmsMessage from an extras bundle
     */
    public SmsMmsMessage(Context _context, Bundle b) {
        context = _context;
        fromAddress = b.getString(EXTRAS_FROM_ADDRESS);
        messageBody = b.getString(EXTRAS_MESSAGE_BODY);
        timestamp = b.getLong(EXTRAS_TIMESTAMP);
        contactId = b.getString(EXTRAS_CONTACT_ID);
        contactLookupKey = b.getString(EXTRAS_CONTACT_LOOKUP);
        contactName = b.getString(EXTRAS_CONTACT_NAME);
        unreadCount = b.getInt(EXTRAS_UNREAD_COUNT, 1);
        threadId = b.getLong(EXTRAS_THREAD_ID, 0);
        messageType = b.getInt(EXTRAS_MESSAGE_TYPE, MESSAGE_TYPE_SMS);
        notify = b.getBoolean(EXTRAS_NOTIFY, false);
        reminderCount = b.getInt(EXTRAS_REMINDER_COUNT, 0);
        messageId = b.getLong(EXTRAS_MESSAGE_ID, 0);
        fromEmailGateway = b.getBoolean(EXTRAS_EMAIL_GATEWAY, false);
        //sms enhancer
        EnableCustomMsg = b.getBoolean(EXTRAS_ENABLECUSTOMMSG,false);
        messageBodyOriginal =b.getString(EXTRAS_MESSAGEBODYORIGINAL);
        HtcSecure =b.getBoolean(EXTRAS_HTCSECURE);
        EnableKeywords =b.getBoolean(EXTRAS_ENABLEKEYWORDS);
        Keywordlist = b.getStringArray(EXTRAS_KEYWORDSLIST);
        KeywordExists = b.getBoolean(EXTRAS_KEYWORDSEXISTS, false);
        KeywordNotification =b.getBoolean(EXTRAS_KEYWORDSNOTIFICATION, false);
        KeywordPopup = b.getBoolean(EXTRAS_KEYWORDSPOPUP, false);
        DefaultKeywords = b.getBoolean(EXTRAS_KEYWORDSDEFAULT,false);
    }

    /**
     * Construct SmsMmsMessage by specifying all data, only used for testing the
     * notification from the preferences screen
     */
    public SmsMmsMessage(Context _context, String _fromAddress, String _messageBody,
            long _timestamp, String _contactId, String _contactLookup, String _contactName,
            int _unreadCount, long _threadId, int _messageType) {
        context = _context;
        fromAddress = _fromAddress;
        messageBody = _messageBody;
        timestamp = _timestamp;
        contactId = _contactId;
        contactLookupKey = _contactLookup;
        contactName = _contactName;
        unreadCount = _unreadCount;
        threadId = _threadId;
        messageType = _messageType;
    }

    /**
     * Convert all SmsMmsMessage data to an extras bundle to send via an intent
     */
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString(EXTRAS_FROM_ADDRESS, fromAddress);
        b.putString(EXTRAS_MESSAGE_BODY, messageBody);
        b.putLong(EXTRAS_TIMESTAMP, timestamp);
        b.putString(EXTRAS_CONTACT_ID, contactId);
        b.putString(EXTRAS_CONTACT_LOOKUP, contactLookupKey);
        b.putString(EXTRAS_CONTACT_NAME, contactName);
        b.putInt(EXTRAS_UNREAD_COUNT, unreadCount);
        b.putLong(EXTRAS_THREAD_ID, threadId);
        b.putInt(EXTRAS_MESSAGE_TYPE, messageType);
        b.putBoolean(EXTRAS_NOTIFY, notify);
        b.putInt(EXTRAS_REMINDER_COUNT, reminderCount);
        b.putLong(EXTRAS_MESSAGE_ID, messageId);
        b.putBoolean(EXTRAS_EMAIL_GATEWAY, fromEmailGateway);
        //sms enhancer
        b.putBoolean(EXTRAS_ENABLECUSTOMMSG, EnableCustomMsg);
        b.putString(EXTRAS_MESSAGEBODYORIGINAL, messageBodyOriginal);
        b.putBoolean(EXTRAS_HTCSECURE, HtcSecure);
        b.putBoolean(EXTRAS_ENABLEKEYWORDS, EnableKeywords);
        b.putStringArray( EXTRAS_KEYWORDSLIST, Keywordlist);
        b.putBoolean(EXTRAS_KEYWORDSEXISTS, KeywordExists);
        b.putBoolean(EXTRAS_KEYWORDSNOTIFICATION, KeywordNotification);
        b.putBoolean(EXTRAS_KEYWORDSPOPUP, KeywordPopup);
        b.putBoolean(EXTRAS_KEYWORDSDEFAULT, DefaultKeywords);
        return b;
    }

    public Intent getPopupIntent() {
        Intent popup = new Intent(context, SmsPopupActivity.class);
        popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        popup.putExtras(toBundle());
        return popup;
    }

    /**
     * Fetch the "reply to" message intent
     *
     * @param replyToThread whether or not to reply using the message threadId
     *        or using the sender address
     *
     * @return the intent to pass to startActivity()
     */
    public Intent getReplyIntent(boolean replyToThread) {
        if (messageType == MESSAGE_TYPE_MMS) {
            locateThreadId();
            return SmsPopupUtils.getSmsToIntent(context, threadId);
        } else if (messageType == MESSAGE_TYPE_SMS) {
            locateThreadId();
            /*
             * There are two ways to reply to a message, by "viewing" the
             * threadId or by sending a new message to the address. In most
             * cases we should just execute the former, but in some cases its
             * better to send a new message to an address (apps like Google
             * Voice will intercept this intent as they don't seem to look at
             * the threadId).
             */
            if (replyToThread && threadId > 0) {
                if (BuildConfig.DEBUG) Log.v("Replying by threadId: " + threadId);
                return SmsPopupUtils.getSmsToIntent(context, threadId);
            } else {
                if (BuildConfig.DEBUG) Log.v("Replying by address: " + fromAddress);
                return SmsPopupUtils.getSmsToIntent(context, fromAddress);
            }
        }
        return null;
    }

    public Intent getReplyIntent() {
        return getReplyIntent(true);
    }

    public void setThreadRead() {
        locateThreadId();
        SmsPopupUtils.setThreadRead(context, threadId);
    }

    public void setMessageRead() {
        locateMessageId();
        SmsPopupUtils.setMessageRead(context, messageId, messageType);
    }

    public void setUnreadCount(int _unreadCount) {
        unreadCount = _unreadCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setTimestamp(long newTimestamp) {
        timestamp = newTimestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void adjustTimestamp(long adjustment) {
        timestamp -= adjustment;
    }

    public MessageClass getMessageClass() {
        return messageClass;
    }

    public CharSequence getFormattedTimestamp() {
        CharSequence formattedTime;
        try {
            formattedTime = DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.v("Error formatting timestamp + " + timestamp);
            formattedTime = "";
        }
        return formattedTime;
    }

    public String getContactName() {
        if (contactName == null) {
            contactName = context.getString(android.R.string.unknownName);
        }
        return contactName;
    }

    public String getMessageBody() {
        if (messageBody == null) {
            messageBody = "";
        }
        return messageBody;
    }

    public int getMessageType() {
        return messageType;
    }

    public boolean isSms() {
        return messageType == MESSAGE_TYPE_SMS;
    }

    public boolean isMms() {
        return messageType == MESSAGE_TYPE_MMS;
    }

    public void setNotify(boolean mNotify) {
        notify = mNotify;
    }

    public boolean shouldNotify() {
        if (BuildConfig.DEBUG) Log.v("shouldNotify() - notify is " + notify);
        return notify;
    }

    public int getReminderCount() {
        return reminderCount;
    }

    public void updateReminderCount(int count) {
        reminderCount = count;
    }

    public void incrementReminderCount() {
        reminderCount++;
    }

    public void delete() {
        SmsPopupUtils.deleteMessage(context, getMessageId(), threadId, messageType);
    }

    public void locateThreadId() {
        if (threadId == 0) {
            threadId = SmsPopupUtils.findThreadIdFromAddress(context, fromAddress);
        }
    }

    public long getThreadId() {
        locateThreadId();
        return threadId;
    }

    public void locateMessageId() {
        if (messageId == 0) {
            if (threadId == 0) {
                locateThreadId();
            }
            /*if(EnableCustomMsg && messageBodyOriginal!=null)
            {
          	  messageBody = messageBodyOriginal;
            }*/
            messageId = SmsPopupUtils.findMessageId(
                    context, threadId, timestamp, messageBody, messageType);
        }
    }

    public long getMessageId() {
        locateMessageId();
        return messageId;
    }

    public String getContactId() {
        return contactId;
    }

    public String getContactLookupKey() {
        return contactLookupKey;
    }

    public Uri getContactLookupUri() {
        if (contactId == null) {
            return null;
        }

        return Uri.withAppendedPath(Contacts.CONTENT_URI, contactId);

        // This seems to fail even though the docs say to use it.
        // return Contacts.getLookupUri(Long.valueOf(contactId),
        // contactLookupKey);
    }

    public String getAddress() {
        return fromAddress;
    }

    public boolean isEmail() {
        return fromEmailGateway;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String text) {
        replyText = text;
    }

    /**
     * Sned a reply to this message
     *
     * @param quickreply the message to send
     * @return true of the message was sent, false otherwise
     */
    public boolean replyToMessage(String quickReply) {

        // Mark the message we're replying to as read
        setMessageRead();

        // Send new message
        SmsMessageSender sender =
                new SmsMessageSender(context, new String[] {fromAddress}, quickReply, getThreadId());

        return sender.sendMessage();
    }
    //sms enhancer properties
    public boolean isCustomMessage()
    {
    	return EnableCustomMsg;
    }
    public void isDefault(boolean def)
    {
    	DefaultKeywords = def;
    	}

    public boolean isDefault()
    {
    	return DefaultKeywords ;
    }
    public void isCustomMessage(boolean existe)
    {
    	EnableCustomMsg =existe;
    }
    public void MoveHtcSecure(boolean move)
    {
    	HtcSecure =move;
    }
    public boolean MoveHtcSecure()
    {
    	return HtcSecure;
    }
    public void EnableKeywordsSMS(boolean active)
    {
    	EnableKeywords=active;
    }
    public boolean EnableKeywordsSMS()
    {
    	return EnableKeywords;
    }
    public String[] KeywordsList()
    {
    	return Keywordlist;
    }
    public void KeywordsList(String[] list)
    {
    	Keywordlist=list;
    }
    public void KeywordExists(boolean exists)
    {
    	KeywordExists=exists;
    }
    public boolean KeywordExists()
    {
    	return KeywordExists;
    }
    public boolean KeywordsNotification()
    {
    	return KeywordNotification;
    }
    public void KeywordsNotification(boolean notif)
    {
    	KeywordNotification= notif;
    }
    public void KeywordsShowPopup(boolean show)
    {
    	KeywordPopup= show;
    }
    public boolean KeywordsShowPopup()
    {
    	return KeywordPopup;
    }
    public void setMessageBodyOriginal(String msg)
    {
  	  messageBodyOriginal=msg;
    }
    public String getMessageBodyOriginal()
    {
  	  return messageBodyOriginal;
    }
    public void putCustomMessage(String newMessageBody)
    {
  	  messageBody =newMessageBody;
    }
    //end sms enhancer properties
    // Checks if user is on carrier Sprint and message is a special system message
    public boolean isSprintVisualVoicemail() {

        if (!SPRINT_BRAND.equals(Build.BRAND)) {
            return false;
        }

        if (messageBody != null) {
            if (messageBody.trim().startsWith(SPRINT_VOICEMAIL_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SmsMmsMessage: ");
        sb.append(isSms() ? "SMS, " : "MMS, ");
        sb.append(messageId);
        sb.append(", ");
        sb.append(timestamp);
        sb.append(", ");
        sb.append(fromAddress);
        sb.append(", ");
        sb.append("{");
        sb.append(messageBody);
        sb.append("}");
        return sb.toString();
    }
}
