package ch.mitto.missito.security.signal.model;

import com.google.gson.annotations.SerializedName;

public class OTPKeyData {

    @SerializedName("id")
    public int keyId;

    @SerializedName("data")
    public String key;

}
