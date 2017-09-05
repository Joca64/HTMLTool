package dataTypes;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ValidatorResult{

    @SerializedName("url")
    @Expose
    private String URL;
    @SerializedName("messages")
    @Expose
    private List<ValidationMessage> messages;
    @SerializedName("source")
    @Expose
    private ValidationSource source;

    public String getEncoding() { return this.source.getEncoding(); }

    public String getURL() { return this.URL; }

    public List<ValidationMessage> getMessages() { return this.messages; }
}
