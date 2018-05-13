package ch.mitto.missito.security;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import static android.content.Intent.ACTION_SCREEN_OFF;

public final class AppStateChangeReceiver {

    private final Application.ActivityLifecycleCallbacks activityStartedCallback = new ActivityStartedCallback();
    private final ComponentCallbacks2 uiHiddenCallback = new UiHiddenCallback();
    private final BroadcastReceiver screenOffBroadcastReceiver = new ScreenOffBroadcastReceiver();

    private boolean background = true;
    private StateListener appStateListener;
    private boolean isFirstLaunch = true;

    public AppStateChangeReceiver(Application app) {
        app.registerActivityLifecycleCallbacks(activityStartedCallback);
        app.registerComponentCallbacks(uiHiddenCallback);
        app.registerReceiver(screenOffBroadcastReceiver, new IntentFilter(ACTION_SCREEN_OFF));
    }

    public void setListener(StateListener listener) {
        this.appStateListener = listener;
    }

    private void onAppDidEnterForeground() {
        if (appStateListener != null) {
            if (background) {
                appStateListener.onAppEnterForeground();
            }
            background = false;
        }
    }

    private void onAppDidEnterBackground() {
        if (appStateListener != null) {
            if (!background) {
                appStateListener.onAppEnterBackground();
            }
            background = true;
        }
    }

    private class ActivityStartedCallback extends ActivityLifecycleCallbacksWrapper {

        @Override
        public void onActivityStarted(Activity activity) {
            if (isFirstLaunch) {
                isFirstLaunch = false;
                background = true;
            }

            if (background) {
                onAppDidEnterForeground();
            }
        }
    }

    private class UiHiddenCallback extends ComponentCallbacks2Wrapper {

        @Override
        public void onTrimMemory(int level) {
            if (level >= TRIM_MEMORY_UI_HIDDEN) {
                onAppDidEnterBackground();
            }
        }
    }

    private class ScreenOffBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            onAppDidEnterBackground();
        }
    }
}