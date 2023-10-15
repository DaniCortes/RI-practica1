package org.example;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.Link;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

public class Document {

  private final File file;
  private String type;
  private String language;
  private String encoding;
  private final Map<String, Integer> occurrences;
  private List<Map.Entry<String, Integer>> sortedOccurrences;
  private final ArrayList<Link> links;
  private final BodyContentHandler textHandler;

  private final LinkContentHandler linkHandler;
  private final Metadata metadata;

  public Document(File file) throws TikaException, IOException, SAXException {
    this.file = file;

    AutoDetectParser parser = new AutoDetectParser();
    occurrences = new HashMap<>();
    linkHandler = new LinkContentHandler();
    textHandler = new BodyContentHandler(-1);
    TeeContentHandler teeHandler = new TeeContentHandler(linkHandler, textHandler);
    metadata = new Metadata();
    ParseContext parseContext = new ParseContext();
    links = new ArrayList<>(linkHandler.getLinks());

    //Esto lo he hecho porque necesito que el FileInputStream tenga soporte
    // de mark/reset ya que si el InputStream que le paso al charsetDetector
    // no tiene este soporte, da error.
    InputStream is = new BufferedInputStream(new FileInputStream(file));
    parser.parse(is, teeHandler, metadata, parseContext);
    setTypeAndCharset(is);
    retrieveLanguage();
    retrieveOccurrences();
    writeOccurrencesInFile();
    writeRanksInFile();
    writeLinksInFile();
  }

  private void setTypeAndCharset(InputStream is) throws IOException {
    String[] typeAndCharset = metadata.get("Content-Type").split(";");

    type = typeAndCharset[0];

    //Haciendo esto me ahorro tener que usar otro get de metadata y también
    // el crear el CharsetDetector, con lo que esto conlleva

    if (typeAndCharset.length == 2) {
      encoding = typeAndCharset[1];
      encoding = encoding.split("=")[1];
    }

    if (encoding == null) {
      encoding = metadata.get("Content-Encoding");
      if (encoding == null) {
        CharsetDetector charsetDetector = new CharsetDetector();
        charsetDetector.setText(is);
        CharsetMatch charsetMatch = charsetDetector.detect();
        encoding = charsetMatch.getName();
      }
    }
  }

  private String identifyLanguage() {
    LanguageDetector identifier = new OptimaizeLangDetector().loadModels();
    LanguageResult language = identifier.detect(textHandler.toString());
    return language.getLanguage();
  }

  private void retrieveOccurrences() {
    String[] everyWord = textHandler.toString().split("\\s+");
    for (String word : everyWord) {
      word = trim(word);
      if (word.trim().isEmpty()) {
        continue;
      }
      if (!occurrences.containsKey(word)) {
        occurrences.put(word, 1);
        continue;
      }
      occurrences.replace(word, occurrences.get(word) + 1);
    }
  }

  private String trim(String word) {
    String regex = ", | ; | : | \\. | \" | ' | < | > | \\( | \\) | \\[ | ] | ¿ | \\? | \\* |"
        + " \\^ | \\+ | = | \\$ | % | € | / | \\\\ | - | \\{ | } | ! | ¡ | \\| | ~ | @ | ; |"
        + " « | » | — | “ | ” | • | ’ | ‘ | ™";
    regex = regex.replaceAll("\\s", "");

    word = word.replaceAll(regex, "").trim().toLowerCase();
    return word;
  }

  private void retrieveLanguage() {
    language = metadata.get("dc.language");

    if (language == null) {
      language = metadata.get("Content-Language");
      if (language == null) {
        language = identifyLanguage();
      }
    }
  }

  private String getFilenameWithoutExtension() {
    return FilenameUtils.removeExtension(file.getName());
  }

  private String createFile(String folder) throws IOException {
    String filename = getFilenameWithoutExtension() + "-" + folder;
    String filePath = "src/main/resources/" + folder + "/" + filename + ".dat";
    File file = new File(filePath);

    if (file.createNewFile()) {
      System.out.println("File " + filename + " created.");
    } else {
      System.out.println("File " + filename + " already exists. "
          + "Proceeding to delete its contents.");
      new FileWriter(filePath, false).close();
      System.out.println("File contents deleted.");
    }

    return filePath;
  }

  private void writeOccurrencesInFile() throws IOException {
    sortedOccurrences = new ArrayList<>(occurrences.entrySet());
    sortedOccurrences.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    String filePath = createFile("occurrences");
    int limit = 20;

    try (FileWriter writer = new FileWriter(filePath)) {
      for (Map.Entry<String, Integer> occurrence : sortedOccurrences) {
        if (getName().equals("the-snow-queen.txt")) {
          limit = 5;
        } else if (getName().equals("war-of-the-worlds.htm")) {
          limit = 15;
        }
        if (occurrence.getValue() <= limit) {
          break;

        }
        writer.write(occurrence.getKey() + " " + occurrence.getValue()
            + String.format("%n"));
      }
      System.out.println("Occurrences saved." + String.format("%n"));
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  private void writeRanksInFile() throws IOException {
    String filePath = createFile("ranks");

    try (FileWriter writer = new FileWriter(filePath)) {
      int i = 0;
      for (Map.Entry<String, Integer> occurrence : sortedOccurrences) {
        if (occurrence.getValue() <= 5) {
          break;
        }
        writer.write(i++ + " " + occurrence.getValue()
            + String.format("%n"));
      }
      System.out.println("Ranks saved." + String.format("%n"));
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  private void writeLinksInFile() throws IOException {
    String filePath = createFile("links");

    try (FileWriter writer = new FileWriter(filePath)) {
      for (Link link : linkHandler.getLinks()) {
        if (link.getText().isEmpty()) {
          continue;
        }
        writer.write(link.getText() + String.format("%n"));
      }
      System.out.println("Links saved." + String.format("%n"));
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  public String getName() {
    return file.getName();
  }

  public String getType() {
    return type;
  }

  public String getLanguage() {
    return language;
  }

  public String getEncoding() {
    return encoding;
  }

  public String toString() {
    return "File name: " + getName()
        + "\nFile type: " + getType()
        + "\nFile language: " + getLanguage()
        + "\nFile encoding: " + getEncoding()
        + "\n";
  }

  public ArrayList<Link> getLinks() {
    return links;
  }

}
