package com.zenkun.smsenhancer.receiver;

import com.zenkun.smsenhancer.BuildConfig;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.zenkun.smsenhancer.service.SmsReceiverService;
import com.zenkun.smsenhancer.util.Log;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v("SMSReceiver: onReceive()");
        intent.setClass(context, SmsReceiverService.class);
        intent.putExtra("result", getResultCode());
        WakefulIntentService.sendWakefulWork(context.getApplicationContext(), intent);
    }
}