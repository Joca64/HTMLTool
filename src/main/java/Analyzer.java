import Utils.Utils;
import dataTypes.Webpage;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Analyzer {

    private static final boolean DEBUG = false;

    private static int numWARCs = 0;
    private static AtomicInteger currentPage = new AtomicInteger(0);
    private static AtomicInteger successful = new AtomicInteger(0);
    private static AtomicInteger failedNotHTML = new AtomicInteger(0);
    private static AtomicInteger failedNoBytes = new AtomicInteger(0);
    private static AtomicInteger failedValidation = new AtomicInteger(0);
    private static AtomicInteger failedParsing = new AtomicInteger(0);
    private static AtomicInteger numForced = new AtomicInteger(0);

    private static ArrayList<String> errorMessages;
    private static ArrayList<String> errorPatterns;
    private static ArrayList<String> errorMessagesHTML5;
    private static ArrayList<String> errorPatternsHTML5;

    private static ArrayList<Pattern> errorPatternsBuilt;
    private static ArrayList<Pattern> errorPatternsHTML5Built;

    private static HashMap<String, HashMap<String, Integer>> totalElementAttributes = new HashMap<String, HashMap<String, Integer>>();
    private static HashMap<String, HashMap<String, Integer>> totalErrorVariables = new HashMap<String, HashMap<String, Integer>>();
    private static HashMap<String, HashMap<String, Integer>> totalWarningVariables = new HashMap<String, HashMap<String, Integer>>();
    private static HashMap<String, Integer> totalLinkProtocols = new HashMap<String, Integer>();
    private static HashMap<String, Integer> totalErrorOccurrences = new HashMap<String, Integer>();
    private static HashMap<String, Integer> totalWarningOccurrences = new HashMap<String, Integer>();
    private static HashMap<String, Integer> totalElementOccurrences = new HashMap<String, Integer>();
    private static HashMap<String, Integer> totalImageFormats = new HashMap<String, Integer>();

    private static HashMap<String, HashMap<String, Integer>> totalElementAttributesHTML5 = new HashMap<String, HashMap<String, Integer>>();
    private static HashMap<String, HashMap<String, Integer>> totalErrorVariablesHTML5 = new HashMap<String, HashMap<String, Integer>>();
    private static HashMap<String, HashMap<String, Integer>> totalWarningVariablesHTML5 = new HashMap<String, HashMap<String, Integer>>();
    private static HashMap<String, Integer> totalLinkProtocolsHTML5 = new HashMap<String, Integer>();
    private static HashMap<String, Integer> totalErrorOccurrencesHTML5 = new HashMap<String, Integer>();
    private static HashMap<String, Integer> totalWarningOccurrencesHTML5 = new HashMap<String, Integer>();
    private static HashMap<String, Integer> totalElementOccurrencesHTML5 = new HashMap<String, Integer>();
    private static HashMap<String, Integer> totalImageFormatsHTML5 = new HashMap<String, Integer>();

    private static HashSet<String> urlList = new HashSet<String>();


    public static void main(String[] args) {
        if(args.length < 4) {
            System.out.println("Usage: HTMLAnalyzer [input dir] [tool dir] [# of threads] [number of files to process] [last processed file - optional]");
            return;
        }

        String inputFolder = args[0];
        String toolFolder = args[1];
        int numThreads = Integer.parseInt(args[2]);
        int numFilesToProcess = Integer.parseInt(args[3]);
        boolean append;
        String lastProcessedFile;
        if(args.length == 5) {
            append = true;
            lastProcessedFile = args[4];
        }
        else {
            append = false;
            lastProcessedFile = null;
        }

        long start = System.currentTimeMillis();
        System.out.println("-> Start: " + start);

        //Incremental output files
        OutputStreamWriter resultsWriter = Utils.getFileWriter("resources/results/results.csv", append);
        OutputStreamWriter resultsHTML5Writer = Utils.getFileWriter("resources/results/resultsHTML5.csv", append);
        OutputStreamWriter elementsWriter = Utils.getFileWriter("resources/results/elements.csv", append);
        OutputStreamWriter elementsHTML5Writer = Utils.getFileWriter("resources/results/elementsHTML5.csv", append);
        OutputStreamWriter protocolsWriter = Utils.getFileWriter("resources/results/protocols.csv", append);
        OutputStreamWriter protocolsHTML5Writer = Utils.getFileWriter("resources/results/protocolsHTML5.csv", append);
        OutputStreamWriter imagesWriter = Utils.getFileWriter("resources/results/images.csv", append);
        OutputStreamWriter imagesHTML5Writer = Utils.getFileWriter("resources/results/imagesHTML5.csv", append);
        OutputStreamWriter errorsWriter = Utils.getFileWriter("resources/results/errors.csv", append);
        OutputStreamWriter errorsHTML5Writer = Utils.getFileWriter("resources/results/errorsHTML5.csv", append);
        OutputStreamWriter warningsWriter = Utils.getFileWriter("resources/results/warnings.csv", append);
        OutputStreamWriter warningsHTML5Writer = Utils.getFileWriter("resources/results/warningsHTML5.csv", append);
        OutputStreamWriter attributesWriter = Utils.getFileWriter("resources/results/attributes.csv", append);
        OutputStreamWriter attributesHTML5Writer = Utils.getFileWriter("resources/results/attributesHTML5.csv", append);
        OutputStreamWriter errorVariablesWriter = Utils.getFileWriter("resources/results/errorvariables.csv", append);
        OutputStreamWriter errorVariablesHTML5Writer = Utils.getFileWriter("resources/results/errorvariablesHTML5.csv", append);
        OutputStreamWriter warningVariablesWriter = Utils.getFileWriter("resources/results/warningvariables.csv", append);
        OutputStreamWriter warningVariablesHTML5Writer = Utils.getFileWriter("resources/results/warningvariablesHTML5.csv", append);
        CSVPrinter results, resultsHTML5, elements, elementsHTML5, protocols, protocolsHTML5, images, imagesHTML5,
                errors, errorsHTML5, warnings, warningsHTML5, attributes, attributesHTML5, errorVariables, errorVariablesHTML5,
                warningVariables, warningVariablesHTML5;

        //Create output files
        results = createOutputFile(getFileHeader("results"), resultsWriter, append);
        resultsHTML5 = createOutputFile(getFileHeader("resultsHTML5"), resultsHTML5Writer, append);
        elements = createOutputFile(getFileHeader("elements"), elementsWriter, append);
        elementsHTML5 = createOutputFile(getFileHeader("elementsHTML5"), elementsHTML5Writer, append);
        protocols = createOutputFile(getFileHeader("protocols"), protocolsWriter, append);
        protocolsHTML5 = createOutputFile(getFileHeader("protocolsHTML5"), protocolsHTML5Writer, append);
        images = createOutputFile(getFileHeader("images"), imagesWriter, append);
        imagesHTML5 = createOutputFile(getFileHeader("imagesHTML5"), imagesHTML5Writer, append);
        errors = createOutputFile(getFileHeader("errors"), errorsWriter, append);
        errorsHTML5 = createOutputFile(getFileHeader("errorsHTML5"), errorsHTML5Writer, append);
        warnings = createOutputFile(getFileHeader("warnings"), warningsWriter, append);
        warningsHTML5 = createOutputFile(getFileHeader("warningsHTML5"), warningsHTML5Writer, append);
        attributes = createOutputFile(getFileHeader("attributes"), attributesWriter, append);
        attributesHTML5 = createOutputFile(getFileHeader("attributesHTML5"), attributesHTML5Writer, append);
        errorVariables = createOutputFile(getFileHeader("variables"), errorVariablesWriter, append);
        errorVariablesHTML5 = createOutputFile(getFileHeader("variablesHTML5"), errorVariablesHTML5Writer, append);
        warningVariables = createOutputFile(getFileHeader("variables"), warningVariablesWriter, append);
        warningVariablesHTML5 = createOutputFile(getFileHeader("variablesHTML5"), warningVariablesHTML5Writer, append);

        HashMap<String, CSVPrinter> outputFiles = new HashMap<String, CSVPrinter>();
        outputFiles.put("results", results);
        outputFiles.put("resultsHTML5", resultsHTML5);
        outputFiles.put("elements", elements);
        outputFiles.put("elementsHTML5", elementsHTML5);
        outputFiles.put("protocols", protocols);
        outputFiles.put("protocolsHTML5", protocolsHTML5);
        outputFiles.put("images", images);
        outputFiles.put("imagesHTML5", imagesHTML5);
        outputFiles.put("errors", errors);
        outputFiles.put("errorsHTML5", errorsHTML5);
        outputFiles.put("warnings", warnings);
        outputFiles.put("warningsHTML5", warningsHTML5);
        outputFiles.put("attributes", attributes);
        outputFiles.put("attributesHTML5", attributesHTML5);
        outputFiles.put("errorVariables", errorVariables);
        outputFiles.put("errorVariablesHTML5", errorVariablesHTML5);
        outputFiles.put("warningVariables", warningVariables);
        outputFiles.put("warningVariablesHTML5", warningVariablesHTML5);

        //Path to WARC files
        String path = inputFolder;
        //Get list of WARC files
        ArrayList<File> listOfFiles = getFileList(path);
        numWARCs = listOfFiles.size();

        //Read error messages file
        errorMessages = readErrorMessages("resources/messages/errorMessages.txt");
        errorPatterns = readErrorMessages("resources/messages/errorPatterns.txt");
        errorPatternsBuilt = new ArrayList<Pattern>();

        for(String pat : errorPatterns)
            errorPatternsBuilt.add(Pattern.compile(pat.toLowerCase()));

        errorMessagesHTML5 = readErrorMessages("resources/messages/html5Messages.txt");
        errorPatternsHTML5 = readErrorMessages("resources/messages/html5Patterns.txt");

        errorPatternsHTML5Built = new ArrayList<Pattern>();
        for(String pat : errorPatternsHTML5)
            errorPatternsHTML5Built.add(Pattern.compile(pat.toLowerCase()));

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        boolean startProcessing = false;

        //Starting from scratch, begin with first file
        if(!append) {
            startProcessing = true;

            //Attach redirection targets to the originally crawled URL
            System.out.println("-> Processing redirections...");
            attachRedirection(listOfFiles, toolFolder);
            listOfFiles = getFileList(path);
        }

        //Process a single file at a time
        int queuedFiles = 0;
        String lastFile = "";
        for (File file : listOfFiles) {
            //Skip previous files
            if(!startProcessing)
            {
                if(file.getName().equals(lastProcessedFile))
                    startProcessing = true;
            }
            else {
                Worker worker = new Worker(DEBUG, file, errorMessages, errorPatternsBuilt, errorMessagesHTML5, errorPatternsHTML5Built, outputFiles, currentPage,
                        successful, failedNotHTML, failedNoBytes, failedParsing, failedValidation, numForced, urlList, toolFolder);
                executor.execute(worker);
                queuedFiles = queuedFiles + 1;
                lastFile = file.getName();
                if(queuedFiles == numFilesToProcess) {
                    break;
                }
            }
        }
        executor.shutdown();
        while(!executor.isTerminated())
        {
            //Waiting for threads to finish
        }

        System.out.println("->Done.");

        if(!listOfFiles.get(listOfFiles.size()-1).getName().equals(lastFile))
            System.out.println(lastFile);
        else
        {
            System.out.println("-> Outputting totals...");
            //Create output files for totals
            outputTotals();

            //Create output file with final set of statistics
            outputFinalFile(start);
        }
    }

    private static void attachRedirection(ArrayList<File> listOfFiles, String folder)
    {
        HashMap<String, String> urls = new HashMap<String, String>();

        int counter = 0;
        int max = listOfFiles.size();
        //First pass through files, collect original URLs and redirection targets
        for(File file : listOfFiles) {
            counter = counter + 1;

            getURLTarget(file, urls);
        }

        createRedirectionFile(urls, folder);

    }

    public static void createRedirectionFile(HashMap<String, String> urls, String folder)
    {
        File file = new File(folder +"resources/redirects.txt");
        try {
            OutputStreamWriter fW  = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8").newEncoder());

            for(String key : urls.keySet()) {
                fW.write(key + "£££" + urls.get(key) +"\n");
                fW.flush();
            }

            fW.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getURLTarget(File file, HashMap<String, String> urls) {
        BufferedReader br = null;
        FileReader fr = null;
        String origin = null;
        String target = null;

        try {
            fr = new FileReader(file);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8").newDecoder()));

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
                    if (sCurrentLine.startsWith("WARC-Target-URI: "))
                        origin = sCurrentLine.substring(17, lineLength);
                    else if (sCurrentLine.startsWith("Location: ")) {
                        target = sCurrentLine.substring(10, lineLength);
                        break;
                    }
                }
            }
            if(origin != null && target != null) {
                if(!target.startsWith("h")) {
                    while(target.contains("//"))
                        target = target.replace("//","/");
                    if(origin.endsWith("/") && target.startsWith("/"))
                        target = (origin + target.substring(1)).toLowerCase();
                    else
                        target = (origin + target).toLowerCase();
                }

                urls.put(target, origin);

                file.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
                if (fr != null)
                    fr.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void addSource(File file, HashMap<String, String> urls)
    {
        RandomAccessFile fr = null;
        String total = new String();
        String url = null;
        String target = null;

        try {
            fr = new RandomAccessFile(file, "rw");

            String sCurrentLine;
            int warcFlag = 0;
            boolean preHeader = true;

            //Going through each line of the WARC file
            while((sCurrentLine = fr.readLine()) != null) {
                int lineLength = sCurrentLine.length();
                //Discard unwanted warcinfo
                if (sCurrentLine.startsWith("WARC/1.0")) {
                    warcFlag = warcFlag + 1;
                    if (warcFlag >= 3)
                        break;
                }

                if(warcFlag == 1)
                    total = total + sCurrentLine + ("\n");
                //Process response / resource
                else if (warcFlag == 2) {
                    //Add properties and HTML
                    if (sCurrentLine.startsWith("WARC-Target-URI: ")) {
                        url = sCurrentLine.substring(17, lineLength);
                        total = total + sCurrentLine + "\n";
                        for(String key : urls.keySet())
                        {
                            String tempValue = urls.get(key);
                            if(tempValue.equals(url))
                            {
                                //Write Redirected from: on file
                                total = total + "Redirected from: " +key +"\n";
                                //Erase key
                                urls.remove(key);
                                break;
                            }
                        }
                    }
                    else
                        total = total + sCurrentLine + "\n";
                }
                else
                    total = total + sCurrentLine;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fr != null) {
                    fr.seek(0);
                    fr.write(total.getBytes());
                    fr.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void debugHashMaps(HashMap<String, HashMap<String, Integer>> attributes) {

        for (String element : attributes.keySet()) {
            System.out.println("\tElement: " + element);
            HashMap<String, Integer> insideMap = attributes.get(element);
            for (String attribute : insideMap.keySet())
                System.out.println("\t\tAttribute: " + attribute + ", occurred " + insideMap.get(attribute) + " times.");
        }

    }

    //Creates an output file with supplied header
    private static CSVPrinter createOutputFile(Object[] header, OutputStreamWriter fw, boolean append) {
        CSVPrinter csvPrinter = null;
        CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter('£').withRecordSeparator("\n");

        try {
            csvPrinter = new CSVPrinter(fw, csvFormat);

            //Skip header if appending to file
            if(!append)
                csvPrinter.printRecord(header);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return csvPrinter;
    }

    //Returns the header for the desired file
    private static Object[] getFileHeader(String file) {
        if (file.equals("results") || file.equals("resultsHTML5"))
            return new Object[]{"WARC", "URL", "Original URL", "Number of HTML Elements", "DOCTYPE", "Number of Hyperlinks", "Number of internal links", "Number of bookmarks",
                    "Number of external links", "Number of images", "Number of comments", "File Size", "Text Size", "Size of Elements", "Size of comments", "Text to elements ratio",
                    "Elements to size ratio", "Text to size ratio", "Comments to size ratio", "Number of Errors", "Number of Warnings", "Type", "HTTP Code",
                    "Content Type", "Server", "Charset"};
        else if (file.equals("errors") || file.equals("errorsHTML5"))
            return new Object[]{"WARC", "Error", "Number of occurrences"};
        else if (file.equals("warnings") || file.equals("warningsHTML5"))
            return new Object[]{"WARC", "Warning", "Number of occurrences"};
        else if (file.equals("elements") || file.equals("elementsHTML5"))
            return new Object[]{"WARC", "Element", "Number of occurrences"};
        else if (file.equals("protocols") || file.equals("protocolsHTML5"))
            return new Object[]{"WARC", "Protocol", "Number of occurrences"};
        else if (file.equals("images") || file.equals("imagesHTML5"))
            return new Object[]{"WARC", "Image Format", "Number of occurrences"};
        else if (file.equals("attributes") || file.equals("attributesHTML5"))
            return new Object[]{"WARC", "Element", "Attribute", "Number of occurrences"};
        else if (file.equals("variables") || file.equals("variablesHTML5"))
            return new Object[]{"WARC", "Message", "Variable", "Number of occurrences"};
        else
            return new Object[]{file, "Number of occurrences"};
    }

    //Fetches all the WARC files from a directory
    private static ArrayList<File> getFileList(String path) {
        //Read all files, add them to the array
        File folder = new File(path);
        System.out.print("-> Info: Fetching WARC file list...");
        File[] listOfFiles = folder.listFiles();
        System.out.println(" Done");

        System.out.print("-> Info: Ordering WARC files alphabetically...");
        ArrayList<File> orderedList = new ArrayList<File>();

        //Transfer files to ArrayList
        assert listOfFiles != null;
        Collections.addAll(orderedList, listOfFiles);

        //Sort them alphabetically
        Collections.sort(orderedList, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        System.out.println(" Done");

        return orderedList;
    }

    //Reads all messages from a file and puts them inside an ArrayList. Used for generic
    //error/warning validation messages and patterns used for their identification
    private static ArrayList<String> readErrorMessages(String filePath) {
        ArrayList<String> errorList = new ArrayList<String>();
        File file = new File(filePath);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String error;
            while ((error = br.readLine()) != null) {
                errorList.add(error.toLowerCase());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return errorList;
    }

    //Add this web page's contents to the totals
    private static void addToTotals(Webpage webpage) {
        totalElementOccurrences = Utils.hashAddHashContents(totalElementOccurrences, webpage.getElements());
        totalLinkProtocols = Utils.hashAddHashContents(totalLinkProtocols, webpage.getLinkProtocols());
        totalImageFormats = Utils.hashAddHashContents(totalImageFormats, webpage.getImageFormats());
        totalErrorOccurrences = Utils.hashAddHashContents(totalErrorOccurrences, webpage.getErrors());
        totalWarningOccurrences = Utils.hashAddHashContents(totalWarningOccurrences, webpage.getWarnings());
        totalElementAttributes = Utils.bigHashAddHashContents(totalElementAttributes, webpage.getElementAttributes());
        totalErrorVariables = Utils.bigHashAddHashContents(totalErrorVariables, webpage.getErrorVariables());
        totalWarningVariables = Utils.bigHashAddHashContents(totalWarningVariables, webpage.getWarningVariables());

        //If an HTML5 document, add to its specific totals as well
        String doctype = webpage.getDoctype().toLowerCase();
        if (doctype.equals("<!doctype html>")) {
            totalElementOccurrencesHTML5 = Utils.hashAddHashContents(totalElementOccurrencesHTML5, webpage.getElements());
            totalLinkProtocolsHTML5 = Utils.hashAddHashContents(totalLinkProtocolsHTML5, webpage.getLinkProtocols());
            totalImageFormatsHTML5 = Utils.hashAddHashContents(totalImageFormatsHTML5, webpage.getImageFormats());
            totalErrorOccurrencesHTML5 = Utils.hashAddHashContents(totalErrorOccurrencesHTML5, webpage.getErrors());
            totalWarningOccurrencesHTML5 = Utils.hashAddHashContents(totalWarningOccurrencesHTML5, webpage.getWarnings());
            totalElementAttributesHTML5 = Utils.bigHashAddHashContents(totalElementAttributesHTML5, webpage.getElementAttributes());
            totalErrorVariablesHTML5 = Utils.bigHashAddHashContents(totalErrorVariablesHTML5, webpage.getErrorVariables());
            totalWarningVariablesHTML5 = Utils.bigHashAddHashContents(totalWarningVariablesHTML5, webpage.getWarningVariables());
        }
    }

    //Output files for total error, warning, elements, protocols and image formats
    private static void outputTotals() {
        //Elements
        System.out.print("-> Outputting total elements...");
        outputTotal("elements", totalElementOccurrences);
        System.out.println("\tDone!");
        System.out.print("-> Outputting total HTML5 elements...");
        outputTotal("elementsHTML5", totalElementOccurrencesHTML5);
        System.out.println("\tDone!");
        //Errors
        System.out.print("-> Outputting total errors...");
        outputTotal("errors", totalErrorOccurrences);
        System.out.println("\tDone!");
        System.out.print("-> Outputting total HTML5 errors...");
        outputTotal("errorsHTML5", totalErrorOccurrencesHTML5);
        System.out.println("\tDone!");
        //Warnings
        System.out.print("-> Outputting total warnings...");
        outputTotal("warnings", totalWarningOccurrences);
        System.out.println("\tDone!");
        System.out.print("-> Outputting total HTML5 warnings...");
        outputTotal("warningsHTML5", totalWarningOccurrencesHTML5);
        System.out.println("\tDone!");
        //Images
        System.out.print("-> Outputting total image formats...");
        outputTotal("images", totalImageFormats);
        System.out.println("\tDone!");
        System.out.print("-> Outputting total HTML5 image formats...");
        outputTotal("imagesHTML5", totalImageFormatsHTML5);
        System.out.println("\tDone!");
        //Protocols
        System.out.print("-> Outputting total link protocols...");
        outputTotal("protocols", totalLinkProtocols);
        System.out.println("\tDone!");
        System.out.print("-> Outputting total HTML5 link protocols...");
        outputTotal("protocolsHTML5", totalLinkProtocolsHTML5);
        System.out.println("\tDone!");
        //Attributes
        System.out.print("-> Outputting total attributes...");
        outputBigTotal("attributes", "attributes", totalElementAttributes);
        System.out.println("\tDone!");
        System.out.print("-> Outputting total HTML5 attributes...");
        outputBigTotal("attributesHTML5", "attributesHTML5", totalElementAttributesHTML5);
        System.out.println("\tDone!");
        //Error variables
        System.out.print("-> Outputting total error variables...");
        outputBigTotal("errorvariables", "errorVariables", totalErrorVariables);
        System.out.println("\tDone!");
        System.out.print("-> Outputting total HTML5 error variables...");
        outputBigTotal("errorvariablesHTML5", "errorVariablesHTML5", totalErrorVariablesHTML5);
        System.out.println("\tDone!");
        //Warning variables
        System.out.print("-> Outputting total warning variables...");
        outputBigTotal("warningvariables", "warningVariables", totalWarningVariables);
        System.out.println("\tDone!");
        System.out.print("-> Outputting total HTML5 warning variables...");
        outputBigTotal("warningvariablesHTML5", "warningVariablesHTML5", totalWarningVariablesHTML5);
        System.out.println("\tDone!");
    }

    //Writes the contents of the hash map to an output file
    private static void outputHashMap(String name, HashMap<String, Integer> contents) {
        OutputStreamWriter fr = Utils.getFileWriter("resources/results/" + name + ".csv", false);
        CSVPrinter output = createOutputFile(getFileHeader(name), fr, false);

        //Output every key, value pair
        for (String entry : contents.keySet()) {
            ArrayList<String> temp = new ArrayList<String>();
            temp.add(entry);
            temp.add("" + contents.get(entry));
            try {
                output.printRecord(temp);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("-> ERROR: Couldnt write to file " + name);
            }
        }
        //Close file
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Writes the content of a hash map with another hash map as a value into an output file
    private static void outputBigHashMap(HashMap<String, HashMap<String, Integer>> bigMap, String fileName, String folderName) {
        int counter = 0;

        for (String key : bigMap.keySet()) {
            //File name is a concatenation of supplied string and counter
            OutputStreamWriter fr = Utils.getFileWriter("resources/results/" + folderName + "/" + (fileName +"-" +counter) + ".csv", false);
            String[] header = {key, "Number of occurrences"};
            CSVPrinter output = createOutputFile(header, fr, false);

            HashMap<String, Integer> attributes = bigMap.get(key);
            //Output every key, value pair
            for (String entry : attributes.keySet()) {
                ArrayList<String> temp = new ArrayList<String>();
                temp.add(entry);
                temp.add("" + attributes.get(entry));
                try {
                    output.printRecord(temp);
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("-> ERROR: Couldnt write to file " + fileName);
                }
            }
            //Close file
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Increase the file counter
            counter = counter + 1;
        }

    }

    //Outputs file for runtime statistics
    private static void outputFinalFile(long start) {
        File file = new File("resources/results/stats.txt");
        try {
            FileWriter fr = new FileWriter(file);

            //Print totals
            System.out.println("\tThere was a total of " + totalErrorOccurrences.keySet().size() + " different errors.");
            fr.write("\tThere was a total of " + totalErrorOccurrences.keySet().size() + " different errors.\n");
            fr.flush();

            System.out.println("\tThere was a total of " + totalWarningOccurrences.keySet().size() + " different warnings.");
            fr.write("\tThere was a total of " + totalWarningOccurrences.keySet().size() + " different warnings.\n");
            fr.flush();

            System.out.println("\tThere was a total of " + totalElementOccurrences.keySet().size() + " different elements.");
            fr.write("\tThere was a total of " + totalElementOccurrences.keySet().size() + " different elements.\n");
            fr.flush();

            System.out.println("\tA total of " + failedNoBytes + " pages were excluded for having 0 bytes.");
            fr.write("\tA total of " + failedNoBytes + " pages were excluded for having 0 bytes.\n");
            fr.flush();

            System.out.println("\tA total of " + failedValidation + " pages were excluded for failing to be validated.");
            fr.write("\tA total of " + failedValidation + " pages were excluded for failing to be validated.\n");
            fr.flush();

            System.out.println("\tA total of " + failedParsing + " pages were excluded for failing to be parsed.");
            fr.write("\tA total of " + failedParsing + " pages were excluded for failing to be parsed.\n");
            fr.flush();

            System.out.println("\tA total of " + successful + " pages out of " + numWARCs + " were successfully analyzed.");
            fr.write("\tA total of " + successful + " pages out of " + numWARCs + " were successfully analyzed.\n");
            fr.flush();

            System.out.println("\tA total of " + numForced + " pages required forced settings to pass validation.");
            fr.write("\tA total of " + numForced + " pages required forced settings to pass validation.\n");
            fr.flush();


            //Time spent processing data
            long end = System.currentTimeMillis();
            fr.write("-> Start: " + start + ", End: " + end + ", Took: " +(end - start) +" ms, " + ((end - start) / 1000) + " seconds.\n");
            fr.flush();
            fr.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Converts the contents of a Hash Map into an ArrayList
    private static ArrayList<String> hashToArray(String warcName, HashMap<String, Integer> hash) {
        ArrayList<String> list = new ArrayList<String>();

        for (String key : hash.keySet()) {

        }

        return list;
    }

    //Outputs the totals for a single type of element
    private static void outputTotal(String filename, HashMap<String, Integer> list) {
        File file = new File("resources/results/" + filename + ".csv");
        BufferedReader br;
        String sCurrentLine;
        boolean isFirstLine = true;

        try {
            br = new BufferedReader(new FileReader(file));

            while ((sCurrentLine = br.readLine()) != null) {
                //Skip first line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                //Splits line on £
                String[] tokens = sCurrentLine.split("£");

                //Start at 1 so we skip the WARC name
                for (int i = 1; i < tokens.length; i++) {
                    int index = tokens[i].lastIndexOf("x");
                    if(index == -1)
                        continue;
                    String element = tokens[i].substring(0, index);
                    String occurrences = tokens[i].substring(index + 1);

                    //Element is already catalogued, sum its current value to this one
                    if (Utils.hashHasKey(list, element)) {
                        int currentValue = list.get(element);
                        try
                        {
                            currentValue = currentValue + Integer.parseInt(occurrences.replace("\"",""));
                        } catch (NumberFormatException ne)
                        {
                            outputDebugInfo(filename, tokens, element, occurrences);
                        }
                        list.put(element, currentValue);
                    }
                    //Element is not catalogued yet, add it
                    else {
                        try {
                            list.put(element, Integer.parseInt(occurrences.replace("\"", "")));
                        } catch (NumberFormatException ne)
                        {
                            outputDebugInfo(filename, tokens, element, occurrences);
                        }
                    }
                }
            }

            outputHashMap("totals/" + filename, list);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Outputs DEBUG information whenever an Integer.parseInt fails
    private static void outputDebugInfo(String filename, String[] tokens, String element, String occurrences) {
        File file = new File("resources/results/debug.txt");

        try {
            FileWriter fr = new FileWriter(file);

            System.out.println("-> Error parsing Integer for " +filename);
            fr.write("-> Error parsing Integer for " +filename);
            fr.flush();
            System.out.println("-> WARC file = " +tokens[0]);
            fr.write("-> WARC file = " +tokens[0]);
            fr.flush();
            System.out.println("-> Element: " +element);
            fr.write("-> Element: " +element);
            fr.flush();
            System.out.println("-> Occurrences: " +occurrences);
            fr.write("-> Occurrences: " +occurrences);
            fr.flush();
            fr.write("-> Token list has " +tokens.length +" items\n-------------------");
            fr.flush();

            for(int i = 1; i < tokens.length; i++) {
                fr.write(tokens[i]);
                fr.flush();

            }
            fr.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Outputs the totals for multiple elements
    private static void outputBigTotal(String filename, String foldername, HashMap<String, HashMap<String, Integer>> list) {
        File file = new File("resources/results/" + filename + ".csv");
        BufferedReader br;
        String sCurrentLine;
        boolean isFirstLine = true;

        try {
            br = new BufferedReader(new FileReader(file));

            //Read file line by line
            while ((sCurrentLine = br.readLine()) != null) {
                //Skip first line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                //Splits line on £
                String[] tokens = sCurrentLine.split("£");

                //[0] = WARC name, [1] = Element name, [2..N] = attributex#
                //Start at 1 so we skip the WARC name
                //Element is already catalogued, sum current values to the ones being read
                if (Utils.bigHashHasKey(list, tokens[1])) {
                    String name;
                    int index, value = 0;
                    //Get current information for this element
                    HashMap<String, Integer> smallList = list.get(tokens[1]);

                    //Go through each attribute
                    for (int attribute = 2; attribute < tokens.length; attribute++) {
                        //Extract read attribute and occurrences
                        index = tokens[attribute].lastIndexOf("x");
                        if(index == -1)
                            continue;
                        name = tokens[attribute].substring(0, index);
                        try {
                            value = Integer.parseInt(tokens[attribute].substring(index + 1).replace("\"", ""));
                        } catch (NumberFormatException ne)
                        {
                            outputDebugInfo(filename, tokens, "big", "big");
                        }

                        //Attribute is already catalogued, sum values
                        if (Utils.hashHasKey(smallList, name)) {
                            int currentValue = smallList.get(name);
                            smallList.put(name, currentValue + value);
                        }
                        //Attribute is new, add it
                        else {
                            smallList.put(name, value);
                        }
                    }
                    //Add to big list
                    list.put(tokens[1], smallList);
                }
                //Element is new
                else {
                    HashMap<String, Integer> newElement = new HashMap<String, Integer>();
                    String name;
                    int index, value = 0;

                    //Go through each attribute and add it
                    for (int attribute = 2; attribute < tokens.length; attribute++) {
                        index = tokens[attribute].lastIndexOf("x");
                        name = tokens[attribute].substring(0, index);
                        try {
                            value = Integer.parseInt(tokens[attribute].substring(index + 1).replace("\"", ""));
                        } catch (NumberFormatException ne)
                        {
                            outputDebugInfo(filename, tokens, "big", "big");
                        }
                        newElement.put(name, value);
                    }
                    //Add to big list
                    list.put(tokens[1], newElement);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        outputBigHashMap(list, filename, foldername);
    }
}