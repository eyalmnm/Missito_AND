package ch.mitto.missito.security;

import android.content.ComponentCallbacks2;
import android.content.res.Configuration;

public class ComponentCallbacks2Wrapper implements ComponentCallbacks2 {
    @Override
    public void onTrimMemory(int level) {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }
}
