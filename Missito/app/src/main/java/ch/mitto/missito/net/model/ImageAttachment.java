package ch.mitto.missito.net.model;

import java.io.Serializable;

import ch.mitto.missito.db.model.attach.ImageAttachRec;

public class ImageAttachment implements Serializable {

    public String fileName;
    public String link;
    public long size;
    public String secret;
    public String thumbnail;

    public ImageAttachment(ImageAttachRec imageAttachRec) {
        this.fileName = imageAttachRec.fileName;
        this.link = imageAttachRec.link;
        this.size = imageAttachRec.size;
        this.secret = imageAttachRec.secret;
        this.thumbnail = imageAttachRec.thumbnail;
    }
}
