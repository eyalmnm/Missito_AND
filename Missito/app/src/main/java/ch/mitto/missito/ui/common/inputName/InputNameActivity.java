package ch.mitto.missito.ui.common.inputName;

import android.ch.mitto.missito.R;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import ch.mitto.missito.Application;
import ch.mitto.missito.net.ConnectionManager;
import ch.mitto.missito.util.PrefsHelper;

public class InputNameActivity extends AppCompatActivity implements ConnectionManager.GetUserNameHandler {

    private InputNameFragment inputNameFragment;
    private ActionBar actionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inputNameFragment = new InputNameFragment();
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, inputNameFragment).commit();
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.input_your_name);
            if (!PrefsHelper.getUsername().isEmpty()) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
        invokeUserNameFromServer();
    }

    // Get the current user name as it stored on server
    private void invokeUserNameFromServer() {
        Application.app.connectionManager.getUserName(this);
    }

    @Override
    public void onBackPressed() {
        if (!PrefsHelper.getUsername().isEmpty()) {
            super.onBackPressed();
        }
    }

    // ConnectionManager.GetUserNameHandler callback method
    @Override
    public void onResult(final String name) {
        if (name != null && false == name.isEmpty()) {
            if (null != inputNameFragment) {
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        PrefsHelper.saveUsername(name);
                        inputNameFragment.setName();
                        if (null != actionBar) {
                            actionBar.setHomeButtonEnabled(true);
                            actionBar.setDisplayHomeAsUpEnabled(true);
                        }
                    }
                });
            }
        }
    }
}
