package ch.mitto.missito.ui.tabs.contacts;

import android.ch.mitto.missito.R;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.ui.common.ContactPicker.MarkListener;
import ch.mitto.missito.ui.common.fastscroll.RecyclerViewFastScroller;
import ch.mitto.missito.util.AvatarWrapper;

public class ContactPickerAdapter extends RecyclerView.Adapter<ContactPickerAdapter.ViewHolder>
        implements RecyclerViewFastScroller.BubbleTextGetter {

    protected List<Contact> items;
    private MarkListener markListener;

    public ContactPickerAdapter(List<MissitoContact> items, MarkListener markListener) {
        this.items = new ArrayList<>();
        this.markListener = markListener;
        addItems(items);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_picker, parent, false);
        return new ViewHolder(view);
    }

    public void setItems(List<MissitoContact> items) {
        addItems(items);
        notifyDataSetChanged();
        markListener.onUpdateCountOfMarkedItems(0);
    }

    private void addItems(List<MissitoContact> items) {
        this.items.clear();
        for (MissitoContact item : items) {
            this.items.add(new Contact(item));
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void markAllItems(boolean mark) {
        for (Contact item : items) {
            item.selected = mark;
        }
        markListener.onUpdateCountOfMarkedItems(mark ? items.size() : 0);
        notifyDataSetChanged();
    }

    public boolean isMarkedAllItems(){
        for(Contact item: items){
            if(!item.selected)
                return false;
        }
        return true;
    }

    private int countOfMarkedItems(){
        int c = 0;
        for (Contact item: items) {
            if (item.selected) {
                c++;
            }
        }
        return c;
    }

    @Override
    public String getTextToShowInBubble(int pos) {
        return Character.toString(items.get(pos).name.charAt(0));
    }

    public ArrayList<String> getSelectedContacts() {
        ArrayList<String> phones = new ArrayList<>();
        for (Contact item : items) {
            if (item.selected) {
                phones.add(item.phone);
            }
        }
        return phones;
    }

    public ArrayList<String> getAllContacts() {
        ArrayList<String> phones = new ArrayList<>();
        for (Contact item : items) {
            phones.add(item.phone);
        }
        return phones;
    }

    private static class Contact {

        public String phone;
        public String name;
        public boolean selected;
        public String avatarPath;

        public Contact(MissitoContact contact) {
            name = contact.name;
            phone = contact.phone;
            selected = false;
            avatarPath = contact.avatarPath;
        }
    }

    final class ViewHolder extends RecyclerView.ViewHolder {

        @OnClick(R.id.container)
        public void onClick() {
            selectedCheckbox.setChecked(!selectedCheckbox.isChecked());
            items.get(position).selected = selectedCheckbox.isChecked();
            notifyDataSetChanged();
            markListener.onUpdateCountOfMarkedItems(countOfMarkedItems());
        }

        private final AvatarWrapper avatar;

        @BindView(R.id.initials_txt)
        TextView initials;

        @BindView(R.id.profile_avatar)
        RoundedImageView profileAvatar;

        @BindView(R.id.name_txt)
        TextView name;

        @BindView(R.id.phone_number)
        TextView phoneNumber;

        @BindView(R.id.cb_select)
        CheckBox selectedCheckbox;

        private int position;

        private ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            avatar = new AvatarWrapper(itemView.getContext(), profileAvatar, initials);
        }

        public void setData(final Contact contact, int position) {
            this.position = position;
            name.setText(contact.name);
            phoneNumber.setText(contact.phone);
            avatar.update(contact.phone, contact.avatarPath, contact.name);
            selectedCheckbox.setChecked(contact.selected);
        }
    }
}
