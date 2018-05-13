package ch.mitto.missito.db.model.attach;

import ch.mitto.missito.net.model.ImageAttachment;
import ch.mitto.missito.util.Helper;
import io.realm.RealmObject;

import java.io.File;
import java.io.Serializable;

public class ImageAttachRec extends RealmObject implements Serializable {

    public String fileName;
    public String localFileURI;
    public String link;
    public long size;
    public String secret;
    public String thumbnail;

    public ImageAttachRec() {
    }

    public ImageAttachRec(String fileName, String localFileURI, String link, long size, String secret, String thumbnail) {
        this.fileName = fileName;
        this.localFileURI = localFileURI;
        this.link = link;
        this.size = size;
        this.secret = secret;
        this.thumbnail = thumbnail;
    }

    public ImageAttachRec(ImageAttachment imageAttachment, String localFileURI) {
        this.fileName = imageAttachment.fileName;
        this.localFileURI = localFileURI;
        this.link = imageAttachment.link;
        this.size = imageAttachment.size;
        this.secret = imageAttachment.secret;
        this.thumbnail = imageAttachment.thumbnail;
    }

    public ImageAttachRec(File imageFile, String thumbnail) {
        this(imageFile.getName(), Helper.getUriFromFile(imageFile).toString(), null, imageFile.length(), null, thumbnail);
    }
}
