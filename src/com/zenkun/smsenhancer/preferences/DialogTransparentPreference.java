package com.zenkun.smsenhancer.preferences;

import com.zenkun.smsenhancer.R;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;

public class DialogTransparentPreference extends android.preference.DialogPreference implements SeekBar.OnSeekBarChangeListener {
	private static final String androidns="http://schemas.android.com/apk/res/android";

	  private SeekBar mSeekBar;
	  private TextView mSplashText,mValueText;
	  private Context mContext;
	  private int transparencyLevel=100;

	  private String mDialogMessage, mSuffix;
	  private int mDefault, mMax, mValue = 0;

	  public DialogTransparentPreference(Context context, AttributeSet attrs) { 
	    super(context,attrs); 
	    mContext = context;

	    mDialogMessage = context.getResources().getString(R.string.pref_transparency_message);
	    mSuffix = context.getResources().getString(R.string.pref_transparency_percent);
	    mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", 0);
	    mMax = attrs.getAttributeIntValue(androidns,"max", 100);
	    
/*	    mDialogMessage = attrs.getAttributeValue(androidns,"dialogMessage");
	    mSuffix = attrs.getAttributeValue(androidns,"text");
	    mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", 0);
	    mMax = attrs.getAttributeIntValue(androidns,"max", 100);
*/
	  }
	  @Override 
	  protected View onCreateDialogView() {
	    LinearLayout.LayoutParams params;
	    LinearLayout layout = new LinearLayout(mContext);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    layout.setPadding(6,6,6,6);
	    
	    mSplashText = new TextView(mContext);
	    if (mDialogMessage != null)
	      mSplashText.setText(mDialogMessage);
	    layout.addView(mSplashText);

	    mValueText = new TextView(mContext);
	    mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
	    mValueText.setTextSize(30);
	    params = new LinearLayout.LayoutParams(
	        LinearLayout.LayoutParams.FILL_PARENT, 
	        LinearLayout.LayoutParams.WRAP_CONTENT);
	    layout.addView(mValueText, params);

	    mSeekBar = new SeekBar(mContext);
	    mSeekBar.setOnSeekBarChangeListener(this);
	    layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

	    if (shouldPersist())
	      mValue = getPersistedInt(mDefault);

	    mSeekBar.setMax(mMax);
	    mSeekBar.setProgress(mValue);
	    transparencyLevel=mValue;
	    
	    
	    return layout;
	  }
	  /* (non-Javadoc)
	 * @see android.preference.DialogPreference#onPrepareDialogBuilder(android.app.AlertDialog.Builder)
	 */
	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);
		
		
	}
	@Override 
	  protected void onBindDialogView(View v) {
	    super.onBindDialogView(v);
	    mSeekBar.setMax(mMax);
	    mSeekBar.setProgress(mValue);
	    
	  }
	  /* (non-Javadoc)
	 * @see android.preference.DialogPreference#showDialog(android.os.Bundle)
	 */
	@Override
	protected void showDialog(Bundle state) {
		// TODO Auto-generated method stub
		super.showDialog(state);
		try
	    {
		    WindowManager.LayoutParams lp = this.getDialog().getWindow().getAttributes();
		    lp.alpha= (float) (transparencyLevel*0.01);
		    this.getDialog().getWindow().setAttributes(lp);
	    }catch (Exception e) {
			// TODO: handle exception
		}
	}
	@Override
	  protected void onSetInitialValue(boolean restore, Object defaultValue)  
	  {
	    super.onSetInitialValue(restore, defaultValue);
	    if (restore) 
	      mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
	    else 
	      mValue = (Integer)defaultValue;
	  }

	  public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
	  {
		  
	    String t = String.valueOf(100-value);
	    mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
	    transparencyLevel=value;
	    
	    try
	    {
	    WindowManager.LayoutParams lp = this.getDialog().getWindow().getAttributes();  
	    
	    if(value <26)
	    {
	    	lp.alpha=0.3f;
	    }else
	    {
	    	lp.alpha=value*0.01f;
	    }
	    
	    this.getDialog().getWindow().setAttributes(lp);
	    }catch (Exception e) {
	    	//need try because first load is null
			// TODO: handle exception
		}
	    callChangeListener(new Integer(value));
	  }
	  /* (non-Javadoc)
	 * @see android.preference.DialogPreference#onClick(android.content.DialogInterface, int)
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		if (shouldPersist() && which == -1) //-1= OK
		{
		      persistInt(transparencyLevel);
		}
		super.onClick(dialog, which);
	}
	public void onStartTrackingTouch(SeekBar seek) {}
	  public void onStopTrackingTouch(SeekBar seek) {}

	  public void setMax(int max) { mMax = max; }
	  public int getMax() { return mMax; }

	  public void setProgress(int progress) { 
	    mValue = progress;
	    if (mSeekBar != null)
	      mSeekBar.setProgress(progress); 
	  }
	  public int getProgress() { return mValue; }
	}