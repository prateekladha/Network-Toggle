package io.appium.networktoggle;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.WindowManager;

import java.util.Date;
import java.util.HashMap;

public class SMSApp extends BroadcastReceiver {
    private static final String LOG_TAG = "SMSApp";
    /* package */ static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    static final int TIME_OUT = 10000;
    static final int MSG_DISMISS_DIALOG = 0;
    private static AlertDialog d;

    public SMSApp() {
    }

    private static Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_DISMISS_DIALOG:
                    if (d != null && d.isShowing()) {
                        d.dismiss();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                HashMapNew Dictionary = new HashMapNew();
                for (int i = 0; i < messages.length; i++) {
                    SmsMessage message = messages[i];
                    StringBuilder buf = new StringBuilder();
                    buf.append("Received SMS from  ");
                    buf.append(message.getDisplayOriginatingAddress());
                    buf.append(" - ");
                    buf.append(message.getDisplayMessageBody());
                    Log.i(LOG_TAG, "[SMSApp] onReceiveIntent: " + Long.valueOf(new Date().getTime()) + " " + buf);
                    String key = message.getDisplayOriginatingAddress();
                    Dictionary.put(key, Dictionary.get(message.getDisplayOriginatingAddress()) + message.getDisplayMessageBody());
                }

                if(messages.length > 0) {
                    String address = messages[messages.length - 1].getDisplayOriginatingAddress();
                    String msg = Dictionary.get(address);
                    if(address.trim().contains("WYNKED") || address.trim().contains("VSIPAY") || address.trim().contains("AIRAPP") || address.trim().contains("AIRINF") || address.trim().contains("AIRSEP")) {
                        d = new AlertDialog.Builder(context).setTitle(address).setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                            }
                        }).create();
                        d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        d.show();
                        mHandler.sendEmptyMessageDelayed(MSG_DISMISS_DIALOG, TIME_OUT);
                    }
                }
            }
        }
    }

    public static class HashMapNew extends HashMap<String, String> {
        static final long serialVersionUID = 1L;

        public String get(Object key){
            String value = (String)super.get(key);
            if (value == null) {
                return "";
            }
            return value;
        }
    }
}
