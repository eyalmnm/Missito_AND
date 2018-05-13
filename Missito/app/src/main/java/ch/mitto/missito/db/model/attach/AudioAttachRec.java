package ch.mitto.missito.db.model.attach;


import java.io.Serializable;

import ch.mitto.missito.net.model.AudioAttachment;
import io.realm.RealmObject;

public class AudioAttachRec extends RealmObject implements Serializable {

    public String title;
    public String fileName;
    public String localFileURI;
    public String link;
    public long size;
    public String secret;

    public AudioAttachRec() {
    }

    public AudioAttachRec(String title, String fileName, String link, long size, String secret, String localFileURI) {
        this.title = title;
        this.fileName = fileName;
        this.link = link;
        this.size = size;
        this.secret = secret;
        this.localFileURI = localFileURI;
    }

    public AudioAttachRec(AudioAttachment audioAttachment) {
        this.title = audioAttachment.title;
        this.fileName = audioAttachment.fileName;
        this.link = audioAttachment.link;
        this.size = audioAttachment.size;
        this.secret = audioAttachment.secret;
    }
}
