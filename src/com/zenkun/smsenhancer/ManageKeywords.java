package com.zenkun.smsenhancer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.zenkun.smsenhancer.ManagePreferences.Defaults;

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
	    			(R.string.pref_keywords_key, Defaults.PREFS_KEYWORDS_ENABLE,SmsPopupDbAdapter.KEY_KEYWORDS_ENABLE_NUM);
	    message.EnableKeywordsSMS(keywordsEnable);
	    
	    
	    String[] keywordsList =mPrefs.getString
	    		(R.string.pref_keywordslist_key,"",SmsPopupDbAdapter.KEY_KEYWORDS_LIST_NUM).split(",");
	    if(Log.DEBUG) Log.v("Searching Keywords");
	    message.KeywordsList(keywordsList);
	    
	    boolean KeywordExists= false;
	    
	    
	    if(keywordsEnable) //if the option its enable start looking keywords on message this will decide if show popup or not
	    {
	   
	    	String body = message.getMessageBody().toUpperCase();

	    	 for (String palabrasClaves : keywordsList) {
	 			if(body.indexOf(" "+palabrasClaves.toUpperCase()+" ")!=-1 || body.startsWith(palabrasClaves.toUpperCase()+" ")
	 					|| body.endsWith(" "+palabrasClaves.toUpperCase()) | body.equals(palabrasClaves.toUpperCase()) )
	 			{
	 				if(Log.DEBUG) Log.v("Keywords Found for: "+palabrasClaves);
	 				KeywordExists =true;
	 				message.KeywordExists(KeywordExists);
	 			 	boolean MoveHtcSecure =mPrefs.getBoolean
	 				(R.string.pref_keywords_htcsecurebox_key, Defaults.PREFS_KEYWORDS_ENABLE,SmsPopupDbAdapter.KEY_KEYWORDS_HTCSECURE_NUM);
	 		    		message.MoveHtcSecure(MoveHtcSecure);
	 				
	 				break;
	 			} 
	 		}
	    	 
		   
	    }
	    return KeywordExists;
	}
	

}
