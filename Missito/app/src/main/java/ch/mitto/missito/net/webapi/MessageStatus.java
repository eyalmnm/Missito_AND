package ch.mitto.missito.net.webapi;


public enum MessageStatus {

    RECEIVED(0),
    DELIVERED(1),
    ACKNOWLEDGED(2),
    SEEN(3);

    public int value;

    MessageStatus(int value) {
        this.value = value;
    }

}
