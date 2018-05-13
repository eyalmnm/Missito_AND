package ch.mitto.missito.security.signal.model;

import com.google.gson.annotations.SerializedName;

public class SignedPreKeyData {

    @SerializedName("id")
    public int keyId;

    @SerializedName("data")
    public String key;

    @SerializedName("signature")
    public String keySignature;

    public SignedPreKeyData(int keyId, String key, String keySignature) {
        this.keyId = keyId;
        this.key = key;
        this.keySignature = keySignature;
    }
}
