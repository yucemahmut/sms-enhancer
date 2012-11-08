package com.zenkun.smsenhancer.util;

import com.zenkun.smsenhancer.provider.SmsMmsMessage;
import com.zenkun.smsenhancer.provider.SmsPopupContract.ContactNotifications;
import com.zenkun.smsenhancer.util.ManagePreferences.Defaults;

import com.zenkun.smsenhancer.R;

public class ManageKeywords {
	
	private SmsMmsMessage message;
	private ManagePreferences mPrefs;
	public ManageKeywords(SmsMmsMessage message,ManagePreferences pref)
	{
		this.message =message;
		mPrefs =pref;
	}
	
	public void useDatabase(boolean db)
	{
		mPrefs.setDatabase(db);
		
	}
	public SmsMmsMessage getMessage()
	{
		return this.message;
	}
	
	public boolean ExistsKeywords()
	{
	
		//smsenhancer check if there is keywords enable
	    boolean keywordsEnable =mPrefs.getBoolean
	    			(R.string.pref_keywords_key, Defaults.PREFS_KEYWORDS_ENABLE,ContactNotifications.KEY_KEYWORDS_ENABLE);
	    message.EnableKeywordsSMS(keywordsEnable);
	    boolean customSms = mPrefs.getBoolean(R.string.pref_keywords_enablecustommsg_key
	    		, Defaults.PREFS_KEYWORDS_CUSTOM_MSG_ENABLE,ContactNotifications.KEY_KEYWORDS_CUSTOMMSG_ENABLE);
	    
	    
	    String[] keywordsList =mPrefs.getString
	    		(R.string.pref_keywordslist_key,"",ContactNotifications.KEY_KEYWORDS_LIST).split(",");
	 //   if(Log.DEBUG) Log.v("Searching Keywords");
	    message.KeywordsList(keywordsList);
	    
	    boolean KeywordExists= false;
	    if(keywordsEnable) //if the option its enable start looking keywords on message this will decide if show popup or not
	    {
	   
	    	String body = message.getMessageBody().toUpperCase();

	    	 for (String palabrasClaves : keywordsList) {
	 			if(body.indexOf(" "+palabrasClaves.toUpperCase()+" ")!=-1 || body.startsWith(palabrasClaves.toUpperCase()+" ")
	 					|| body.endsWith(" "+palabrasClaves.toUpperCase()) | body.equals(palabrasClaves.toUpperCase()) )
	 			{
	 			//	if(Log.DEBUG) Log.v("Keywords Found for: "+palabrasClaves);
	 				KeywordExists =true;
	 				message.KeywordExists(KeywordExists);
	 				if(customSms)
	 				{
	 					String sms = mPrefs.getString(R.string.pref_keywords_custommsg_key
				    			, Defaults.PREFS_KEYWORDS_CUSTOM_MSG_TEXT,ContactNotifications.KEY_KEYWORDS_CUSTOMMSG);
				    	message.setMessageBodyOriginal(message.getMessageBody());
				    	message.putCustomMessage(sms);
				    	message.isCustomMessage(customSms);
	 					
	 				}
	 				
	 			 	boolean MoveHtcSecure =mPrefs.getBoolean
	 				(R.string.pref_keywords_htcsecurebox_key, Defaults.PREFS_KEYWORDS_ENABLE,ContactNotifications.KEY_KEYWORDS_HTCSECURE);
	 		    		message.MoveHtcSecure(MoveHtcSecure);
	 				break;
	 			} 
	 		}
	    	 
		   
	    }
	    return KeywordExists;
	}
	

}
