package ch.mitto.missito.net.model;

import java.io.Serializable;

import ch.mitto.missito.db.model.attach.LocationAttachRec;

public class LocationAttachment implements Serializable {

    public double lat;
    public double lon;
    public double radius;
    public String label;

    public LocationAttachment() {
    }

    public LocationAttachment(LocationAttachRec locationAttachRec) {
        this.lat = locationAttachRec.lat;
        this.lon = locationAttachRec.lon;
        this.radius = locationAttachRec.radius;
        this.label = locationAttachRec.label;
    }
}
