package ch.mitto.missito.net.model;

import com.google.gson.annotations.SerializedName;

public enum Qos {
    @SerializedName("transient")
    TRANSIENT,
    @SerializedName("regular")
    REGULAR,
    @SerializedName("mandatory")
    MANDATORY
}
