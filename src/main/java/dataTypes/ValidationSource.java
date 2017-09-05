package dataTypes;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ValidationSource {
    @SerializedName("encoding")
    @Expose
    private String encoding;
    @SerializedName("type")
    @Expose
    private String type;

    public String getEncoding() { return this.encoding; }
}
