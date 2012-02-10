package com.zenkun.smsenhancer.preferences;


import com.zenkun.smsenhancer.License;
import com.zenkun.smsenhancer.R;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.widget.Toast;

public class KewyordsContact extends CheckBoxPreference{
	  private Context context;
	  public KewyordsContact(Context c, AttributeSet attrs, int defStyle) {
		    super(c, attrs, defStyle);
		    context = c;
		  //  setChecked(false);
		  }

		  public KewyordsContact(Context c, AttributeSet attrs) {
		    super(c, attrs);
		    context = c;
		  }

		  public KewyordsContact(Context c) {
		    super(c);
		    context = c;
		  }

		  @Override
		  protected void onClick() {
		    super.onClick();
		    if(!License.isPlusVersion(context))
		    {
		    	Toast.makeText(context, R.string.pro_versionTitle, Toast.LENGTH_LONG).show();
		    	setChecked(false);
		    	setSummaryOff(R.string.pro_version);
		    }
		   
		  }
		}