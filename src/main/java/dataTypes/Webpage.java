package dataTypes;

import Utils.Utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

public class Webpage {
    //WARC metadata
    private String WARCname;
    private String resourceType;
    private URI uri;
    private URI originalURI;
    private String HTML;
    private String HTTPCode;
    private String server;
    private String contentType;

    //HTML parsing data
    private float fileSize;
    private float sizeText;
    private float sizeElements;
    private float sizeComments;
    private float textToElementsRatio;
    private float textToSizeRatio;
    private float elementsToSizeRatio;
    private float commentsToSizeRatio;
    private int numHTMLelem;
    private String doctype;
    private HashMap<String, Integer> elements;
    private HashMap<String, Integer> linkProtocols;
    private HashMap<String, Integer> imageFormats;
    private HashMap<String, HashMap<String, Integer>> elementAttributes;
    private int numLinks;
    private int numLinksInt;
    private int numLinksBook;
    private int numLinksExt;
    private int numComments;
    private String charset;
    private String textContent;
    private String commentsContent;
    private int numImages;

    //Validation data
    private int numErrors;
    private int numWarnings;
    private HashMap<String, Integer> errors;
    private HashMap<String, Integer> warnings;
    private HashMap<String, HashMap<String, Integer>> errorVariables;
    private HashMap<String, HashMap<String, Integer>> warningVariables;

    public Webpage(String name) {
        this.WARCname = name.substring(0, name.length()-5); //Filename minus .warc extension
        this.resourceType = "NA";
        this.uri = null;
        this.originalURI = null;
        this.HTML = "";
        this.HTTPCode = "NA";
        this.server = "NA";
        this.contentType = "NA";

        this.fileSize = 0;
        this.sizeText = 0;
        this.sizeElements = 0;
        this.sizeComments = 0;
        this.textToElementsRatio = 0;
        this.textToSizeRatio = 0;
        this.elementsToSizeRatio = 0;
        this.commentsToSizeRatio = 0;
        this.numHTMLelem = 0;
        this.doctype = "This document does not declare its DOCTYPE";
        this.elements = new HashMap<String, Integer>();
        this.linkProtocols = new HashMap<String, Integer>();
        this.imageFormats = new HashMap<String, Integer>();
        this.elementAttributes = new HashMap<String, HashMap<String, Integer>>();
        this.numLinks = 0;
        this.numLinksInt = 0;
        this.numLinksBook = 0;
        this.numLinksExt = 0;
        this.numComments = 0;
        this.charset = "NA";
        this.textContent = "";
        this.commentsContent = "";
        this.numImages = 0;

        this.numErrors = 0;
        this.numWarnings = 0;
        this.errors = new HashMap<String, Integer>();
        this.warnings = new HashMap<String, Integer>();
        this.errorVariables = new HashMap<String, HashMap<String, Integer>>();
        this.warningVariables = new HashMap<String, HashMap<String, Integer>>();
    }

    public URI getOriginalURI() { return this.originalURI; }

    public void setOriginalURI(String uri) {
        try {
            this.originalURI = new URI(uri);
        } catch(Throwable t) {
            this.originalURI = null;
        }
    }

    public String getWARCname() { return this.WARCname; }

    public void setResourceType(String resource) { this.resourceType = resource; }

    public void setURI(String uri) {
        try {
            this.uri = new URI(uri);
        } catch(Throwable t) {
            this.uri = null;
        }
    }

    public void setContentType(String content) { this.contentType = content; }

    public void setHTTPCode(String code) { this.HTTPCode = code; }

    public void setServer(String server) { this.server = server; }

    public void setHTML(String html) { this.HTML = html; }

    public String getHTML() { return this.HTML; }

    public void setFileSize(float size) { this.fileSize = size; }

    public void setNumHTMLelem(int numElem) { this.numHTMLelem = numElem; }

    public void setDoctype(String type) { this.doctype = type; }

    public void incrementNumComments() { this.numComments = this.numComments + 1; }

    public void addCommentContent(String comment) { this.commentsContent = this.commentsContent + comment; }

    public void addTextContent(String content) { this.textContent = this.textContent + content; }

    public boolean hasElement(String element){
        return Utils.hashHasKey(this.elements, element);
    }

    public void incrementElement(String key){
        this.elements = Utils.hashIncreaseValue(this.elements, key, 1);
    }

    public void addElement(String key) { this.elements.put(key, 1); }

    public HashMap<String, HashMap<String, Integer>> getElementAttributes() { return this.elementAttributes; }

    public HashMap<String, Integer> getSpecificElementAttributes(String element) {
        return this.elementAttributes.get(element);
    }

    public void setSpecificElementAttributes(HashMap<String, Integer> list, String element) { this.elementAttributes.put(element, list); }

    public void incrementNumLinks() { this.numLinks = this.numLinks + 1; }

    public URI getURI() { return this.uri; }

    public void incrementNumLinksInt() { this.numLinksInt = numLinksInt + 1; }

    public HashMap<String, Integer> getLinkProtocols() { return this.linkProtocols; }

    public void addProtocol(String protocol) { this.linkProtocols.put(protocol, 1); }

    public void incrementProtocol(String protocol){
        this.linkProtocols = Utils.hashIncreaseValue(this.linkProtocols, protocol, 1);
    }

    public void incrementNumLinksBook() { this.numLinksBook = this.numLinksBook + 1; }

    public void incrementNumLinksExt() { this.numLinksExt = this.numLinksExt + 1; }

    public void incrementNumImages() { this.numImages = this.numImages + 1; }

    public boolean hasImageFormat(String format){
        return Utils.hashHasKey(this.imageFormats, format);
    }

