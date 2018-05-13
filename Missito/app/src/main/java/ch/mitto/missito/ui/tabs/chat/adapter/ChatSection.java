package ch.mitto.missito.ui.tabs.chat.adapter;


import android.ch.mitto.missito.R;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import ch.mitto.missito.Application;

import static android.text.format.DateUtils.FORMAT_SHOW_TIME;

class ChatSection {

    private static final long SECTION_DT = 2 * 60 * 60;

    public ArrayList<ChatMessage> messages = new ArrayList<>();
    public int counterpartyDeviceId = -1;

    public ChatSection(int counterpartyDeviceId) {
        this.counterpartyDeviceId = counterpartyDeviceId;
    }

    public int size() {
        return messages.size();
    }

    public void append(ChatMessage message) {
        messages.add(message);
        updateInGroupTypeWithNieghbours(messages.size() - 1);
    }

    public ChatMessage remove(int index) {
        ChatMessage message = messages.remove(index);
        if (index > 0) {
            updateInGroupType(index - 1);
        }
        if (index < messages.size()) {
            updateInGroupType(index);
        }
        return message;
    }

    private ChatMessage removeLast() {
        if (!messages.isEmpty()) {
            return messages.remove(messages.size() - 1);
        }
        return null;
    }

    private void updateInGroupTypeWithNieghbours(int index) {
        updateInGroupType(index);
        if (index > 0) {
            updateInGroupType(index - 1);
        }
        if (index < messages.size() - 1) {
            updateInGroupType(index + 1);
        }
    }

    private void updateInGroupType(int index) {
        ChatMessage message = messages.get(index);
        ChatMessage prevMessage = null;
        ChatMessage nextMessage = null;

        if (index > 0) {
            prevMessage = messages.get(index - 1);
        }
        if (index < messages.size() - 1) {
            nextMessage = messages.get(index + 1);
        }

        boolean starting = prevMessage == null || prevMessage.direction != message.direction;
        boolean ending = nextMessage == null || nextMessage.direction != message.direction;

        if (starting) {
            message.inGroupType = ending ? ChatMessage.MessageInGroupType.SINGLE : ChatMessage.MessageInGroupType.FIRST;
        } else if (ending) {
            message.inGroupType = ChatMessage.MessageInGroupType.LAST;
        } else {
            message.inGroupType = ChatMessage.MessageInGroupType.MIDDLE;
        }
    }

    public void insert(ChatMessage message) {
        if (messages.isEmpty()) {
            append(message);
            return;
        }
        for (int i = messages.size() - 1 ; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.date.before(message.date)) {
                messages.add(i + 1, message);
                updateInGroupTypeWithNieghbours(i + 1);
                return;
            }
        }
        messages.add(0, message);
        updateInGroupTypeWithNieghbours(0);
    }

    public boolean shouldIncludeNext(ChatMessage message) {
        if (messages.isEmpty()) {
            return  true;
        }
        Date lastMessageDate = messages.get(messages.size() - 1).date;
        Date newMessageDate = message.date;

        if (!DateUtils.isSameDay(lastMessageDate, newMessageDate)) {
            return false;
        }
        return newMessageDate.getTime() - lastMessageDate.getTime() < SECTION_DT * 1000L
                && hasSuitableSenderDeviceId(message);
    }

    private boolean hasSuitableSenderDeviceId(ChatMessage message) {
        return counterpartyDeviceId == -1 || message instanceof OutgoingChatMessage
                || (((IncomingChatMessage)message).senderDeviceId == counterpartyDeviceId);

    }

    public boolean tooLateFor(ChatMessage message) {
        if (messages.isEmpty()) {
            return false;
        }
        Date firstMessageDate = messages.get(0).date;
        Date newMessageDate = message.date;

        long firstMessageTime = firstMessageDate.getTime();
        long newMessageTime = newMessageDate.getTime();

        if (firstMessageTime <= newMessageTime) {
            return false;
        }

        if (firstMessageTime - newMessageTime >= SECTION_DT * 1000L) {
            return true;
        }

        return !DateUtils.isSameDay(firstMessageDate, newMessageDate);
    }

    public boolean tooEarlyFor(ChatMessage message) {
        if (messages.isEmpty()) {
            return false;
        }

        Date lastMessageDate = messages.get(messages.size() - 1).date;
        Date newMessageDate = message.date;

        long lastMessageTime = lastMessageDate.getTime();
        long newMessageTime = newMessageDate.getTime();

        if (lastMessageTime >= newMessageTime) {
            return false;
        }

        if (newMessageTime - lastMessageTime >= SECTION_DT * 1000L) {
            return true;
        }

        return !DateUtils.isSameDay(lastMessageDate, newMessageDate);
    }

    public String formatTitle(boolean isNewSenderDeviceId) {
        if (messages.isEmpty()) {
            return "";
        } else {
            Date date = messages.get(0).date;
            String time = android.text.format.DateUtils.formatDateTime(Application.app, date.getTime(), FORMAT_SHOW_TIME);

            Date currentDate = new Date();

            String deviceIdInfo = "";
            if (isNewSenderDeviceId) {
                deviceIdInfo = Application.app.getString(R.string.user_switched_to_device);
            }

            if (DateUtils.isSameDay(date, currentDate)) {
                return time + deviceIdInfo;
            }

            Calendar currentCalendar = Calendar.getInstance();
            Calendar messageDateBasedCalendar = Calendar.getInstance();
            messageDateBasedCalendar.setTime(date);

            if (messageDateBasedCalendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {
                return DateFormatUtils.format(date, "EEE. dd, ") + time + deviceIdInfo;
            }

            if (messageDateBasedCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
                return DateFormatUtils.format(date, "MMMM EEE. dd, ") + time + deviceIdInfo;
            }

            return DateFormatUtils.format(date, "yyyy MMMM EEE. dd, ") + time + deviceIdInfo;
        }
    }

    public ChatSection[] split(ChatMessage splitMessage, int secondSectionDeviceId) {
        Date splitDate = splitMessage.date;
        ChatSection sectionBefore = new ChatSection(counterpartyDeviceId);
        ChatSection sectionAfter = new ChatSection(secondSectionDeviceId);
        sectionAfter.append(splitMessage);
        for (ChatMessage message : messages) {
            if (message.date.before(splitDate)) {
                sectionBefore.append(message);
            } else {
                sectionAfter.append(message);
            }
        }
        return new ChatSection[]{sectionBefore, sectionAfter};
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

}
