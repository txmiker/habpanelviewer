package de.vier_bier.habpanelviewer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/**
 * Activity that support controlling screen state.
 */
public abstract class ScreenControllingActivity extends Activity {
    private static final String TAG = "HPV-ScreenControllingAc";

    public static final String ACTION_KEEP_SCREEN_ON = "ACTION_KEEP_SCREEN_ON";
    private static final String FLAG_KEEP_SCREEN_ON = "keepScreenOn";
    private static boolean mKeepScreenOn = false;

    public static final String ACTION_SET_BRIGHTNESS = "ACTION_SET_BRIGHTNESS";
    private static final String FLAG_BRIGHTNESS = "brightness";
    private static float mBrightness = -1;

    public static void setBrightness(Context ctx, float brightness) {
        Log.d(TAG, "sending brightness intent: " + brightness);
        mBrightness = brightness;

        Intent i = new Intent(ACTION_SET_BRIGHTNESS);
        i.putExtra(FLAG_BRIGHTNESS, brightness);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    public static void setKeepScreenOn(Context ctx, boolean keepOn) {
        Log.d(TAG, "sending keepOn intent: " + keepOn);
        mKeepScreenOn = keepOn;

        Intent i = new Intent(ACTION_KEEP_SCREEN_ON);
        i.putExtra(FLAG_KEEP_SCREEN_ON, keepOn);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean showOnLockScreen = prefs.getBoolean("pref_show_on_lock_screen", false);
        if (showOnLockScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        IntentFilter f = new IntentFilter();
        f.addAction(ACTION_KEEP_SCREEN_ON);
        f.addAction(ACTION_SET_BRIGHTNESS);
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, f);
        Log.d(TAG, "registered receiver");

        Log.d(TAG, "onStart: set keep on: " + mKeepScreenOn);
        getScreenOnView().setKeepScreenOn(mKeepScreenOn);

        Log.d(TAG, "onStart: set brightness: " + mBrightness);
        setBrightness(mBrightness);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent);
        Log.d(TAG, "receiver unregistered");

        super.onStop();
    }

    public abstract View getScreenOnView();

    private BroadcastReceiver onEvent = new BroadcastReceiver() {
        public void onReceive(Context ctx, Intent i) {
            if (ACTION_KEEP_SCREEN_ON.equals(i.getAction())) {
                final boolean keepOn = i.getBooleanExtra(FLAG_KEEP_SCREEN_ON, false);

                runOnUiThread(() -> {
                    Log.d(TAG, "onReceive: set keep on: " + keepOn);
                    getScreenOnView().setKeepScreenOn(keepOn);
                });
            } else if (ACTION_SET_BRIGHTNESS.equals(i.getAction())) {
                final float brightness = i.getFloatExtra(FLAG_BRIGHTNESS, 1.0f);

                runOnUiThread(() -> {
                    Log.d(TAG, "onReceive: set brightness: " + brightness);
                    setBrightness(brightness);
                });

            }
        }
    };

    protected void setBrightness(float brightness) {
        final WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = brightness;
        getWindow().setAttributes(layout);
    }
}
