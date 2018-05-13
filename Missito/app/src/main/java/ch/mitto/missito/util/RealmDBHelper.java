package ch.mitto.missito.util;


import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ch.mitto.missito.Application;
import ch.mitto.missito.model.message.ContactData;
import ch.mitto.missito.db.model.ChatRec;
import ch.mitto.missito.db.model.ContactRec;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.db.model.attach.AudioAttachRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.net.broker.model.ContactEntry;
import ch.mitto.missito.ui.tabs.chat.ClearHistoryOption;
import ch.mitto.missito.ui.tabs.chats.model.Chat;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.db.model.attach.VideoAttachRec;
import ch.mitto.missito.net.broker.model.IncomingMessage;
import ch.mitto.missito.net.model.IncomingMessageStatus;
import ch.mitto.missito.net.model.MessageBody;
import ch.mitto.missito.net.model.OutgoingMessageStatus;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class RealmDBHelper {

    private static final String LOG_TAG = RealmDBHelper.class.getSimpleName();

    /*
     *CONTACTS
     *
     */
    public static void addMissitoContacts(final ArrayList<ContactEntry> newContacts, final Date availableSince) {
        if (!newContacts.isEmpty()) {
            final Realm realm = Application.app.connectionManager.realm;
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    for (ContactEntry contact : newContacts) {
                        String phone = Helper.addPlus(contact.userId);
                        realm.copyToRealmOrUpdate(new ContactRec(
                                Application.app.contacts.getContactName(phone), phone, contact.deviceId, availableSince));
                    }
                }
            });
        }
    }

    public static List<ContactRec> getMissitoContacts() {
        Realm realm = Application.app.connectionManager.realm;
        return realm.where(ContactRec.class).findAll();
    }

    public static void addAllContacts(final List<ContactData> contact) {
        Application.app.connectionManager.realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (ContactData item : contact) {
                    realm.copyToRealmOrUpdate(new ContactData(item));
                }
            }
        });

        for (final ContactData item : contact) {
            Application.app.connectionManager.realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmQuery<ContactData> contactData = realm.where(ContactData.class).contains("phone", item.phone);
                    if (contactData.count() == 0) {
                        realm.copyToRealmOrUpdate(new ContactData(item));
                    }
                }
            });
        }
    }

    public static ArrayList<ContactData> getAllContacts() {
        RealmResults<ContactData> all = Application.app.connectionManager.realm.where(ContactData.class).findAll();
        ArrayList<ContactData> contacts = new ArrayList<>();
        for (ContactData contact : all) {
            contacts.add(contact);
        }
        return contacts;
    }


    /*
     *CHATS
     *
     */
    public static ArrayList<Chat> getChats() {
        Realm realm = Application.app.connectionManager.realm;

        ArrayList<Chat> chats = new ArrayList<>();

        RealmResults<ChatRec> all = realm.where(ChatRec.class).findAllSorted("lastMessage.timestamp", Sort.DESCENDING);
        for (ChatRec chatRec : all) {
            chats.add(new Chat(chatRec));
        }

        return chats;
    }

    public static int getChatsCount() {
        Realm realm = Application.app.connectionManager.realm;
        return (int) realm.where(ChatRec.class).count();
    }

    public static boolean existsChats() {
        return getChatsCount() > 0;
    }

    public static ChatRec getChat(String contactPhone) {
        return Application.app.connectionManager.realm
                .where(ChatRec.class)
                .contains("id", contactPhone)
                .findFirst();
    }

    /**
     * Increments unreadCount property for given contact.
     * Note: Must call this from a Realm write transaction
     *
     * @param contact
     */
    public static void incrementUnreadCount(final MissitoContact contact) {
        contact.unreadCount++;
        Realm realm = Application.app.connectionManager.realm;
        Integer deviceId = Application.app.contacts.deviceIds.get(contact.phone);
        if (deviceId == null) {
            deviceId = 0;
        }
        realm.copyToRealmOrUpdate(new ContactRec(contact, deviceId));
    }

    public static void updateContactLastActiveDeviceId(final MissitoContact contact, final int deviceId) {
        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(new ContactRec(contact, deviceId));
            }
        });
    }

    public static void resetUnreadCount(final MissitoContact contact) {
        contact.unreadCount = 0;
        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Integer deviceId = Application.app.contacts.deviceIds.get(contact.phone);
                if (deviceId == null) {
                    deviceId = 0;
                }
                realm.copyToRealmOrUpdate(new ContactRec(contact, deviceId));

                ChatRec chatRec = realm.where(ChatRec.class).contains("id", contact.phone).findFirst();
                if (chatRec != null) {
                    chatRec.unreadCount = 0;
                }
            }
        });
    }

    public static RealmResults<MessageRec> getChatHistory(String interlocutor){
        String phone = Application.app.connectionManager.uid;
        return Application.app.connectionManager
                .realm
                .where(MessageRec.class)
                .in("senderUid", new String[]{phone, interlocutor})
                .in("destUid", new String[]{phone, interlocutor})
                .findAllSorted("timestamp");
    }

    public static void clearChatHistory(final String interlocutor, final ClearHistoryOption clearHistoryOption) {
        Application.app.connectionManager.realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                switch (clearHistoryOption) {
                    case LAST_HOUR:
                        c.add(Calendar.HOUR, -1);
                        break;
                    case LAST_DAY:
                        c.add(Calendar.DAY_OF_YEAR, -1);
                        break;
                    default:
                        c.setTimeInMillis(0);
                }

                RealmResults<MessageRec> messagesToDelete = getChatHistory(interlocutor).where()
                        .greaterThan("timestamp", c.getTimeInMillis()).findAll();

                if (messagesToDelete == null || messagesToDelete.size() == 0 ) {
                    return;
                }

                //delete files of attachments
                for (MessageRec messageRec : messagesToDelete) {
                    AttachmentRec attachmentRec = messageRec.attach;
                    if (attachmentRec == null) {
                        continue;
                    }
                    if (attachmentRec.hasImages() && attachmentRec.images.get(0).localFileURI != null) {
                        FileUtils.deleteQuietly(new File(URI.create(attachmentRec.images.get(0).localFileURI)));
                    }
                    if (attachmentRec.hasVideo() && attachmentRec.video.get(0).localFileURI != null) {
                        FileUtils.deleteQuietly(new File(URI.create(attachmentRec.video.get(0).localFileURI)));
                    }
                    if (attachmentRec.hasAudio() && attachmentRec.audio.get(0).localFileURI != null) {
                        FileUtils.deleteQuietly(new File(URI.create(attachmentRec.audio.get(0).localFileURI)));
                    }
                }
                messagesToDelete.deleteAllFromRealm();

                ChatRec chat = getChat(interlocutor);
                RealmResults<MessageRec> chatHistory = getChatHistory(interlocutor);
                if (chatHistory.size() == 0) {
                    if (chat != null) chat.deleteFromRealm();
                    MissitoConfig.clearAttachments(interlocutor);
                } else {
                    chat.lastMessage = chatHistory.last();
                }
            }
        });
    }


    /*
     *MESSAGES
     *
     */
    public static MessageRec getMessageByLocalId(String localId) {
        return Application.app.connectionManager.realm.where(MessageRec.class).contains("localMsgId", localId).findFirst();
    }

    public static MessageRec getMessageByServerId(String serverId) {
        return Application.app.connectionManager.realm.where(MessageRec.class).contains("serverMsgId", serverId).findFirst();
    }

    public static void setAudioMsgDownloadLink(MessageRec msg, String downloadURL) {
        Application.app.connectionManager.realm.beginTransaction();
        msg.attach.audio.first().link = downloadURL;
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void setAudioMsgFileURI(AudioAttachRec audio, String fileURI) {
        Application.app.connectionManager.realm.beginTransaction();
        audio.localFileURI = fileURI;
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void setVideoMsgFileURI(VideoAttachRec video, String fileURI) {
        Application.app.connectionManager.realm.beginTransaction();
        video.localFileURI = fileURI;
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void setImageMsgFileURI(ImageAttachRec image, String fileURI) {
        Application.app.connectionManager.realm.beginTransaction();
        image.localFileURI = fileURI;
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void removeLocalFileURIFromImages(RealmList<ImageAttachRec> images) {
        Application.app.connectionManager.realm.beginTransaction();
        if (images != null) {
            for (ImageAttachRec image : images) {
                image.localFileURI = null;
            }
        }
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void removeLocalFileURIFromVideo(RealmList<VideoAttachRec> video) {
        Application.app.connectionManager.realm.beginTransaction();
        if (video != null) {
            for (VideoAttachRec image : video) {
                image.localFileURI = null;
            }
        }
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void saveIncomingMessage(final MessageBody messageBody, final IncomingMessage incomingMessage) {
        Application.app.connectionManager.realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {

                MessageRec message;
                if ((message = realm.where(MessageRec.class).contains("uniqueId", messageBody.uniqueId).findFirst()) != null) {
                    Log.w(LOG_TAG, String.format("Duplicate message with uniqueId=%s", messageBody.uniqueId));
                    message.serverMsgId = incomingMessage.id;
                    return;
                }

                Application.app.contacts.incrementContactUnreadCount(incomingMessage.senderUid);

                message = realm.where(MessageRec.class).contains("serverMsgId", incomingMessage.id).findFirst();
                if (message != null) {
                    message.body = messageBody.text;
                    message.attach = AttachmentRec.make(messageBody.attach);
                } else {
                    message = realm.copyToRealmOrUpdate(new MessageRec(incomingMessage, messageBody, IncomingMessageStatus.RECEIVED, null));
                }

                ChatRec chatRec = realm.where(ChatRec.class).contains("id", message.senderUid).findFirst();
                if (chatRec == null) {
                    MissitoContact contact = Application.app.contacts.missitoContactsByPhone.get(message.senderUid);
                    Integer deviceId = Application.app.contacts.deviceIds.get(contact.phone);
                    if (deviceId == null) {
                        deviceId = 0;
                    }
                    ContactRec participant = new ContactRec(contact, deviceId);
                    chatRec = new ChatRec(message, message.senderUid, 0, new RealmList<>(participant));
                    chatRec.participants.add(participant);
                }

                chatRec.lastMessage = message;
                chatRec.unreadCount++;
                realm.insertOrUpdate(chatRec);
                NotificationHelper.notifyMessageSaved(message);
            }
        });
    }

    public static void saveOutgoingMessage(final MessageRec message) {
        Application.app.connectionManager.realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Log.d(LOG_TAG, String.format("Outgoing message saved in DB with id [%s]", message.localMsgId));
                MessageRec messageRec = realm.copyToRealmOrUpdate(message);

                ChatRec chatRec = realm.where(ChatRec.class).contains("id", message.destUid).findFirst();
                if (chatRec == null) {
                    MissitoContact contact = Application.app.contacts.missitoContactsByPhone.get(message.destUid);
                    Integer deviceId = Application.app.contacts.deviceIds.get(contact.phone);
                    if (deviceId == null) {
                        deviceId = 0;
                    }
                    ContactRec contactRec = new ContactRec(contact, deviceId);
                    chatRec = new ChatRec(message, message.destUid, 0, new RealmList<>(contactRec));
                    chatRec.participants.add(contactRec);
                }
                chatRec.lastMessage = messageRec;
                realm.insertOrUpdate(chatRec);
            }
        });
    }

    public static void changeURIForImagesToLocal(MessageRec message, String destinationPhone) {
        Application.app.connectionManager.realm.beginTransaction();
        if (message.attach.images != null) {
            RealmList<ImageAttachRec> images = message.attach.images;
            for (ImageAttachRec image :images) {
                File file = new File(MissitoConfig.getAttachmentsPath(destinationPhone) + image.fileName);
                if (file.exists()) {
                    image.localFileURI = Helper.getUriFromFile(file).toString();
                } else {
                    Log.w(LOG_TAG, String.format("Could not change 'realmImage.localFileURI` from [%s] to [%s]", image.localFileURI, file.getAbsolutePath()));
                }
            }
            message.attach.images = images;
        }
        Application.app.connectionManager.realm.copyToRealmOrUpdate(message);
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void updateLinkAndSecretForImage(ImageAttachRec realmImage, String link, String secret){
        Application.app.connectionManager.realm.beginTransaction();
        realmImage.link = link;
        realmImage.secret = secret;
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void changeURIForVideoToLocal(MessageRec message, String destinationPhone) {
        Application.app.connectionManager.realm.beginTransaction();
        if (message.attach.video != null) {
            RealmList<VideoAttachRec> videoRecords = message.attach.video;
            for (VideoAttachRec videoAttachRec : videoRecords) {
                File file = new File(MissitoConfig.getAttachmentsPath(destinationPhone) + videoAttachRec.fileName);
                if (file.exists()) {
                    videoAttachRec.localFileURI = Helper.getUriFromFile(file).toString();
                } else {
                    Log.w(LOG_TAG, String.format("Could not change 'videoAttachRec.localFileURI` from [%s] to [%s]: file doesn't exist!", videoAttachRec.localFileURI, file.getAbsolutePath()));
                }
            }
            message.attach.video = videoRecords;
        }
        Application.app.connectionManager.realm.copyToRealmOrUpdate(message);
        Application.app.connectionManager.realm.commitTransaction();
    }

    public static void updateLinkAndSecretForVideo(VideoAttachRec realmVideo, String link, String secret){
        Application.app.connectionManager.realm.beginTransaction();
        realmVideo.link = link;
        realmVideo.secret = secret;
        Application.app.connectionManager.realm.commitTransaction();
    }




    /*
     *MESSAGE STATUS
     *
     */
    public static String setOutgoingMsgStatus(final String serverMsgId, final String status) {
        Realm realm = Application.app.connectionManager.realm;
        final MessageRec msg = realm.where(MessageRec.class).contains("serverMsgId", serverMsgId).findFirst();
        if (msg != null && isNotSeen(msg)) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    msg.outgoingStatus = status;
                    Log.d(LOG_TAG, String.format("Updated outgoing message with server id [%2$s] to status %1$s ", status, msg.serverMsgId));
                }
            });
            return msg.localMsgId;
        } else {
            Log.d(LOG_TAG, String.format("Could not find message with server id [%1$s]", serverMsgId));
            return null;
        }
    }

    public static void setOutgoingMsgStatus(final String localMsgId, final String serverMsgId, final OutgoingMessageStatus status) {
        Realm realm = Application.app.connectionManager.realm;
        final MessageRec message = realm.where(MessageRec.class).contains("localMsgId", localMsgId).findFirst();
        if (message != null) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    message.serverMsgId = serverMsgId;
                    message.outgoingStatus = status.value;

                    Log.d(LOG_TAG, String.format("Updated outgoing message with server id [%2$s] to status %1$s ", status, message.serverMsgId));
                }
            });
        } else {
            Log.d(LOG_TAG, String.format("Could not find message with server id [%1$s]", serverMsgId));
        }
    }

    public static void setIncomingMsgStatus(final String serverMsgId, final IncomingMessageStatus status) {
        Realm realm = Application.app.connectionManager.realm;
        final MessageRec message = realm.where(MessageRec.class).contains("serverMsgId", serverMsgId).findFirst();
        if (message != null) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    message.incomingStatus = status.value;
                    Log.d(LOG_TAG, String.format("Updated outgoing message with server id [%2$s] to status %1$s ", status, message.serverMsgId));
                }
            });
        } else {
            Log.d(LOG_TAG, String.format("Could not find message with server id [%1$s]", serverMsgId));
        }
    }

    public static void setIncomingMsgStatus(List<String> messageIds, IncomingMessageStatus status) {
        for (String messagesId : messageIds) {
            setIncomingMsgStatus(messagesId, status);
        }
    }

    private static boolean isNotSeen(MessageRec msg) {
        return !OutgoingMessageStatus.SEEN.value.equals(msg.outgoingStatus);
    }

}
