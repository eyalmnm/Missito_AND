package ch.mitto.missito.security.signal.model;

public class OTPKeysData {

    public int startId;

    public String[] keys;

    public OTPKeysData(int startId, String[] keys) {
        this.startId = startId;
        this.keys = keys;
    }
}
