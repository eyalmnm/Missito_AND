package ch.mitto.missito.net.model;

import java.io.Serializable;

import ch.mitto.missito.db.model.attach.VideoAttachRec;

public class VideoAttachment implements Serializable {

    public String title;
    public String fileName;
    public String link;
    public long size;
    public String secret;
    public String thumbnail;

    public VideoAttachment() {
    }

    public VideoAttachment(VideoAttachRec videoAttachRec) {
        this.title = videoAttachRec.title;
        this.fileName = videoAttachRec.fileName;
        this.link = videoAttachRec.link;
        this.size = videoAttachRec.size;
        this.secret = videoAttachRec.secret;
        this.thumbnail = videoAttachRec.thumbnail;
    }
}
