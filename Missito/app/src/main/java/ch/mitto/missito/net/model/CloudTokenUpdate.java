package ch.mitto.missito.net.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CloudTokenUpdate implements Serializable {

    public String token;
    @SerializedName("token_type")
    public String tokenType;
    public String device;

    public CloudTokenUpdate(String token, String tokenType, String device) {
        this.token = token;
        this.tokenType = tokenType;
        this.device = device;
    }
}
