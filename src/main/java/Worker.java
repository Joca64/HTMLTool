import Utils.Utils;
import dataTypes.ValidationMessage;
import dataTypes.ValidatorResult;
import dataTypes.Webpage;
import okhttp3.OkHttpClient;
import org.apache.commons.csv.CSVPrinter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker implements Runnable{

    private boolean DEBUG, skipFlag;
    private File file;
    ArrayList<String> errorMessages, errorMessagesHTML5;
    ArrayList<Pattern> errorPatterns, errorPatternsHTML5;
    HashMap<String, CSVPrinter> files;
    private String BASE_URL = "http://localhost/w3c-validator/";
    private Retrofit retrofit;
    private ValidatorAPI service;
    private AtomicInteger currentPage, successful, failedNotHTML, failedNoBytes, failedParsing, failedValidation, numForced;
    private HashSet<String> urlList;
    private String toolFolder;

    public HashMap<String, String> URIs = new HashMap<String, String>();

    public Worker(boolean DEBUG, File file, ArrayList<String> errorM, ArrayList<Pattern> errorP, ArrayList<String> errorM5, ArrayList<Pattern> errorP5,
                  HashMap<String, CSVPrinter> files, AtomicInteger currentPage, AtomicInteger successful, AtomicInteger failedNotHTML,
                  AtomicInteger failedNoBytes, AtomicInteger failedParsing, AtomicInteger failedValidation,
                  AtomicInteger numForced, HashSet<String> urlList, String toolFolder)
    {
        this.DEBUG = DEBUG;
        this.file = file;
        this.errorMessages = errorM;
        this.errorPatterns = errorP;
        this.errorMessagesHTML5 = errorM5;
        this.errorPatternsHTML5 = errorP5;
        this.files = files;
        this.currentPage = currentPage;
        this.successful = successful;
        this.failedNotHTML = failedNotHTML;
        this.failedNoBytes = failedNoBytes;
        this.failedParsing = failedParsing;
        this.failedValidation = failedValidation;
        this.numForced = numForced;
        this.urlList = urlList;
        this.toolFolder = toolFolder;

        httpSetup();
    }

    public void run() {
        if(DEBUG)
            System.out.println(Thread.currentThread().getName() +" is starting");
        if(currentPage.incrementAndGet() % 100 == 0)
           System.out.println("Current number of analyzed pages: " +currentPage +" " +System.currentTimeMillis());
        this.skipFlag = false;

        Webpage webpage = new Webpage(file.getName());

        if(DEBUG)
            System.out.println(Thread.currentThread().getName() +"-> Starting processing of WARC: " + webpage.getWARCname());

        //Read WARC file
        skipFlag = readFile(file, webpage, toolFolder);

        //Not an HTML file, skip to next WARC
        if (skipFlag) {
            failedNotHTML.incrementAndGet();
            return;
        }

        //Create a file to validate
        createHTMLFile(webpage);

        //Page has 0 bytes, skip to next WARC
        if (webpage.getFileSize() == 0) {
            if(DEBUG)
                System.out.println(Thread.currentThread().getName() +"   INFO: File has 0 bytes, skipping it.");
            failedNoBytes.incrementAndGet();
            return;
        }

        //Parse HTML elements
        skipFlag = parseHTML(webpage);

        //HTML parsing was unsuccessful, skip to next page
        if (skipFlag) {
            if(DEBUG)
                System.out.println(Thread.currentThread().getName() +"   INFO: Parsing was unsuccessful, skipping it.");
            failedParsing.incrementAndGet();
            return;
        }

        //Validate HTML file
        webpage = validatePage(webpage);

        //Validation was unsuccessful, skip to next page
        if (webpage == null) {
            if(DEBUG)
                System.out.println(Thread.currentThread().getName() +"   INFO: Validation was unsuccessful, skipping it.");
            failedValidation.incrementAndGet();
            return;
        }


        //Add results for this page to the general stats output file
        outputPageData(webpage, files.get("results"), files.get("resultsHTML5"));

        //Add element information
        outputHashInformation(webpage.getElements(), webpage.getDoctype(), webpage.getWARCname(), files.get("elements"), files.get("elementsHTML5"));
        //Add protocol information
        if (webpage.getNumLinks() > 0)
            outputHashInformation(webpage.getLinkProtocols(), webpage.getDoctype(), webpage.getWARCname(), files.get("protocols"), files.get("protocolsHTML5"));
        //Add image information
        if (webpage.getNumImages() > 0)
            outputHashInformation(webpage.getImageFormats(), webpage.getDoctype(), webpage.getWARCname(), files.get("images"), files.get("imagesHTML5"));
        //Add error information
        if (webpage.getNumErrors() > 0)
            outputHashInformation(webpage.getErrors(), webpage.getDoctype(), webpage.getWARCname(), files.get("errors"), files.get("errorsHTML5"));
        //Add warning information
        if (webpage.getNumWarnings() > 0)
            outputHashInformation(webpage.getWarnings(), webpage.getDoctype(), webpage.getWARCname(), files.get("warnings"), files.get("warningsHTML5"));
        //Add attribute information
        outputBigHashInformation(webpage.getElementAttributes(), webpage.getDoctype(), webpage.getWARCname(), files.get("attributes"), files.get("attributesHTML5"));
        //Add error variables information
        outputBigHashInformation(webpage.getErrorVariables(), webpage.getDoctype(), webpage.getWARCname(), files.get("errorVariables"), files.get("errorVariablesHTML5"));
        //Add warning variables information
        outputBigHashInformation(webpage.getWarningVariables(), webpage.getDoctype(), webpage.getWARCname(), files.get("warningVariables"), files.get("warningVariablesHTML5"));
        //Add page to HashSet to remove future duplicates
        urlList.add(webpage.getURI().toString());

        successful.incrementAndGet();
        deleteTempHTMLFile();
    }

    //Enables HTTP connections to the validators
    private void httpSetup() {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        service = retrofit.create(ValidatorAPI.class);
    }

    private Webpage findIfRedirected(Webpage webpage, String folder)
    {
        synchronized (URIs) {

            URIs = hashFromFile(folder);

            if (URIs.containsKey(webpage.getURI().toString())) {
                //Update webpage's original URL
                webpage.setOriginalURI(URIs.get(webpage.getURI().toString()));
                //Remove set from hashmap
                URIs.remove(webpage.getURI().toString());
                //Update the file
                hashToFile(folder, URIs);
            }
        }

        return webpage;
    }

    private void hashToFile(String folder, HashMap<String, String> URLs)
    {
        File file = new File(folder + "resources/redirects.txt");
        try {
            OutputStreamWriter fW  = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8").newEncoder());

            for(String key : URLs.keySet()) {
                fW.write(key + "£££" + URLs.get(key) +"\n");
                fW.flush();
            }

            fW.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, String> hashFromFile(String folder)
    {
        HashMap<String, String> URLs = new HashMap<String, String>();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(folder + "resources/redirects.txt")));
            String line;
            String redirectedTo = null;
            String redirector = null;

            while((line = br.readLine()) != null)
            {
                String[] tokens = line.split("£££");
                if(tokens.length == 2) {
                    redirectedTo = tokens[0];
                    redirector = tokens[1];
                    URLs.put(redirectedTo, redirector);
                }
            }

            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return URLs;
    }

    //Reads a WARC file and parses through it. Adds the resource type, URI, HTML and other metadata to a new Webpage
    //Returns true if process aborted, false it not
    private boolean readFile(File file, Webpage webpage, String folder) {
        boolean failed = false;
        String HTML = "";
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            String sCurrentLine;
            int warcFlag = 0;
            boolean preHeader = true;

            //Going through each line of the WARC file
            while ((sCurrentLine = br.readLine()) != null) {
                int lineLength = sCurrentLine.length();

                //Discard unwanted warcinfo
                if (sCurrentLine.startsWith("WARC/1.0")) {
                    warcFlag = warcFlag + 1;
                    if (warcFlag >= 3)
                        break;
                    continue;
                }

                //Process response / resource
                if (warcFlag == 2) {
                    //Add properties and HTML
                    if (sCurrentLine.startsWith("WARC-Type: "))
                        webpage.setResourceType(sCurrentLine.substring(11, lineLength));
                    else if (sCurrentLine.startsWith("WARC-Target-URI: "))
                        webpage.setURI(sCurrentLine.substring(17, lineLength));
                    else if (sCurrentLine.startsWith("Content-Type: ") && preHeader) {
                        webpage.setContentType(sCurrentLine.substring(14, lineLength));
                        if (webpage.getContentType().equals("audio/mpeg") || webpage.getContentType().equals("application/x-shockwave-flash") || webpage.getContentType().equals("video/mpeg")
                                || webpage.getContentType().equals("application/pdf") || webpage.getContentType().equals("application/msword"))
                            return true;
                    } else if (sCurrentLine.startsWith("HTTP/") && preHeader)
                        webpage.setHTTPCode(sCurrentLine);
                    else if (sCurrentLine.startsWith("Server: ") && preHeader)
                        webpage.setServer(sCurrentLine.substring(8, lineLength));
                    else if (!preHeader)
                        HTML = HTML + sCurrentLine + "\n";
                    else if (sCurrentLine.startsWith("<")) {
                        preHeader = false;
                        HTML = HTML + sCurrentLine + "\n";
                    }
                }
            }
            webpage.setHTML(HTML);
            webpage = findIfRedirected(webpage, folder);
        } catch (IOException e) {
            e.printStackTrace();
            failed = true;
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return failed;
    }

    //Creates a temporary HTML file to be submitted for validation
    private void createHTMLFile(Webpage webpage) {
        String filepath = "resources/tempHTML" +Thread.currentThread().getName() +".html";

        try {
            //Create HTML file
            File file = new File(filepath);
            OutputStreamWriter fW  = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8").newEncoder());
            fW.write(webpage.getHTML());
            fW.flush();
            fW.close();

            //Set HTML file size
            webpage.setFileSize(Utils.getFileSize(filepath));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Parse HTML elements and attributes
    private boolean parseHTML(Webpage webpage) {
        Document doc;
        try {
            doc = Jsoup.parse(webpage.getHTML());
        } catch (Throwable t) {
            if(this.DEBUG)
                System.out.println("-> ERROR: Jsoup failed to parse the HTML file from " + webpage.getWARCname() + ", skipping it.");
            return true;
        }

        Elements elements = doc.getAllElements();

        //Extract number of HTML elements
        webpage.setNumHTMLelem(elements.size());

        //Extract DOCTYPE
        webpage.setDoctype(getDocumentType(doc));

        //Go through every HTML element
        for (Element element : elements) {
            //Add comments' information for all child elements
            webpage = addComments(webpage, element);

            //Add textual content to the pile, so we can get its total size later
            if (element.ownText() != null && !element.ownText().equals(""))
                webpage.addTextContent(element.ownText());

            //Get HTML tag name
            String tag = Utils.removeLineBreaks(element.tagName().toLowerCase());

            //Add tag occurrence to the total of this page
            if (webpage.hasElement(tag))
                webpage.incrementElement(tag);
            else
                webpage.addElement(tag);

            //Process attributes for this element
            if (element.attributes().size() > 0) {
                webpage = processElementAttributes(webpage, element, tag);
            }

            //Process hyperlinks
            if (tag.equals("a") || tag.equals("area"))
                webpage = processHyperlinks(webpage, element);

            //Process images
            if (tag.equals("img"))
                webpage = processImages(webpage, element);
        }

        return false;
    }

    //Receives a Jsoup document and extracts its doctype
    private String getDocumentType(Document doc) {
        List<Node> nodes = doc.childNodes();
        for (Node node : nodes) {
            if (node instanceof DocumentType) {
                DocumentType documentType = (DocumentType) node;

                return documentType.toString().toLowerCase();
            }
        }
        return "This document does not declare its DOCTYPE";
    }

    //Adds comment information for all child elements to the webpage
    private static Webpage addComments(Webpage webpage, Element element) {
        for (Node node : element.childNodes()) {
            if (node instanceof Comment) {
                webpage.incrementNumComments();
                webpage.addCommentContent(((Comment) node).getData());
            }
        }
        return webpage;
    }

    //Adds new attribute occurrences and increases the value of existing ones for the supplied element
    private static Webpage processElementAttributes(Webpage webpage, Element element, String tag) {
        //Current element is already catalogued
        if (Utils.bigHashHasKey(webpage.getElementAttributes(), tag)) {
            //Get current values
            HashMap<String, Integer> elementAttributes = webpage.getSpecificElementAttributes(tag);
            //Cycle through all attributes of the currently analyzed element
            for (Attribute attribute : element.attributes()) {
                String attributeName = Utils.removeLineBreaks(attribute.getKey().toLowerCase());
                //Current attribute is already catalogued for this element, increment it
                if (Utils.hashHasKey(elementAttributes, attributeName))
                    elementAttributes = Utils.hashIncreaseValue(elementAttributes, attributeName, 1);
                //Current attribute is new for this element, add it
                else
                    elementAttributes.put(attributeName, 1);
            }
            //Add the new values to the web page
            webpage.setSpecificElementAttributes(elementAttributes, tag);
        }
        //Current element is not catalogued, add it
        else {
            //Create new element attributes list
            HashMap<String, Integer> newElement = new HashMap<String, Integer>();
            //Add attributes
            for (Attribute attribute : element.attributes())
                newElement.put(Utils.removeLineBreaks(attribute.getKey().toLowerCase()), 1);
            //Add the new values to the web page
            webpage.setSpecificElementAttributes(newElement, tag);
        }
        return webpage;
    }

    //Process hyperlink information
    private static Webpage processHyperlinks(Webpage webpage, Element element) {
        //Increase hyperlinks counter
        webpage.incrementNumLinks();

        String linkPath = element.attr("href");
        String selfProtocol = webpage.getURI().getScheme();
        //Hyperlinks that share the domain or begin with . or / or # are considered internal
        if (linkPath.startsWith(selfProtocol) || linkPath.startsWith(".") || linkPath.startsWith("/") || linkPath.startsWith("#")) {
            //Increment number of internal links
            webpage.incrementNumLinksInt();

            //Check if current protocol is already on this web page's list and add it / increment it
            if (Utils.hashHasKey(webpage.getLinkProtocols(), selfProtocol))
                webpage.incrementProtocol(selfProtocol);
            else
                webpage.addProtocol(selfProtocol);

            //Link is a bookmark, increase its counter as well
            if (linkPath.startsWith("#"))
                webpage.incrementNumLinksBook();
        }
        //Remaining hyperlinks are considered external
        else {
            //Increment number of external links
            webpage.incrementNumLinksExt();

            //Find protocol used in hyperlink
            String protocol;

            try {
                URI link = new URI(linkPath);
                protocol = link.getScheme();
                if (protocol == null || protocol.equals(""))
                    protocol = "n/a";
                protocol = Utils.removeLineBreaks(protocol.toLowerCase());
            } catch (URISyntaxException e) {
                //e.printStackTrace();
                protocol = "n/a";
            }

            protocol = Utils.removeLineBreaks(protocol.toLowerCase());

            //Check if current protocol is already on this web page's list and add it / increment it
            if (Utils.hashHasKey(webpage.getLinkProtocols(), protocol))
                webpage.incrementProtocol(protocol);
            else
                webpage.addProtocol(protocol);
        }
        return webpage;
    }

    //Process image information
    private static Webpage processImages(Webpage webpage, Element element) {
        //Increase image counter
        webpage.incrementNumImages();

        String imageSource = element.attr("src");
        String fileType;

        //Image SRC is empty
        if (imageSource.equals(""))
            fileType = "empty";
            //Image SRC contains information
        else {
            int extensionIndex = imageSource.lastIndexOf(".");
            if (extensionIndex > 0) {
                //Remove string content starting from last .
                fileType = imageSource.substring(extensionIndex);
                fileType = fileType.toLowerCase();
                //Remove possible trailing parameters that start with ? and &
                if (fileType.indexOf("?") > 0)
                    fileType = fileType.substring(0, fileType.indexOf("?"));
                if (fileType.indexOf("&") > 0)
                    fileType = fileType.substring(0, fileType.indexOf("&"));

                //String too large or of known non-image extensions? Considered a link.
                if (fileType.length() > 6 || fileType.contains("/") || fileType.equals(".php") || fileType.equals(".html") || fileType.equals(".asp") ||
                        fileType.equals("htm") || fileType.equals(".cgi") || fileType.equals(".com") || fileType.equals(".gov"))
                    fileType = "hyperlink";
                else if (fileType.length() <= 2 || !fileType.startsWith("."))
                    fileType = "n/a";
            }
            //Image SRC does not contain a . considering it a n/a
            else
                fileType = "n/a";
        }

        fileType = Utils.removeLineBreaks(fileType.trim().toLowerCase());

        //Format already catalogued, increment it. New? Add it.
        if (webpage.hasImageFormat(fileType))
            webpage.incrementImageFormat(fileType);
        else
            webpage.addImageFormat(fileType);

        return webpage;
    }


    //Send web page through validation
    private Webpage validatePage(Webpage webpage) {
        Call<ValidatorResult> call;
        boolean hadToForceEncodingAndDoctype = false;
        Response<ValidatorResult> response;

        for (int i = 0; i < 3; i++) {
            try {
                //Auto detect charset and doctype
                if (i == 0) {
                    if(DEBUG)
                        System.out.println(Thread.currentThread().getName() +" Autodetecting charset and doctype for validation");
                    call = this.service.getTestResult("file:/" +this.toolFolder +"resources/tempHTML" + Thread.currentThread().getName() + ".html");
                }
                //Force UTF-8 charset
                if (i == 1) {
                    if(DEBUG)
                        System.out.println(Thread.currentThread().getName() +" Forcing UTF-8 for validation");
                    call = this.service.getTestResultUTF8("file:/" +this.toolFolder +"resources/tempHTML" + Thread.currentThread().getName() + ".html");
                }
                //Force UTF-8 charset and HTML5
                else {
                    if(DEBUG)
                        System.out.println(Thread.currentThread().getName() +" Forcing UTF-8 and HTML5 for validation");
                    call = this.service.getTestResultUTF8HTML5("file:/" +this.toolFolder +"resources/tempHTML" + Thread.currentThread().getName() + ".html");
                }

                //Submit HTML file for validation
                response = call.execute();

                //Successful response, parse results, leave
                if (response.isSuccessful()) {
                    webpage = getValidationInformation(webpage, response.body());

                    //Track number of pages that successfully validated after forcing settings
                    if (hadToForceEncodingAndDoctype)
                        numForced.incrementAndGet();

                    return webpage;
                }

            } catch (IOException e) {
                if(this.DEBUG) {
                    e.printStackTrace();
                    System.out.println(Thread.currentThread().getName() +"-> ERROR: Validation failed for " + webpage.getWARCname() + ", changing default encoding / DOCTYPE and retrying.");
                }
                hadToForceEncodingAndDoctype = true;
            }
        }
        if(DEBUG)
            System.out.println(Thread.currentThread().getName() +"-> ERROR: Validation failed for " + webpage.getWARCname() + ", skipping it.");

        return null;
    }

    //Process validation results
    private Webpage getValidationInformation(Webpage webpage, ValidatorResult result) {
        //Extract charset
        webpage.setCharset(result.getEncoding());

        //Extract charset, text content, elements, comments and their ratios
        //This is done here because the original charset of the document is known through the validator response
        try {
            final byte[] text = webpage.getTextContent().getBytes(webpage.getCharset());
            webpage.setSizeText(text.length);
            webpage.setSizeElements(webpage.getFileSize() - text.length);
            webpage.setTextToElementsRatio(webpage.getSizeText() / webpage.getSizeElements());
            webpage.setElementsToSizeRatio(webpage.getSizeElements() / webpage.getFileSize());
            webpage.setTextToSizeRatio(webpage.getSizeText() / webpage.getFileSize());

            final byte[] comments = webpage.getCommentsContent().getBytes(webpage.getCharset());
            webpage.setCommentsSize(comments.length);
            webpage.setCommentsToSizeRatio(webpage.getSizeComments() / webpage.getFileSize());

            //Extract validation errors and warnings
            if (result.getMessages().size() > 0)
                webpage = fillValidationMessages(webpage, result.getMessages());
        } catch (UnsupportedEncodingException e) {
            if(this.DEBUG) {
                e.printStackTrace();
                System.out.println(Thread.currentThread().getName() +"-> ERROR: Encoding error, SKIPPING PAGE.");
            }
            return null;
        }

        return webpage;
    }

    //Process the list of validation error and warning messages
    private Webpage fillValidationMessages(Webpage webpage, List<ValidationMessage> messages) {
        ArrayList<String> processedMessage;

        //No errors nor warnings
        if (messages != null && messages.isEmpty())
            return webpage;

        //Process each message
        for (ValidationMessage message : messages) {
            processedMessage = new ArrayList<String>();

            //Message is a validation error
            if (message.getType().equals("error")) {
                webpage.incrementNumErrors();

                String errorID = message.getMessageID();

                //HTML5 specific errors
                if (errorID.equals("html5")) {
                    processedMessage = processMessage(message.getMessage(), errorPatternsHTML5, errorMessagesHTML5);
                    processedMessage.set(0, errorID + " error - " + processedMessage.get(0));
                }
                //Remaining errors
                else {
                    processedMessage = processMessage(message.getMessage(), errorPatterns, errorMessages);

                    if (processedMessage.get(0).startsWith("helloEMPTY")) {
                        processedMessage.set(0, errorID + " error - " + message.getMessage());
                    } else
                        processedMessage.set(0, "error - " + errorID + " - " + processedMessage.get(0));
                }

                //Add error message to the page
                webpage.increaseErrorMessage(processedMessage);
                //Add its variables in case it contains them
                if (processedMessage.size() > 1 && !processedMessage.get(1).equals(""))
                    webpage.addErrorMessageVariables(processedMessage);
            }
            //Message is a validation warning
            else if (message.getSubtype() != null && message.getSubtype().equals("warning")) {
                webpage.incrementNumWarnings();

                String warningID = message.getMessageID();

                //HTML5 specific warnings
                if (warningID.equals("html5")) {
                    processedMessage = processMessage(message.getMessage(), errorPatternsHTML5, errorMessagesHTML5);
                    processedMessage.set(0, message.getSubtype() + " html5 - " + processedMessage.get(0));
                }
                //Remaining warnings
                else {
                    processedMessage = processMessage(message.getMessage(), errorPatterns, errorMessages);
                    processedMessage.set(0, message.getSubtype() + " - " + message.getMessageID() + " - " + processedMessage.get(0));
                }

                //Add warning message to the page
                webpage.increaseWarningMessage(processedMessage);
                //Add its variables in case it contains them
                if (processedMessage.size() > 1 && !processedMessage.get(1).equals(""))
                    webpage.addWarningMessageVariables(processedMessage);
            }
        }
        if(this.DEBUG)
            System.out.println(Thread.currentThread().getName() +" This page had a total of " + webpage.getNumErrors() + " errors and " + webpage.getNumWarnings() + " warnings.");

        return webpage;
    }

    //Go through all patterns and match them to the validation message
    //In case there is no match, returns the original message
    private static ArrayList<String> processMessage(String message, ArrayList<Pattern> patterns, ArrayList<String> errorMessages) {
        int numPatterns = patterns.size();
        ArrayList<String> matchInformation;

        for (int i = 0; i < numPatterns; i++) {
            matchInformation = patternMatch(errorMessages.get(i), patterns.get(i), message);
            if(matchInformation != null && matchInformation.size() > 1)
            //if (matchInformation != null)
                return matchInformation;
        }

        message = Utils.removeLineBreaks(message);

        //There was no match for known patterns, returning original message only
        matchInformation = new ArrayList<String>();
        matchInformation.add(message);
        return matchInformation;
    }

    //Checks if there is a match between a string and a pattern
    private static ArrayList<String> patternMatch(String error, Pattern pat, String message) {
        //Pattern pattern = Pattern.compile(pat.toLowerCase());
        Matcher matcher = pat.matcher(message.toLowerCase());

        //There is a match with the supplied pattern
        if (matcher.find()) {
            ArrayList<String> matches = new ArrayList<String>();
            //Include message on first index
            matches.add(error);

            //Message has no variables, return solely the error / warning string
            if (matcher.groupCount() == 0)
                return matches;

            //Group all variables into one string
            String messageVariables = "";
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (i + 1 <= matcher.groupCount())
                    messageVariables = messageVariables + Utils.removeLineBreaks(matcher.group(i)) + " | ";
                else
                    messageVariables = messageVariables + Utils.removeLineBreaks(matcher.group(i));
            }
            matches.add(messageVariables.toLowerCase());

            return matches;
        }

        return null;
    }

    //Adds the web page's general information to the output file
    private void outputPageData(Webpage webpage, CSVPrinter html, CSVPrinter html5) {
        try {
            synchronized (html) {
                //Information for the global file
                ArrayList<String> pageInfo = new ArrayList<String>();
                pageInfo.add(webpage.getWARCname());
                pageInfo.add(webpage.getURI().toString());
                if(webpage.getOriginalURI() == null)
                    pageInfo.add("N.D.");
                else
                    pageInfo.add(webpage.getOriginalURI().toString());
                pageInfo.add("" + webpage.getNumHTMLelem());
                pageInfo.add(webpage.getDoctype());
                pageInfo.add("" + webpage.getNumLinks());
                pageInfo.add("" + webpage.getNumLinksInt());
                pageInfo.add("" + webpage.getNumLinksBook());
                pageInfo.add("" + webpage.getNumLinksExt());
                pageInfo.add("" + webpage.getNumImages());
                pageInfo.add("" + webpage.getNumComments());
                pageInfo.add("" + webpage.getFileSize());
                pageInfo.add("" + webpage.getSizeText());
                pageInfo.add("" + webpage.getSizeElements());
                pageInfo.add("" + webpage.getSizeComments());
                pageInfo.add("" + webpage.getTextToElementsRatio());
                pageInfo.add("" + webpage.getElementsToSizeRatio());
                pageInfo.add("" + webpage.getTextToSizeRatio());
                pageInfo.add("" + webpage.getCommentsToSizeRatio());
                pageInfo.add("" + webpage.getNumErrors());
                pageInfo.add("" + webpage.getNumWarnings());
                pageInfo.add(webpage.getResourceType());
                pageInfo.add(webpage.getHTTPCode());
                pageInfo.add(webpage.getContentType());
                pageInfo.add(webpage.getServer());
                pageInfo.add(webpage.getCharset());

                html.printRecord(pageInfo);
                html.flush();

                //If an HTML5 document, output to its specific file as well
                String doctype = webpage.getDoctype().toLowerCase();

                if (doctype.contains("<!doctype html>")) {
                    synchronized (html5) {
                        html5.printRecord(pageInfo);
                        html5.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Outputs all information contained in the supplied hash map
    private void outputHashInformation(HashMap<String, Integer> list, String doctype, String warcName, CSVPrinter html, CSVPrinter html5) {
        ArrayList<String> pageInfo = new ArrayList<String>();
        String temp;

        try {
            temp = hashToString(warcName, list, true);
            //Hash map has data, return
            if (temp == null || temp.equals(""))
                return;
            pageInfo.add(temp);
            synchronized (html)
            {
                html.printRecord(pageInfo);
                html.flush();
            }

            //If an HTML5 document, output to its specific file as well
            doctype = doctype.toLowerCase();
            if (doctype.equals("<!doctype html>")) {
                synchronized (html5) {
                    html5.printRecord(pageInfo);
                    html5.flush();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Output all information contained in the 'big' hash map
    private void outputBigHashInformation(HashMap<String, HashMap<String, Integer>> list, String doctype, String warcName, CSVPrinter html, CSVPrinter html5) {
        ArrayList<String> pageInfo;

        try {
            for (String key : list.keySet()) {
                //Reset line info
                pageInfo = new ArrayList<String>();

                //Get all information for this element in a single line, output it
                String temp = hashToString(warcName, list.get(key), false);
                if (temp == null || temp.equals(""))
                    continue;
                pageInfo.add(warcName + "£" + key + "£" + temp);
                synchronized (html) {
                    html.printRecord(pageInfo);
                    html.flush();
                }

                //If an HTML5 document, output to its specific file as well
                doctype = doctype.toLowerCase();
                if (doctype.equals("<!doctype html>")) {
                    synchronized (html5) {
                        html5.printRecord(pageInfo);
                        html5.flush();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Converts the contents of a Hash Map into a String
    private String hashToString(String warcName, HashMap<String, Integer> hash, boolean includeWARCname) {
        String result;
        if (includeWARCname)
            result = warcName + "£";
        else
            result = "";

        for (String key : hash.keySet())
            result = result + key + "x" + hash.get(key) + "£";

        //Remove trailing £
        result = result.substring(0, result.length() - 1);

        return result;
    }

    //Deletes the temporary HTML file used for validation
    private void deleteTempHTMLFile()
    {
        File file = new File("resources/tempHTML" +Thread.currentThread().getName() +".html");
        file.delete();
    }
}
