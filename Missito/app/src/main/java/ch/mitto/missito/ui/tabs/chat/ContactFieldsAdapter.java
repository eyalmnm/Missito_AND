package ch.mitto.missito.ui.tabs.chat;

import android.ch.mitto.missito.R;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.db.model.attach.ContactAttachRec;
import ch.mitto.missito.db.model.common.RealmString;

public class ContactFieldsAdapter extends RecyclerView.Adapter<ContactFieldsAdapter.ContactFieldViewHolder> {

    private List<ContactField> items;
    private OnItemClickListener listener;

    public ContactFieldsAdapter(ContactAttachRec contact, OnItemClickListener listener) {
        initItems(contact);
        this.listener = listener;
    }

    private void initItems(ContactAttachRec contact) {
        items = new ArrayList<>();
        for (RealmString phone : contact.phones) {
            ContactField field = new ContactField(ContactField.Type.PHONE, phone.string);
            items.add(field);
        }
        for (RealmString email : contact.emails) {
            ContactField field = new ContactField(ContactField.Type.EMAIL, email.string);
            items.add(field);
        }
    }

    @Override
    public ContactFieldViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.dialog_contact_item, parent, false);
        return new ContactFieldViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactFieldViewHolder holder, int position) {
        holder.setData(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    class ContactFieldViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.contact_itemTv)
        TextView itemTv;

        public ContactFieldViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void setData(final ContactField item, final OnItemClickListener listener) {
            itemTv.setText(item.value);
            itemTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.type.equals(ContactField.Type.EMAIL)) {
                        listener.onEmailClicked(item.value);
                    } else {
                        listener.onPhoneClicked(item.value);
                    }
                }
            });
        }
    }

    private static class ContactField {
        enum Type {PHONE, EMAIL}

        public Type type;
        public String value;

        public ContactField(Type type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    public interface OnItemClickListener {
        void onPhoneClicked(String phone);

        void onEmailClicked(String email);
    }
}



