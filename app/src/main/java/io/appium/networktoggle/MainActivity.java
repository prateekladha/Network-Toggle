package io.appium.networktoggle;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.SyncStateContract;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private ActivityManager am;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extras = this.getIntent().getExtras();

        if(extras != null && !extras.isEmpty() && extras.size() > 0) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
            layout.setVisibility(View.VISIBLE);
            Iterator iter = extras.keySet().iterator();
            int i = 0;
            while (iter.hasNext()) {
                String name = (String) iter.next();
                if(name.trim().equalsIgnoreCase("wifi") || name.trim().equalsIgnoreCase("data") || name.trim().equalsIgnoreCase("airplane")) {
                    Service service = ServicesFactory.getService(this, name);
                    if (service != null) {
                        String value = extras.getString(name);
                        updateView(i, name, value);
                        boolean status = (value.equalsIgnoreCase("on")) ? service.enable() : service.disable();
                    }
                }
                else if(name.trim().equalsIgnoreCase("run")){
                    String command = extras.getString(name).trim();
                    if(!command.equalsIgnoreCase("")){
                        String commandText = command.trim().replace("PIPE", "|");
                        commandText = commandText.trim();
                        Log.d(TAG, commandText);
                        Process process = null;
                        try {
                            process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", "-l", commandText}, null, null);
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            String line;
                            StringBuilder sb = new StringBuilder();
                            while ((line = bufferedReader.readLine())!=null){
                                sb.append(line + "\n");
                            }
                            updateView(i, name, sb.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else if(name.trim().equalsIgnoreCase("broadcast")){
                    String[] cmd = extras.getString(name).trim().split(",");
                    String intent = cmd[0];
                    final String path = cmd.length > 1 ? cmd[1] : "";

                    switch(intent.trim()){
                        case "android.intent.action.MEDIA_MOUNTED":
                            Log.d(TAG, Environment.getExternalStorageDirectory().getAbsolutePath() + path);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                MediaScannerConnection.scanFile(getApplicationContext(), new String[]{Environment.getExternalStorageDirectory().getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.d(TAG, "Scan completed");
                                    }
                                });
                            }
                            else{
                                if(path.trim().equalsIgnoreCase("")){
                                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
                                }
                                else {
                                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory() + path)));
                                }
                            }
                            break;
                        default:
                            if(path.trim().equalsIgnoreCase("")){
                                sendBroadcast(new Intent(intent.trim(), null));
                            }
                            else {
                                sendBroadcast(new Intent(intent.trim(), Uri.parse("file://" + Environment.getExternalStorageDirectory() + path)));
                            }
                    }
                }
                else if(name.trim().equalsIgnoreCase("stop")) {
                    if (Build.VERSION.SDK_INT < 21) {
                        String packageName = extras.getString(name).trim();
                        am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                        killPackageProcesses(packageName);
                    }
                }
                i++;
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    MainActivity.this.finish();
                }
            }, 2000);
        }

        /*List<ApplicationInfo> packages;
        PackageManager pm;
        pm = getPackageManager();
        //get a list of installed apps.
        packages = pm.getInstalledApplications(0);

        ActivityManager mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        for (ApplicationInfo packageInfo : packages) {
            if((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM)==1)continue;
            if(packageInfo.packageName.equals("mypackage")) continue;
            mActivityManager.killBackgroundProcesses(packageInfo.packageName);
        }*/
    }

    public int findPIDbyPackageName(String packageName) {
        int result = -1;

        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo pi : am.getRunningAppProcesses()){
                if (pi.processName.equalsIgnoreCase(packageName)) {
                    result = pi.pid;
                }
                if (result != -1) break;
            }
        } else {
            result = -1;
        }

        return result;
    }

    public boolean isPackageRunning(String packageName) {
        return findPIDbyPackageName(packageName) != -1;
    }

    public boolean killPackageProcesses(String packageName) {
        boolean result = false;

        if (am != null) {
            am.killBackgroundProcesses(packageName);
            result = !isPackageRunning(packageName);
        } else {
            result = false;
        }

        return result;
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
        }
        return super.onOptionsItemSelected(item);
    }
    private void updateView(int index, String name, String value) {
        int viewId = this.getResources().getIdentifier("notice_" + index, "id", this.getPackageName());
        final TextView notice = (TextView) findViewById(viewId);

        if(!name.trim().equalsIgnoreCase("run")) {
            int stringId = this.getResources().getIdentifier(name + "_" + value, "string", this.getPackageName());
            notice.setText(getResources().getString(stringId));
        }
        else{
            notice.setText(value);
        }
    }
}