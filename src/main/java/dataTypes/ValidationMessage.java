package dataTypes;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ValidationMessage {
    @SerializedName("lastLine")
    @Expose
    private int lastLine;
    @SerializedName("lastColumn")
    @Expose
    private int lastColumn;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("messageid")
    @Expose
    private String messageid;
    @SerializedName("explanation")
    @Expose
    private String explanation;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("subtype")
    @Expose
    private String subtype;

    public String getType() { return this.type; }

    public String getSubtype() { return this.subtype; }

    public String getMessage() { return this.message; }

    public String getMessageID() { return this.messageid; }
}
