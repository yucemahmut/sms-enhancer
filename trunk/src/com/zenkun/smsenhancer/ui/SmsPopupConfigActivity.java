package com.zenkun.smsenhancer.ui;

import java.io.File;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.zenkun.smsenhancer.preferences.AppEnabledCheckBoxPreference;
import com.zenkun.smsenhancer.preferences.ButtonListPreference;
import com.zenkun.smsenhancer.preferences.DialogPreference;
import com.zenkun.smsenhancer.preferences.QuickReplyCheckBoxPreference;
import com.zenkun.smsenhancer.util.Eula;
import com.zenkun.smsenhancer.util.Log;
import com.zenkun.smsenhancer.util.SmsPopupUtils;

import com.zenkun.smsenhancer.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SmsPopupConfigActivity extends SherlockPreferenceActivity{
    private static final int DIALOG_DONATE = Menu.FIRST;
    private static final int DIALOG_EMAIL = Menu.FIRST + 1;
    private static final int SELECT_PICTURE = 1;
    private Preference donateDialogPref = null;
    private QuickReplyCheckBoxPreference quickReplyPref;
    private ButtonListPreference button1;
    private ButtonListPreference button2;
    private ButtonListPreference button3;
    private String version = "";
    private String BackgroundPath="";
    private Preference customImg;
    private static String preferencias ="/data/data/com.zenkun.smsenhancer/shared_prefs/com.zenkun.smsenhancer_preferences.xml";
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //borramos preferencias (workaround)
        SharedPreferences mPreferencias = getSharedPreferences("misPreferencias",MODE_PRIVATE);
        SharedPreferences.Editor editor = mPreferencias.edit();
    	boolean primeraVez= mPreferencias.getBoolean("primeraVez", true);
    	if(primeraVez)
    	{
    		Log.v("Primera vez");
	        File f = new File (preferencias);
	        if(f.exists())
	        {
	        	Log.v("Borrando preferencias");
	        	try
	        	{
	        		f.delete();
	        		editor.putBoolean("primeraVez",false);
	        		editor.commit();
	        	}catch(Exception ex)
	        	{
	        		Log.v("Error al borrar:"+ex.getMessage());
	        	}
	        }
    	}else
    	{
    		Log.v("NO esPrimera vez");
    	}
        
        addPreferencesFromResource(R.xml.preferences);

        // Try and find app version number
        try {
            // Get version number, not sure if there is a better way to do this
            version = " v" +
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
        }

        // Set the version number in the about dialog preference
        final DialogPreference aboutPref =
                (DialogPreference) findPreference(getString(R.string.pref_about_key));
        aboutPref.setDialogTitle(getString(R.string.app_name) + version);
        aboutPref.setDialogLayoutResource(R.layout.about);

        // Set intent for contact notification option
        final PreferenceScreen contactsPS =
                (PreferenceScreen) findPreference(getString(R.string.contacts_key));
        contactsPS.setIntent(
                new Intent(this, com.zenkun.smsenhancer.ui.ConfigContactsActivity.class));

        // Set intent for quick message option
        final PreferenceScreen quickMessagePS =
                (PreferenceScreen) findPreference(getString(R.string.quickmessages_key));
        quickMessagePS.setIntent(
                new Intent(this,
                        com.zenkun.smsenhancer.ui.ConfigQuickMessagesActivity.class));

        // Button 1 preference
        button1 =
                (ButtonListPreference) findPreference(getString(R.string.pref_button1_key));
        button1.refreshSummary();
        button1.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                quickReplyPref.setChecked(
                        isQuickReplyActive((String) newValue, button2.getValue(), button3
                                .getValue()));
                updateReplyTypePref((String) newValue, button2.getValue(), button3.getValue());
                return true;
            }
        });

        // Button 2 preference
        button2 =
                (ButtonListPreference) findPreference(getString(R.string.pref_button2_key));
        button2.refreshSummary();
        button2.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                quickReplyPref.setChecked(
                        isQuickReplyActive((String) newValue, button1.getValue(), button3
                                .getValue()));
                updateReplyTypePref((String) newValue, button1.getValue(), button3.getValue());
                return true;
            }
        });

        // Button 3 preference
        button3 =
                (ButtonListPreference) findPreference(getString(R.string.pref_button3_key));
        button3.refreshSummary();
        button3.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                quickReplyPref.setChecked(
                        isQuickReplyActive((String) newValue, button1.getValue(), button2
                                .getValue()));
                updateReplyTypePref((String) newValue, button1.getValue(), button2.getValue());
                return true;
            }
        });

        // Quick Reply checkbox preference
        quickReplyPref =
                (QuickReplyCheckBoxPreference) findPreference(getString(R.string.pref_quickreply_key));

        quickReplyPref.setChecked(
                isQuickReplyActive(button1.getValue(), button2.getValue(), button3.getValue()));

        // Refresh reply type pref
        updateReplyTypePref(button1.getValue(), button2.getValue(), button3.getValue());

        /*
         * This is a really manual way of dealing with this, but I didn't think it was worth
         * spending the time to make it more generic. This will basically look through the active
         * buttons and switch any Reply buttons to Quick Reply buttons when enabling and the
         * opposite when disabling.
         */
        quickReplyPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int button1val = Integer.valueOf(button1.getValue());
                int button2val = Integer.valueOf(button2.getValue());
                int button3val = Integer.valueOf(button3.getValue());

                int count = 0;

                if (button1.isReplyButton())
                    count++;
                if (button2.isReplyButton())
                    count++;
                if (button3.isReplyButton())
                    count++;

                if (count > 1) {
                    Toast.makeText(SmsPopupConfigActivity.this,
                            R.string.pref_quickreply_bothreplybuttons, Toast.LENGTH_LONG).show();
                    return false;
                } else if (count == 0) {
                    Toast.makeText(SmsPopupConfigActivity.this,
                            R.string.pref_quickreply_noreplybuttons, Toast.LENGTH_LONG).show();
                    return false;
                }

                if (Boolean.FALSE == newValue) {

                    // Quick Reply should be turned off
                    if (button1val == ButtonListPreference.BUTTON_QUICKREPLY) {
                        button1.setValue(String.valueOf(ButtonListPreference.BUTTON_REPLY));
                    } else if (button2val == ButtonListPreference.BUTTON_QUICKREPLY) {
                        button2.setValue(String.valueOf(ButtonListPreference.BUTTON_REPLY));
                    } else if (button3val == ButtonListPreference.BUTTON_QUICKREPLY) {
                        button3.setValue(String.valueOf(ButtonListPreference.BUTTON_REPLY));
                    }
                    button1.refreshSummary();
                    button2.refreshSummary();
                    button3.refreshSummary();

                    return true;
                } else if (Boolean.TRUE == newValue) {

                    // Quick Reply should be turned on
                    if (button1val == ButtonListPreference.BUTTON_REPLY) {
                        button1.setValue(String.valueOf(ButtonListPreference.BUTTON_QUICKREPLY));
                    } else if (button2val == ButtonListPreference.BUTTON_REPLY) {
                        button2.setValue(String.valueOf(ButtonListPreference.BUTTON_QUICKREPLY));
                    } else if (button3val == ButtonListPreference.BUTTON_REPLY) {
                        button3.setValue(String.valueOf(ButtonListPreference.BUTTON_QUICKREPLY));

                    }
                    button1.refreshSummary();
                    button2.refreshSummary();
                    button3.refreshSummary();

                    return true;
                }

                return false;
            }
        });

        // Donate dialog preference
        donateDialogPref = findPreference(getString(R.string.pref_donate_key));
        if (donateDialogPref != null) {
            donateDialogPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showDialog(DIALOG_DONATE);
                    return true;
                }
            });
        }
        //sms enhancer prefs
        customImg = 
            	findPreference(getString(R.string.pref_enableBackgroundselect_key));
           
            customImg.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		
        		@Override
        		public boolean onPreferenceClick(Preference preference) {
        			 Intent intent = new Intent();
        			    intent.setType("image/*");
        			    intent.setAction(Intent.ACTION_GET_CONTENT);
        			    startActivityForResult(Intent.createChooser(intent,getString(R.string.pref_selectpicture_intent))
        			    				, SELECT_PICTURE);
        			    return true;
        		}
        	});
            
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            String x = mPrefs.getString(getString(R.string.pref_backgroundPath_key), "Default");
            customImg.setSummary(x);
            
          customImg.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        	@Override
        	public boolean onPreferenceChange(Preference preference, Object newValue) {
        		preference.setSummary(BackgroundPath==""?"Default": BackgroundPath);
        		return true;
        	}
        });
          final Preference twitter = 
        	    	findPreference(getString(R.string.pref_twitter_key));
        	    twitter.setIntent( new Intent(
        							android.content.Intent.ACTION_VIEW,
        							Uri.parse("http://twitter.com/#!/z3nkun")));
        // Split long messages preference (for some CDMA carriers like Verizon)
        CheckBoxPreference splitLongMessagesPref =
                (CheckBoxPreference) findPreference(getString(R.string.pref_split_message_key));

        // This pref is only shown for CDMA phones
        if (splitLongMessagesPref != null) {
            TelephonyManager mTM = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (mTM.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
                PreferenceCategory quickreplyPrefCategory =
                        (PreferenceCategory) findPreference(getString(R.string.pref_quickreply_cat_key));
                quickreplyPrefCategory.removePreference(splitLongMessagesPref);
                splitLongMessagesPref = null;
            }
        }

        Eula.show(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                BackgroundPath=getPath(selectedImageUri);
               customImg.setSummary(BackgroundPath) ;
               SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    		    SharedPreferences.Editor settings = mPrefs.edit();
    		    settings.putString(getString(R.string.pref_backgroundPath_key), BackgroundPath==""?"Default":BackgroundPath);
    		    settings.commit();
            }else
            {
          	  super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
    public String getPath(Uri uri) {
      	try
      	{
      	    String[] projection = { MediaStore.Images.Media.DATA };
      	    Cursor cursor = managedQuery(uri, projection, null, null, null);
      	    int column_index = cursor
      	            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      	    cursor.moveToFirst();
      	    return cursor.getString(column_index);
      	}catch (Exception e) {
      		e.printStackTrace();
      		return "Default";
      	}
      }
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Donate Dialog
        if (donateDialogPref != null) {
            boolean donated = myPrefs.getBoolean(this.getString(R.string.pref_donated_key), false);
            // boolean donated = true;
            if (donated) {
                PreferenceCategory otherPrefCategory =
                        (PreferenceCategory) findPreference(getString(R.string.pref_other_key));
                otherPrefCategory.removePreference(donateDialogPref);
                donateDialogPref = null;
            }
        }

        /*
         * This is quite hacky - in case the app was enabled or disabled externally (by
         * ExternalEventReceiver) this will refresh the checkbox that is visible to the user
         */
        AppEnabledCheckBoxPreference mEnabledPreference =
                (AppEnabledCheckBoxPreference) findPreference(getString(R.string.pref_enabled_key));

        boolean enabled = myPrefs.getBoolean(getString(R.string.pref_enabled_key), true);
        mEnabledPreference.setChecked(enabled);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

        case DIALOG_DONATE:
            LayoutInflater factory = getLayoutInflater();
            final View donateView = factory.inflate(R.layout.donate, null);

            Button donateMarketButton = (Button) donateView.findViewById(R.id.DonateMarketButton);
            donateMarketButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(SmsPopupUtils.DONATE_MARKET_URI);
                    SmsPopupConfigActivity.this.startActivity(
                            Intent.createChooser(i, getString(R.string.pref_donate_title)));
                }
            });

            Button donatePaypalButton = (Button) donateView.findViewById(R.id.DonatePaypalButton);
            donatePaypalButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(SmsPopupUtils.DONATE_PAYPAL_URI);
                    SmsPopupConfigActivity.this.startActivity(i);
                }
            });

            return new AlertDialog.Builder(this)
                    .setIcon(R.drawable.sms_enhancer)
                    .setTitle(R.string.pref_donate_title)
                    .setView(donateView)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        case DIALOG_EMAIL:
            
            Dialog.OnClickListener listener = new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case Dialog.BUTTON_POSITIVE:
                        SmsPopupUtils.launchEmailToIntent(SmsPopupConfigActivity.this, 
                                getString(R.string.app_name) + version, true);                        
                        break;
                    case Dialog.BUTTON_NEGATIVE:
                        SmsPopupUtils.launchEmailToIntent(SmsPopupConfigActivity.this, 
                                getString(R.string.app_name) + version, false);
                        break;
                    }
                }
            };
            
            return new AlertDialog.Builder(this)
                    .setIcon(R.drawable.sms_enhancer)
                    .setTitle(R.string.pref_sendemail_title)
                    .setMessage(R.string.pref_sendemail_dialog)
                    .setPositiveButton(R.string.pref_sendemail_buttonok, listener)
                    .setNegativeButton(R.string.pref_sendemail_buttoncancel, listener)
                    .create();
        }

        return super.onCreateDialog(id);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.smspopup_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.email_menu_item:
            showDialog(DIALOG_EMAIL);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Quick method to work out if Quick Reply is active or not (to toggle the pref)
     */
    private boolean isQuickReplyActive(String val1, String val2, String val3) {
        if (Integer.valueOf(val1) == ButtonListPreference.BUTTON_QUICKREPLY
                || Integer.valueOf(val2) == ButtonListPreference.BUTTON_QUICKREPLY
                || Integer.valueOf(val3) == ButtonListPreference.BUTTON_QUICKREPLY) {
            return true;
        }
        return false;
    }

    /*
     * Updates reply-type preference based on the value passed
     */
    private void updateReplyTypePref(String val1, String val2, String val3) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor settings = mPrefs.edit();

        if (Integer.valueOf(val1) == ButtonListPreference.BUTTON_REPLY_BY_ADDRESS
                || Integer.valueOf(val2) == ButtonListPreference.BUTTON_REPLY_BY_ADDRESS
                || Integer.valueOf(val3) == ButtonListPreference.BUTTON_REPLY_BY_ADDRESS) {
            settings.putBoolean(getString(R.string.pref_reply_to_thread_key), false);
            // if (BuildConfig.DEBUG) Log.v("Reply to address set");
        } else {
            settings.putBoolean(getString(R.string.pref_reply_to_thread_key), true);
            // if (BuildConfig.DEBUG) Log.v("Reply to threadId set");
        }

        settings.commit();
    }

}