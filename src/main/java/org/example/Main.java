package org.example;


import org.apache.tika.metadata.Metadata;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;

import java.io.*;

public class Main {
    public static void main(String[] args) throws Exception {
        File directory = new File("src/main/resources");
        File[] fileList = directory.listFiles();

        assert fileList != null;
        for (File file : fileList) {
            parseFile(file);
            System.out.println("\n");

        }
    }

    private static String identifyLanguage(String text) {
        LanguageDetector identifier = new OptimaizeLangDetector().loadModels();
        LanguageResult language = identifier.detect(text);
        return language.getLanguage();
    }

    public static void parseFile(File f) throws Exception {
        String fileName = f.getName();
        String fileType;
        String fileEncoding = null;
        String fileLanguage;

        AutoDetectParser parser = new AutoDetectParser();
        LinkContentHandler linkHandler = new LinkContentHandler();
        BodyContentHandler textHandler = new BodyContentHandler(-1);
        TeeContentHandler teeHandler = new TeeContentHandler(linkHandler,
                textHandler);
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();

        //Esto lo he hecho porque necesito que el FileInputStream tenga soporte
        // de mark/reset ya que si el InputStream que le paso al charsetDetector
        // no tiene este soporte, da error.
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        parser.parse(is, teeHandler, metadata, parseContext);
        //System.out.println("Links:\n" + linkHandler.getLinks());

        String[] fileTypeAndCharset = metadata.get("Content-Type").split(";");
        fileType = fileTypeAndCharset[0];

        //Haciendo esto me ahorro tener que usar otro get de metadata y también
        // el crear el CharsetDetector, con todo lo que esto conlleva
        int i = 0;
        for (String ignored : fileTypeAndCharset) {
            i++;
        }

        if (i == 2) {
            fileEncoding = fileTypeAndCharset[1];
            fileEncoding = fileEncoding.split("=")[1];
        }


        if (fileEncoding == null) {
            fileEncoding = metadata.get("Content-Encoding");
            if (fileEncoding == null) {
                CharsetDetector charsetDetector = new CharsetDetector();
                charsetDetector.setText(is);
                CharsetMatch charsetMatch = charsetDetector.detect();
                fileEncoding = charsetMatch.getName();

            }
        }

        fileLanguage = metadata.get("dc.language");

        if (fileLanguage == null) {
            fileLanguage = metadata.get("Content-Language");
            if (fileLanguage == null) {
                fileLanguage = identifyLanguage(textHandler.toString());
            }
        }

        System.out.println("File name: " + fileName);
        System.out.println("File type: " + fileType);
        System.out.println("File language: " + fileLanguage);
        System.out.println("File encoding: " + fileEncoding);

    }

}