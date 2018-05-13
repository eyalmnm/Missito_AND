package ch.mitto.missito.ui.tabs.contacts;


import android.ch.mitto.missito.R;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.util.AvatarWrapper;

public class ContactItemViewHolder extends RecyclerView.ViewHolder {

    private final AvatarWrapper avatar;

    @BindView(R.id.avatar)
    RoundedImageView avatarView;

    @BindView(R.id.name_txt)
    TextView nameText;

    @BindView(R.id.initials_txt)
    TextView initialsText;

    @BindView(R.id.last_seen_txt)
    TextView status;

    @BindView(R.id.phone_number)
    TextView phoneNumber;

    @BindView(R.id.blocked)
    TextView blocked;

    @BindView(R.id.new_label)
    TextView newLabel;

    @BindView(R.id.img_muted)
    ImageView muted;

    private View itemView;
    private Context context;

    public ContactItemViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        context = itemView.getContext();
        ButterKnife.bind(this, itemView);
        avatar = new AvatarWrapper(context, avatarView, initialsText);
    }

    public void setData(final MissitoContact contact, final Listener listener) {
        avatar.update(contact);
        blocked.setVisibility(contact.blocked ? View.VISIBLE : View.GONE);

        status.setText(contact.getLastSeenLabel(context));
        status.setTextColor(ContextCompat.getColor(context, contact.isOnline ? R.color.green : R.color.manatee));
        nameText.setText(contact.name);
        phoneNumber.setText(String.format("%s", contact.phone));

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onContactClick(contact);
                }
            }
        });
        newLabel.setVisibility(contact.isNew() && !contact.blocked ? View.VISIBLE : View.GONE);
        muted.setVisibility(contact.muted && !contact.blocked ? View.VISIBLE : View.GONE);
    }

    public interface Listener {
        void onContactClick(MissitoContact contact);
    }
}
