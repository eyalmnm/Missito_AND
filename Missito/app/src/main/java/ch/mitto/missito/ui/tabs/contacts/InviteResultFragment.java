package ch.mitto.missito.ui.tabs.contacts;

import android.app.Activity;
import android.ch.mitto.missito.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.dd.CircularProgressButton;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.Application;
import ch.mitto.missito.ui.MainActivityAccess;

public class InviteResultFragment extends Fragment {

    private static final String ARG_PHONES = "phones";
    private static final String ARG_LANG = "lang";

    private ArrayList<String> phones;
    private Listener listener;
    private String lang;

    @BindView(R.id.progress_button)
    CircularProgressButton progressButton;

    @BindView(R.id.wait_message)
    TextView waitMessage;

    @BindView(R.id.btn_cancel)
    Button btnCancel;

    @OnClick(R.id.btn_cancel)
    public void onCancelPressed() {
        listener.onCancel(this);
    }

    public InviteResultFragment() {
    }

    public static InviteResultFragment newInstance(ArrayList<String> phones, String lang) {
        InviteResultFragment fragment = new InviteResultFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PHONES, phones);
        args.putString(ARG_LANG, lang);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listener.updateTitle(getString(R.string.invite_friends), false);
        if (getArguments() != null) {
            phones = getArguments().getStringArrayList(ARG_PHONES);
            lang = getArguments().getString(ARG_LANG);
            sendInvites();
        }
    }

    private void sendInvites() {
        Application.app.connectionManager.apiRequests.sendInvites(lang, phones, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressButton.setProgress(100);
                waitMessage.setText(R.string.invitations_send);
                progressButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onCancel(InviteResultFragment.this);
                    }
                });
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                waitMessage.setText(R.string.coudl_not_send_invites);
                btnCancel.setVisibility(View.VISIBLE);
                progressButton.setProgress(-1);
                progressButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        waitMessage.setText(R.string.please_wait);
                        progressButton.setProgress(0);
                        progressButton.setProgress(50);
                        btnCancel.setVisibility(View.GONE);
                        sendInvites();
                    }
                });
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invite_result, container, false);
        ButterKnife.bind(this, view);
        progressButton.setIndeterminateProgressMode(true);
        progressButton.setProgress(50);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement InviteResultFragment.Listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface Listener extends MainActivityAccess {
        void onCancel(InviteResultFragment sender);
    }
}
