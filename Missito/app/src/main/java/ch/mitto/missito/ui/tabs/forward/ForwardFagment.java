package ch.mitto.missito.ui.tabs.forward;

import android.ch.mitto.missito.R;

import ch.mitto.missito.Application;
import ch.mitto.missito.ui.common.ContactPickerFragment;



public class ForwardFagment extends ContactPickerFragment {

    @Override
    protected int getTitleRes() {
        return R.string.forward_to;
    }

    @Override
    protected void updateContacts() {
        contacts.addAll(Application.app.contacts.missitoContacts);
    }

    @Override
    protected int getDoneBtnTextRes() {
        return R.plurals.forward_to_n_users;
    }

}
