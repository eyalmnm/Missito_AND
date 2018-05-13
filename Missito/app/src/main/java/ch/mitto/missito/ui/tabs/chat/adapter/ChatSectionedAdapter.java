package ch.mitto.missito.ui.tabs.chat.adapter;

import android.ch.mitto.missito.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.MessageType;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.ui.tabs.chat.message.AudioPlayerDownloadListener;
import ch.mitto.missito.ui.tabs.chat.message.ImageCellListener;
import ch.mitto.missito.ui.tabs.chat.message.VideoCellListener;
import ch.mitto.missito.ui.tabs.chat.message.incoming.IncomingAudioMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.incoming.IncomingContactMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.incoming.IncomingImageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.incoming.IncomingLocationMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.incoming.IncomingMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.incoming.IncomingTextMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.incoming.IncomingVideoViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.outgoing.OutgoingAudioMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.outgoing.OutgoingContactMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.outgoing.OutgoingImageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.outgoing.OutgoingLocationMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.outgoing.OutgoingMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.outgoing.OutgoingTextMessageViewHolder;
import ch.mitto.missito.ui.tabs.chat.message.outgoing.OutgoingVideoViewHolder;
import ch.mitto.missito.util.RealmDBHelper;

public class ChatSectionedAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    private static final int[] ITEM_LAYOUT_IDS = {R.layout.item_msg_text,
            R.layout.item_msg_image_outgoing, R.layout.item_msg_contact,
            R.layout.item_msg_location, R.layout.item_msg_audio, R.layout.item_msg_video,
            R.layout.item_msg_text_party, R.layout.item_msg_image_incoming,
            R.layout.item_msg_contact_party, R.layout.item_msg_location_party,
            R.layout.item_msg_audio_party, R.layout.item_msg_video_party};
    private ArrayList<ChatSection> sections = new ArrayList<>();
    private MissitoContact contact;
    private Listener listener;
    private int counterpartyLastDeviceId = -1;

    public ChatSectionedAdapter(List<MessageRec> history, MissitoContact contact, Listener listener) {
        this.contact = contact;
        this.listener = listener;
        populateSections(history);
    }

    private void populateSections(List<MessageRec> history) {
        for (MessageRec messageRec : history) {
            append(ChatMessage.make(messageRec, contact));
        }
    }

    public void append(ChatMessage message) {
        int counterpartyDeviceId = counterpartyLastDeviceId;
        if (message instanceof IncomingChatMessage) {
            counterpartyDeviceId = ((IncomingChatMessage)message).senderDeviceId;
        }
        if (counterpartyDeviceId != counterpartyLastDeviceId) {
            counterpartyLastDeviceId = counterpartyDeviceId;
            sections.add(new ChatSection(counterpartyDeviceId));
        }
        if (sections.isEmpty()) {
            sections.add(new ChatSection(-1));
        }
        ChatSection section = sections.get(sections.size() - 1);
        if (section.shouldIncludeNext(message)) {
            section.append(message);
            section.counterpartyDeviceId = counterpartyDeviceId;
        } else {
            ChatSection newSection = new ChatSection(counterpartyDeviceId);
            sections.add(newSection);
            newSection.append(message);
        }
        notifyDataSetChanged();
    }

    public void clearChatHistory() {
        sections.clear();
        notifyDataSetChanged();
    }

    public void updateMessageProgress(String localMsgId, float progress) {
        for (int i = 0; i < sections.size(); i++) {
            List<ChatMessage> messages = sections.get(i).messages;
            for (int j = 0; j < messages.size(); j++) {
                ChatMessage msg = messages.get(j);
                if (msg.id.equals(localMsgId)) {
                    msg.progress = progress;
                    msg.isLoading = progress > 0 && progress < 1;
                    notifyItemChanged(getAbsolutePosition(i, j));
                    return;
                }
            }
        }
    }

    public ChatMessage get(int section, int row) {
        return sections.get(section).messages.get(row);
    }

    public void insert(ChatMessage message) {

        int destSection = sections.size();
        boolean createSection = true;
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).tooLateFor(message)) {
                destSection = i;
                break;
            } else if (!sections.get(i).tooEarlyFor(message)) {
                if (i < sections.size() - 1) {
                    ChatSection nextSection = sections.get(i + 1);
                    // For the sake of simplicity we don't merge sections
                    if (!nextSection.isEmpty() && nextSection.messages.get(0).date.before(message.date)) {
                        continue;
                    }
                }
                destSection = i;
                createSection = false;
                break;
            }
        }

        int messageDeviceId = -1;
        if (message instanceof IncomingChatMessage) {
            messageDeviceId = ((IncomingChatMessage) message).senderDeviceId;
        }
        int prevSectionDeviceId = -1;
        if (destSection > 0 && createSection) {
            prevSectionDeviceId = sections.get(destSection - 1).counterpartyDeviceId;
        } else if (!sections.isEmpty()) {
            prevSectionDeviceId = sections.get(destSection).counterpartyDeviceId;
        }

        if (messageDeviceId == -1) {
            messageDeviceId = prevSectionDeviceId;
        }

        if (createSection) {
            ChatSection newSection = new ChatSection(messageDeviceId);
            sections.add(destSection, newSection);
            newSection.insert(message);
        } else {
            ChatSection targetSection = sections.get(destSection);
            if (messageDeviceId == prevSectionDeviceId || prevSectionDeviceId == -1) {
                targetSection.insert(message);
                if (prevSectionDeviceId == 1) {
                    propagateSenderDeviceId(destSection, prevSectionDeviceId, messageDeviceId);
                }
            } else {
                ChatSection[] newSections = targetSection.split(message, messageDeviceId);
                sections.remove(destSection);
                if (!newSections[0].isEmpty()) {
                    sections.add(destSection, newSections[0]);
                    destSection++;
                }
                sections.add(destSection, newSections[1]);
                propagateSenderDeviceId(destSection + 1, prevSectionDeviceId, messageDeviceId);
            }
        }
        notifyDataSetChanged();
    }

    private void propagateSenderDeviceId(int pos, int oldSenderDeviceId, int newSenderDeviceId) {
        for (int i = pos; i < sections.size(); i++) {
            ChatSection chatSection = sections.get(i);
            if (chatSection.counterpartyDeviceId == oldSenderDeviceId) {
                chatSection.counterpartyDeviceId = newSenderDeviceId;
            } else {
                break;
            }
        }
    }

    public void updateMessageStatus(String interlocutor) {
        // TODO: do it more efficiently - see iOS code
        sections.clear();
        populateSections(RealmDBHelper.getChatHistory(interlocutor));
        notifyDataSetChanged();
    }

    @Override
    public int getSectionCount() {
        return sections.size();
    }

    @Override
    public int getItemCount(int section) {
        return sections.get(section).size();
    }

    @Override
    public int getItemViewType(int section, int relativePosition, int absolutePosition) {
        ChatMessage message = sections.get(section).messages.get(relativePosition);
        return getViewTypeFor(message.type, message.direction);
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder holder, int section, boolean expanded) {
        HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

        ChatSection currentSection = sections.get(section);
        ChatSection prevSection = section > 0 ? sections.get(section - 1) : null;

        int currentDeviceId = currentSection.counterpartyDeviceId;
        int prevDeviceId = prevSection != null ? prevSection.counterpartyDeviceId : -1;

        boolean isNewSenderDeviceId = prevDeviceId != -1 && currentDeviceId != prevDeviceId;
        headerHolder.updateFor(currentSection, isNewSenderDeviceId);
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {
        // no footers
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {
        ChatMessage message = sections.get(section).messages.get(relativePosition);
        if (holder instanceof IncomingMessageViewHolder) {
            ((IncomingMessageViewHolder) holder).setMessage((IncomingChatMessage) message);
        } else {
            ((OutgoingMessageViewHolder) holder).setMessage((OutgoingChatMessage) message);
        }
    }

    @Override
    public SectionedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(getLayoutId(viewType), parent, false);
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return new HeaderViewHolder(view);

            case 0:
                return new OutgoingTextMessageViewHolder(view, listener);
            case 1:
                return new OutgoingImageViewHolder(view, listener);
            case 2:
                return new OutgoingContactMessageViewHolder(view, listener);
            case 3:
                return new OutgoingLocationMessageViewHolder(view, listener);
            case 4:
                return new OutgoingAudioMessageViewHolder(view, listener);
            case 5:
                return new OutgoingVideoViewHolder(view, listener); // TODO: proper listener

            case 6:
                return new IncomingTextMessageViewHolder(view, listener);
            case 7:
                return new IncomingImageViewHolder(view, listener);
            case 8:
                return new IncomingContactMessageViewHolder(view, listener);
            case 9:
                return new IncomingLocationMessageViewHolder(view, listener);
            case 10:
                return new IncomingAudioMessageViewHolder(view, listener);
            case 11:
                return new IncomingVideoViewHolder(view, listener); // TODO: proper listener

            default:
                throw new IllegalArgumentException("Unknown message type");
        }
    }

    private int getLayoutId(int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return R.layout.item_ts_header;
        } else {
            return ITEM_LAYOUT_IDS[viewType];
        }
    }

    private int getViewTypeFor(MessageType messageType, ChatMessage.Direction direction) {
        int delta = direction == ChatMessage.Direction.OUTGOING ? 0 : 6;
        switch (messageType) {

            case TYPING:
                throw new IllegalArgumentException("Can't display typing message in a cell");
            case TEXT:
                return 0 + delta;
            case IMAGE:
                return 1 + delta;
            case CONTACT:
                return 2 + delta;
            case GEO:
                return 3 + delta;
            case AUDIO:
                return 4 + delta;
            case VIDEO:
                return 5 + delta;
            default:
                throw new IllegalArgumentException("Unknown message type");
        }
    }

    public interface Listener extends ImageCellListener, AudioPlayerDownloadListener, VideoCellListener {
    }

    static class HeaderViewHolder extends SectionedViewHolder {

        @BindView(R.id.date_txt)
        public TextView dateTextView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void updateFor(ChatSection section, boolean isNewSenderDeviceId) {
            dateTextView.setText(section.formatTitle(isNewSenderDeviceId));
        }
    }
}
