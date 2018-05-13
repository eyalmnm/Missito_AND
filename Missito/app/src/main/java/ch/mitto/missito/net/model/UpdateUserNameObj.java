package ch.mitto.missito.net.model;

import java.util.HashMap;

import ch.mitto.missito.util.Helper;

public class UpdateUserNameObj {

    public String name;
    public String uid;

    protected HashMap<String, String> data = new HashMap<String, String>();

    public UpdateUserNameObj(String name, String uid){
        data.put("name", name);
        data.put("uid", Helper.removePlus(uid));
        this.name = name;
        this.uid = uid;
    }
}
