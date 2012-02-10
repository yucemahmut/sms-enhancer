package com.zenkun.smsenhancer;

import android.content.Context;

public class License { //since its open source it doesent matter license 
	public static boolean isPlusVersion(Context c)
	{
		  return  c.getPackageName().equals("com.zenkun.smsenhancerplus");
	}

}
