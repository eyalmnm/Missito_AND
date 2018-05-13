package ch.mitto.missito.ui.signin;

import android.ch.mitto.missito.BuildConfig;
import android.ch.mitto.missito.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.events.SmsReceiveEvent;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.PrefsHelper;

/**
 * Fragment to be displayed when user can input his confirmation code.
 */
public class EnterCodeFragment extends Fragment implements
        TextWatcher,
        View.OnTouchListener {

    private static final String LOG_TAG = EnterCodeFragment.class.getSimpleName();
    public static final int RESEND_TIMEOUT = 20000; // 20 seconds in milliseconds
    private static final String ARG_PARAM_PHONE = "phone number";
    private Listener listener;
    private String phoneNumber;
    private int currentIndex;
    private Handler labelUpdateHandler;

    @OnClick(R.id.btnDidntGetCode)
    public void onDidntGetCodePressed() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse(String.format("mailto:%s", BuildConfig.SUPPORT_EMAIL)));
        intent.putExtra(Intent.EXTRA_SUBJECT, "SMS Code not received");
        String format = String.format("Number: %1$s Device: %2$s Android version: %3$s App version: v.%4$s", phoneNumber, Build.MODEL, Build.VERSION.RELEASE, getAppVersion());
        intent.putExtra(Intent.EXTRA_TEXT, format);

        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Could not get app version");
            return null;
        }
    }

    @BindView(R.id.enterCode)
    TextView codeSentLabel;

    @OnClick(R.id.resendCode)
    public void onResendPressed() {
        listener.onSmsResendRequest();
        PrefsHelper.setNextSmsResendTime(RESEND_TIMEOUT);
        setHandlers();
    }

    @BindView(R.id.resendCode)
    TextView resendCode;

    @BindViews({R.id.code1, R.id.code2, R.id.code3, R.id.code4, R.id.code5})
    List<EditText> codeInputs;

    public EnterCodeFragment() {
        // Required empty public constructor
    }

    /**
     * Prepare a fragment instance.
     *
     * @param phoneNr phone number acquired from user.
     * @return A new instance of fragment EnterCodeFragment.
     */
    public static EnterCodeFragment newInstance(String phoneNr) {
        EnterCodeFragment fragment = new EnterCodeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_PHONE, phoneNr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            phoneNumber = getArguments().getString(ARG_PARAM_PHONE);
        }
        ActionBar actionBar = ((SigninActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.verification);
        }
    }

    private String getCode() {
        String code = "";
        for (EditText input : codeInputs) {
            code += input.getText().toString();
        }

        return code;
    }

    private void clearInput() {
        for (int i = 0; i < codeInputs.size(); i++) {
            EditText input = codeInputs.get(i);
            input.setText("");
        }
        codeInputs.get(0).requestFocus();
        currentIndex = 0;
    }

    private void setHandlers() {
        long currentTimeMillis = System.currentTimeMillis();
        if (PrefsHelper.getNextSmsResendTime() >= currentTimeMillis) {

            final DateFormat outputFormat = new SimpleDateFormat("mm:ss", getResources().getConfiguration().locale);
            labelUpdateHandler = new Handler();
            labelUpdateHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    long remainTime = PrefsHelper.getNextSmsResendTime() - System.currentTimeMillis();
                    if (remainTime > 1000) {
                        String formattedTime = outputFormat.format(remainTime);
                        formatResendCodeLabel(String.format(getString(R.string.resend_sms), formattedTime), R.color.manatee, false);
                        labelUpdateHandler.postDelayed(this, 1000);
                    } else {
                        formatResendCodeLabel(getString(R.string.resend_code), R.color.malibu, true);
                    }
                }
            }, 0);
        }
    }

    private void formatResendCodeLabel(String text, int colorId, boolean clickable) {
        resendCode.setText(text);
        resendCode.setTextColor(ContextCompat.getColor(getContext(), colorId));
        resendCode.setClickable(clickable);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enter_code, container, false);
        ButterKnife.bind(this, view);
        setCodeSentLabel();
        setHandlers();

        codeInputs.get(0).requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);


        for (EditText editText : codeInputs) {
            editText.addTextChangedListener(this);
            editText.setOnTouchListener(this);
        }

        return view;
    }

    private void setCodeSentLabel() {
        String sentCode = getString(R.string.send_code);
        String phone = Helper.formatPhoneInternational(phoneNumber);
        SpannableString ss = new SpannableString(String.format(sentCode, phone));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                getActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };

        int phoneNumberStartIndex = sentCode.length() - 2; // string length  - '%s' length
        int phoneNumberEndIndex = phoneNumberStartIndex + phone.length(); // string length + length + phone number length
        ss.setSpan(clickableSpan, phoneNumberStartIndex, phoneNumberEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        codeSentLabel.setMovementMethod(LinkMovementMethod.getInstance());
        codeSentLabel.setHighlightColor(Color.TRANSPARENT);
        codeSentLabel.setText(ss);
    }

    @Override
    public void onPause() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(codeInputs.get(currentIndex).getWindowToken(), 0);
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement EnterCodeFragment.Listener");
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        labelUpdateHandler.removeCallbacksAndMessages(null);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

        switch (s.length()) {
            case 0:
                if (currentIndex >= 1) {
                    codeInputs.get(--currentIndex).requestFocus();
                    EditText editText =  codeInputs.get(currentIndex);
                    editText.setSelection(editText.length());
                }
                break;
            case 1:
                if (currentIndex == 4) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            listener.onCodeEntered(EnterCodeFragment.this, getCode());
                            clearInput();
                        }
                    }, 500);
                    return;
                }
                break;
            default:
                codeInputs.get(currentIndex).setText(String.valueOf(s.charAt(0)));
                codeInputs.get(++currentIndex).append(String.valueOf(s.charAt(s.length() - 1)));
                codeInputs.get(currentIndex).requestFocus();
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final SmsReceiveEvent sms) {
        listener.onCodeEntered(EnterCodeFragment.this, sms.getReceivedCode());
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        codeInputs.get(currentIndex).requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        return true;
    }

    public interface Listener {
        void onCodeEntered(EnterCodeFragment sender, String code);

        void onSmsResendRequest();
    }
}
