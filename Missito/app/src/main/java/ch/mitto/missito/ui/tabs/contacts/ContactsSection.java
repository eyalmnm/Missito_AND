package ch.mitto.missito.ui.tabs.contacts;

import android.ch.mitto.missito.R;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.net.ConnectionChangeReceiver;
import ch.mitto.missito.ui.tabs.interfaces.FilterableSection;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class ContactsSection extends StatelessSection implements FilterableSection {

    private ArrayList<MissitoContact> contacts;
    private ArrayList<MissitoContact> filteredList;
    private ContactItemViewHolder.Listener listener;


    public ContactsSection(ArrayList<MissitoContact> contacts, ContactItemViewHolder.Listener listener) {
        super(new SectionParameters.Builder(R.layout.item_missito_contact)
                .headerResourceId(R.layout.contact_header)
                .build());

        this.contacts = contacts;
        filteredList = new ArrayList<>(contacts);
        sort();
        this.listener = listener;
    }

    @Override
    public int getContentItemsTotal() {
        return filteredList.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new ContactItemViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ContactItemViewHolder) holder).setData(filteredList.get(position), listener);
    }

    public void setItems(ArrayList<MissitoContact> contacts) {
        this.contacts = contacts;
        filteredList = new ArrayList<>(contacts);
        sort();
    }

    private void sort() {
        Collections.sort(filteredList, new Comparator<MissitoContact>() {
            @Override
            public int compare(MissitoContact o1, MissitoContact o2) {
                int isNewStatusCmp = Boolean.valueOf(o2.isNew()).compareTo(o1.isNew());
                int status = ConnectionChangeReceiver.isOnline() ? Boolean.valueOf(o2.isOnline).compareTo(o1.isOnline) : 0;
                int blocked = Boolean.valueOf(o1.blocked).compareTo(o2.blocked);
                int name = o1.name.toLowerCase().compareTo(o2.name.toLowerCase());

                if (isNewStatusCmp != 0) {
                    return isNewStatusCmp;
                } else if (blocked != 0) {
                    return blocked;
                } else {
                    return status != 0 ? status : name;
                }
            }
        });
    }

    @Override
    public void filter(String query) {
        if (TextUtils.isEmpty(query)) {
            filteredList = new ArrayList<>(contacts);
            this.setVisible(true);
        } else {
            filteredList.clear();
            for (MissitoContact value : contacts) {
                if (value.name.toLowerCase().contains(query.toLowerCase()) || value.phone.contains(query)) {
                    filteredList.add(value);
                }
            }

            this.setVisible(!filteredList.isEmpty());
        }
    }
}
