package ch.mitto.missito.ui.tabs.chat.message.outgoing;

import android.ch.mitto.missito.R;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.db.model.attach.ContactAttachRec;
import ch.mitto.missito.db.model.common.RealmString;
import ch.mitto.missito.ui.tabs.chat.adapter.OutgoingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.CellActionListener;
import ch.mitto.missito.ui.tabs.chat.message.ChatCellHelper;
import ch.mitto.missito.util.ImageHelper;
import io.realm.RealmList;

/**
 * Created by Mark on 18.07.2017.
 */

public class OutgoingContactMessageViewHolder extends OutgoingMessageViewHolder {

    @BindView(R.id.name)
    TextView nameContact;
    @BindView(R.id.phones)
    TextView numberContact;
    @BindView(R.id.emails)
    TextView emailContact;
    @BindView(R.id.contact_image)
    ImageView contactImage;
    @BindView(R.id.dots_menu)
    FrameLayout dotsMenu;
    private CellActionListener listener;

    public OutgoingContactMessageViewHolder(View itemView, CellActionListener listener) {
        super(itemView);
        this.listener = listener;
        ButterKnife.bind(this, itemView);
    }

    public void setMessage(final OutgoingChatMessage message) {
        super.setMessage(message);
        ContactAttachRec contact = message.attachmentRec.contacts.get(0);
        nameContact.setText(contact.name + " " + (contact.surname == null ? "" : contact.surname));

        if (contact.avatar != null) {
            ImageHelper.fromBase64(contact.avatar, contactImage).execute();
        } else {
            contactImage.setImageResource(R.mipmap.ic_launcher);
        }

        RealmList<RealmString> phones = contact.phones;
        numberContact.setVisibility(View.GONE);
        if (phones.size() > 0) {
            List<String> stringPhones = new ArrayList<>();
            for (RealmString email : phones) {
                stringPhones.add(email.string);
            }
            numberContact.setVisibility(View.VISIBLE);
            numberContact.setText(TextUtils.join("\n", stringPhones));
        }

        RealmList<RealmString> emails = contact.emails;
        emailContact.setVisibility(View.GONE);
        if (emails.size() > 0) {
            List<String> stringEmails = new ArrayList<>();
            for (RealmString email : emails) {
                stringEmails.add(email.string);
            }
            emailContact.setVisibility(View.VISIBLE);
            emailContact.setText(TextUtils.join("\n", stringEmails));
        }

        dotsMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatCellHelper.contactMessageAlertDialog(context, message, listener).show();
            }
        });
    }
}
