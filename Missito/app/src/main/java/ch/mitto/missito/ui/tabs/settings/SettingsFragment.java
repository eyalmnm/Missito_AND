package ch.mitto.missito.ui.tabs.settings;

import android.ch.mitto.missito.R;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.makeramen.roundedimageview.RoundedImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.Application;
import ch.mitto.missito.ui.MainActivity;
import ch.mitto.missito.ui.common.inputName.InputNameActivity;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.PrefsHelper;

public class SettingsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";

    @BindView(R.id.profileAvatar)
    RoundedImageView roundedAvatar;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.phone)
    TextView phoneNumber;

    @OnClick(R.id.profileInfoLayout)
    public void openProfileDetails() {
        startActivity(new Intent(getContext(), InputNameActivity.class));
    }
    // TODO: Rename and change types of parameters
    private String mParam1;
    private OnFragmentInteractionListener mListener;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, "dummy");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_profile_fragment, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);
        phoneNumber.setText(Helper.formatPhoneInternational(Application.app.connectionManager.uid));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        String username = PrefsHelper.getUsername();
        name.setText(username.isEmpty() ? getString(R.string.missito_user) : username);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @OnClick(R.id.profile_privacy)
    public void onAccountClick() {
        startActivity(new Intent(getContext(), PrivacySettingsActivity.class));
    }

    @OnClick(R.id.about_view)
    public void onAboutClick() {
        startActivity(new Intent(getContext(), BuildConfigActivity.class));
    }

    @OnClick(R.id.logout_view)
    void onLogoutClick() {
        Application.app.connectionManager.logout();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
