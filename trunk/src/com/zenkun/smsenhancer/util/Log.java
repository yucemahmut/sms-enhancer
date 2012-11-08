package com.zenkun.smsenhancer.util;

public class Log {
    public final static String LOGTAG = "SMS Enhancer";

    public static void v(String msg) {
        android.util.Log.v(LOGTAG, msg);
    }

    public static void e(String msg) {
        android.util.Log.e(LOGTAG, msg);
    }
}
