package ch.mitto.missito.net.model;

import java.io.Serializable;
import java.util.ArrayList;

import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.db.model.attach.AudioAttachRec;
import ch.mitto.missito.db.model.attach.ContactAttachRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.db.model.attach.LocationAttachRec;
import ch.mitto.missito.db.model.attach.VideoAttachRec;

public class Attachment implements Serializable {

    public ArrayList<ImageAttachment> images;
    public ArrayList<ContactAttachment> contacts;
    public ArrayList<LocationAttachment> locations;
    public ArrayList<AudioAttachment> audio;
    public ArrayList<VideoAttachment> video;

    public Attachment(ArrayList<ImageAttachment> images,
                      ArrayList<ContactAttachment> contacts,
                      ArrayList<LocationAttachment> locations,
                      ArrayList<AudioAttachment> audio,
                      ArrayList<VideoAttachment> video) {
        this.images = images;
        this.contacts = contacts;
        this.locations = locations;
        this.audio = audio;
        this.video = video;
    }

    public static Attachment make(AttachmentRec from) {
        if (from == null) {
            return null;
        }

        ArrayList<ImageAttachment> imageAttachments = null;
        if (from.images != null) {
            imageAttachments = new ArrayList<>();
            for (ImageAttachRec image : from.images) {
                imageAttachments.add(new ImageAttachment(image));
            }
        }

        ArrayList<ContactAttachment> contactAttachments = null;
        if (from.contacts != null) {
            contactAttachments = new ArrayList<>();
            for (ContactAttachRec contact : from.contacts) {
                contactAttachments.add(
                        new ContactAttachment(contact));
            }
        }

        ArrayList<LocationAttachment> locationAttachments = null;
        if (from.locations != null) {
            locationAttachments = new ArrayList<>();
            for (LocationAttachRec location : from.locations) {
                locationAttachments.add(new LocationAttachment(location));
            }
        }

        ArrayList<AudioAttachment> audioAttachment = null;
        if (from.audio != null) {
            audioAttachment = new ArrayList<>();
            for (AudioAttachRec audio : from.audio) {
                audioAttachment.add(new AudioAttachment(audio));
            }
        }

        ArrayList<VideoAttachment> videoAttachment = null;
        if (from.video != null) {
            videoAttachment = new ArrayList<>();
            for (VideoAttachRec video : from.video) {
                videoAttachment.add(new VideoAttachment(video));
            }
        }

        return new Attachment(imageAttachments, contactAttachments, locationAttachments, audioAttachment, videoAttachment);
    }
}
