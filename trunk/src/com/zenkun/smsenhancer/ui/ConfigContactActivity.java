package com.zenkun.smsenhancer.ui;


import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.zenkun.smsenhancer.preferences.CustomLEDColorListPreference;
import com.zenkun.smsenhancer.preferences.CustomLEDPatternListPreference;
import com.zenkun.smsenhancer.preferences.CustomVibrateListPreference;
import com.zenkun.smsenhancer.preferences.TestNotificationDialogPreference;
import com.zenkun.smsenhancer.provider.SmsPopupContract.ContactNotifications;
import com.zenkun.smsenhancer.util.Log;
import com.zenkun.smsenhancer.util.ManageNotification;
import com.zenkun.smsenhancer.util.SmsPopupUtils;
import com.zenkun.smsenhancer.util.SmsPopupUtils.ContactIdentification;

import com.zenkun.smsenhancer.BuildConfig;
import com.zenkun.smsenhancer.R;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;

public class ConfigContactActivity extends SherlockPreferenceActivity{
    private long rowId;
    private RingtonePreference ringtonePref;
    public static final String EXTRA_CONTACT_ID =
            "com.zenkun.smsenhancerpro.EXTRA_CONTACT_ID";
    public static final String EXTRA_CONTACT_URI =
            "com.zenkun.smsenhancerpro.EXTRA_CONTACT_URI";

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getSupportActionBar();
		 bar.setHomeButtonEnabled(true);
		 bar.setDisplayHomeAsUpEnabled(true);
        /*if (SmsPopupUtils.isHoneycomb()) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }*/

        // Create and setup preferences
        createOrFetchContactPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();

        final SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Uri ringtoneUri = Uri.parse(myPrefs.getString(getString(R.string.c_pref_notif_sound_key),
                ManageNotification.defaultRingtone));
        final Ringtone mRingtone = RingtoneManager.getRingtone(this, ringtoneUri);

