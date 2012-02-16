package com.zenkun.smsenhancer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (Log.DEBUG) Log.v("SMSReceiver: onReceive()");
    intent.setClass(context, SmsReceiverService.class);
    intent.putExtra("result", getResultCode());
    //this.abortBroadcast();
    /*
     * This service will process the activity and show the popup (+ play notifications)
     * after it's work is done the service will be stopped.
     */
    SmsReceiverService.beginStartingService(context, intent);
  }
}