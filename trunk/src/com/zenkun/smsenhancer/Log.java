package com.zenkun.smsenhancer;

public class Log {
  public final static String LOGTAG = "SMSEnhancer";

  public static final boolean DEBUG = true;

  public static void v(String msg) {
    android.util.Log.v(LOGTAG, msg);
  }

  public static void e(String msg) {
    android.util.Log.e(LOGTAG, msg);
  }
}
