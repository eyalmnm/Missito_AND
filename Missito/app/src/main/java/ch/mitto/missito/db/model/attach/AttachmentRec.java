package ch.mitto.missito.db.model.attach;

import java.io.Serializable;

import ch.mitto.missito.net.model.Attachment;
import ch.mitto.missito.net.model.AudioAttachment;
import ch.mitto.missito.net.model.ContactAttachment;
import ch.mitto.missito.net.model.ImageAttachment;
import ch.mitto.missito.net.model.LocationAttachment;
import ch.mitto.missito.net.model.VideoAttachment;
import io.realm.RealmList;
import io.realm.RealmObject;

public class AttachmentRec extends RealmObject {

    public RealmList<ImageAttachRec> images;
    public RealmList<ContactAttachRec> contacts;
    public RealmList<LocationAttachRec> locations;
    public RealmList<AudioAttachRec> audio;
    public RealmList<VideoAttachRec> video;

    public AttachmentRec() {
    }

    public AttachmentRec(RealmList<ImageAttachRec> images,
                         RealmList<ContactAttachRec> contacts,
                         RealmList<LocationAttachRec> locations,
                         RealmList<AudioAttachRec> audio, RealmList<VideoAttachRec> video) {
        this.images = images;
        this.contacts = contacts;
        this.locations = locations;
        this.audio = audio;
        this.video = video;
    }

    public static AttachmentRec make(Attachment from) {
        if (from == null) {
            return null;
        }

        RealmList<ImageAttachRec> imageAttachRecs = null;
        if (from.images != null) {
            imageAttachRecs = new RealmList<>();
            for (ImageAttachment image : from.images) {
                imageAttachRecs.add(
                        new ImageAttachRec(image, null));
            }
        }

        RealmList<ContactAttachRec> contactAttachRecs = null;
        if (from.contacts != null) {
            contactAttachRecs = new RealmList<>();
            for (ContactAttachment contact : from.contacts) {
                contactAttachRecs.add(new ContactAttachRec(contact));
            }
        }

        RealmList<LocationAttachRec> locationAttachRecs = null;
        if (from.locations != null) {
            locationAttachRecs = new RealmList<>();
            for (LocationAttachment location : from.locations) {
                locationAttachRecs.add(
                        new LocationAttachRec(location));
            }
        }

        RealmList<AudioAttachRec> audioAttachRec = null;
        if (from.audio != null) {
            audioAttachRec = new RealmList<>();
            for (AudioAttachment audio : from.audio) {
                audioAttachRec.add(new AudioAttachRec(audio));
            }
        }

        RealmList<VideoAttachRec> videoAttachRec = null;
        if (from.video != null) {
            videoAttachRec = new RealmList<>();
            for (VideoAttachment video : from.video) {
                videoAttachRec.add(new VideoAttachRec(video));
            }
        }

        return new AttachmentRec(imageAttachRecs, contactAttachRecs, locationAttachRecs, audioAttachRec, videoAttachRec);
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
