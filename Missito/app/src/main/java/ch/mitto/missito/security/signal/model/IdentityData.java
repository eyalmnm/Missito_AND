package ch.mitto.missito.security.signal.model;

public class IdentityData {

    public int regId;

    public String identityPK;

    public IdentityData(int registrationId, String identityPublicKey) {
        regId = registrationId;
        identityPK = identityPublicKey;
    }
}