    public void incrementImageFormat(String format) {
        this.imageFormats = Utils.hashIncreaseValue(this.imageFormats, format, 1);
    }

    public void addImageFormat(String format){
        this.imageFormats.put(format, 1);
    }

    public void setCharset(String charset) { this.charset = charset; }

    public String getTextContent() { return this.textContent; }

    public String getCharset() { return this.charset; }

    public void setSizeText(float size) { this.sizeText = size; }

    public void setSizeElements(float size) { this.sizeElements = size; }

    public float getFileSize() { return this.fileSize; }

    public void setTextToElementsRatio(float ratio) { this.textToElementsRatio = ratio; }

    public float getSizeText() { return this.sizeText; }

    public float getSizeElements() { return this.sizeElements; }

    public void setElementsToSizeRatio(float ratio) { this.elementsToSizeRatio = ratio; }

    public void setTextToSizeRatio(float ratio) { this.textToSizeRatio = ratio; }

    public String getCommentsContent() { return this.commentsContent; }

    public void setCommentsSize(float size) { this.sizeComments = size; }

    public void setCommentsToSizeRatio(float ratio) { this.commentsToSizeRatio = ratio; }

    public float getSizeComments() { return this.sizeComments; }

    public void incrementNumErrors() { this.numErrors = this.numErrors + 1; }

    public void increaseErrorMessage(ArrayList<String> message)
    {
        //Error is already catalogued
        if(Utils.hashHasKey(this.errors, message.get(0)))
            this.errors = Utils.hashIncreaseValue(this.errors, message.get(0), 1);
        //Error is new for this page
        else
            this.errors.put(message.get(0), 1);
    }

    public void addErrorMessageVariables(ArrayList<String> message)
    {
        //Error is already catalogued
        if(Utils.bigHashHasKey(this.errorVariables, message.get(0)))
        {
            HashMap<String, Integer> cataloguedError = errorVariables.get(message.get(0));
            //This specific combination of variables is already catalogued
            if(Utils.hashHasKey(cataloguedError, message.get(1)))
            {
                Utils.hashIncreaseValue(cataloguedError, message.get(1), 1);
                this.errorVariables.put(message.get(0), cataloguedError);
            }
            //This specific combination of variables is new
            else
            {
                cataloguedError.put(message.get(1), 1);
                this.errorVariables.put(message.get(0), cataloguedError);
            }
        }
        //Error is new for this page
        else
        {
            HashMap<String, Integer> newError = new HashMap<String, Integer>();
            newError.put(message.get(1), 1);

            this.errorVariables.put(message.get(0), newError);
        }
    }

    public void incrementNumWarnings() { this.numWarnings = this.numWarnings + 1; }

    public void increaseWarningMessage(ArrayList<String> message)
    {
        //Error is already catalogued
        if(Utils.hashHasKey(this.warnings, message.get(0)))
            this.warnings = Utils.hashIncreaseValue(this.warnings, message.get(0), 1);
            //Error is new for this page
        else
            this.warnings.put(message.get(0), 1);
    }

    public void addWarningMessageVariables(ArrayList<String> message)
    {
        //Warning is already catalogued
        if(Utils.bigHashHasKey(this.warningVariables, message.get(0)))
        {
            HashMap<String, Integer> cataloguedWarning = warningVariables.get(message.get(0));
            //This specific combination of variables is already catalogued
            if(Utils.hashHasKey(cataloguedWarning, message.get(1)))
            {
                Utils.hashIncreaseValue(cataloguedWarning, message.get(1), 1);
                this.warningVariables.put(message.get(0), cataloguedWarning);
            }
            //This specific combination of variables is new
            else
            {
                cataloguedWarning.put(message.get(1), 1);
                this.warningVariables.put(message.get(0), cataloguedWarning);
            }
        }
        //Warning is new for this page
        else
        {
            HashMap<String, Integer> newWarning = new HashMap<String, Integer>();
            newWarning.put(message.get(1), 1);

            this.warningVariables.put(message.get(0), newWarning);
        }
    }

    public int getNumErrors() { return this.numErrors; }

    public int getNumWarnings() { return this.numWarnings; }

    public HashMap<String, Integer> getElements() { return this.elements; }

    public HashMap<String, Integer> getImageFormats() { return this.imageFormats; }

    public HashMap<String, Integer> getErrors() { return this.errors; }

    public HashMap<String, Integer> getWarnings() { return this.warnings; }

    public HashMap<String, HashMap<String, Integer>> getErrorVariables() { return this.errorVariables; }

    public HashMap<String, HashMap<String, Integer>> getWarningVariables() { return this.warningVariables; }

    public int getNumHTMLelem() { return this.numHTMLelem; }

    public String getDoctype() { return this.doctype; }

    public int getNumLinks() { return this.numLinks; }

    public int getNumLinksInt() { return this.numLinksInt; }

    public int getNumLinksBook() { return this.numLinksBook; }

    public int getNumLinksExt() { return this.numLinksExt; }

    public int getNumImages() { return this.numImages; }

    public int getNumComments() { return this.numComments; }

    public float getTextToElementsRatio() { return this.textToElementsRatio; }

    public float getElementsToSizeRatio() { return this.elementsToSizeRatio; }

    public float getTextToSizeRatio() { return this.textToSizeRatio; }

    public float getCommentsToSizeRatio() { return this.commentsToSizeRatio; }

    public String getResourceType() { return this.resourceType; }

    public String getHTTPCode() { return this.HTTPCode; }

    public String getContentType() { return this.contentType; }

    public String getServer() { return this.server; }
}