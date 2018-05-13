package ch.mitto.missito.events;

/**
 * Created by usr1 on 10/25/17.
 */

public class ClearHistoryEvent {

    private String contactPhone;

    public ClearHistoryEvent(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactPhone() {
        return contactPhone;
    }
}
