package ch.mitto.missito.ui.tabs.chat.adapter;

import java.io.Serializable;
import java.util.ArrayList;

import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.db.model.attach.AudioAttachRec;
import ch.mitto.missito.db.model.attach.ContactAttachRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.db.model.attach.LocationAttachRec;
import ch.mitto.missito.db.model.attach.VideoAttachRec;

public class ChatMessageAttachment implements Serializable {

    public ArrayList<ImageAttachRec> images;
    public ArrayList<ContactAttachRec> contacts;
    public ArrayList<LocationAttachRec> locations;
    public ArrayList<AudioAttachRec> audio;
    public ArrayList<VideoAttachRec> video;

    public ChatMessageAttachment(ArrayList<ImageAttachRec> images, ArrayList<ContactAttachRec> contacts,
                                 ArrayList<LocationAttachRec> locations, ArrayList<AudioAttachRec> audio,
                                 ArrayList<VideoAttachRec> video) {
        this.images = images;
        this.contacts = contacts;
        this.locations = locations;
        this.audio = audio;
        this.video = video;
    }

    public ChatMessageAttachment(AttachmentRec attachmentRec) {
        images = attachmentRec.images == null ? null : new ArrayList<>(attachmentRec.images);
        contacts = attachmentRec.contacts == null ? null : new ArrayList<>(attachmentRec.contacts);
        locations = attachmentRec.locations == null ? null : new ArrayList<>(attachmentRec.locations);
        audio = attachmentRec.audio == null ? null : new ArrayList<>(attachmentRec.audio);
        video = attachmentRec.video == null ? null : new ArrayList<>(attachmentRec.video);
    }

    public boolean isEmpty() {
        return  !hasImages() &&
                !hasVideo() &&
                !hasLocations() &&
                !hasAudio() &&
                !hasContacts();
    }

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    public boolean hasVideo() {
        return video != null  && !video.isEmpty();
    }

    public boolean hasLocations() {
        return locations != null && !locations.isEmpty();
    }

    public boolean hasAudio() {
        return audio != null && !audio.isEmpty();
    }

    public boolean hasContacts() {
        return contacts != null && !contacts.isEmpty();
    }
}
