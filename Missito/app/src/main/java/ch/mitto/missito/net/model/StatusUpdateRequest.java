package ch.mitto.missito.net.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StatusUpdateRequest implements Serializable {
    public ArrayList<String> received = new ArrayList<>();
    public ArrayList<String> seen = new ArrayList<>();

    public StatusUpdateRequest(List<String> received, List<String> seen) {
        if (seen != null) {
            this.seen.addAll(seen);
        }

        if (received != null) {
            this.received.addAll(received);
        }
    }
}
