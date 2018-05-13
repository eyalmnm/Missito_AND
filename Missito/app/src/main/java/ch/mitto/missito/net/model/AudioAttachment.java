package ch.mitto.missito.net.model;

import java.io.Serializable;

import ch.mitto.missito.db.model.attach.AudioAttachRec;

public class AudioAttachment implements Serializable {

    public String title;
    public String fileName;
    public String link;
    public long size;
    public String secret;

    public AudioAttachment() {
    }

    public AudioAttachment(AudioAttachRec audioAttachRec) {
        this.title = audioAttachRec.title;
        this.fileName = audioAttachRec.fileName;
        this.link = audioAttachRec.link;
        this.size = audioAttachRec.size;
        this.secret = audioAttachRec.secret;
    }
}
