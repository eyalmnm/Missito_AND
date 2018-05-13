package ch.mitto.missito.db.model.attach;

import java.io.Serializable;

import ch.mitto.missito.net.model.VideoAttachment;
import io.realm.RealmObject;

public class VideoAttachRec extends RealmObject implements Serializable {

    public String title;
    public String fileName;
    public String localFileURI;
    public String link;
    public long size;
    public String secret;
    public String thumbnail;

    public VideoAttachRec() {
    }

    public VideoAttachRec(VideoAttachment videoAttachment) {
        this.title = videoAttachment.title;
        this.fileName = videoAttachment.fileName;
        this.link = videoAttachment.link;
        this.size = videoAttachment.size;
        this.secret = videoAttachment.secret;
        this.thumbnail = videoAttachment.thumbnail;
    }

    public VideoAttachRec(String title, String fileName, String link, long size, String secret, String thumbnail, String localFileURI) {
        this.title = title;
        this.fileName = fileName;
        this.link = link;
        this.size = size;
        this.secret = secret;
        this.thumbnail = thumbnail;
        this.localFileURI = localFileURI;
    }
}
