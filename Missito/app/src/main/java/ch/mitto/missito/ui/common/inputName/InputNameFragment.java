package ch.mitto.missito.ui.common.inputName;

import android.ch.mitto.missito.R;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.net.ConnectionManager;
import ch.mitto.missito.ui.MainActivity;
import ch.mitto.missito.ui.signin.SigninActivity;
import ch.mitto.missito.util.PrefsHelper;

public class InputNameFragment extends Fragment {

    @BindView(R.id.input_name_etv)
    AppCompatEditText inputNameEtv;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input_name, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);
        inputNameEtv.setText(PrefsHelper.getUsername());
        inputNameEtv.setSelection(inputNameEtv.length());
        return view;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.input_name_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                break;
            case R.id.action_done:
                if (inputNameEtv.length() > 0) {
                    updateSurname(inputNameEtv.getText().toString());
                } else {
                    inputNameEtv.setError(getString(R.string.please_enter_your_name));
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSurname(String newSurname) {
        //Send username to backend and server
        PrefsHelper.saveUsername(newSurname);
        String phone = Application.app.connectionManager.uid;
        Application.app.connectionManager.updateUserName(newSurname, phone);
        Intent startIntent = new Intent(getActivity(), MainActivity.class);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startIntent);
    }

    // update the user name as it stored in the server
    public void setName() {
        if (null != inputNameEtv) {
            inputNameEtv.setText(PrefsHelper.getUsername());
            inputNameEtv.setSelection(inputNameEtv.length());
        }
    }
}
