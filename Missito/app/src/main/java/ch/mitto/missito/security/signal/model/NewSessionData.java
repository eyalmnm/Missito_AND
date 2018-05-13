package ch.mitto.missito.security.signal.model;

import com.google.gson.annotations.SerializedName;

public class NewSessionData {

    public IdentityData identity;

    @SerializedName("signed_pre_key_public")
    public SignedPreKeyData signedPreKey;

    public OTPKeyData otpk;

}
