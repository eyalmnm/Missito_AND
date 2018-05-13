package ch.mitto.missito.db.model.attach;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

import ch.mitto.missito.net.model.LocationAttachment;
import io.realm.RealmObject;

public class LocationAttachRec extends RealmObject implements Serializable {

    public double lat;
    public double lon;
    public double radius;
    public String label;

    public LocationAttachRec() {
    }

    public LocationAttachRec(LatLng latLng, double radius, String label) {
        this.lat = latLng.latitude;
        this.lon = latLng.longitude;
        this.radius = radius;
        this.label = label;
    }

    public LocationAttachRec(LocationAttachment locationAttachment) {
        this.lat = locationAttachment.lat;
        this.lon = locationAttachment.lon;
        this.radius = locationAttachment.radius;
        this.label = locationAttachment.label;
    }
}
