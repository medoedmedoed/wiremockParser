package org.example;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseFile {
    static final Pattern patternMethod = Pattern.compile(".*method\": \"(.*?)\".*");
    static final Pattern patternUrl = Pattern.compile(".*urlPattern\": \"(.*?)\".*");
    static final Pattern patternPort = Pattern.compile("port ([0-9]+)");

    public static ForInflux parseFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        ForInflux forInflux = new ForInflux(file.getPath());
        while (reader.ready()){

            String str = reader.readLine();

            if(Pattern.matches(patternMethod.toString(), str)){

            Matcher matcher = patternMethod.matcher(str);
                if(matcher.find()) {
                    forInflux.setMethod(matcher.group(1));
                }

            } else if (Pattern.matches(patternUrl.toString(), str)) {

                Matcher matcher = patternUrl.matcher(str);
                if(matcher.find()) {
                    forInflux.setUrlPattern(matcher.group(1));
                }
            }
        }
        reader.close();
        return forInflux;
    }

    public static int parseBatFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file.getPath()+"/start.bat"));
        int port = 0;
        while(reader.ready()) {
            String str = reader.readLine();
            Matcher matcher = patternPort.matcher(str);
            if (matcher.find()) {
                port = Integer.parseInt(matcher.group(1));
            }
        }
        return port;
    }
}