        if (mRingtone == null) {
            ringtonePref.setSummary(getString(R.string.ringtone_silent));
        } else {
            ringtonePref.setSummary(mRingtone.getTitle(this));
        }
    }

    private void createOrFetchContactPreferences() {

        // This Uri can be built by either ContactNotifications.buildContactUri() or
        // ContactNotifications.buildLookupUri().
        final Uri contactNotificationsUri = getIntent().getParcelableExtra(EXTRA_CONTACT_URI);

        if (BuildConfig.DEBUG) {
            Log.v("EXTRA_CONTACT_URI = " + contactNotificationsUri);
        }

        Cursor c = getContentResolver().query(contactNotificationsUri, null, null, null, null);

        // Wow is this ugly, should tidy this up.
        if (c == null || c.getCount() != 1) {
        	// If cursor is null or count is not 1..
        	if (c != null) {
        		c.close(); // close up cursor
        	}
        	// create new contact notification and return new cursor
            c = createContact(contactNotificationsUri);
        }

        // check cursor again, as we may have created a new contact notification
        if (c == null || c.getCount() != 1) {

        	// something went really wrong now
            if (c != null) {
                c.close(); // close up cursor again
            }
            finish(); // get out of here
            return;
        }

        // Let Activity manage the cursor
        startManagingCursor(c);

        // Retrieve preferences from database
        retrievePreferences(c);

        // Add preference layout from XML
        addPreferencesFromResource(R.xml.configcontact);

        // Customize Activity title + main notif enabled preference summaries
        String contactName =
                c.getString(c.getColumnIndex(ContactNotifications.CONTACT_NAME));

        if (SmsPopupUtils.isHoneycomb()) {
            setTitle(contactName);
        } else {
            setTitle(getString(R.string.contact_customization_title, contactName));
        }

        CheckBoxPreference enabledPref =
                (CheckBoxPreference) findPreference(getString(R.string.c_pref_notif_enabled_key));
        enabledPref.setSummaryOn(
                getString(R.string.contact_customization_enabled, contactName));
        enabledPref.setSummaryOff(
                getString(R.string.contact_customization_disabled, contactName));
        enabledPref.setOnPreferenceChangeListener(onPrefChangeListener);

        /*
         * Main Prefs
         */
        CheckBoxPreference enablePopupPref =
                (CheckBoxPreference) findPreference(getString(R.string.c_pref_popup_enabled_key));
        enablePopupPref.setOnPreferenceChangeListener(onPrefChangeListener);

        ringtonePref =
                (RingtonePreference) findPreference(getString(R.string.c_pref_notif_sound_key));
        ringtonePref.setOnPreferenceChangeListener(onPrefChangeListener);

        TestNotificationDialogPreference testPref =
                (TestNotificationDialogPreference) findPreference(
                getString(R.string.c_pref_notif_test_key));
        testPref.setContactId(c.getLong(c.getColumnIndex(ContactNotifications._ID)));

        /*
         * Vibrate Prefs
         */
        CheckBoxPreference enableVibratePref =
                (CheckBoxPreference) findPreference(getString(R.string.c_pref_vibrate_key));
        enableVibratePref.setOnPreferenceChangeListener(onPrefChangeListener);

        CustomVibrateListPreference vibratePatternPref =
                (CustomVibrateListPreference) findPreference(
                getString(R.string.c_pref_vibrate_pattern_key));
        vibratePatternPref.setOnPreferenceChangeListener(onPrefChangeListener);
        vibratePatternPref.setRowId(rowId);

        /*
         * LED Prefs
         */
        CheckBoxPreference enableLEDPref =
                (CheckBoxPreference) findPreference(getString(R.string.c_pref_flashled_key));
        enableLEDPref.setOnPreferenceChangeListener(onPrefChangeListener);

        CustomLEDColorListPreference ledColorPref =
                (CustomLEDColorListPreference) findPreference(
                getString(R.string.c_pref_flashled_color_key));
        ledColorPref.setOnPreferenceChangeListener(onPrefChangeListener);
        ledColorPref.setRowId(rowId);

        CustomLEDPatternListPreference ledPatternPref =
                (CustomLEDPatternListPreference) findPreference(
                getString(R.string.c_pref_flashled_pattern_key));
        ledPatternPref.setOnPreferenceChangeListener(onPrefChangeListener);
        ledPatternPref.setRowId(rowId);
        //smsEnhancer prefs
        CheckBoxPreference enableKeywords =
                (CheckBoxPreference) findPreference(getString(R.string.c_pref_keywords_key));
            enableKeywords.setOnPreferenceChangeListener(onPrefChangeListener);
            
            EditTextPreference keywordsList = (EditTextPreference)
            findPreference(getString(R.string.c_pref_keywordslist_key));
            keywordsList.setOnPreferenceChangeListener(onPrefChangeListener);
            
            CheckBoxPreference enableNotificationOnKeywords =
                (CheckBoxPreference) findPreference(getString(R.string.c_pref_keywordslistNotification_key));
            enableNotificationOnKeywords.setOnPreferenceChangeListener(onPrefChangeListener);

            CheckBoxPreference enableCustomMsg =
                (CheckBoxPreference) findPreference(getString(R.string.c_pref_keywords_enablecustommsg_key));
            enableCustomMsg.setOnPreferenceChangeListener(onPrefChangeListener);
            
            EditTextPreference CustomMsg = (EditTextPreference)
            findPreference(getString(R.string.c_pref_keywords_custommsg_key));
            CustomMsg.setOnPreferenceChangeListener(onPrefChangeListener);
            
            CheckBoxPreference enablePopup =
                (CheckBoxPreference) findPreference(getString(R.string.c_pref_keywords_custommsg_popup_key));
            enablePopup.setOnPreferenceChangeListener(onPrefChangeListener);
            
            CheckBoxPreference moveHtc = 
          	  (CheckBoxPreference) findPreference(getString(R.string.c_pref_keywords_htcsecurebox_key));
            moveHtc.setOnPreferenceChangeListener(onPrefChangeListener);
    }

    /*
     * All preferences will trigger this when changed
     */
    private OnPreferenceChangeListener onPrefChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return storePreferences(preference, newValue);
        }
    };

    /*
     * Store a single preference back to the database
     */
    private boolean storePreferences(Preference preference, Object newValue) {
        String key = preference.getKey();
        String column = null;

        if (key.equals(getString(R.string.c_pref_notif_enabled_key))) {
            column = ContactNotifications.ENABLED;
        } else if (key.equals(getString(R.string.c_pref_popup_enabled_key))) {
            column = ContactNotifications.POPUP_ENABLED;
        } else if (key.equals(getString(R.string.c_pref_notif_sound_key))) {
            column = ContactNotifications.RINGTONE;
        } else if (key.equals(getString(R.string.c_pref_vibrate_key))) {
            column = ContactNotifications.VIBRATE_ENABLED;
        } else if (key.equals(getString(R.string.c_pref_vibrate_pattern_key))) {
            column = ContactNotifications.VIBRATE_PATTERN;
        } else if (key.equals(getString(R.string.c_pref_vibrate_pattern_custom_key))) {
            column = ContactNotifications.VIBRATE_PATTERN_CUSTOM;
        } else if (key.equals(getString(R.string.c_pref_flashled_key))) {
            column = ContactNotifications.LED_ENABLED;
        } else if (key.equals(getString(R.string.c_pref_flashled_color_key))) {
            column = ContactNotifications.LED_COLOR;
        } else if (key.equals(getString(R.string.c_pref_flashled_color_custom_key))) {
            column = ContactNotifications.LED_COLOR_CUSTOM;
        } else if (key.equals(getString(R.string.c_pref_flashled_pattern_key))) {
            column = ContactNotifications.LED_PATTERN;
        } else if (key.equals(getString(R.string.c_pref_flashled_pattern_custom_key))) {
            column = ContactNotifications.LED_PATTERN_CUSTOM;
        }//sms enhancer 
        else if (key.equals(getString(R.string.c_pref_keywords_key))) {
            column = ContactNotifications.KEY_KEYWORDS_ENABLE;
        } else if (key.equals(getString(R.string.c_pref_keywordslist_key))) {
            column = ContactNotifications.KEY_KEYWORDS_LIST;
        } else if (key.equals(getString(R.string.c_pref_keywordslistNotification_key))) {
            column = ContactNotifications.KEY_KEYWORDS_NOTIF;
        } else if (key.equals(getString(R.string.c_pref_keywords_enablecustommsg_key)) ) {
            column = ContactNotifications.KEY_KEYWORDS_CUSTOMMSG_ENABLE;
        } else if (key.equals(getString(R.string.c_pref_keywords_custommsg_key))) {
            column = ContactNotifications.KEY_KEYWORDS_CUSTOMMSG;
        } else if (key.equals(getString(R.string.c_pref_keywords_custommsg_popup_key))) {
            column = ContactNotifications.KEY_KEYWORDS_SHOWPOPUP;
        } else if (key.equals(getString(R.string.c_pref_keywords_htcsecurebox_key))) {
            column = ContactNotifications.KEY_KEYWORDS_HTCSECURE;
        } else {
            return false;
        }

        ContentValues vals = new ContentValues();
        if (newValue.getClass().equals(Boolean.class)) {
            vals.put(column, (Boolean) newValue);
        } else {
            vals.put(column, String.valueOf(newValue));
        }
        int rows = getContentResolver().update(
                ContactNotifications.buildContactUri(rowId), vals, null, null);
        return rows == 1 ? true : false;
    }

    /*
     * Retrieve all preferences from the database into preferences
     */
    private void retrievePreferences(Cursor c) {
        if (c == null || c.getCount() != 1) {
            return;
        }

        if (c.moveToFirst()) {

            rowId = c.getLong(c.getColumnIndexOrThrow(ContactNotifications._ID));

            final String one = "1";
            SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = myPrefs.edit();

            /*
             * Fetch Main Prefs
             */
            editor.putBoolean(getString(R.string.c_pref_notif_enabled_key),
                    one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.ENABLED))));
            editor.putBoolean(
                    getString(R.string.c_pref_popup_enabled_key),
                    one.equals(c.getString(
                            c.getColumnIndexOrThrow(ContactNotifications.POPUP_ENABLED))));
            editor.putString(getString(R.string.c_pref_notif_sound_key),
                    c.getString(c.getColumnIndexOrThrow(ContactNotifications.RINGTONE)));

            /*
             * Fetch Vibrate prefs
             */
            editor.putBoolean(getString(R.string.c_pref_vibrate_key),
                    one.equals(c.getString(c
                            .getColumnIndexOrThrow(ContactNotifications.VIBRATE_ENABLED))));
            editor.putString(getString(R.string.c_pref_vibrate_pattern_key),
                    c.getString(c.getColumnIndexOrThrow(ContactNotifications.VIBRATE_PATTERN)));
            editor.putString(getString(R.string.c_pref_vibrate_pattern_custom_key),
                    c.getString(c
                            .getColumnIndexOrThrow(ContactNotifications.VIBRATE_PATTERN_CUSTOM)));

            /*
             * Fetch LED prefs
             */
            editor.putBoolean(getString(R.string.c_pref_flashled_key),
                    one.equals(c.getString(c
                            .getColumnIndexOrThrow(ContactNotifications.LED_ENABLED))));
            editor.putString(getString(R.string.c_pref_flashled_color_key),
                    c.getString(c.getColumnIndexOrThrow(ContactNotifications.LED_COLOR)));
            editor.putString(getString(R.string.c_pref_flashled_color_custom_key),
                    c.getString(c.getColumnIndexOrThrow(ContactNotifications.LED_COLOR_CUSTOM)));
            editor.putString(getString(R.string.c_pref_flashled_pattern_key),
                    c.getString(c.getColumnIndexOrThrow(ContactNotifications.LED_PATTERN)));
            editor.putString(getString(R.string.c_pref_flashled_pattern_custom_key),
                    c.getString(c.getColumnIndexOrThrow(ContactNotifications.LED_PATTERN_CUSTOM)));

            //fetch smsenhancer prefs
            editor.putBoolean(getString(R.string.c_pref_keywords_key),
                    one.equals(c.getString(c
                            .getColumnIndexOrThrow(ContactNotifications.KEY_KEYWORDS_ENABLE))));
            editor.putString(getString(R.string.c_pref_keywordslist_key),
                    c.getString(c.getColumnIndexOrThrow(ContactNotifications.KEY_KEYWORDS_LIST)));
            editor.putBoolean(getString(R.string.c_pref_keywordslistNotification_key),
                    one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.KEY_KEYWORDS_NOTIF))));
            editor.putBoolean(getString(R.string.c_pref_keywords_enablecustommsg_key),
                    one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.KEY_KEYWORDS_CUSTOMMSG_ENABLE))));
            editor.putString(getString(R.string.c_pref_keywords_custommsg_key),
                    c.getString(c.getColumnIndexOrThrow(ContactNotifications.KEY_KEYWORDS_CUSTOMMSG)));
            editor.putBoolean(getString(R.string.c_pref_keywords_custommsg_popup_key),
                    one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.KEY_KEYWORDS_SHOWPOPUP))));
            editor.putBoolean(getString(R.string.c_pref_keywords_htcsecurebox_key),
                    one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.KEY_KEYWORDS_HTCSECURE))));
            // Commit prefs
            editor.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.config_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.save_menu_item:
            finish();
            return true;
        case R.id.remove_menu_item:
            getContentResolver().delete(
                    ContactNotifications.buildContactUri(rowId), null, null);
            finish();
            return true;
        }
        return false;
    }

    private Cursor createContact(String contactId, String contactLookupKey) {
        final ContactIdentification contactInfo =
                SmsPopupUtils.getPersonNameByLookup(this, contactLookupKey, contactId);

        if (contactInfo == null) {
            return null;
        }

        final String contactName = contactInfo.contactName;

        if (contactName == null) {
            return null;
        }

        final ContentValues vals = new ContentValues();
        vals.put(ContactNotifications.CONTACT_NAME, contactName.trim());
        vals.put(ContactNotifications.CONTACT_ID, contactId);
        vals.put(ContactNotifications.CONTACT_LOOKUPKEY, contactLookupKey);

        final Uri contactUri = getContentResolver().insert(ContactNotifications.CONTENT_URI, vals);

        return getContentResolver().query(contactUri, null, null, null, null);
    }

    private Cursor createContact(Uri contactUri) {
        return createContact(
                ContactNotifications.getContactId(contactUri),
                ContactNotifications.getLookupKey(contactUri));
    }

}
