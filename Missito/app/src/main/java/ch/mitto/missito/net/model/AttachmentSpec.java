package ch.mitto.missito.net.model;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by alexgridnev on 6/28/17.
 */
public class AttachmentSpec implements Serializable {

    public String uploadURL;
    public String downloadURL;
    public HashMap<String, String> uploadFields;

    public AttachmentSpec(String uploadURL, String downloadURL, HashMap<String, String> uploadFields) {
        this.uploadURL = uploadURL;
        this.downloadURL = downloadURL;
        this.uploadFields = uploadFields;
    }
}
