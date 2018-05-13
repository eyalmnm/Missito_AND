package ch.mitto.missito.net.model;

import java.util.ArrayList;
import java.util.List;

public class ContactsStatusUpdateRequest {

    public ArrayList<String> normal = new ArrayList<>();
    public ArrayList<String> block = new ArrayList<>();
    public ArrayList<String> muted = new ArrayList<>();

    public ContactsStatusUpdateRequest(List<String> normal, List<String> block, List<String> muted) {
        if (normal != null) {
            this.normal.addAll(normal);
        }

        if (block != null) {
            this.block.addAll(block);
        }

        if (muted != null) {
            this.muted.addAll(muted);
        }
    }
}
