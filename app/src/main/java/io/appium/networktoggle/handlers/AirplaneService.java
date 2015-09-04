package io.appium.networktoggle.handlers;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import io.appium.networktoggle.Service;

/**
 * Created by prateek on 4/10/15.
 */
public class AirplaneService extends Service {
    private static final String TAG = "SETTINGS (AIRPLANE)";

    public AirplaneService(Context context) {
        super(context);
    }

    public boolean enable() {
        Log.d(TAG, "Enabling airplane");
        return setAirplane(true);
    }

    public boolean disable() {
        Log.d(TAG, "Disabling airplane");
        return setAirplane(false);
    }

    private boolean setAirplane(boolean state) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) { //if less than verson 4.2
                Settings.System.putInt(
                        mContext.getContentResolver(),
                        Settings.System.AIRPLANE_MODE_ON, state ? 1 : 0);
            } else {
                Settings.Global.putInt(
                        mContext.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, state ? 1 : 0);
            }
        }
        catch(Exception ex){
            //Do Nothing
        }
        return true;
    }
}
