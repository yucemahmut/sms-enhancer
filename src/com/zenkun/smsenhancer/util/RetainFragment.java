package com.zenkun.smsenhancer.util;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class RetainFragment extends Fragment {
	
	private static final String TAG = "com.zenkun.smsenhancer.retainfragment";
	private Object mObject;
	
	public RetainFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
		final RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
		if (fragment != null) {
			return fragment;
		}
		return new RetainFragment();
	}
	
	public void setObject(Object obj) {
		mObject = obj;
	}
	
	public Object getObject() {
		return mObject;
	}

}
