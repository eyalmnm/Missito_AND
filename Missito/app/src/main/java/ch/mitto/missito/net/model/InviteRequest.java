package ch.mitto.missito.net.model;


import java.io.Serializable;
import java.util.ArrayList;

public class InviteRequest implements Serializable {

    public String lang;
    public ArrayList<String> phones;

    public InviteRequest(String lang, ArrayList<String> phones) {
        this.lang = lang;
        this.phones = phones;
    }
}
