package Utils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;

//Set of useful tools that make life easier
public abstract class Utils
{
    //Returns a FileWriter for a supplied File path, used to avoid try catching on main code
    public static OutputStreamWriter getFileWriter(String filePath, boolean append)
    {
        OutputStreamWriter fr = null;
        //FileWriter fr = null;
        try {
            if(append)
                fr  = new OutputStreamWriter(new FileOutputStream(filePath, true), Charset.forName("UTF-8").newEncoder());
                //fr = new FileWriter(new File(filePath), append);
            else
                fr  = new OutputStreamWriter(new FileOutputStream(filePath), Charset.forName("UTF-8").newEncoder());
                //fr = new FileWriter(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("-> Error trying to create file '" +filePath +"'");
        }
        return fr;
    }

    //Returns true if the supplied hash map contains the key, false if it doesn't
    public static boolean hashHasKey(HashMap<String, Integer> map, String key)
    {
        return map.containsKey(key);
    }

    //Returns true if the supplied hash map contains the key, false if it doesn't, used for hash maps with other hash maps as values
    public static boolean bigHashHasKey(HashMap<String, HashMap<String,Integer>> map, String key)
    {
        return map.containsKey(key);
    }

    //Increases a hash map key's value by a specified amount
    public static HashMap<String, Integer> hashIncreaseValue(HashMap<String, Integer> map, String key, int value)
    {
        int oldValue = map.get(key);
        map.put(key, oldValue + value);
        return map;
    }

    //Copies the contents of source to target. If new, adds them, if already present, sums their totals
    public static HashMap<String, Integer> hashAddHashContents(HashMap<String, Integer> target, HashMap<String, Integer> source)
    {
        for(String key : source.keySet())
        {
            int sourceValue = source.get(key);
            if(hashHasKey(target, key))
            {
                int targetValue = target.get(key);
                target.put(key, targetValue + sourceValue);
            }
            else
                target.put(key, sourceValue);
        }
        return target;
    }

    //Copies the contents of source to target. If new, adds them, if already present, sums their totals. Used for hash maps with other hash maps as values
    public static HashMap<String, HashMap<String, Integer>> bigHashAddHashContents(HashMap<String, HashMap<String, Integer>> target, HashMap<String, HashMap<String, Integer>> source)
    {
        //Go through each of source's keys
        for(String element : source.keySet())
        {
            //Get its list of attributes
            HashMap<String, Integer> insideMap = source.get(element);

            //Target already has this element catalogued, sum its attributes
            if(bigHashHasKey(target, element))
            {
                HashMap<String, Integer> currentValues = target.get(element);
                currentValues = hashAddHashContents(currentValues, insideMap);
                target.put(element, currentValues);
            }
            //Element is not catalogued, add it and its attributes
            else
                target.put(element, insideMap);
        }

        return target;
    }

    //Returns the size of a file
    public static float getFileSize(String filepath) {
        File file = new File(filepath);

        return file.length();
    }

    //Attempt to remove characters that might cause an undesired line break
    public static String removeLineBreaks(String string)
    {
        int max = string.length();
        char character;
        String result = "";

        for(int i=0; i< max; i++)
        {
            character = string.charAt(i);

            if(character != '\r' && character != '\n' && character != '\u0085' && character != '\u0228' && character != '\u2029' && character != '\0')
                result = result + character;
        }

        return result.replace("\n","").replace("\r","");
    }
}
