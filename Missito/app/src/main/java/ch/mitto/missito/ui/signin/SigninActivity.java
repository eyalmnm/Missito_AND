package ch.mitto.missito.ui.signin;

import android.Manifest;
import android.app.ProgressDialog;
import android.ch.mitto.missito.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.net.ConnectionManager;
import ch.mitto.missito.net.model.OTPReqResponse;
import ch.mitto.missito.ui.MainActivity;
import ch.mitto.missito.ui.common.inputName.InputNameActivity;
import ch.mitto.missito.ui.signin.Picker.Country;
import ch.mitto.missito.ui.signin.Picker.CountryPicker;
import ch.mitto.missito.ui.signin.Picker.Utils;
import ch.mitto.missito.util.PrefsHelper;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import ch.mitto.missito.util.Helper;

import static ch.mitto.missito.ui.MainActivity.PERMISSIONS_GRANTED_EVT;


public class SigninActivity extends AppCompatActivity implements
        CountryPicker.Listener,
        EnterPhoneFragment.Listener,
        EnterCodeFragment.Listener,
        MaterialSearchView.OnQueryTextListener,
        ConnectionManager.GetUserNameHandler {

    private static final String LOG_TAG = SigninActivity.class.getSimpleName();
    private static final String TAG_COUNTRY_PICKER = "COUNTRY_PICKER";
    private static final int PERMISSION_REQ_CODE = 1017;

    private String token;
    private String phone;
    private ProgressDialog progressDialog;
    private AlertDialog alertDialog;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.search_view)
    MaterialSearchView searchView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.account_setup);
        }

        searchView.setOnQueryTextListener(this);
        token = PrefsHelper.getOtpToken();

        Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.content);
        if (existingFragment == null) {
            final FragmentManager fm = getSupportFragmentManager();
            Fragment phoneFragment = EnterPhoneFragment.newInstance(Utils.getUserCountryInfo(this), false);
            FragmentTransaction lTran = fm.beginTransaction();
            lTran.add(R.id.content, phoneFragment);
            lTran.commit();
        }
    }

    @Override
    public void onOnPhoneNrEntered(EnterPhoneFragment sender, final String phoneNr) {
        phone = phoneNr.replaceAll("\\s+", "").replaceAll("\\++", "");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && !Helper.canReceiveSms(this)) {
                requestPermissions();
        }else{
            sendSMSRequest();
        }
    }

    private void showEnterCodeFragment() {
        final FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tran = fm.beginTransaction();
        tran.replace(R.id.content, EnterCodeFragment.newInstance(phone));
        tran.addToBackStack(null);
        tran.commit();
    }

    @Override
    public void onCountrySelectPressed(Country country) {
        CountryPicker picker = CountryPicker.newInstance(country);
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.content, picker, TAG_COUNTRY_PICKER)
                .commit();
    }

    @Override
    public void onCodeEntered(final EnterCodeFragment sender, String code) {
        showProgress();
        Application.app.connectionManager.checkOTP(phone, token, code, new ConnectionManager.OTPCheckHandler() {
            @Override
            public void onResult(VolleyError error) {
                if (error != null) {
                    progressDialog.dismiss();
                    Log.e(LOG_TAG, "checkOTP failed", error);
                    String errorText;
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;

                        if (statusCode == 500) {
                            errorText = getString(R.string.wrong_code);
                        } else {
                            errorText = String.format(getString(R.string.unknown_error), statusCode);
                        }
                    } else {
                        errorText = error.getMessage();
                    }

                    alertDialog = Helper.showErrorDialog(SigninActivity.this, null, errorText);
                    return;
                } else {
                    if (false == PrefsHelper.getUsername().isEmpty()) {
                        progressDialog.dismiss();
                        Intent startIntent = new Intent(SigninActivity.this, MainActivity.class);
                        startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(startIntent);
                        finish();
                    } else {
                        // Get the user name from server in case it not exist
                        Application.app.connectionManager.getUserName(SigninActivity.this);
                    }
                }
            }
        });
    }

    // ConnectionManager.GetUserNameHandler callback method
    @Override
    public void onResult(final String name) {
        progressDialog.dismiss();
        if (name == null || name.isEmpty()) {
            startActivity(new Intent(SigninActivity.this, InputNameActivity.class));
        } else {
            PrefsHelper.saveUsername(name);
            Intent startIntent = new Intent(SigninActivity.this, MainActivity.class);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(startIntent);
        }
        finish();
    }


    @Override
    public void onSmsResendRequest() {
        if (PrefsHelper.getNextSmsResendTime() < System.currentTimeMillis()) {
            showProgress();
            Application.app.connectionManager.apiRequests.requestOTP(phone, new Response.Listener<OTPReqResponse>() {
                @Override
                public void onResponse(OTPReqResponse response) {
                    PrefsHelper.setNextSmsResendTime(EnterCodeFragment.RESEND_TIMEOUT);
                    token = response.token;
                    PrefsHelper.saveOtpToken(response.token);
                    progressDialog.dismiss();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    PrefsHelper.setNextSmsResendTime(EnterCodeFragment.RESEND_TIMEOUT);
                    progressDialog.dismiss();
                    alertDialog = Helper.showErrorDialog(SigninActivity.this, getString(R.string.cant_contact_auth), error.getLocalizedMessage());
                }
            });
        } else {
            showEnterCodeFragment();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog.show(this, "", getString(R.string.please_wait));
        } else if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        searchView.setMenuItem(item);
        return false;
    }

    public static void start(Context context) {
        Intent startIntent = new Intent(context, SigninActivity.class);
        context.startActivity(startIntent);
    }

    @Override
    public void onCountrySelected(Country country) {
        EnterPhoneFragment phoneFragment = EnterPhoneFragment.newInstance(country, true);
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().
                beginTransaction()
                .replace(R.id.content, phoneFragment)
                .commit();
        searchView.closeSearch();
    }

    @Override
    public void onTitleChangeRequest(int titleResId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(titleResId);
        }
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onHomeButtonShowRequest(boolean show) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(show);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_COUNTRY_PICKER);

        if (fragment != null && fragment instanceof FilterableFragment) {
            ((FilterableFragment) fragment).filter(newText);
        }
        return true;
    }


    private void sendSMSRequest(){
        showProgress();
        Application.app.connectionManager.apiRequests.requestOTP(phone, new Response.Listener<OTPReqResponse>() {
            @Override
            public void onResponse(OTPReqResponse response) {
                PrefsHelper.setNextSmsResendTime(EnterCodeFragment.RESEND_TIMEOUT);
                token = response.token;
                PrefsHelper.saveOtpToken(response.token);
                progressDialog.dismiss();
                showEnterCodeFragment();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                PrefsHelper.setNextSmsResendTime(EnterCodeFragment.RESEND_TIMEOUT);
                progressDialog.dismiss();
                alertDialog = Helper.showErrorDialog(SigninActivity.this, getString(R.string.cant_contact_auth), error.getLocalizedMessage());
            }
        });
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS},
                PERMISSION_REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_CODE:
                sendSMSRequest();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
