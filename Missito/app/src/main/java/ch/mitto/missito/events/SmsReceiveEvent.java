package ch.mitto.missito.events;

/**
 * Created by usr1 on 10/25/17.
 */

public class SmsReceiveEvent {

    private String receivedCode;

    public SmsReceiveEvent(String receivedCode) {
        this.receivedCode = receivedCode;
    }

    public String getReceivedCode() {
        return receivedCode;
    }
}
