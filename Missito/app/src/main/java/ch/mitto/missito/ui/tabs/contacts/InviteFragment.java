package ch.mitto.missito.ui.tabs.contacts;

import android.ch.mitto.missito.R;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Iterator;

import ch.mitto.missito.Application;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.ui.common.ContactPickerFragment;

public class InviteFragment extends ContactPickerFragment {

    private AppCompatButton inviteAllButton;

    @Override
    protected int getTitleRes() {
        return R.string.invite_friends;
    }

    @Override
    protected void updateContacts() {
        contacts.addAll(Application.app.contacts.systemContacts.values());
        contacts.removeAll(Application.app.contacts.missitoContacts);
        try {
            PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber numberPhone = phoneNumberUtil.parse(Application.app.connectionManager.uid, "");
            String myDialCode = "+" + String.valueOf(numberPhone.getCountryCode());
            Log.d(InviteFragment.class.getSimpleName(), String.format("My dial code %s", myDialCode));
            for (Iterator<MissitoContact> iter = contacts.iterator(); iter.hasNext(); ) {
                MissitoContact contact = iter.next();
                if (!contact.phone.startsWith(myDialCode)) {
                    Log.d(InviteFragment.class.getSimpleName(), String.format("contact.phone [%s] doesnt start with my dial code", contact.phone));
                    iter.remove();
                } else {
                    Log.d(InviteFragment.class.getSimpleName(), String.format("contact.phone [%s] start with my dial code", contact.phone));
                }
            }
        } catch (NumberParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected int getDoneBtnTextRes() {
        return R.plurals.invite_n_users;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (contacts.size() == 0) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
        View rootView =  super.onCreateView(inflater, container, savedInstanceState);
        inviteAllButton = (AppCompatButton) inflater.inflate(R.layout.button_invite_all, (ViewGroup) rootView, false);
        inviteAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onContactsSelected(adapter.getAllContacts());
            }
        });
        ((ViewGroup)rootView).addView(inviteAllButton, 0);
        return rootView;
    }

    @Override
    protected void onChangeKeyboardState(boolean keyboardIsVisible) {
        super.onChangeKeyboardState(keyboardIsVisible);
        if (inviteAllButton != null ){
            inviteAllButton.setVisibility(keyboardIsVisible ? View.GONE : View.VISIBLE);
        }
    }
}
