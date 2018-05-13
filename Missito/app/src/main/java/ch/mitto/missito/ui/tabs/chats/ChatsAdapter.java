package ch.mitto.missito.ui.tabs.chats;

import android.ch.mitto.missito.R;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.ui.tabs.chats.MarginSpan2;
import ch.mitto.missito.ui.tabs.chats.model.Chat;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.util.AvatarWrapper;
import ch.mitto.missito.util.Helper;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

public class ChatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Chat> items;
    private Listener listener;

    public ChatsAdapter(List<Chat> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).setData(items.get(position));
    }

    public void setItems(List<Chat> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void updateChats(List<Chat> chats) {
        items = chats;
        notifyDataSetChanged();
    }

    public Chat getChat(String phone) {
        MissitoContact contact = Application.app.contacts.missitoContactsByPhone.get(phone);
        for (Chat allChat : items) {
            if (allChat.participants.contains(contact)) {
                return allChat;
            }
        }
        return null;
    }

    public interface Listener {
        void onChatSelected(Chat chat);
    }

    final class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.name_txt)
        TextView nameText;

        @BindView(R.id.initials_txt)
        TextView initialsText;

        @BindView(R.id.last_seen_txt)
        TextView lastSeen;

        @BindView(R.id.last_msg_txt)
        TextView lastMessageText;

        @BindView(R.id.unread_count)
        TextView unreadCount;

        @BindView(R.id.icon)
        ImageView icon;

        @BindView(R.id.avatar)
        RoundedImageView avatar;

        @BindView(R.id.img_muted)
        ImageView muted;

        @BindView(R.id.online_status)
        ImageView onlineStatus;

        private View rootView;
        private Context context;
        private AvatarWrapper avatarWrapper;

        private ViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
            avatarWrapper = new AvatarWrapper(context, avatar, initialsText);
        }

        void setData(final Chat chat) {
            MissitoContact contact = chat.participants.get(0);

            if (chat.isTyping) {
                String typing = context.getString(R.string.typing);
                lastMessageText.setText(getSpannableTyping(typing));
            } else {
                icon.setImageResource(chat.lastMessage.getMessageIcon());
                icon.setColorFilter(context.getResources().getColor(R.color.colorPrimary));
                lastMessageText.setText(getSpannableTextBody(chat));
            }

            avatarWrapper.update(contact);

            lastMessageText.setTextColor(ContextCompat.getColor(context, chat.isTyping ? R.color.colorPrimary : R.color.manatee));
            icon.setVisibility(chat.isTyping ? View.GONE : View.VISIBLE);
            nameText.setText(contact.name);
            initialsText.setText(Helper.getInitials(contact.name));

            unreadCount.setVisibility(chat.unreadCount == 0 || contact.muted ? View.GONE : View.VISIBLE);
            muted.setVisibility(contact.muted ? View.VISIBLE : View.GONE);
            onlineStatus.setVisibility(contact.isOnline ? View.VISIBLE : View.GONE);
            unreadCount.setText(String.valueOf(chat.unreadCount));
            lastSeen.setText(Helper.formatWhen(context, chat.lastMessage.timestamp));
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onChatSelected(chat);
                    }
                }
            });
        }

        @NonNull
        private SpannableString getSpannableTextBody(Chat chat) {
            String text = chat.lastMessage.getMsgText();
            if (text == null) {
                text = "";
            }
            SpannableString ss = new SpannableString(text);
            ss.setSpan(new MarginSpan2(1, (int) (lastMessageText.getLineHeight() + Helper.dipToPixels(context, 2))), 0, ss.length(), 0); // set padding equal to icon size + 2 dp
            return ss;
        }

        @NonNull
        private SpannableString getSpannableTyping(String typing) {
            SpannableString ss = new SpannableString(typing);
            CalligraphyTypefaceSpan typefaceSpan = new CalligraphyTypefaceSpan(TypefaceUtils.load(context.getAssets(), "fonts/sf-ui-text-italic.otf"));
            ss.setSpan(typefaceSpan, 0, typing.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return ss;
        }
    }
}
