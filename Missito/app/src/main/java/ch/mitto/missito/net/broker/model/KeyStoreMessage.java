package ch.mitto.missito.net.broker.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class KeyStoreMessage implements Serializable {

    @SerializedName("otpKeysLow")
    protected int otpkLow;

    public boolean isOTPKLow() {
        return otpkLow == 1;
    }
}
